package antor.parvez.liveness

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Outline
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@ExperimentalGetImage
class LivenessCheckActivity : AppCompatActivity() {
    private lateinit var previewImageView: ImageView
    private lateinit var viewFinder: PreviewView
    private lateinit var resultsTextView: TextView
    private lateinit var captureBtn: TextView
    private lateinit var preview: Preview
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageAnalysis: ImageAnalysis
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)
    private val executor: Executor = Executors.newSingleThreadExecutor()

    private val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    private var isFaceDetected = false
    private var isFaceAngleDetected = false
    private var isEyeBlinkDetected = false
    private var isSmileDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liveness_check)

        preview = Preview.Builder().build()
        viewFinder = findViewById(R.id.viewFinder)
        previewImageView = findViewById(R.id.previewImage)
        resultsTextView = findViewById(R.id.resultsTextView)
        captureBtn = findViewById(R.id.captureBtn)


        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else {
            startCamera()
        }

        // Apply a round outline to the PreviewView
        viewFinder.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2.0f)
            }
        }
        viewFinder.clipToOutline = true

    }

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        preview.setSurfaceProvider(viewFinder.surfaceProvider)

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        imageAnalysis.setAnalyzer(executor, FaceAnalyzer())

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector, // Use the front camera
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Camera initialization failed: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun checkSuccessDetection() {
        if (isFaceDetected && isFaceAngleDetected && isEyeBlinkDetected && isSmileDetected) {
            // Show "Success Detection" message
            Toast.makeText(this, "Success Detection", Toast.LENGTH_LONG).show()
        }
    }

    private var currentDetectionStep = DetectionStep.FACE


    inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        private var leftHeadAngleDetected = false
        private var rightHeadAngleDetected = false

        override fun analyze(image: ImageProxy) {
            val mediaImage = image.image
            if (mediaImage != null) {
                val inputImage =
                    InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)

                faceDetector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        for (face in faces) {
                            val headAngle = face.headEulerAngleY

                            when (currentDetectionStep) {
                                DetectionStep.FACE -> {
                                    // Face detection
                                    isFaceDetected = true
                                    updateResultsText("Face detected. Detecting head angle...")
                                    currentDetectionStep = DetectionStep.FACE_ANGLE
                                }
                                DetectionStep.FACE_ANGLE -> {
                                    if (headAngle < -35) {
                                        leftHeadAngleDetected = true
                                        updateResultsText("Head turned left. Detecting right head angle...")
                                    }
                                    if (headAngle > 35) {
                                        rightHeadAngleDetected = true
                                        updateResultsText("Head turned right. Detecting left head angle...")
                                    }

                                    if (leftHeadAngleDetected && rightHeadAngleDetected) {
                                        currentDetectionStep = DetectionStep.EYE_BLINK
                                        updateResultsText("Head angle detected. Detecting eye blink...")
                                    }
                                }
                                DetectionStep.EYE_BLINK -> {
                                    // Eye blink detection
                                    val isEyeBlinking =
                                        face.leftEyeOpenProbability!! < 0.3 && face.rightEyeOpenProbability!! < 0.3
                                    if (isEyeBlinking) {
                                        isEyeBlinkDetected = true
                                        updateResultsText("Eye blink detected. Detecting smile...")
                                        currentDetectionStep = DetectionStep.SMILE
                                    }
                                }
                                DetectionStep.SMILE -> {
                                    // Smile detection
                                    if (face.smilingProbability!! > 0.5) {
                                        isSmileDetected = true
                                        updateResultsText("Smile detected. Success detection...")
                                        currentDetectionStep = DetectionStep.SUCCESS
                                    }
                                }
                                DetectionStep.SUCCESS -> {
                                    // All steps are done, close the detection process
                                    updateResultsText("Success Detection, Take a selfie")
                                    cameraProvider.unbindAll()

                                }
                            }

                            checkSuccessDetection()
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        // Log and display an error message using a toast
                        Log.e("LivenessCheck", "Face detection failed: ${e.message}")
                        Toast.makeText(
                            applicationContext,
                            "Face detection failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnCompleteListener {
                        image.close()
                    }
            } else {
                // Log and display an error message using a toast
                Log.e("LivenessCheck", "Image is null")
                Toast.makeText(applicationContext, "Image is null", Toast.LENGTH_LONG).show()
            }
        }

        private fun updateResultsText(text: String) {
            runOnUiThread {
                resultsTextView.text = text
            }
        }
    }


    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }


}


enum class DetectionStep {
    FACE,
    FACE_ANGLE,
    EYE_BLINK,
    SMILE,
    SUCCESS
}


