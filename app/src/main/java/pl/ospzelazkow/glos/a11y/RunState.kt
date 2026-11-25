package pl.ospzelazkow.glos.a11y

object RunState {
    @Volatile var running = false
    @Volatile var lastChallenge = 0L
}