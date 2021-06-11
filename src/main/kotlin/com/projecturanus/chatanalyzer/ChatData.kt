package com.projecturanus.chatanalyzer

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LogData(val name: String?, val server: String?, val chats: List<ChatData>)

@JsonClass(generateAdapter = true)
data class ChatData(val name: String, val content: String, val time: String)
