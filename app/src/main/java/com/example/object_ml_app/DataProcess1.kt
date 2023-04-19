package com.example.a0413yolov8kotlin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import androidx.camera.core.ImageProxy
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.FloatBuffer
import java.util.*
import kotlin.math.max
import kotlin.math.min



class DataProcess1(val context: Context) {

    lateinit var classes: Array<String>

    companion object {
        const val BATCH_SIZE = 1  // 모델에 입력할 이미지의 개수를 지정하는 상수로, 값은 1로 지정되어 있습니다.
        const val INPUT_SIZE = 640 // 입력 이미지의 크기를 지정하는 상수로, 값은 640으로 지정되어 있습니다.
        const val PIXEL_SIZE = 3 //이미지의 픽셀 당 바이트 수를 지정하는 상수로, 값은 3으로 지정되어 있습니다. 이는 RGB 이미지를 처리할 때 사용됩니다.
        // 높이면 높을수록 정확도는 높아지지만, 처리 속도는 느려집니다.
        const val FILE_NAME = "yolov8n.onnx" //모델 파일의 이름을 지정하는 상수로, 값은 "yolov8n.onnx"로 지정되어 있습니다.
        const val LABEL_NAME = "yolov8n.txt" //클래스 레이블이 저장된 파일의 이름을 지정하는 상수로, 값은 "yolov8n.txt"로 지정되어 있습니다.
    }

    fun imageToBitmap(getbitmap: Bitmap): Bitmap {
        val bitmap = getbitmap
        val bitmap640 = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val matrix = Matrix()
        return Bitmap.createBitmap(bitmap640, 0, 0, INPUT_SIZE, INPUT_SIZE, matrix, true)
    }

