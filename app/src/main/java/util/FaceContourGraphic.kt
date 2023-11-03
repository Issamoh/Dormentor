package util

import android.graphics.*
import androidx.annotation.ColorInt
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour

class FaceContourGraphic(
    overlay: GraphicOverlay,
    private val face: Face,
    private val imageRect: Rect
) : GraphicOverlay.Graphic(overlay) {

    private val facePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint

    init {
        val selectedColor = Color.WHITE

        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor

        idPaint = Paint()
        idPaint.color = selectedColor
        idPaint.textSize = ID_TEXT_SIZE

        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
    }

    private fun Canvas.drawFace(facePosition: Int, @ColorInt selectedColor: Int) {
        val contour = face.getContour(facePosition)
        val path = Path()
        contour?.points?.forEachIndexed { index, pointF ->
            if (index == 0) {
                path.moveTo(
                    pointF.x,
                    pointF.y
                )
            }
            path.lineTo(
                pointF.x,
                pointF.y
            )
        }
        val paint = Paint().apply {
            color = selectedColor
            style = Paint.Style.STROKE
            strokeWidth = BOX_STROKE_WIDTH
        }
        drawPath(path, paint)
    }

    override fun draw(canvas: Canvas?) {

        val contours = face.allContours

        contours.forEach {

            val contoursF = rectifyPoints(it.points,  imageRect.height().toFloat(), imageRect.width().toFloat()  )
            face.getContour(it.faceContourType)?.points?.clear()
            face.getContour(it.faceContourType)?.points?.addAll(contoursF)

//            contoursF.forEach { point ->
//                canvas?.drawCircle(point.x, point.y, FACE_POSITION_RADIUS, facePositionPaint)
//            }
        }

        // face
        canvas?.drawFace(FaceContour.FACE, Color.WHITE)

        // left eye
        canvas?.drawFace(FaceContour.LEFT_EYEBROW_TOP, Color.WHITE)
        canvas?.drawFace(FaceContour.LEFT_EYE, Color.WHITE)
        canvas?.drawFace(FaceContour.LEFT_EYEBROW_BOTTOM, Color.WHITE)

//        // right eye
//        canvas?.drawFace(FaceContour.RIGHT_EYE, Color.DKGRAY)
//        canvas?.drawFace(FaceContour.RIGHT_EYEBROW_BOTTOM, Color.GRAY)
//        canvas?.drawFace(FaceContour.RIGHT_EYEBROW_TOP, Color.GREEN)

//        // nose
//        canvas?.drawFace(FaceContour.NOSE_BOTTOM, Color.LTGRAY)
//        canvas?.drawFace(FaceContour.NOSE_BRIDGE, Color.MAGENTA)

        // rip
        canvas?.drawFace(FaceContour.LOWER_LIP_BOTTOM, Color.WHITE)
        canvas?.drawFace(FaceContour.LOWER_LIP_TOP, Color.WHITE)
        canvas?.drawFace(FaceContour.UPPER_LIP_BOTTOM, Color.WHITE)
        canvas?.drawFace(FaceContour.UPPER_LIP_TOP, Color.WHITE)
    }

    companion object {
        private const val FACE_POSITION_RADIUS = 3.0f
        private const val ID_TEXT_SIZE = 30.0f
        private const val BOX_STROKE_WIDTH = 9.0f
    }

}