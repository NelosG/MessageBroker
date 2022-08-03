package com.solanteq.solar.edu.pga.util

import java.util.concurrent.CompletableFuture

/**
 * @author gpushkarev
 * @since 2.0.0
 */
data class Listen<V : Any>(
    val future: CompletableFuture<V>,
    val responder: (V) -> V,
)
