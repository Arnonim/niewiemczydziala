package pl.ospzelazkow.glos.web

import kotlinx.coroutines.flow.MutableSharedFlow

sealed class Cmd {
    data class Load(val url: String) : Cmd()
    object WipeCookies : Cmd()
    data class SetEmail(val email: String, val index: Int) : Cmd()
    object Submit : Cmd()
    object Kill : Cmd()
}

sealed class Evt {
    data class Flag(val title: String) : Evt()
    data class Done(val phase: String) : Evt()
    data class Quiz(val q: String, val a: String, val solved: Boolean) : Evt()
    data class Loaded(val url: String) : Evt()
    data class Failed(val code: Int, val desc: String) : Evt()
}

object VoteBus {
    val cmds = MutableSharedFlow<Cmd>(extraBufferCapacity = 32)
    val evts = MutableSharedFlow<Evt>(extraBufferCapacity = 256)
}