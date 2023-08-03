package com.solanteq.solar.edu.pga.queue;

/**
 * Queue interface.
 *
 * @author Nikita Koval
 */
public interface Queue<V> {

    void add(V x);

    V poll();

    V peek();

    boolean isEmpty();
}
