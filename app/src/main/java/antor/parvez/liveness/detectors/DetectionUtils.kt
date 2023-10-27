package antor.parvez.liveness.detectors

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.acos
import kotlin.math.sqrt

object DetectionUtils {

    fun isFacing(face: Face): Boolean {
        return face.headEulerAngleZ < 7.78f && face.headEulerAngleZ > -7.78f
                && face.headEulerAngleY < 11.8f && face.headEulerAngleY > -11.8f
                && face.headEulerAngleX < 19.8f && face.headEulerAngleX > -19.8f
    }

    fun isMouthOpened(face: Face): Boolean {
        val left = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position ?: return false
        val right = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position ?: return false
        val bottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position ?: return false

        // Square of lengths be a2, b2, c2
        val a2 = lengthSquare(right, bottom)
        val b2 = lengthSquare(left, bottom)
        val c2 = lengthSquare(left, right)

        // length of sides be a, b, c
        val a = sqrt(a2)
        val b = sqrt(b2)

        // From Cosine law
        val gamma = acos((a2 + b2 - c2) / (2 * a * b))

        // Converting to degrees
        val gammaDeg = gamma * 180 / Math.PI
        return gammaDeg < 115f
    }

    private fun lengthSquare(a: PointF, b: PointF): Float {
        val x = a.x - b.x
        val y = a.y - b.y
        return x * x + y * y
    }


    //Abstract the detection area into an 8x8 grid.
    //The center point of the face is limited to a 4x4 square in the middle.
    //The face cannot be larger than 6x6 or smaller than 3x3.
    fun isFaceInDetectionRect(face: Face, detectionSize: Int): Boolean {
        val fRect = face.boundingBox
        val fx = fRect.centerX()
        val fy = fRect.centerY()
        val gridSize = detectionSize / 8
        if (fx < gridSize * 2 || fx > gridSize * 6 || fy < gridSize * 2 || fy > gridSize * 6) {
            Log.d("isFaceInDetectionRect", "face center point is out of rect: ($fx, $fy)")
            return false
        }

        val fw = fRect.width()
        val fh = fRect.height()
        if (fw < gridSize * 3 || fw > gridSize * 6 || fh < gridSize * 3 || fh > gridSize * 6) {
            Log.d("isFaceInDetectionRect", "unexpected face size: $fw x $fh")
            return false
        }
        return true
    }

    fun isEyeBlink(face: Face): Boolean {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        // Check if any of the required landmarks are missing
        if (leftEye == null || rightEye == null) {
            return false
        }

        return face.leftEyeOpenProbability!! < 0.3 && face.rightEyeOpenProbability!! < 0.3
    }



}