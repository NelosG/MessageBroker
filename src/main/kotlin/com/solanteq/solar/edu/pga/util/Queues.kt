package com.solanteq.solar.edu.pga.util

import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * @author gpushkarev
 * @since 1.0.0
 */
class Queues<V : Any> {
    val listens: Queue<Listen<V>> = LinkedList()
    val sends: Queue<Send<V>> = LinkedList()

    val lock = ReentrantLock()

    fun isEmpty(): Boolean {
        return listens.isEmpty() && sends.isEmpty()
    }

    fun getListen(): Listen<V>? {
        while (!listens.isEmpty()) {
            val listen = listens.poll()
            if (!listen.future.isCancelled) {
                return listen
            }
        }
        return null
    }

    fun getSend(): Send<V>? {
        while (!sends.isEmpty()) {
            val send = sends.poll()
            if (!send.future.isCancelled) {
                return send
            }
        }
        return null
    }
}
