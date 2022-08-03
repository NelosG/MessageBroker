package com.solanteq.solar.edu.pga

import com.solanteq.solar.edu.pga.util.Listen
import com.solanteq.solar.edu.pga.util.Queues
import com.solanteq.solar.edu.pga.util.Send
import com.solanteq.solar.edu.pga.util.Task
import java.util.concurrent.*


/**
 * Ваша реализация упрощенного брокера сообщений:
 * 1. Методы [listenAndReply] и [sendAndReceive] могут вызываться в разном порядке. То есть подписка на запрос
 * может быть как до самого запроса, так и после. В обоих случаях реализация должна успешно работать;
 * 2. Любой [CompletableFuture] могут отменить через [CompletableFuture.cancel]. В этом случае либо подписка,
 * либо сам запрос должны перестать учитываться. Например, сделали подписку, отменили подписку и отправили запрос.
 * В этом случае запрос не должен «сматчиться»;
 * 3. Таймауты внутри не нужны. Отмена может произойти только с вызывающей стороны;
 * 4. Реализация должна быть линеаризуемой;
 * 5. Методы должны сразу возвращать [CompletableFuture] для ожидания выполнения операции.
 * В явном виде в потоке вызывающей стороны не должно происходить выполнение логики.
 *
 * @author gpushkarev
 * @since 1.0.0
 */
class MessageBrokerImpl<K : Any, V : Any> : MessageBroker<K, V> {

    private val executors: ExecutorService
    private val controlThread: Thread
    private val mapQueues: ConcurrentHashMap<K, Queues<V>> = ConcurrentHashMap()
    private val queue: BlockingQueue<Task<K, V>> = LinkedBlockingQueue()


    constructor() : this(Runtime.getRuntime().availableProcessors())

    constructor(count: Int) {
        controlThread = Thread {
            controlThreadFun()
        }
        controlThread.start()
        executors = Executors.newFixedThreadPool(if (count != 1) count - 1 else 1)
    }

    private fun processMap() {
        mapQueues.forEach { entry ->
            val queues = entry.value
            try {
                queues.lock.lock()

                if (mapQueues[entry.key] == queues) {
                    while (true) {
                        val forProcess = queues.getForProcess() ?: break
                        val listen = forProcess.first
                        val send = forProcess.second
                        match(listen, send)
                    }
                    if (queues.isEmpty()) {
                        mapQueues.remove(entry.key, queues)
                    }
                }
            } finally {
                queues.lock.unlock()
            }
        }
    }

    private fun controlThreadFun() {
        try {
            while (!Thread.currentThread().isInterrupted) {
                processTask(queue.take())
            }
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun processTask(task: Task<K, V>) {
        val key = task.key

        while (true) {
            val queues = mapQueues.getOrPut(key) { Queues() }
            try {
                // Check that queues hasn't been deleted
                queues.lock.lock()

                if (mapQueues[key] == queues) {
                    when (task.taskType) {
                        Task.TaskType.LISTEN -> {
                            val listen = task.listen ?: throw IllegalStateException("Listen missing")
                            queues.listens.add(listen)
                        }

                        Task.TaskType.SEND -> {
                            val send = task.send ?: throw IllegalStateException("Send missing")
                            queues.sends.add(send)
                        }
                    }
                    executors.submit(this::processMap)
                    return
                }
            } finally {
                queues.lock.unlock()
            }
        }
    }


    override fun listenAndReply(key: K, responder: (V) -> V): CompletableFuture<V> {
        val future = CompletableFuture<V>()
        queue.put(Task(key, Listen(future, responder)))
        return future
    }

    override fun sendAndReceive(key: K, value: V): CompletableFuture<V> {
        val future = CompletableFuture<V>()
        queue.put(Task(key, Send(future, value)))
        return future
    }

    private fun match(listen: Listen<V>, send: Send<V>) {
        listen.future.complete(send.value)
        send.future.complete(listen.responder(send.value))
    }
}
