package com.solanteq.solar.edu.pga

import com.solanteq.solar.edu.pga.util.Listen
import com.solanteq.solar.edu.pga.util.Queues
import com.solanteq.solar.edu.pga.util.Send
import com.solanteq.solar.edu.pga.util.State
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * @author gpushkarev
 * @since 1.0.0
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
        var queues = mapQueues.getOrPut(key) { Queues(State.ACTIVE) }

        while (!queues.state.compareAndSet(State.ACTIVE, State.LOCKED)) {
            queues = mapQueues.getOrPut(key) { Queues(State.ACTIVE) }
        }
        addFunc(queues)
        if (!queues.state.compareAndSet(State.LOCKED, State.ACTIVE)) {
            throw IllegalStateException("Someone unlock locked value") // Не должно кидаться
        }

        executors.submit {
            processKey(key)
        }
    }

    private fun processKey(key: K) {
        try {
            var queues = mapQueues.getValue(key)

            try {
                while (!queues.state.compareAndSet(State.ACTIVE, State.LOCKED)) {
                    if (queues.state.compareAndSet(State.REMOVED, State.REMOVED)) {
                        return
                    }
                    queues = mapQueues.getValue(key)
                }


                val forProcess = queues.getForProcess() ?: return
                val listen = forProcess.first
                val send = forProcess.second

                match(listen, send)

                if (queues.isEmpty()) {
                    if (queues.state.compareAndSet(State.LOCKED, State.PREPARED_FOR_REMOVE)) {
                        if (queues.isEmpty()) {
                            mapQueues.remove(key, queues)
                            queues.state.compareAndSet(State.PREPARED_FOR_REMOVE, State.REMOVED)
                        } else {
                            queues.state.compareAndSet(State.PREPARED_FOR_REMOVE, State.LOCKED)
                        }
                    }
                }

            } finally {
                queues.state.compareAndSet(State.LOCKED, State.ACTIVE)
            }


        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun match(listen: Listen<V>, send: Send<V>) {
        listen.future.complete(send.value)
        send.future.complete(listen.responder(send.value))
    }
}
