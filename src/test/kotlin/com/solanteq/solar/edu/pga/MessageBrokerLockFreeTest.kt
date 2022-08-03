package com.solanteq.solar.edu.pga

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
class MessageBrokerLockFreeTest {

    private lateinit var messageBroker: MessageBrokerLockFreeImpl<String, String>

    @BeforeEach
    fun init() {
        messageBroker = MessageBrokerLockFreeImpl()
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
        messageBroker.listenAndReply(key) { value }
        assertEquals(response.get(), value)
    }

    @Test
    fun correctnessTest1() {
        correctnessTest1Impl(1, 10_000)
    }

    @Test
    fun correctnessTest2() {
        correctnessTest2Impl(1, 10_000)
    }

    @Test
    fun correctnessMultiTest1() {
        runBlocking {
            var id = 0
            repeat(1_000) {
                launch {
                    correctnessTest1Impl(id, 1_000)
                }
                id++
            }
        }
    }

    @Test
    fun correctnessMultiTest2() {
        runBlocking {
            var id = 0
            repeat(1_000) {
                launch {
                    correctnessTest2Impl(id, 1_000)
                }
                id++
            }
        }
    }

    @Test
    fun multithreadingTest() {

    }

    private fun correctnessTest1Impl(id: Int, count: Int) {
        val key = "correctnessTest$id"

        val request = ArrayList<String>(count)
        val response = ArrayList<String>(count)

        val requestRes = ArrayList<CompletableFuture<String>>(count)
        val responseRes = ArrayList<CompletableFuture<String>>(count)

        var i = 0
        repeat(count) {
            val requestString = i++.toString()
            request.add(requestString)
            requestRes.add(messageBroker.sendAndReceive(key, requestString))
        }
        request.forEach {
            val func = { s: String -> s + 10 }
            response.add(func(it))
            responseRes.add(messageBroker.listenAndReply(key, func))
        }

        request.indices.forEach { ind ->
            assertEquals(request[ind], responseRes[ind].get())
        }

        response.indices.forEach { ind ->
            assertEquals(response[ind], requestRes[ind].get())
        }
    }

    private fun correctnessTest2Impl(id: Int, count: Int) {
        val key = "correctnessTest$id"

        val request = ArrayList<String>(count)
        val response = ArrayList<String>(count)

        val requestRes = ArrayList<CompletableFuture<String>>(count)
        val responseRes = ArrayList<CompletableFuture<String>>(count)

        var i = 0
        repeat(count) {
            val requestString = i++.toString()
            request.add(requestString)
            requestRes.add(messageBroker.sendAndReceive(key, requestString))

            val func = { s: String -> s + 10 }
            response.add(func(requestString))
            responseRes.add(messageBroker.listenAndReply(key, func))
        }

        request.indices.forEach { ind ->
            assertEquals(request[ind], responseRes[ind].get())
        }

        response.indices.forEach { ind ->
            assertEquals(response[ind], requestRes[ind].get())
        }
    }
}
