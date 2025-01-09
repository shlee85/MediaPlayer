package com.example.mediaplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Build.VERSION_CODES.P
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private var mService:MusicPlayerService ?= null
    private var mJob: Job? = null
    private var mGetMediaMetadata: GetMediaMetadata? = null
    private lateinit var mAdapter: MusicListAdapter
    private var musicFiles: MutableList<MusicFile> = mutableListOf()
    private var mMenuVisible = false
    private var mCurrentPlayTitle: String? = null
    private var mCurrentPlayPath: String? = null
    private var mIsFirstPlay = true
    private var mIsShuffle = false

    //private val mMusicFilePath = "/storage/emulated/0/Music/"
    //private val mMusicFilePath = "/storage/emulated/0/Download"
    private val mMusicFilePath = "/data/data/com.example.mediaplayer/files" //퍼미션 문제로인하여 해당 앱의 data폴더를 접근 처리 하였음.

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

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPlay.setOnClickListener(this)
        binding.btnPause.setOnClickListener(this)
        binding.btnRepeat.setOnClickListener(this)
        binding.btnSelectRepeat.setOnClickListener(this)
        binding.btnMenu.setOnClickListener(this)
        binding.btnNextPlay.setOnClickListener(this)
        binding.btnPrevPlay.setOnClickListener(this)

        binding.customSeekbar.progress = 0

        mGetMediaMetadata = GetMediaMetadata(this)

        //recyclerview초기화
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(this)

        //테스트용 더미 데이터.
        //musicFiles.add(MusicFile("erick.mp3", "00:03:28"))

        //음악 파일 리스트업
        getMusicFiles()

        mAdapter = MusicListAdapter(musicFiles) {
            //선택된 파일 처리
            for(file in musicFiles) {
                file.isSelected = file == it
                Log.i(TAG, "선택된 : ${file.title}")
            }

            mAdapter.notifyDataSetChanged()
        }

        binding.menuRecyclerView.adapter = mAdapter

        mAdapter.setOnItemClickListener(object: MusicListAdapter.onItemClickListener {
            override fun onItemClick(click: MusicFile) {    //재생 목록에서 클릭시 무조건 해당 데이터 재생
                Log.i(TAG, "onItemClick Callback!![${click.title}, ${click.path}]")
                mCurrentPlayTitle = click.title
                mCurrentPlayPath = click.path

                if(mService?.isPlaying() == true) {
                    Log.i(TAG, "현재 재생 중입니다. 기존 재생을 멈추고 초기화 합니다.")
                    mService?.stop()
                    resetSeekBar()
                }

                play(true)
            }
        })
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

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("DefaultLocale")
    private fun play(isSelected: Boolean) {
        if(mCurrentPlayTitle == null) {
            mCurrentPlayTitle = musicFiles[0].title
            mCurrentPlayPath = musicFiles[0].path
            Log.i(TAG, "mCurrentPlayPath = $mCurrentPlayPath")
        }

        if(isSelected) {
            mService?.selectedPlay(mCurrentPlayPath!!)
        } else {
            //앱 시작 후 play버튼만 눌렀을 경우 뮤릭플레이 리스트에 첫번째 항목을 재생 한다.
            if(mIsFirstPlay) {
                mService?.play(musicFiles[0].path)
                mIsFirstPlay = false
            } else {
                mService?.play()
            }
        }

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

                //다음 곡이 있는지 체크. 있는 경우 재생 없는 경우 조건(반복, 또는 1번 재생)에 따라서 첫번째 곡 재생
            }
        })

        //재생 정보 (suspend함수를 호출하기 위해 코루틴 빌더를 사용)
        GlobalScope.launch {
            val mediaMetadata = mGetMediaMetadata?.retrieveMetadataFromFilePath(mCurrentPlayPath?:"null")
            Log.i(TAG, "title = ${mediaMetadata?.title}")
            Log.i(TAG, "artist = ${mediaMetadata?.artist}")
            Log.i(TAG, "album = ${mediaMetadata?.album}")
            Log.i(TAG, "bitmap = ${mediaMetadata?.albumArt}")

            runOnUiThread {
                binding.musicPlayTitle.text = mediaMetadata?.title
                binding.audioImage.setImageBitmap(mediaMetadata?.albumArt)
                //binding.audioImage.setImageBitmap(albumArt)
            }
        }
    }

    private fun pause() {
        val position = mService?.getCurrentPosition()

        Log.i(TAG, "position : $position")
        mService?.pause()
        binding.btnPlay.visibility = View.VISIBLE
        binding.btnPause.visibility = View.INVISIBLE
    }

    private fun forward() {
        Log.i(TAG, "forward []")

        //셔플인가?
        //(셔플이 아닌 경우) 다음 파일이 존재하는가?
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
                play(false)
            }

            binding.btnPause.id -> {
                Log.i(TAG, "pause()")
                pause()
            }

            binding.btnRepeat.id -> {
                Log.i(TAG, "repeat()")
                binding.btnRepeat.visibility = View.INVISIBLE
                binding.btnSelectRepeat.visibility = View.VISIBLE

                mService?.repeat(true)
            }

            binding.btnSelectRepeat.id -> {
                Log.i(TAG, "selectRepeat()")
                binding.btnRepeat.visibility = View.VISIBLE
                binding.btnSelectRepeat.visibility = View.INVISIBLE

                mService?.repeat(false)
            }

            binding.btnMenu.id -> {
                Log.i(TAG, "btnMenu")

                if(!mMenuVisible) {
                    binding.menuRecyclerView.visibility = View.INVISIBLE
                    mMenuVisible = true
                } else {
                    binding.menuRecyclerView.visibility = View.VISIBLE
                    mMenuVisible = false
                }
            }

            binding.btnNextPlay.id -> {
                Log.i(TAG, "btnNextPlay")

                if(mIsShuffle) {
                    Log.i(TAG, "셔플 모드 입니다.")
                } else {
                    Log.i(TAG, "셔플 모드가 아닙니다.")
                    Log.i(TAG, "현재 재생 정보[${mCurrentPlayTitle}][$mCurrentPlayTitle]")
                    var idx = 0
                    for(music in musicFiles) {
                        if(mCurrentPlayPath == music.path) {
                            Log.i(TAG, "현재 위치[$idx][${musicFiles.size}][${musicFiles[idx].title}]")
                            if(idx < musicFiles.size-1) {   //다음 위치에 파일이 있는 경우
                                mCurrentPlayTitle = musicFiles[idx+1].title
                                mCurrentPlayPath = musicFiles[idx+1].path
                                mService?.nextPlay(mCurrentPlayPath!!)
                                break
                            }
                        }

                        idx++
                    }
                }
            }

            binding.btnPrevPlay.id -> {
                Log.i(TAG, "btnPrevPlay")

                if(mIsShuffle) {
                    Log.i(TAG, "셔플 모드 입니다.")
                } else {
                    Log.i(TAG, "셔플 모드가 아닙니다.")
                    Log.i(TAG, "현재 재생 정보[${mCurrentPlayTitle}][$mCurrentPlayTitle]")
                    var idx = 0
                    for(music in musicFiles) {
                        if(mCurrentPlayPath == music.path) {
                            Log.i(TAG, "현재 위치[$idx][${musicFiles.size}][${musicFiles[idx].title}]")
                            if(idx > 0) {   //이전 위치에 파일이 있는 경우
                                mCurrentPlayTitle = musicFiles[idx - 1].title
                                mCurrentPlayPath = musicFiles[idx - 1].path
                                mService?.prevPlay(mCurrentPlayPath!!)
                                break
                            }
                        }

                        idx++
                    }
                }
            }
        }
    }

    private fun resetSeekBar() {
        mJob?.cancel()
        mJob = null
    }

    @SuppressLint("DefaultLocale")
    private fun getMusicFiles()/*: List<MusicFile>*/ {
        val directory = File(mMusicFilePath)
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles() // 디렉토리 내부 파일 리스트 가져오기
            if (files != null) {
                for (file in files) {
                    if (file.isFile && file.extension.equals("mp3", ignoreCase = true)) {
                        val mediaMetadata = mGetMediaMetadata?.retrieveMetadataFromFilePath(
                            mMusicFilePath+"/"+file.name)

                        mediaMetadata?.duration?.let {
                            MusicFile(mMusicFilePath+"/"+file.name, file.name,
                                String.format("%02d:%02d", it.toInt() / 1000 / 60, it.toInt() / 1000 % 60),
                                mediaMetadata.albumArt
                            )
                        }?.let {
                            musicFiles.add(it)
                        } // 필요한 데이터를 리스트에 추가,file.name, "00:00")) // 필요한 데이터를 리스트에 추가
                    }
                }
            } else {
                Log.i(TAG, "디렉토리에 파일이 없습니다.")
            }
        } else {
            Log.i(TAG, "디렉토리가 존재하지 않거나 유효하지 않습니다.")
        }
    }

    private fun checkNextMusic() {
        Log.i(TAG, "checkNextMusic()")

    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }
}