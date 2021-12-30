package pl.titil.metronome

import android.content.*
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import java.util.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import pl.titil.metronome.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var isServiceBound = false
    private var beatService: BeatService? = null

    private lateinit var btnStart: AppCompatButton
    private lateinit var btnStop: AppCompatButton
    private lateinit var seekBar: SeekBar
    private lateinit var txtBpm: AppCompatTextView
    private lateinit var beatAnimator: BeatAnimator

    private lateinit var tickButtons: TickButtons
    private lateinit var ticksModel: TicksViewModel

    var sJob: Job? = null
    var bJob: Job? = null

    fun getTickColor(index: Int): Int {
        return when (ticksModel.getTickAt(index)) {
            TickState.NONE -> Color.LTGRAY
            else -> resources.getColor(R.color.design_default_color_primary, theme)
        }
    }

    private val srvConnection = object : ServiceConnection {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            Log.d("service.connected", isServiceBound.toString())
            beatService = (binder as BeatService.Binder).getService()

            btnStart.isEnabled = !beatService!!.isPlaying
            btnStop.isEnabled = beatService!!.isPlaying

            sJob = lifecycleScope.launch {
                beatService!!.onOffFlow.collect {
                    delay(100)
                    runOnUiThread {
                        btnStart.isEnabled = !it
                        btnStop.isEnabled = it
                    }
                }
            }

            bJob = lifecycleScope.launch {
                beatService!!.beatChannel.receiveAsFlow().collect {
                    runOnUiThread {
                        tickButtons.setCurrent(it, ::getTickColor,
                            resources.getColor(R.color.orange_500, theme))
                        beatAnimator.animate(beatService!!.interval, it)
                    }
                }
            }

            if (!beatService!!.config.isSet()) {
                ticksModel.setConfig(ticksModel.config.value!!)
            } else {
                ticksModel.setConfig(beatService!!.config.get())
            }

            val circle = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_circle)
            val square = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_square)

            ticksModel.config.observe(this@MainActivity) {
                beatService!!.config.set(it)
                tickButtons.reconfigureDisplay(it, ::getTickColor, circle!!, square!!)
            }

            if (beatService!!.interval == 0L) {
                ticksModel.bpm.value = ticksModel.bpm.value
            } else {
                ticksModel.bpm.value = ((60f / beatService!!.interval.toFloat()) * 1000f).toInt()
            }
            ticksModel.bpm.observe(this@MainActivity) {
                if (beatService != null) {
                    beatService!!.interval = ((60f / it.toFloat()) * 1000f).toLong()
                }
                txtBpm.text = it.toString()
            }

            seekBar.progress = ticksModel.bpm.value!!
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            Log.d("service.disconnected", "$beatService?")
            ensureCleanup()
            isServiceBound = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_top_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.quit -> {
                beatService!!.stopAll()
                finishAndRemoveTask()
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        ticksModel = ViewModelProvider(this).get(TicksViewModel::class.java)

        btnStart = binding.controls.btnStart
        btnStop = binding.controls.btnStop
        txtBpm = binding.visuals.bpmText
        beatAnimator = BeatAnimator(binding.visuals.imageCanvas)

        with(binding.visuals) {
            tickButtons = TickButtons(arrayListOf(btnTick1, btnTick2, btnTick3, btnTick4))
        }

        tickButtons.bindModel(ticksModel)

        ensureBound()

        val intent = Intent(this, BeatService::class.java)
        btnStart.setOnClickListener {
            it.isEnabled = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        btnStop.setOnClickListener {
            it.isEnabled = false
            beatService!!.stopAll()
        }

        val progressMin = 40
        seekBar = binding.controls.seekBar.apply {
            max = 240
            progress = ticksModel.bpm.value!!
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.min = progressMin
        }
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress < progressMin) {
                    seek.progress = progressMin
                } else {
                    ticksModel.bpm.value = progress
                }
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
    }

    override fun onResume() {
        super.onResume()
        ensureBound()
    }

    override fun onStop() {
        Log.d("application.stop", isServiceBound.toString())
        ensureCleanup()
        ensureUnbound()
        super.onStop()
    }

    override fun onDestroy() {
        Log.d("application.destroy", isServiceBound.toString())
        ensureCleanup()
        ensureUnbound()
        super.onDestroy()
    }

    private fun ensureBound() {
        if (!isServiceBound) {
            val intent = Intent(this, BeatService::class.java)
            isServiceBound = bindService(intent, srvConnection, Context.BIND_AUTO_CREATE)
            Log.d("service.bind", isServiceBound.toString())
        }
    }

    private fun ensureUnbound() {
        if (isServiceBound) {
            unbindService(srvConnection)
            isServiceBound = false
            Log.d("service.unbind", isServiceBound.toString())
        }
    }

    private fun ensureCleanup() {
        for (job in arrayOf(sJob, bJob)) job?.cancel()
        sJob = null
        bJob = null
        ticksModel.config.removeObservers(this)
        ticksModel.bpm.removeObservers(this)
        Log.d("service.cleanup", isServiceBound.toString())
    }
}