    //bitmap이미지를 FloatBuffer로 변환하는 함수
    //FloatBuffer란? : FloatBuffer는 자바에서 제공하는 버퍼 클래스로, 배열의 크기를 미리 지정해두고, 배열에 데이터를 채워넣는 방식으로 동작합니다.
    fun bitmapToFloatBuffer(bitmap: Bitmap): FloatBuffer {


        // 보통 이미지 데이터는 0부터 255까지의 값을 가집니다. 이 값은 픽셀의 밝기나 색상을 나타내며, 이러한 값으로 이미지를 표현합니다. RGB (0은 검은색, 255는 흰색) (255,0,0)
        val imageSTD = 255.0f // 이미지의 표준 편차를 지정하는 상수로, 값은 255.0f로 지정되어 있습니다. 나중에 아래에서 0~255 사이의 값을 0~1 사이의 값으로 변환하기 위해 사용됩니다.

        val buffer = FloatBuffer.allocate(BATCH_SIZE * PIXEL_SIZE * INPUT_SIZE * INPUT_SIZE)
        buffer.rewind()

        val area = INPUT_SIZE * INPUT_SIZE
        val bitmapData = IntArray(area) //한 사진에서 대한 정보, 640x640 사이즈
        bitmap.getPixels(
            bitmapData,
            0,
            bitmap.width,
            0,
            0,
            bitmap.width,
            bitmap.height
        ) // 배열에 정보 담기

        //배열에서 하나씩 가져와서 buffer 에 담기
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val idx = INPUT_SIZE * i + j
                val pixelValue = bitmapData[idx]
                // 위에서부터 차례대로 R 값 추출, G 값 추출, B값 추출 -> 255로 나누어서 0~1 사이로 정규화
                buffer.put(idx, ((pixelValue shr 16 and 0xff) / imageSTD))
                buffer.put(idx + area, ((pixelValue shr 8 and 0xff) / imageSTD))
                buffer.put(idx + area * 2, ((pixelValue and 0xff) / imageSTD))
                //원리 bitmap == ARGB 형태의 32bit, R값의 시작은 16bit (16 ~ 23bit 가 R영역), 따라서 16bit 를 쉬프트
                //그럼 A값이 사라진 RGB 값인 24bit 가 남는다. 이후 255와 AND 연산을 통해 맨 뒤 8bit 인 R값만 가져오고, 255로 나누어 정규화를 한다.
                //다시 8bit 를 쉬프트 하여 R값을 제거한 G,B 값만 남은 곳에 다시 AND 연산, 255 정규화, 다시 반복해서 RGB 값을 buffer 에 담는다.
            }
        }
        buffer.rewind() // position 0
        return buffer
    }


    fun loadModel() {
        // onnx 파일 불러오기
        val assetManager = context.assets
        val outputFile = File(context.filesDir, FILE_NAME) // 파일 경로 생성 시, "/" 대신 File.separator를 사용하는 것이 플랫폼 독립적

        assetManager.open(FILE_NAME).use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
            }
        }
    }

    fun loadLabel() {
        // txt 파일 불러오기
        BufferedReader(InputStreamReader(context.assets.open(LABEL_NAME))).use { reader ->
            val classList = ArrayList<String>()
            reader.forEachLine { line ->
                classList.add(line)
            }
            classes = classList.toTypedArray()
        }
    }



    //출력 배열을 NMS를 통해 처리한 결과를 반환하는 함수

    fun outputsToNPMSPredictions(outputs: Array<*>): ArrayList<Result> {
        val confidenceThreshold = 0.60f // 확률값의 임계치
        val results = ArrayList<Result>() // 결과를 저장할 리스트
        val rows: Int // 배열의 행 개수
        val cols: Int // 배열의 열 개수

        (outputs[0] as Array<*>).also { // 출력 배열의 첫 번째 원소를 통해 행과 열 개수를 가져옴
            rows = it.size
            cols = (it[0] as FloatArray).size
        }

        // 출력 배열의 형태를 [84 8400] -> [8400 84] 로 변환
        val output = Array(cols) { FloatArray(rows) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                output[j][i] = ((((outputs[0]) as Array<*>)[i]) as FloatArray)[j]
            }
        }

        for (i in 0 until cols) {
            var detectionClass: Int = -1 // 검출된 객체의 클래스를 저장할 변수 초기화
            var maxScore = 0f // 가장 큰 확률값을 저장할 변수 초기화
            val classArray = FloatArray(classes.size) // 클래스 확률값을 저장할 배열 초기화

            // 라벨 값만 따로 빼서 1차원 배열을 만든다. (0~3은 좌표값임)
            System.arraycopy(output[i], 4, classArray, 0, classes.size)

            // 라벨 중에서 가장 큰 확률값을 선정한다.
            for (j in classes.indices) {
                if (classArray[j] > maxScore) {
                    detectionClass = j
                    maxScore = classArray[j]
                }
            }

            // 만약 확률값이 임계치(현재는 45%)를 넘어서면 해당 값을 저장한다.
            if (maxScore > confidenceThreshold) {
                val xPos = output[i][0]
                val yPos = output[i][1]
                val width = output[i][2]
                val height = output[i][3]
                // 사각형은 화면 밖으로 나갈 수 없으므로 화면을 넘어가면 최대 화면 값을 가지게 한다.
                val rectF = RectF(
                    max(0f, xPos - width / 2f),
                    max(0f, yPos - height / 2f),
                    min(INPUT_SIZE - 1f, xPos + width / 2f),
                    min(INPUT_SIZE - 1f, yPos + height / 2f)
                )
                val result = Result(detectionClass, maxScore, rectF)
                results.add(result)
            }
        }

        return nms(results) // 결과를 non-maximum suppression(NMS) 알고리즘으로 정제하여 반환
    }

