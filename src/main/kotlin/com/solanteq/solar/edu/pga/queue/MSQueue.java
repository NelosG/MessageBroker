package com.solanteq.solar.edu.pga.queue;

import java.util.concurrent.atomic.AtomicReference;

public class MSQueue<V> implements Queue<V> {
    private final AtomicReference<Node> Head;
    private final AtomicReference<Node> Tail;

    public MSQueue() {
        final Node dummy = new Node(null);
        this.Head = new AtomicReference<>(dummy);
        this.Tail = new AtomicReference<>(dummy);
    }

    @Override
    public boolean isEmpty() {
        return Head == Tail;
    }

    @Override
    public void enqueue(final V x) {
        final Node node = new Node(x);
        Node tail;
        while (true) {
            tail = this.Tail.get();
            if (tail.next.get() == null) {
                if (tail.next.compareAndSet(null, node)) {
                    break;
                }
            } else {
                this.Tail.compareAndSet(tail, tail.next.get());
            }
        }
        this.Tail.compareAndSet(tail, node);
    }

    @Override
    public V dequeue() {
        while (true) {
            final Node head = this.Head.get();
            final Node tail = this.Tail.get();
            final Node next = head.next.get();
            if (head == this.Head.get()) {
                if (head == tail) {
                    if (next == null) {
                        return null;
                    }
                    this.Tail.compareAndSet(tail, next);
                } else {
                    if (this.Head.compareAndSet(head, next)) {
                        return next.x;
                    }
                }
            }
        }
    }

    @Override
    public V peek() {
        final Node next = this.Head.get().next.get();
        if (next == null) {
            return null;
        }
        return next.x;
    }

    private class Node {
        final V x;
        final AtomicReference<Node> next;

        Node(final V x) {
            this.x = x;
            this.next = new AtomicReference<>(null);
        }
    }
}
