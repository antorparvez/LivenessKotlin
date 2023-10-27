package antor.parvez.liveness.tasks

import antor.parvez.liveness.detectors.DetectionUtils
import com.google.mlkit.vision.face.Face

class EyeBlinkDetectionTask : DetectionTask {

    override fun process(face: Face): Boolean {
        return DetectionUtils.isFacing(face) && DetectionUtils.isEyeBlink(face)
    }
}




