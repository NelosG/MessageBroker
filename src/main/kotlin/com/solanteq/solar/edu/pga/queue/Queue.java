package com.solanteq.solar.edu.pga.queue;

/**
 * Queue interface.
 *
 * @author Nikita Koval
 */
public interface Queue<V> {

    void enqueue(V x);

    V dequeue();

    V peek();

    boolean isEmpty();
}
