package com.example.app.kafka

import com.example.config.KafkaFactory
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue

class KafkaConsumerService(private val application: Application) {

    private val consumer = KafkaFactory.createConsumer()
    private val topic = KafkaFactory.config.topic

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private val _lastMessages: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
    val lastMessages: List<String>
        get() = _lastMessages.toList()

    fun start() {
        consumer.subscribe(listOf(topic))

        job = scope.launch {
            try {
                while (isActive) {
                    val records: ConsumerRecords<String, String> =
                        consumer.poll(Duration.ofSeconds(1))
                    for (record in records) {
                        val msg = "${record.topic()}[${record.partition()}]@${record.offset()}: ${record.value()}"
                        application.log.info("Kafka message received: $msg")

                        _lastMessages.add(msg)
                        while (_lastMessages.size > 50) {
                            _lastMessages.poll()
                        }
                    }
                }
            } catch (e: WakeupException) {
                // ignore for shutdown
            } catch (e: Exception) {
                application.log.error("Kafka consumer error", e)
            } finally {
                consumer.close()
            }
        }

        application.environment.monitor.subscribe(ApplicationStopped) {
            stop()
        }
    }

    fun stop() {
        try {
            consumer.wakeup()
        } catch (_: Exception) {
        }
        job?.cancel()
        scope.cancel()
    }
}

