data class Intention(
    val id: Int = 0,
    var title: String,
    var intention: String,
    var multiplier: Double,
    var frequency: String,
    var awakeDevice: Boolean,
    var boostPower: Boolean,
    var timerStartedAt: Long,
    var iterationCompleted: Double,
    var iterationCount: String,
    var timerRunning: Boolean=false,
    var isNotification: Boolean = false,
    var targetLength: Long,

    @Transient
    var newIntention: String = "",
    @Transient
    var mutableIntention: String = "",
    @Transient
    var newMultiplier: Long = 0L,
    @Transient
    var iterationsInLastSecond: Double = 0.0,
    @Transient
    var lastSecond: Long = System.nanoTime(),
    @Transient
    var isFirstIterationSet: Boolean = false,
    @Transient
    var iterations: Double = 0.0,
    @Transient
    var elapsedTime: Long = 0L,
    @Transient
    var lastTime: String = "",
    @Transient
    var updatedIterationCount: String = "",
    @Transient
    var lastHourMark:Long =-1
)
