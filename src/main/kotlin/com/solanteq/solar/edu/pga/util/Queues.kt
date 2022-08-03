package com.solanteq.solar.edu.pga.util

import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock

/**
 * @author gpushkarev
 * @since 2.0.0
 */
open class Queues<V : Any> {

    val listens: Deque<Listen<V>> = ArrayDeque()
    val sends: Deque<Send<V>> = ArrayDeque()

    val lock = ReentrantLock()


    fun isEmpty(): Boolean {
        return listens.isEmpty() && sends.isEmpty()
    }

    fun getForProcess(): Pair<Listen<V>, Send<V>>? {
        var listen: Listen<V>? = null
        while (!listens.isEmpty()) {
            listen = listens.peek()
            if (listen.future.isCancelled) {
                listens.pop()
                continue
            }
            break
        }
        if (listen == null) {
            return null
        }

        var send: Send<V>? = null
        while (!sends.isEmpty()) {
            send = sends.peek()
            if (send.future.isCancelled) {
                sends.pop()
                continue
            }
            break
        }
        if (send == null) {
            return null
        }

        listens.pop()
        sends.pop()
        return Pair(listen, send)

    }
}
