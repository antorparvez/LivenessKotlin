package antor.parvez.liveness.tasks

import antor.parvez.liveness.detectors.DetectionUtils
import antor.parvez.liveness.tasks.DetectionTask
import com.google.mlkit.vision.face.Face

class MouthOpenDetectionTask : DetectionTask {

    override fun process(face: Face): Boolean {
        return DetectionUtils.isFacing(face) && DetectionUtils.isMouthOpened(face)
    }
}