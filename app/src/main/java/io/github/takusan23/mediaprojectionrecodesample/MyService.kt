package io.github.takusan23.mediaprojectionrecodesample

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.lang.RuntimeException
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.media.AudioPlaybackCaptureConfiguration


class MyService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    lateinit var mMediaRecorder: MediaRecorder
    lateinit var projectionManager: MediaProjectionManager
    lateinit var projection: MediaProjection

    val id = "rec_id"

    lateinit var data: Intent
    var code = 114

    var height = 1000
    var width = 1000
    var dpi = 1000

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        data = intent?.getParcelableExtra("data")!!
        code = intent.getIntExtra("code", 114)
        height = intent.getIntExtra("height", 114)
        width = intent.getIntExtra("width", 114)
        dpi = intent.getIntExtra("dpi", 114)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationManager =
                application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(id, "録画通知", NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannel(channel)

            //新規→サービス　から作るとAndroidManifestのServiceに自動で登録してくれるんだけど
            //余計なパラメーター？が入ったせいで「startForeground」が動かなかったのでメモ

            val notification = NotificationCompat.Builder(applicationContext, id)
                .setContentText("録画です")
                .setContentTitle("録画")
                .setSmallIcon(R.drawable.ic_movie_creation_black_24dp)
                .build()

            startForeground(1, notification)

            //録画
            startREC()

        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("終了")
        try {
            mMediaRecorder.stop()
            mMediaRecorder.release()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }

    }


    @TargetApi(Build.VERSION_CODES.O)
    fun startREC() {

        println(height)
        println(width)

        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        //codeはActivity.RESULT_OKとかが入る。騙された。時間返せ！
        projection =
            projectionManager.getMediaProjection(code, data)


        mMediaRecorder = MediaRecorder()
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mMediaRecorder.setVideoEncodingBitRate(1024 * 1000)
        mMediaRecorder.setVideoFrameRate(30)
        mMediaRecorder.setVideoSize(1400, 2800)
        mMediaRecorder.setAudioSamplingRate(44100)
        mMediaRecorder.setOutputFile(getFilePath())
        mMediaRecorder.prepare()

        val virtualDisplay = projection.createVirtualDisplay(
            "recode",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder.surface,
            null,
            null
        )

        //開始
        mMediaRecorder.start()


/*
        val audioPlaybackCaptureConfiguration =
            AudioPlaybackCaptureConfiguration.Builder(projection).build()

        val recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioPlaybackCaptureConfig(audioPlaybackCaptureConfiguration)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(32000)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(2)
            .build()


        recorder.startRecording()
*/


    }

    fun getFilePath(): File {
        //ScopedStorageで作られるサンドボックスへのぱす
        val scopedStoragePath = getExternalFilesDir(null)
        //写真ファイル作成
        val file = File("${scopedStoragePath?.path}/test.mp4")
        return file
    }

}
