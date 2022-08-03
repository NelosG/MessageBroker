package com.solanteq.solar.edu.pga.util

import java.util.concurrent.CompletableFuture

/**
 * @author gpushkarev
 * @since 4.0.0
 */
data class Send<V : Any>(
    val future: CompletableFuture<V>,
    val value: V,
)
