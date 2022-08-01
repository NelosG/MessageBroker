package com.solanteq.solar.edu.pga

import java.util.concurrent.CompletableFuture


/**
 * Ваша реализация упрощенного брокера сообщений:
 * 1. Методы [listenAndReply] и [sendAndReceive] могут вызываться в разном порядке. То есть подписка на запрос
 * может быть как до самого запроса, так и после. В обоих случаях реализация должна успешно работать;
 * 2. Любой [CompletableFuture] могут отменить через [CompletableFuture.cancel]. В этом случае либо подписка,
 * либо сам запрос должны перестать учитываться. Например, сделали подписку, отменили подписку и отправили запрос.
 * В этом случае запрос не должен «сматчиться»;
 * 3. Таймауты внутри не нужны. Отмена может произойти только с вызывающей стороны;
 * 4. Реализация должна быть линеаризуемой;
 * 5. Методы должны сразу возвращать [CompletableFuture] для ожидания выполнения операции.
 * В явном виде в потоке вызывающей стороны не должно происходить выполнение логики.
 *
 * @author <put your nickname here>
 * @since <put the latest version here>
 */
class MessageBrokerImpl<K : Any, V : Any> : MessageBroker<K, V> {
    override fun listenAndReply(key: K, responder: (V) -> V): CompletableFuture<V> {
        TODO("Not yet implemented")
    }

    override fun sendAndReceive(key: K, value: V): CompletableFuture<V> {
        TODO("Not yet implemented")
    }
}

fun main() {
    val messageBroker = MessageBrokerImpl<String, String>()
}
