package com.guardianos.shield.service

fun hexdump(data: ByteArray): String = data.joinToString(" ") { String.format("%02X", it) }
