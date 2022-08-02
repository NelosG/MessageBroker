package com.solanteq.solar.edu.pga

import java.util.concurrent.CompletableFuture

/**
 * Интерфейс упрощенного брокера сообщений
 *
 * @author gpushkarev
 * @since 1.0.0
 */
interface MessageBroker<K : Any, V : Any> {
    /**
     * Подписываемся на первый запрос с ключом [key].
     * Когда ловим такой запрос, то генерируем ответ с помощью функции [responder] и данных входяшего запроса [V].
     *
     * @param key       ключ запроса, который ожидаем
     * @param responder функция для генерации исходящего ответа на входящий запрос
     *
     * @return [CompletableFuture] с данными запроса;
     */
    fun listenAndReply(key: K, responder: (V) -> V): CompletableFuture<V>

    /**
     * Отправляем запрос с ключом [key] и данными [value] с типом [V] и отдаем [CompletableFuture] для ожидания ответа.
     *
     * @param key       ключ запроса
     * @param value     данные запроса
     *
     * @return [CompletableFuture] с данными ответа;
     */
    fun sendAndReceive(key: K, value: V): CompletableFuture<V>
}
