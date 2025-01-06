package com.example.mediaplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class MusicPlayerService: Service() {
    private var mMediaPlayer: MediaPlayer? = null
    private var mBinder: MusicPlayerBinder? = MusicPlayerBinder()
    private var mMediaPlayerListener: OnMediaPlayerListener ?= null

    interface OnMediaPlayerListener {
        fun onCompletion()
    }

    fun setOnMediaPlayerListener(listener: OnMediaPlayerListener) {
        mMediaPlayerListener = listener
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate()")

        startForegroundService()

        //파일 등록
        val afd = resources.openRawResourceFd(R.raw.chocolate) ?: return
        mMediaPlayer = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            prepare()
            isLooping = false   //반복재생 여부
            setOnCompletionListener {
                Log.i(TAG, "#######################")
                Log.i(TAG, "플레이 완료!")
                mMediaPlayerListener?.onCompletion()
                Log.i(TAG, "#######################")
            }
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "onBind()")

        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        return START_STICKY //서비스가 중단되면 다시 실행
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy()")

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
    }

    private fun startForegroundService() {
        Log.i(TAG, "startForegroundService()")

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel( //알림 채널 설정
                "CHANNEL_ID","CHANNEL_NAME", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(mChannel)
        }

        val notification = Notification.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_play)   //알림 아이콘
            .setContentTitle("뮤직 플레이어 앱")   //알림의 제목 설정
            .setContentText("앱이 실행 중입니다.")  //아림의 내용 설정
            .build()

        startForeground(1, notification) //1이상, 알람으로 보여줄 인수 값을 넘겨 준다(notification)
    }

    fun isPlaying(): Boolean {
        Log.i(TAG, "isPlaying")
        return (mMediaPlayer != null && mMediaPlayer?.isPlaying ?: false)
    }

    fun play() {
        if(mMediaPlayer!!.isPlaying) {
            Toast.makeText(this, "이미 음악이 재생 중입니다.", Toast.LENGTH_SHORT).show()
        } else {
            mMediaPlayer?.setVolume(1.0f, 1.0f) //볼륨 저장
            mMediaPlayer?.start()   //음악 재생
        }
    }

    fun pause() {
        mMediaPlayer?.let {
            if(it.isPlaying) {
                it.pause()  //음악을 일시 정지
            }
        }
    }

    fun stop() {
        mMediaPlayer?.let {
            if(it.isPlaying) {
                it.stop()   //음악을 중지
                it.release()    //미디어 플레이어에 할당된 자원을 해제
                mMediaPlayer = null
            }
        }
    }

    fun getCurrentPosition(): Int {
        return mMediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {
        val duration = mMediaPlayer?.duration
        Log.i(TAG, "duration = $duration")

        return duration ?: 0
    }

    //바인더를 반환하여 서비스 함수를 사용할 수 있게 한다.
    inner class MusicPlayerBinder: Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }

    companion object {
        val TAG = MusicPlayerService::class.java.simpleName
    }
}