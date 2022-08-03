package com.solanteq.solar.edu.pga.util

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

/**
 * @author gpushkarev
 * @since 4.0.0
 */
open class Queues<V : Any> {

    val listens: ConcurrentLinkedDeque<Listen<V>> = ConcurrentLinkedDeque()
    val sends: ConcurrentLinkedDeque<Send<V>> = ConcurrentLinkedDeque()

    val lockState: AtomicReference<LockState>
    val validState: AtomicReference<ValidState>


    constructor() : this(LockState.FREE, ValidState.VALID)

    constructor(lockState: LockState, validState: ValidState) {
        this.lockState = AtomicReference(lockState)
        this.validState = AtomicReference(validState)
    }


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
