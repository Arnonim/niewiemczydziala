package pl.ospzelazkow.glos.a11y

object RunState {
    @Volatile var running = false
    @Volatile var lastChallenge = 0L
    @Volatile var a11yConnected = false
    @Volatile var rotateRequest = 0
    @Volatile var rotateDone = 0
}
