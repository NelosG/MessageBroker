package com.solanteq.solar.edu.pga.util

import java.util.concurrent.CompletableFuture

/**
 * @author gpushkarev
 * @since 4.0.0
 */
interface SendListen<V> {
    val future: CompletableFuture<V>
}