package com.solanteq.solar.edu.pga.util

/**
 * @author gpushkarev
 * @since 2.0.0
 */
class Task<K : Any, V : Any> {

    enum class TaskType {
        LISTEN,
        SEND
    }

    val taskType: TaskType
    var listen: Listen<V>? = null
    var send: Send<V>? = null

    val key: K


    constructor(key: K, listen: Listen<V>) {
        taskType = TaskType.LISTEN
        this.listen = listen
        this.key = key
    }

    constructor(key: K, send: Send<V>) {
        taskType = TaskType.SEND
        this.send = send
        this.key = key
    }
}
