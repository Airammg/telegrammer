package com.telegrammer.shared.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class Closeable(private val job: Job) {
    fun close() {
        job.cancel()
    }
}

class FlowWrapper<T>(private val flow: Flow<T>) {
    fun watch(block: (T) -> Unit): Closeable {
        val job = CoroutineScope(Dispatchers.Main).launch {
            flow.collect { block(it) }
        }
        return Closeable(job)
    }
}
