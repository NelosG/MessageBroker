package com.solanteq.solar.edu.pga

import org.junit.jupiter.api.BeforeEach


/**
 * @author gpushkarev
 * @since 4.0.0
 */
class MessageBrokerLockFreeTest : MessageBrokerBaseTest() {

    @BeforeEach
    override fun init() {
        messageBroker = MessageBrokerLockFreeImpl()
    }
}
