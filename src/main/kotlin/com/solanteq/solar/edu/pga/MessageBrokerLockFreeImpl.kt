package com.solanteq.solar.edu.pga

import com.solanteq.solar.edu.pga.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * @author gpushkarev
 * @since 5.0.0
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

            mapQueues.compute(key) {_, v ->
                val queues = v ?: Queues()
                addFunc(queues)
                queues
            }

        executors.submit {
            processKey(key)
        }
    }

    private fun processKey(key: K) {
        try {
            val queues = mapQueues.getValue(key)

            while (!Thread.currentThread().isInterrupted) {
                if (queues.lock.tryLock()) {
                    try {
                        while (!Thread.currentThread().isInterrupted) {
                            val forProcess = queues.getForProcess() ?: break
                            val listen = forProcess.first
                            val send = forProcess.second

                            match(listen, send)
                        }
                        mapQueues.remove(key, queues)
                    } finally {
                        queues.lock.unlock()
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
