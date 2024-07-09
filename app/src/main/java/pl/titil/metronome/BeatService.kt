package pl.titil.metronome

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import java.util.concurrent.Executors.newFixedThreadPool

class BeatService : LifecycleService() {
    private var id = 0L
    private val channelId = "Metronome"
    private val notificationId = 1
    private val binder by lazy { Binder() }

    private lateinit var jobBeat: Job
    private lateinit var jobStart: Job
    private lateinit var receiver: BroadcastReceiver

    private lateinit var beatDispatcher: ExecutorCoroutineDispatcher
    private var canBeStarted: Boolean = true
    var isPlaying: Boolean = false
        set(value) {
            field = value
            lifecycleScope.launch {
                _onOffFlow.emit(value)
            }
        }

    lateinit var beatChannel: Channel<Int>
    private lateinit var _onOffFlow: MutableSharedFlow<Boolean>
    lateinit var onOffFlow: SharedFlow<Boolean>

    private var currentBeat = 0
    var interval = 0L

    private lateinit var player: SoundPlayer
    private lateinit var conf: ArrayList<Int>
    lateinit var config: ConfigWrapper

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    inner class Binder : android.os.Binder() {
        fun getService(): BeatService {
            return this@BeatService
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = SoundPlayer.create(this, R.raw.drum, R.raw.pulse)
        conf = ArrayList()
        config = ConfigWrapper(conf, player.drumID, player.pulseID)
        beatDispatcher = newFixedThreadPool(2).asCoroutineDispatcher()
        beatChannel = Channel(CONFLATED)
        _onOffFlow = MutableSharedFlow()
        onOffFlow = _onOffFlow.asSharedFlow()
        registerReceiver()
    }
    override fun onDestroy() {
        unregisterReceiver()
        stopAll()
        beatDispatcher.apply {
            cancel()
            close()
        }
        Log.d("--- onDestroy ---", isPlaying.toString())
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("--- onStartCommand ---", flags.toString())
        super.onStartCommand(intent, flags, startId)
        if (canBeStarted) {
            canBeStarted = false
            startNotification()
        }
        startMetronome()
        return START_STICKY
    }

    private fun registerReceiver() {
        receiver = ControlReceiver {
            when (it) {
                ControlReceiver.ACTION_TOGGLE -> {
                    if (!isPlaying) startMetronome()
                    else stopMetronome()
                }
                ControlReceiver.ACTION_STOP -> stopAll()
            }
        }
        val filter = IntentFilter().apply {
            addAction(ControlReceiver.ACTION_TOGGLE)
            addAction(ControlReceiver.ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        Log.d("register", receiver.toString())
    }

    private fun unregisterReceiver() {
        unregisterReceiver(receiver)
        Log.d("unregister", receiver.toString())
    }

    fun stopAll() {
        if (::jobStart.isInitialized) jobStart.cancel()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        stopMetronome()
        stopForeground(true)
        if (::jobBeat.isInitialized) jobBeat.cancel()
        stopSelf()
        canBeStarted = true
    }

    private fun startMetronome() {
        if (!isPlaying) {
            isPlaying = true
            id += 1
            val currentId = id
            Log.d("metronome.starting", id.toString())
            jobBeat = lifecycleScope.launch(beatDispatcher) {
                val beatScope = CoroutineScope(Dispatchers.IO)
                while (isPlaying && currentId == id) {
                    val t0 = System.currentTimeMillis()
                    val snd = conf[currentBeat]
                    beatScope.launch {
                        player.play(snd)
                    }
                    beatChannel.send(currentBeat)
                    var nextSnd = 0
                    while (nextSnd == 0) {
                        if (currentBeat <= 2) {
                            currentBeat += 1
                        } else {
                            currentBeat = 0
                        }
                        nextSnd = conf[currentBeat]
                    }
                    val t1 = System.currentTimeMillis()
                    val d = t1 - t0
                    val wait = interval - d
                    delay(wait)
                }
            }
            Log.d("metronome.started", "started")
        }
    }

    private fun stopMetronome() {
        isPlaying = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startNotification() {
        if (isPlaying) {
            return
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "Metronome", NotificationManager.IMPORTANCE_DEFAULT)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }

        val toggleIntent = Intent(ControlReceiver.ACTION_TOGGLE)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE)

        val intentStop = Intent(ControlReceiver.ACTION_STOP)
        val pendingIntentStop = PendingIntent.getBroadcast(
            this, 0, intentStop, PendingIntent.FLAG_IMMUTABLE)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_notification_metronome)
            .setContentTitle("Metronome")
            .setContentIntent(contentIntent)
        startForeground(notificationId, notificationBuilder.setContentText("is running").build())
        jobStart = lifecycleScope.launch {
            onOffFlow.collect {
                val label = if (it) getString(R.string.pause) else getString(R.string.resume)
                val stateText = if (it) getString(R.string.is_running) else getString(R.string.is_paused)
                notificationBuilder
                    .setContentText(stateText)
                    .clearActions()
                    .addAction(0, label, pendingIntent)
                    .addAction(0, getString(R.string.stop), pendingIntentStop)
                notificationManager.notify(notificationId, notificationBuilder.build())
            }
        }
    }
}