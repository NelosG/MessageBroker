package com.solanteq.solar.edu.pga.queue

/**
 * @author gpushkarev
 * @since 4.0.0
 */
interface Queue<V> {
    fun add(x: V)
    fun poll(): V?
    fun peek(): V?
    fun isEmpty(): Boolean
}
