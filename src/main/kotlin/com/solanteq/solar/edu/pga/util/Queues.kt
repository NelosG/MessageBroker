package com.solanteq.solar.edu.pga.util

import com.solanteq.solar.edu.pga.queue.MSQueue
import com.solanteq.solar.edu.pga.queue.Queue
import java.util.concurrent.atomic.AtomicReference

/**
 * @author gpushkarev
 * @since 4.0.0
 */
class Queues<V>(state: State) {

    val listens: Queue<Listen<V>> = MSQueue()
    val sends: Queue<Send<V>> = MSQueue()

    val state: AtomicReference<State> = AtomicReference(state)

    fun isEmpty(): Boolean {
        return listens.isEmpty() && sends.isEmpty()
    }

    private fun <T : SendListen<V>> getLastNotCancelled(queue: Queue<T>): T? {
        var res: T? = null
        while (!queue.isEmpty()) {
            res = queue.peek()
            if (res?.future?.isCancelled == true) {
                queue.poll()
                continue
            }
            break
        }
        return res
    }

    fun getForProcess(): Pair<Listen<V>, Send<V>>? {
        val listen = getLastNotCancelled(listens) ?: return null
        val send = getLastNotCancelled(sends) ?: return null
        listens.poll()
        sends.poll()
        return Pair(listen, send)
    }
}
