package com.solanteq.solar.edu.pga.util

import java.util.concurrent.CompletableFuture

/**
 * @author gpushkarev
 * @since 1.0.0
 */
data class Send<V>(
    override val future: CompletableFuture<V>,
    val value: V,
) : SendListen<V>
