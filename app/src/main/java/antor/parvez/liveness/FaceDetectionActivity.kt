package antor.parvez.liveness

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import antor.parvez.liveness.databinding.ActivityFaceDetectionBinding
import antor.parvez.liveness.detectors.FaceAnalyzer
import antor.parvez.liveness.detectors.LivenessDetector
import antor.parvez.liveness.tasks.*
import java.io.File

class FaceDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceDetectionBinding
    private lateinit var cameraController: LifecycleCameraController
    private var imageFiles = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Permission deny", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.launch(Manifest.permission.CAMERA)

        binding.cameraPreview.clipToOutline = true
        binding.cameraPreview.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2.0f)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startCamera() {
        cameraController = LifecycleCameraController(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            FaceAnalyzer(buildLivenessDetector())
        )
        cameraController.bindToLifecycle(this)
        binding.cameraPreview.controller = cameraController
    }


     private fun buildLivenessDetector(): LivenessDetector {
         val listener = object : LivenessDetector.Listener {
             @SuppressLint("SetTextI18n")
             override fun onTaskStarted(task: DetectionTask) {
                 when (task) {
                     is FacingDetectionTask ->
                         binding.guide.text = "ক্যামেরার দিকে তাকান, অথবা ক্যামেরা ঠিক করুন"

                    /* is BlinkDetectionTask ->
                         binding.guide.text = "Blink your eyes"*/

                     is ShakeDetectionTask ->
                         binding.guide.text = "আস্তে আস্তে আপনি আপনার মাথা বাম-ডান দিকে নিন"
                     is MouthOpenDetectionTask ->
                         binding.guide.text = "দয়া করে আপনার মুখ খোলুন"
                     is SmileDetectionTask ->
                         binding.guide.text = "দয়া করে একটু হাসুন"
                 }
             }

             override fun onTaskCompleted(task: DetectionTask, isLastTask: Boolean) {
                 takePhoto(File(cacheDir, "${System.currentTimeMillis()}.jpg")) {
                     imageFiles.add(it.absolutePath)
                     if (isLastTask) {
                         finishForResult()
                     }
                 }
             }

             override fun onTaskFailed(task: DetectionTask, code: Int) {
                 if (code == LivenessDetector.ERROR_MULTI_FACES) {
                     Toast.makeText(
                         this@FaceDetectionActivity,
                         "একসাথে একটির বেশি চেহারা দেওয়া যাবে না !!!",
                         Toast.LENGTH_LONG
                     ).show()
                 }
             }
         }

         return LivenessDetector(
             FacingDetectionTask(),
             ShakeDetectionTask(),
             MouthOpenDetectionTask(),
             SmileDetectionTask()
         ).also { it.setListener(listener) }
     }

    private fun finishForResult() {
        val result = ArrayList(imageFiles.takeLast(4))
        setResult(RESULT_OK, Intent().putStringArrayListExtra(ResultContract.RESULT_KEY, result))
        finish()
    }


    private fun takePhoto(file: File, onSaved: (File) -> Unit) {
        cameraController.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(e: ImageCaptureException) {
                    e.printStackTrace()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(file)
                }
            }
        )
    }

    class ResultContract : ActivityResultContract<Any?, List<String>?>() {

        companion object {
            const val RESULT_KEY = "images"
        }

        override fun createIntent(context: Context, input: Any?): Intent {
            return Intent(context, FaceDetectionActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): List<String>? {
            if (resultCode == RESULT_OK && intent != null) {
                return intent.getStringArrayListExtra(RESULT_KEY)
            }
            return null
        }
    }
}