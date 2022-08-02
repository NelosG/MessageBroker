package com.solanteq.solar.edu.pga

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

/**
 * @author gpushkarev
 * @since 1.0.0
 */
class MessageBrokerTest {

    private lateinit var messageBroker: MessageBrokerImpl<String, String>

    @BeforeEach
    fun init() {
        messageBroker = MessageBrokerImpl()
    }

    @Test()
    fun shouldReturnResult() {
        val key = "key"
        val value = "fffffffffff"

        val request = messageBroker.listenAndReply(key) { return@listenAndReply value }
        val response = messageBroker.sendAndReceive(key, value)

        assertEquals(value, request.get())
        assertEquals(value, response.get())
    }

    @Test
    fun shouldBeCancelled() {
        val key = "key"
        val value = "fffffffffff"

        val request = messageBroker.listenAndReply(key) { "Hello $value" }
        request.cancel(true)

        val response = messageBroker.sendAndReceive(key, value)

        assertThrows<CancellationException> {
            (request.get())
        }
        runBlocking {
            launch {
                messageBroker.listenAndReply(key) { value }
            }
            assertEquals(withContext(Dispatchers.IO) {
                response.get()
            }, value)
        }
    }

    @Test
    fun correctnessTest1() {
        val count = 1_000
        val key = "correctnessTest1"

        val request = ArrayList<String>(count)
        val responce = ArrayList<String>(count)

        val requestRes = ArrayList<CompletableFuture<String>>(count)
        val responceRes = ArrayList<CompletableFuture<String>>(count)

        var i = 0
        repeat(count) {
            val requestString = i++.toString()
            request.add(requestString)
            requestRes.add(messageBroker.sendAndReceive(key, requestString))
        }
        request.forEach {
            val func = { s: String -> s + 10 }
            responce.add(func(it))
            responceRes.add(messageBroker.listenAndReply(key, func))
        }

        request.indices.forEach{ ind ->
            assertEquals(request[ind], responceRes[ind].get())
        }

        responce.indices.forEach{ ind ->
            assertEquals(responce[ind], requestRes[ind].get())
        }
    }

    @Test
    fun correctnessTest2() {
        val count = 1_000
        val key = "correctnessTest2"

        val request = ArrayList<String>(count)
        val responce = ArrayList<String>(count)

        val requestRes = ArrayList<CompletableFuture<String>>(count)
        val responceRes = ArrayList<CompletableFuture<String>>(count)

        var i = 0
        repeat(count) {
            val requestString = i++.toString()
            request.add(requestString)
            requestRes.add(messageBroker.sendAndReceive(key, requestString))

            val func = { s: String -> s + 10 }
            responce.add(func(requestString))
            responceRes.add(messageBroker.listenAndReply(key, func))
        }

        request.indices.forEach{ ind ->
            assertEquals(request[ind], responceRes[ind].get())
        }

        responce.indices.forEach{ ind ->
            assertEquals(responce[ind], requestRes[ind].get())
        }
    }
}
