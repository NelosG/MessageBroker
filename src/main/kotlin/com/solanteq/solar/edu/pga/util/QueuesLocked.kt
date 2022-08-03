package com.solanteq.solar.edu.pga.util

import java.util.concurrent.locks.ReentrantLock

/**
 * @author gpushkarev
 * @since 1.0.0
 */
class QueuesLocked<V : Any> : Queues<V>() {
    val lock = ReentrantLock()
}
