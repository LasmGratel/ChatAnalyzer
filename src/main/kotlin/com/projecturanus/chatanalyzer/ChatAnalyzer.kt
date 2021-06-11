package com.projecturanus.chatanalyzer

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import org.bson.BsonDocument
import org.bson.Document
import org.litote.kmongo.KMongo
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.nameWithoutExtension

fun main() = runBlocking {
    val logMap = ConcurrentHashMap<String, Log>()
    val regex = Regex(".*-(\\d\\d\\d\\d-\\d\\d-\\d\\d)-.+")
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val logAdapter = moshi.adapter(LogData::class.java)
    val logPath = Paths.get("logs_json")
    if (!Files.isDirectory(logPath)) {
        println("logs_json folder does not exist")
        return@runBlocking
    }
    val jobs = arrayListOf<Job>()
    val writer = Files.newBufferedWriter(Paths.get("all_player_chats.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    for (logFile in Files.list(logPath)) {
        jobs += GlobalScope.launch {
            if (regex.matches(logFile.fileName?.toString()!!)) {
                val date = regex.find(logFile.fileName?.toString()!!)?.groupValues?.get(1)!!
                val logData = logAdapter.fromJson(Files.readString(logFile))!!
                val log = logMap[logData.name ?: "unnamed"] ?: Log(logData.name ?: "unnamed")
                log.chats.addAll(
                    logData.chats.asSequence()
                        .map {
                            if (it.name != "") {
                                writer.appendLine(it.content)
                            }
                            Chat(it.name, it.content, LocalDateTime.parse("${date}T${it.time}"))
                        })

                logMap[logData.name ?: "unnamed"] = log
            }
        }
    }
    jobs.joinAll()
    writer.close()
    jobs.clear()

    System.setProperty("org.litote.mongo.test.mapping.service", "org.litote.kmongo.jackson.JacksonClassMappingTypeService")
    System.setProperty("org.litote.mongo.mapping.service", "org.litote.kmongo.jackson.JacksonClassMappingTypeService")
    val mongoClient = KMongo.createClient()
    val db = mongoClient.getDatabase("chats")
    for ((name, log) in logMap.entries) {
        jobs += GlobalScope.launch {
            db.createCollection(name)
            val collection = db.getCollection(name)
            val list = log.chats.map {
                Document().apply {
                    append("content", it.content)
                    append("name", it.name)
                    append("time", it.time)
                }
            }
            if (list.isNotEmpty()) collection.insertMany(list)
        }
    }
    jobs.joinAll()
}
