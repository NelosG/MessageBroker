package com.solanteq.solar.edu.pga.queue

import java.util.concurrent.atomic.AtomicReference

/**
 * @author gpushkarev
 * @since 4.0.0
 */
class MSQueue<V> : Queue<V> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(null)
        head = AtomicReference<Node>(dummy)
        tail = AtomicReference<Node>(dummy)
    }

    override fun isEmpty(): Boolean {
        return head === tail
    }

    override fun add(x: V) {
        val node = Node(x)
        var tail: Node
        while (true) {
            tail = this.tail.get()
            if (tail.next.get() == null) {
                if (tail.next.compareAndSet(null, node)) {
                    break
                }
            } else {
                this.tail.compareAndSet(tail, tail.next.get())
            }
        }
        this.tail.compareAndSet(tail, node)
    }

    override fun poll(): V? {
        while (true) {
            val head: Node = head.get()
            val tail: Node = tail.get()
            val next: Node? = head.next.get()
            if (head === tail) {
                if (next == null) {
                    return null
                }
                this.tail.compareAndSet(tail, next)
            } else {
                if (this.head.compareAndSet(head, next)) {
                    return next?.x
                }
            }
        }
    }

    override fun peek(): V? {
        return head.get().next.get()?.x
    }

    private inner class Node(val x: V?) {
        val next: AtomicReference<Node?> = AtomicReference(null)
    }
}
