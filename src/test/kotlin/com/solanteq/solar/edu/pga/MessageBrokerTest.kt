package com.solanteq.solar.edu.pga

import org.junit.jupiter.api.BeforeEach


/**
 * @author gpushkarev
 * @since 1.0.0
 */
class MessageBrokerTest : MessageBrokerBaseTest() {

    @BeforeEach
    override fun init() {
        messageBroker = MessageBrokerImpl()
    }
}
