package com.solanteq.solar.edu.pga.util

import com.solanteq.solar.edu.pga.queue.MSQueue
import com.solanteq.solar.edu.pga.queue.Queue
import java.util.concurrent.atomic.AtomicReference

/**
 * @author gpushkarev
 * @since 3.0.0
 */
open class Queues<V : Any> {

    val listens: Queue<Listen<V>> = MSQueue()
    val sends: Queue<Send<V>> = MSQueue()

    val state: AtomicReference<State>


    constructor() : this(State.ACTIVE)

    constructor(state: State) {
        this.state = AtomicReference(state)
    }


    fun isEmpty(): Boolean {
        return listens.isEmpty() && sends.isEmpty()
    }

    fun getForProcess(): Pair<Listen<V>, Send<V>>? {
        var listen: Listen<V>? = null
        while (!listens.isEmpty()) {
            listen = listens.peek()
            if (listen.future.isCancelled) {
                listens.poll()
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
                sends.poll()
                continue
            }
            break
        }
        if (send == null) {
            return null
        }

        listens.poll()
        sends.poll()
        return Pair(listen, send)

    }
}
