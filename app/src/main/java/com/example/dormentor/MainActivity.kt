package com.example.dormentor

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.dormentor.alerts.AlertViewModel
import com.example.dormentor.alerts.AlertsController
import com.example.dormentor.databinding.ActivityMainBinding
import com.example.dormentor.measuers.FOMController
import com.example.dormentor.measuers.PerclosController
import com.example.dormentor.ui.BitmapViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.text.DecimalFormat
import java.math.RoundingMode


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor:ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //ViewModel to display the partial images and the results of their classification
        val bitmapViewModel : BitmapViewModel = ViewModelProvider(this).get(BitmapViewModel::class.java)

//        used to round probabilities and percentages
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.DOWN

        val eyeBitmapObserver = Observer<Bitmap> {
           viewBinding.eyeImageView.setImageBitmap(it)
        }
        bitmapViewModel.getEyeBitmap().observe(this,eyeBitmapObserver)

        val mouthBitmapObserver = Observer<Bitmap> {
            viewBinding.mouthImageView.setImageBitmap(it)
        }
        bitmapViewModel.getMouthBitmap().observe(this,mouthBitmapObserver)

        val eyeStatLabelObserver = Observer<String> {
            if (it == "Closed") {
                viewBinding.statusEyeIcon.setImageResource(R.mipmap.ic_closed_eye_foreground)
            } else {
                viewBinding.statusEyeIcon.setImageResource(R.mipmap.ic_open_eye_foreground)
            }
        }
        bitmapViewModel.getEyeStatusLabel().observe(this,eyeStatLabelObserver)

        val eyeStatScoreObserver = Observer<Float> {
            var perc = df.format(it.toFloat()*100.0)
            viewBinding.ScoreEye.setText("$perc %")
        }
        bitmapViewModel.getEyeStatusScore().observe(this,eyeStatScoreObserver)

        val mouthStatLabelObserver = Observer<String> {
            if (it == "no_yawn") {
                viewBinding.statusMouthIcon.setImageResource(R.mipmap.ic_no_yawn_foreground)
            } else {
                viewBinding.statusMouthIcon.setImageResource(R.mipmap.ic_yawn_foreground)
            }
        }
        bitmapViewModel.getmouthStatusLabel().observe(this,mouthStatLabelObserver)

        val mouthStatScoreObserver = Observer<Float> {
            var perc = df.format(it.toFloat()*100.0)
            viewBinding.ScoreMouth.setText("$perc %")
        }
        bitmapViewModel.getmouthStatusScore().observe(this,mouthStatScoreObserver)



        val elapsedTimeObserver = Observer<Long> {
            viewBinding.ElapsedTime.setText(it.toString()+" ms")
        }
        bitmapViewModel.getElapsed().observe(this,elapsedTimeObserver)


        viewBinding.debugButton.setOnClickListener {
            bitmapViewModel.changeIsdebugEnabled()
        }

        val debugModeObserver = Observer<Boolean> {
            var visibility:Int
            if (it) {
                visibility = View.VISIBLE
            } else {
                visibility = View.INVISIBLE
            }
            viewBinding.eyeImageView.visibility = visibility
            viewBinding.mouthImageView.visibility = visibility
            viewBinding.statusEyeIcon.visibility = visibility
            viewBinding.statusMouthIcon.visibility = visibility
            viewBinding.ScoreEye.visibility = visibility
            viewBinding.ScoreMouth.visibility = visibility
            viewBinding.ElapsedTime.visibility = visibility
        }
        bitmapViewModel.getIsdebugEnabled().observe(this,debugModeObserver)



        val alertViewModel : AlertViewModel = ViewModelProvider(this).get(AlertViewModel::class.java)
        AlertsController.alertViewModel = alertViewModel






//      PERIODIC CALL TO UPDATE MEASURES
        val periodicHandler = Handler(Looper.getMainLooper())

        val periodicRunnable = object : Runnable {
            override fun run(){
                PerclosController.updatePerclos()
                FOMController.updateFOM()
                periodicHandler.postDelayed(this,10*1000)
            }
        }
        periodicHandler.postDelayed({
            periodicRunnable.run()
            },5000)


        //      Eye visual alert************************************************
        val eyeAlertHiderRunnable = object : Runnable {
            override fun run() {
                viewBinding.eyeDangerContainer.visibility = View.INVISIBLE
            }
        }
        val perclosVisualAlert = Observer<Boolean> {
            if (it) {
                viewBinding.eyeDangerContainer.visibility = View.VISIBLE
                periodicHandler.postDelayed({
                    eyeAlertHiderRunnable.run()
                },7000)
            }
        }
        alertViewModel.getPerclosAlert().observe(this,perclosVisualAlert)

        //      YAWN visual alert************************************************
        val yawnAlertHiderRunnable = object : Runnable {
            override fun run() {
                viewBinding.yawnDangerContainer.visibility = View.INVISIBLE
            }
        }

        val fomVisualAlert = Observer<Boolean> {
            if (it) {
                viewBinding.yawnDangerContainer.visibility = View.VISIBLE
                periodicHandler.postDelayed({
                    yawnAlertHiderRunnable.run()
                },7000)
            }
        }
        alertViewModel.getFomAlert().observe(this,fomVisualAlert)


        //      AUDIO ALERT***************************************************

//        we prepare id for each audio resource because doing it dynamically is slow
        val alarmIds = arrayListOf(R.raw.alarm1,R.raw.alarm2,R.raw.alarm3,R.raw.alarm4)
        val mediaPlayers = arrayListOf<MediaPlayer>()
        for (id in alarmIds){
            mediaPlayers.add(MediaPlayer.create(this, id))
        }

        var choosedAudioIdIndex = 0

        val alertObserver = Observer<Boolean> {
            if (it) {
                Log.d(TAG,choosedAudioIdIndex.toString())
                mediaPlayers[choosedAudioIdIndex].start()
                choosedAudioIdIndex = (choosedAudioIdIndex+1) % 4
            }
        }
        alertViewModel.getisAlert().observe(this,alertObserver)

//        CAMERA MANAGEMENT
            //check if we already have the permissions needed, otherwise request them
            if(allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }

            //Executor thread used for ImagesAnalyzer (should be killed onDestroy)
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    //callback for requesting required permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_PERMISSIONS) { //Check if the request code is correct; ignore it otherwise.
            if(allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions non accordées par l'utilisateur.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    //function called after the dialog of requesting permissions is showed to the user: it checks if the permission is granted for each required one
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this,
            it) == PERMISSION_GRANTED
    }

    private fun startCamera() {
        //ProcessCameraProvider is a singleton used to bind Cameras lifeCycle to any lifeCycleOwner (this activity for this case)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                    // Selecting the front camera as a default
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    //Preview use case
                    val previewUseCase = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
                        }

                    //Images Analysis use case
                    val imageAnalysisUseCase = ImageAnalysis.Builder()
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, FaceDetector(this))
                        }

                    try {
                        //Unbind all the useCases before binding
                        cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            previewUseCase,
                            imageAnalysisUseCase
                        )
                    } catch (e: Exception) {
                        Log.e(TAG,"Preview use case binding failed",e)
                    }
                }, ContextCompat.getMainExecutor(this)
            )

    }


    companion object{
        private const val TAG = "DormentorMainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 7 //a specific code to our app (used to confirm that the permission in question is requested by our app)
        private  val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).toTypedArray()
    }
}
