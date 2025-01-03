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

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    var mService:MusicPlayerService ?= null

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

        setInitTime()
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

    private fun play() {
        mService?.play()
        binding.btnPlay.visibility = View.INVISIBLE
        binding.btnPause.visibility = View.VISIBLE
    }

    private fun pause() {
        val position = mService?.getCurrentPosition()

        Log.i(TAG, "position : $position")
        mService?.pause()
        binding.btnPlay.visibility = View.VISIBLE
        binding.btnPause.visibility = View.INVISIBLE
    }

    private fun setInitTime() {
        val duration = mService?.getDuration()
        binding.customSeekbar.max = duration ?: 0
        binding.durationTime.text = duration?.let {
            String.format("%02d:%02d", it / 1000 / 60, it / 1000 % 60)
        }
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

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }


}