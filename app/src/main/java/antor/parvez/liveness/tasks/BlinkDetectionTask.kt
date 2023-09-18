package antor.parvez.liveness.tasks

import android.util.Log
import antor.parvez.liveness.detectors.DetectionUtils
import com.google.mlkit.vision.face.Face


class BlinkDetectionTask : DetectionTask {
    override fun process(face: Face): Boolean {
        val blinks =  DetectionUtils.isEyeBlinking(face)
        Log.d("TAG", "process blinks: $blinks")
       return blinks
    }


}

