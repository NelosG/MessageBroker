package com.solanteq.solar.edu.pga

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

abstract class MessageBrokerBaseTest {
    protected lateinit var messageBroker: MessageBroker<String, String>

    abstract fun init()

    @Test()
    fun shouldReturnResult() {
        val key = "key"
        val value = "Hello"

        val request = messageBroker.listenAndReply(key) { return@listenAndReply value }
        val response = messageBroker.sendAndReceive(key, value)

        Assertions.assertEquals(value, request.get(10, TimeUnit.SECONDS))
        Assertions.assertEquals(value, response.get(10, TimeUnit.SECONDS))
    }

    @Test
    fun shouldBeCancelled() {
        val key = "key"
        val value = "Hello"

        val request = messageBroker.listenAndReply(key) { "Hello $value" }
        request.cancel(true)

        val response = messageBroker.sendAndReceive(key, value)

        assertThrows<CancellationException> {
            (request.get(10, TimeUnit.SECONDS))
        }
        messageBroker.listenAndReply(key) { value }
        Assertions.assertEquals(response.get(10, TimeUnit.SECONDS), value)
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
    fun correctnessTestWithCancel() {
        runBlocking {
            correctnessTestWithCancelImpl(1, 10_000)
        }
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
    fun correctnessMultiTestWithCancel() {
        runBlocking {
            var id = 0
            repeat(500) {
                correctnessTestWithCancelImpl(id, 500)
                id++
            }
        }
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
            Assertions.assertEquals(request[ind], responseRes[ind].get(10, TimeUnit.SECONDS))
        }

        response.indices.forEach { ind ->
            Assertions.assertEquals(response[ind], requestRes[ind].get(10, TimeUnit.SECONDS))
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
            Assertions.assertEquals(request[ind], responseRes[ind].get(10, TimeUnit.SECONDS))
        }

        response.indices.forEach { ind ->
            Assertions.assertEquals(response[ind], requestRes[ind].get(10, TimeUnit.SECONDS))
        }
    }

    private suspend fun correctnessTestWithCancelImpl(id: Int, count: Int) = coroutineScope {
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
            repeat(count / 100) {
                val future = messageBroker.sendAndReceive(key, requestString)
                launch {
                    future.cancel(true)
                }
            }
        }
        request.forEach {
            val func = { s: String -> s + 10 }
            response.add(func(it))
            responseRes.add(messageBroker.listenAndReply(key, func))
            repeat(count / 100) {
                val future = messageBroker.listenAndReply(key, func)
                launch {
                    future.cancel(true)
                }
            }
        }

        request.indices.forEach { ind ->
            Assertions.assertEquals(request[ind], responseRes[ind].get(10, TimeUnit.SECONDS))
        }

        response.indices.forEach { ind ->
            Assertions.assertEquals(response[ind], requestRes[ind].get(10, TimeUnit.SECONDS))
        }
    }
}
