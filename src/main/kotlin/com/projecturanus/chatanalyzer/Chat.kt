package com.projecturanus.chatanalyzer

import java.time.Instant
import java.time.LocalDateTime

class Log(val name: String) {
    val chats: MutableList<Chat> = arrayListOf()
}

class Chat(val name: String?, val content: String, val time: LocalDateTime) {
}
