package com.example.mediaplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mediaplayer.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private var mService:MusicPlayerService ?= null
    private var mJob: Job? = null

    // 서비스와 구성요소 연결 상태 모니터링
    val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "onServiceConnected")

            mService = (service as MusicPlayerService.MusicPlayerBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected")

            mService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPlay.setOnClickListener(this)
        binding.btnPause.setOnClickListener(this)

        binding.customSeekbar.progress = 0
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume()")

        if(mService == null) {
            //안드로이드 O이상이면 startForegroundService()를 호출해야 한다.
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(
                    this, MusicPlayerService::class.java
                ))
            } else {
                startService(Intent(
                    applicationContext, MusicPlayerService::class.java
                ))
            }

            //activity를 서비스와 바인드 시킨다.
            val intent = Intent(this, MusicPlayerService::class.java)
            bindService(intent, mServiceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")

        //사용자가 Activity를 떠났을 때
        if(mService != null) {
            if(!mService!!.isPlaying()) {   //mService가 재생되고 있지 않다면 서비스를 중단
                mService!!.stopSelf()
            }
            unbindService(mServiceConnection)   //서비스로부터 연결을 끊는다.
            mService = null
        }
    }

    @SuppressLint("DefaultLocale")
    private fun play() {
        mService?.play()
        binding.btnPlay.visibility = View.INVISIBLE
        binding.btnPause.visibility = View.VISIBLE

        val duration = setInitTime()
        if(duration > 0) {
            //재생 업데이트에 관한 코루틴 생성
            mJob = CoroutineScope(Dispatchers.Main).launch {
                while (mService?.isPlaying() == true) {
                    val currentPosition = mService?.getCurrentPosition()
                    binding.customSeekbar.progress = currentPosition ?: 0
                    binding.currentTime.text = currentPosition?.let {
                        String.format("%02d:%02d", it / 1000 / 60, it / 1000 % 60)
                    }

                    delay(1000)
                }
            }
        }

        mService?.setOnMediaPlayerListener(object : MusicPlayerService.OnMediaPlayerListener {
            override fun onCompletion() {
                Log.i(TAG, "파일 재생 끝에 도달.")
                resetSeekBar()
            }
        })
    }

    private fun pause() {
        val position = mService?.getCurrentPosition()

        Log.i(TAG, "position : $position")
        mService?.pause()
        binding.btnPlay.visibility = View.VISIBLE
        binding.btnPause.visibility = View.INVISIBLE
    }

    @SuppressLint("DefaultLocale")
    private fun setInitTime() : Int{
        val duration = mService?.getDuration()
        binding.customSeekbar.max = duration ?: 0   //SEEK BAR의 총 사이즈 max값을 현재 재생 되는 미디어의 duration값으로 초기화
        binding.durationTime.text = duration?.let {
            String.format("%02d:%02d", it / 1000 / 60, it / 1000 % 60)
        }

        return duration ?: 0
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            binding.btnPlay.id -> {
                Log.i(TAG, "play()")
                play()
            }

            binding.btnPause.id -> {
                Log.i(TAG, "pause()")
                pause()
            }
        }
    }

    private fun resetSeekBar() {
        mJob?.cancel()
        mJob = null
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }


}