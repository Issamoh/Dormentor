package com.example.dormentor

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.dormentor.databinding.ActivityMainBinding
import com.example.dormentor.ui.BitmapViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor:ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        //ViewModel to display the image when ready
        val bitmapViewModel : BitmapViewModel = ViewModelProvider(this).get(BitmapViewModel::class.java)

        val eyeBitmapObserver = Observer<Bitmap> {
           viewBinding.eyeImageView.setImageBitmap(it)
        }
        bitmapViewModel.getEyeBitmap().observe(this,eyeBitmapObserver)
        val mouthBitmapObserver = Observer<Bitmap> {
            Log.d(TAG,"image changed")
            viewBinding.mouthImageView.setImageBitmap(it)
        }
        bitmapViewModel.getMouthBitmap().observe(this,mouthBitmapObserver)

        val EyeStatLabelObserver = Observer<String> {
            viewBinding.LabelEye.setText(it)
        }
        bitmapViewModel.getEyeStatusLabel().observe(this,EyeStatLabelObserver)

        val EyeStatScoreObserver = Observer<Float> {
            viewBinding.ScoreEye.setText(it.toString())
        }
        bitmapViewModel.getEyeStatusScore().observe(this,EyeStatScoreObserver)

        viewBinding.retryButton.setOnClickListener {
            bitmapViewModel.changeIsBitmapCreated()
        }

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