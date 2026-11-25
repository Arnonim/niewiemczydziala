package pl.ospzelazkow.glos.web

import kotlinx.coroutines.flow.MutableSharedFlow

object MailBus {
    val results = MutableSharedFlow<String>(extraBufferCapacity = 16)
}