//    위의 코드는 이 코드는 object detection 모델의 출력값(outputs)을 입력으로 받아, 확률값이 일정 임계치 이상인 객체만을 탐지하여 결과를 반환하는 함수입니다.
//
//     먼저, 함수 내에서는 outputs 배열의 첫 번째 원소를 통해 배열의 행(row)과 열(column)의 개수를 가져와 rows와 cols에 저장합니다. 그리고 출력 배열의 형태를
//     [84 8400] -> [8400 84] 로 변환하여 output 변수에 저장합니다. 이후, 열(column)의 개수만큼 반복문을 돌면서, 각 객체마다 탐지된 클래스를 검출하고, 해당 클래스의
//     확률값이 임계치를 넘으면 탐지된 객체의 좌표와 클래스, 확률값을 결과 리스트(results)에 저장합니다.
//
//      위 코드에서 사용된 알고리즘 중에서 중요한 것은 non-maximum suppression(NMS)입니다. NMS는 박스 형태의 객체가 중복되어 탐지될 때, 가장 정확도가 높은 박스를 선택하고 다른
//      박스를 제거하는 알고리즘입니다. 결과 리스트(results)에 대해 NMS 알고리즘을 적용한 결과를 반환합니다.



    //NMS(Normalized Maximum Suppression)은 Object Detection에서 중복된 객체를 제거하는 알고리즘입니다.
    private fun nms(results: ArrayList<Result>): ArrayList<Result> {
        val list = ArrayList<Result>()

        for (i in classes.indices) {
            // 1. 클래스 (라벨들) 중에서 가장 높은 확률값을 가졌던 클래스 찾기
            val pq = PriorityQueue<Result>(50) { o1, o2 ->
                o1.score.compareTo(o2.score)
            }
            val classResults = results.filter { it.classIndex == i }
            pq.addAll(classResults)

            // NMS 처리
            while (pq.isNotEmpty()) {
                // 큐 안에 속한 최대 확률값을 가진 class 저장
                val detections = pq.toTypedArray()
                val max = detections[0]
                list.add(max)
                pq.clear()

                // 교집합 비율 확인하고 50% 넘기면 제거
                for (k in 1 until detections.size) {
                    val detection = detections[k]
                    val rectF = detection.rectF
                    val iouThresh = 0.5f
                    if (boxIOU(max.rectF, rectF) < iouThresh) {
                        pq.add(detection)
                    }
                }
            }
        }
        return list
    }

    // 겹치는 비율 (교집합/합집합)을 계산하는 함수
    private fun boxIOU(a: RectF, b: RectF): Float {
        return boxIntersection(a, b) / boxUnion(a, b)
    }

    // 교집합을 계산하는 함수
    private fun boxIntersection(a: RectF, b: RectF): Float {
        // x1, x2 == 각 rect 객체의 중심 x or y값, w1, w2 == 각 rect 객체의 넓이 or 높이
        val w = overlap(
            (a.left + a.right) / 2f, a.right - a.left,
            (b.left + b.right) / 2f, b.right - b.left
        )
        val h = overlap(
            (a.top + a.bottom) / 2f, a.bottom - a.top,
            (b.top + b.bottom) / 2f, b.bottom - b.top
        )

        return if (w < 0 || h < 0) 0f else w * h
    }

    // 합집합을 계산하는 함수
    private fun boxUnion(a: RectF, b: RectF): Float {
        val i: Float = boxIntersection(a, b)
        return (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
    }

    // 서로 겹치는 부분의 길이를 계산하는 함수
    private fun overlap(x1: Float, w1: Float, x2: Float, w2: Float): Float {
        val l1 = x1 - w1 / 2
        val l2 = x2 - w2 / 2
        val left = max(l1, l2)
        val r1 = x1 + w1 / 2
        val r2 = x2 + w2 / 2
        val right = min(r1, r2)
        return right - left
    }

    //출력 배열을 NMS를 통해 처리한 결과를 반환하는 함수 끝

//    출력 배열을 NMS를 통해 처리하는 코드는 Object Detection 모델의 출력값을 이용하여, NMS(Non-Maximum Suppression) 알고리즘을 수행하는 함수입니다.
//
//    Object Detection 모델은 이미지를 입력받아서, 이미지 내에 존재하는 객체의 위치와 클래스를 예측합니다. 이때, 한 객체가 여러 번 예측되는 경우가 발생할 수 있습니다. 예를 들어, 한 자동차가 이미지에서 여러 개로 보이는 경우 등이 있을 수 있습니다.
//
//    NMS 알고리즘은 이러한 중복 예측된 객체를 제거하여 정확한 예측 결과를 얻기 위해 사용됩니다. 이 코드에서는, NMS 알고리즘을 수행하는 nms() 함수와, NMS 이후의 결과를 리턴하는 outputsToNPMSPredictions() 함수가 포함되어 있습니다.
//
//    outputsToNPMSPredictions() 함수는 Object Detection 모델의 출력값인 outputs를 인자로 받아서, 이를 NMS 알고리즘을 적용하여 객체의 위치와 클래스를 예측하는 Result 객체들의 리스트를 반환합니다.
//
//    nms() 함수는 outputsToNPMSPredictions() 함수에서 반환된 Result 객체들을 입력으로 받아서, 이를 NMS 알고리즘을 적용하여 중복 예측된 객체들을 제거한 후, 정제된 결과를 반환합니다.
//
//    NMS 알고리즘은 각각의 클래스에 대해 다음과 같은 순서로 진행됩니다.
//
//    해당 클래스에 속한 예측 결과 중에서 가장 확률이 높은 결과를 우선적으로 선택합니다.
//    나머지 예측 결과들 중에서, 선택한 결과와 IoU (Intersection over Union) 값이 일정 이상인 결과들을 제거합니다.
//    이러한 과정을 거쳐서 선택된 결과들이 최종적으로 반환됩니다.
}