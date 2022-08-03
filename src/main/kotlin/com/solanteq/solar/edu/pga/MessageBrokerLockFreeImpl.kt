package com.solanteq.solar.edu.pga

import com.solanteq.solar.edu.pga.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * @author gpushkarev
 * @since 4.0.0
 */
class MessageBrokerLockFreeImpl<K : Any, V : Any> : MessageBroker<K, V> {

    private val executors: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val mapQueues: ConcurrentHashMap<K, Queues<V>> = ConcurrentHashMap()

    override fun listenAndReply(key: K, responder: (V) -> V): CompletableFuture<V> {
        val future = CompletableFuture<V>()

        synchroProcessing(key) { queues ->
            queues.listens.add(Listen(future, responder))
        }
        return future
    }

    override fun sendAndReceive(key: K, value: V): CompletableFuture<V> {
        val future = CompletableFuture<V>()

        synchroProcessing(key) { queues ->
            queues.sends.add(Send(future, value))
        }
        return future
    }

    private fun synchroProcessing(key: K, addFunc: (Queues<V>) -> Unit) {
        var queues: Queues<V>

        /* Не совсем уверен не считает ли это блокирующей операцией(
        *   Если queues кем то удалена то новая будет пустой и ее никто не будет пытаться удалить,
        *   пока не сматчит хотя бы 1 пару
        *
        *   расписывать много но ждать будем не долго
         */
        while (true) {
            queues = mapQueues.getOrPut(key) { Queues() }
            addFunc(queues)
            if (queues.validState.compareAndSet(ValidState.VALID, ValidState.VALID)) {
                break
            }
        }

        executors.submit {
            processKey(key)
        }
    }

    private fun processKey(key: K) {
        try {
            var queues = mapQueues.getValue(key)

            while (!Thread.currentThread().isInterrupted) {
                // TRY Process, if can't than someone doing this work
                if (queues.lockState.compareAndSet(LockState.FREE, LockState.LOCKED)) {
                    if (!queues.validState.compareAndSet(ValidState.VALID, ValidState.VALID)) {
                        queues = mapQueues.getValue(key)
                        continue
                    }
                    try {
                        while (!Thread.currentThread().isInterrupted) {
                            val forProcess = queues.getForProcess() ?: return
                            val listen = forProcess.first
                            val send = forProcess.second

                            match(listen, send)
                        }

                        if (queues.isEmpty()) {
                            if (queues.validState.compareAndSet(ValidState.VALID, ValidState.REMOVED)) {
                                mapQueues.remove(key, queues)
                            } else {
                                throw IllegalStateException("Someone delete locked queues")
                            }
                        }
                    } finally {
                        queues.lockState.compareAndSet(LockState.LOCKED, LockState.FREE)
                    }
                    return
                }
            }

        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun match(listen: Listen<V>, send: Send<V>) {
        executors.submit {
            listen.future.complete(send.value)
            send.future.complete(listen.responder(send.value))
        }
    }
}
