package com.solanteq.solar.edu.pga

import com.solanteq.solar.edu.pga.util.Listen
import com.solanteq.solar.edu.pga.util.Queues
import com.solanteq.solar.edu.pga.util.Send
import com.solanteq.solar.edu.pga.util.State
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * @author gpushkarev
 * @since 4.0.0
 */
class MessageBrokerLockFreeImpl<K : Any, V : Any>(
    threads: Int = Runtime.getRuntime().availableProcessors()
) : MessageBroker<K, V> {

    private val executors: ExecutorService = Executors.newFixedThreadPool(threads)
    private val mapQueues: MutableMap<K, Queues<V>> = mutableMapOf()

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
        val queues = mapQueues.getOrPut(key) { Queues(State.ACTIVE) }

        addFunc(queues)
        executors.submit {
            processKey(key)
        }
    }

    private fun processKey(key: K) {
        try {
            val queues = mapQueues[key] ?: return

            try {
                while (!queues.state.compareAndSet(State.ACTIVE, State.LOCKED)) {
                    if (queues.state.compareAndSet(State.REMOVED, State.REMOVED)) {
                        return
                    }
                }

                val forProcess = queues.getForProcess() ?: return
                val listen = forProcess.first
                val send = forProcess.second

                match(listen, send)
            } finally {
                queues.state.compareAndSet(State.LOCKED, State.ACTIVE)

                if (queues.isEmpty()) {
                    if (queues.state.compareAndSet(State.ACTIVE, State.REMOVED)) {
                        if (queues.isEmpty()) {
                            mapQueues.remove(key, queues)
                        } else {
                            queues.state.set(State.ACTIVE)
                        }
                    }
                }
            }


        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun match(listen: Listen<V>, send: Send<V>) {
        listen.future.obtrudeValue(send.value)
        send.future.obtrudeValue(listen.responder(send.value))
    }
}
