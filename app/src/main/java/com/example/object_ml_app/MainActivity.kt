package com.example.object_ml_app
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import android.Manifest
import android.app.Activity

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.a0328kotlin_recy.MainAdapter
import com.example.a0328kotlin_recy.MainData
import com.example.a0413yolov8kotlin.DataProcess
import com.example.a0413yolov8kotlin.DataProcess2
import com.example.object_ml_app.ml.Newfruit
import com.example.object_ml_app.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

// MainActivity 클래스 선언 (AppCompatActivity를 상속받음)
class MainActivity : AppCompatActivity(),TextToSpeech.OnInitListener {


//    val: 불변(Immutable) 변수로, 값의 읽기만 허용되는 변수. 값(Value)의 약자이다.
//    var: 가변(Mutable) 변수로, 값의 읽기와 쓰기가 모두 허용되는 변수. 변수(Variable)의 약자이다


    // 필요한 변수들 미리 선언 (나중에 초기화)
    lateinit var imageView: ImageView
    lateinit var bitmap: Bitmap
    lateinit var button1: Button
    lateinit var button2: Button
    lateinit var listbtn: Button

    lateinit var model: SsdMobilenetV11Metadata1
    lateinit var labels : List<String>



    lateinit var mainAdapter: MainAdapter
    lateinit var rv_profile: RecyclerView
    val datas = mutableListOf<MainData>()



    private lateinit var textView: TextView

    private val dataProcess = DataProcess(context = this)
    private val dataProcess2 = DataProcess2(context = this)
    private lateinit var session: OrtSession
    private lateinit var ortEnvironment: OrtEnvironment
    private lateinit var session2: OrtSession
    private lateinit var ortEnvironment2: OrtEnvironment

    // Paint 객체 생성
    val paint = Paint()



    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val ACTION_IMAGE_CAPTURE = 102
        private const val CAMERA_PERMISSION_REQUEST_CODE = 102
    }

    // 감지된 객체마다 다른 색상을 설정하기 위한 리스트 생성
    val colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK, Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.LTGRAY
    )



    private var tts: TextToSpeech? = null

    // 이미지 전처리를 위한 ImageProcessor 객체 생성
    // ResizeOp을 적용하여 이미지 크기 조절
    val imageProcessor =
        ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()



    // 액티비티 생성 시 호출되는 메소드
    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)
        // activity_main.xml을 화면에 표시
        setContentView(R.layout.activity_main)


        rv_profile = findViewById(R.id.rv_profile)

        initRecycler()




        listbtn = findViewById(R.id.listBtn)


        listbtn.setOnClickListener {
            val intent = Intent(this, WordListActivity::class.java)
            startActivity(intent)
        }

        imageView = findViewById(R.id.imageView)
        button1 = findViewById(R.id.loadBtn)
        button2 = findViewById(R.id.takeBtn)
//        textView = findViewById(R.id.textView)

        dataProcess.loadModel() // onnx 모델 불러오기
        dataProcess.loadLabel() // coco txt 파일 불러오기


        try {
            // ortEnvironment 변수 초기화
            ortEnvironment = OrtEnvironment.getEnvironment()
            session = ortEnvironment.createSession(
                this.filesDir.absolutePath.toString() + "/" + DataProcess.FILE_NAME,
                OrtSession.SessionOptions()
            )
        } catch (e: OrtException) {
            // 예외 처리: ONNX 모델 로드 및 세션 생성 시 예외 처리
            e.printStackTrace()
            // 예외 처리에 따른 처리 로직 추가
        }


        // 2번모델용 시작
        dataProcess2.loadModel() // onnx 모델 불러오기
        dataProcess2.loadLabel() // coco txt 파일 불러오기


        try {
            // ortEnvironment2 변수 초기화
            ortEnvironment2 = OrtEnvironment.getEnvironment()
            session2 = ortEnvironment2.createSession(
                this.filesDir.absolutePath.toString() + "/" + DataProcess2.FILE_NAME2,
                OrtSession.SessionOptions()
            )
        } catch (e: OrtException) {
            // 예외 처리: ONNX 모델 로드 및 세션 생성 시 예외 처리
            e.printStackTrace()
            // 예외 처리에 따른 처리 로직 추가
        }
        // 2번모델용 끝
        button1.setOnClickListener {
            openGallery()
        }

        button2.setOnClickListener {
            takePhoto()
        }

        // 카메라 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // 사용자가 권한을 거부하고 "다시 묻지 않음" 옵션을 선택한 경우에 대한 처리 로직 추가
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            }
        }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.KOREAN)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            } else {
//                btnSpeak!!.isEnabled = true
            }
        }
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, ACTION_IMAGE_CAPTURE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            // 받아온 이미지를 uri 변수에 저장
            val uri = data?.data

            // MediaStore를 사용하여 uri로부터 이미지를 bitmap 변수에 저장
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

            // 이미지의 방향 정보를 읽어옴
            val inputStream = contentResolver.openInputStream(uri!!)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            //이미지 회전 방지코드
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            }

            //이미지 회전
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // 이미지 처리 함수 호출
            imageProcess(bitmap)

        } else if (requestCode == ACTION_IMAGE_CAPTURE) {
            // 사진을 찍은 결과값을 bitmap 변수에 저장
            bitmap = data?.extras?.get("data") as Bitmap

            //이미지 퀄리티 높이기
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()
            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

            //이미지의 방향 정보를 읽어옴
            val exif = ExifInterface(ByteArrayInputStream(byteArray))
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            }
            //이미지 회전
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            saveFile(RandomFileName(), "image/jpeg", bitmap)
            // 이미지 처리 함수 호출
            imageProcess(bitmap)
        }

    }


    private fun imageProcess(bitmap0: Bitmap) {

        val predictedClassNames = ArrayList<String>() // ArrayList to store predicted class names

        val bitmap1 = dataProcess.imageToBitmap(bitmap0)
        val bitmap2 = dataProcess2.imageToBitmap(bitmap0)


        val floatBuffer1 = dataProcess.bitmapToFloatBuffer(bitmap1)
        val floatBuffer2 = dataProcess2.bitmapToFloatBuffer(bitmap2)
        val inputName = session.inputNames.iterator().next() // session 이름
        val inputName2 = session2.inputNames.iterator().next() // session 이름

        // 모델의 요구 입력값 [1 3 640 640] [배치 사이즈, 픽셀(RGB), 너비, 높이], 모델마다 크기는 다를 수 있음.
        val shape = longArrayOf(
            DataProcess.BATCH_SIZE.toLong(),
            DataProcess.PIXEL_SIZE.toLong(),
            DataProcess.INPUT_SIZE.toLong(),
            DataProcess.INPUT_SIZE.toLong()
        )
        val inputTensor1 = OnnxTensor.createTensor(ortEnvironment, floatBuffer1, shape)
        val inputTensor2 = OnnxTensor.createTensor(ortEnvironment2, floatBuffer2, shape)
        val resultTensor1 = session.run(Collections.singletonMap(inputName, inputTensor1))
        val resultTensor2 = session2.run(Collections.singletonMap(inputName2, inputTensor2))
        val outputs1 = resultTensor1.get(0).value as Array<*> // [1 84 8400]
        val outputs2 = resultTensor2.get(0).value as Array<*> // [1 84 8400]
        val results1 = dataProcess.outputsToNPMSPredictions(outputs1)
        val results2 = dataProcess2.outputsToNPMSPredictions(outputs2)


        //results1, results2 값을 하나로 합침
//        val results3 = results + results2





        Log.d("results1 Class", "results1 : $results1")
        Log.d("results2 Class", "results2 : $results2")
//        Log.d("results3 Class", "results3 : $results3")
//        val results = dataProcess2.outputsToNPMSPredictions(outputs)


        datas.clear()

        val canvas = Canvas(bitmap1)

        dataProcess.loadLabel()
        dataProcess2.loadLabel()

        // 사각형 그리기 x1, y1, x2, y2
        for (result in results1) {

            val predictedClassIndex = result.classIndex

            Log.d("results4 Class", "result : $result")

            // Accessing the properties from the model's output
            val x1 = result.rectF.left
            val y1 = result.rectF.top
            val x2 = result.rectF.right
            val y2 = result.rectF.bottom

            val rect = RectF(
                x1,
                y1,
                x2,
                y2
            )

            Log.d("Predicted Class", "x1 : $x1, y1 : $y1, x2 : $x2, y2 : $y2")

            // 사각형 그리기
            paint.color = colors[Random().nextInt(colors.size)]
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            canvas.drawRect(rect, paint)

            // 검출 된 객체의 이름과 퍼센트 표시
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            paint.textSize = 50f



            if(predictedClassIndex >= 0 && predictedClassIndex < dataProcess.classes.size ) {
                canvas.drawText(
//                    dataProcess.classes[predictedClassIndex] + " " + result.score,
                    dataProcess.classes[predictedClassIndex] ,
                    x1,
                    y1 + 30f,
                    paint
                )
            }


            // Retrieving the corresponding class label from the `classes` array
            if (predictedClassIndex >= 0 && predictedClassIndex < dataProcess.classes.size) {
                val predictedClassName1 = dataProcess.classes[predictedClassIndex]
                datas.add(MainData( name = dataProcess.classes[predictedClassIndex] ,  play = R.drawable.ic_baseline_play_arrow_24 , add = R.drawable.ic_baseline_add_circle_24  ))

                Log.d("Predicted Class", predictedClassName1)
                // Add predicted class name to the ArrayList
                predictedClassNames.add(predictedClassName1)

            }
        }
        for (result in results2) {

            val predictedClassIndex = result.classIndex

            Log.d("results4 Class", "result : $result")

            // Accessing the properties from the model's output
            val x1 = result.rectF.left
            val y1 = result.rectF.top
            val x2 = result.rectF.right
            val y2 = result.rectF.bottom

            val rect = RectF(
                x1,
                y1,
                x2,
                y2
            )

            Log.d("Predicted Class", "x1 : $x1, y1 : $y1, x2 : $x2, y2 : $y2")

            // 사각형 그리기
            paint.color = colors[Random().nextInt(colors.size)]
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            canvas.drawRect(rect, paint)

            // 검출 된 객체의 이름과 퍼센트 표시
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL
            paint.textSize = 50f




            if(predictedClassIndex >= 0 && predictedClassIndex < dataProcess2.classes.size) {
                canvas.drawText(
                    dataProcess2.classes[predictedClassIndex],
                    x1,
                    y1 + 100f,
                    paint
                )
            }

            // Retrieving the corresponding class label from the `classes` array
            if (predictedClassIndex >= 0  && predictedClassIndex < dataProcess2.classes.size) {

                val predictedClassName2 = dataProcess2.classes[predictedClassIndex]

                // Add predicted class name to the ArrayList
                datas.add(MainData( name = dataProcess2.classes[predictedClassIndex] ,  play = R.drawable.ic_baseline_play_arrow_24 , add = R.drawable.ic_baseline_add_circle_24  ))

                predictedClassNames.add(predictedClassName2)
            }
        }
        // array에 추가
        mainAdapter.datas = datas
        mainAdapter.notifyDataSetChanged()
        imageView.setImageBitmap(bitmap1)
        val predictedClassesText = predictedClassNames.toString()
//        textView.text = predictedClassesText
    }


    // 사진 저장
    fun saveFile(fileName:String, mimeType:String, bitmap: Bitmap):Uri?{

        var CV = ContentValues()

        // MediaStore 에 파일명, mimeType 을 지정
        CV.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        CV.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        // 안정성 검사
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            CV.put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        // MediaStore 에 파일을 저장
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, CV)
        if(uri != null){
            var scriptor = contentResolver.openFileDescriptor(uri, "w")

            val fos = FileOutputStream(scriptor?.fileDescriptor)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                CV.clear()
                // IS_PENDING 을 초기화
                CV.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, CV, null, null)
            }
        }
        return uri
    }

    // 파일명을 날짜 저장
    fun RandomFileName() : String{
        val fileName = SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis())
        return fileName
    }
    // 액티비티 종료 시 호출되는 메소드
    override fun onDestroy() {
        super.onDestroy()
        // 사용한 모델 종료
        model.close()
        // Shutdown TTS when
        // activity is destroyed
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }


    // 객체 감지 및 출력을 위한 함수
    fun get_predictions() {

        var image = TensorImage.fromBitmap(bitmap) // Bitmap을 TensorImage 객체로 변환
        image = imageProcessor.process(image) // 이미지 프로세싱

        val outputs = model.process(image) // 모델 실행
        // 결과값들을 각각의 배열로 저장
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true) // 이미지 복사
        val canvas = Canvas(mutable) // 캔버스 객체 생성

        val h = mutable.height // 이미지 높이
        val w = mutable.width // 이미지 너비

        paint.textSize= h/10f // 텍스트 크기 설정
        paint.strokeWidth= h/85f // 선 굵기 설정

        var x = 0 // 인덱스
        datas.clear()
        // 예측값들을 반복하면서 사각형과 텍스트 그리기
        scores.forEachIndexed(){index, f1->
            x = index
            x *= 4
            if (f1 > 0.5){
                paint.setColor(colors.get(index)) // 색깔 설정
                paint.style= Paint.Style.STROKE // 선 스타일 설정
                canvas.drawRect(RectF(locations.get(x+1)*w,locations.get(x)*h,locations.get(x+3)*w,locations.get(x+2)*h), paint) // 사각형 그리기
                paint.style= Paint.Style.FILL // 채우기 스타일 설정
                canvas.drawText(labels.get(classes.get(index).toInt())+" "+f1.toString(), locations.get(x+1)*w, locations.get(x)*h, paint) // 텍스트 그리기
          // array 초기화


                // array에 추가
                datas.add(MainData( name = labels.get(classes.get(index).toInt()),  play = R.drawable.ic_baseline_play_arrow_24 , add = R.drawable.ic_baseline_add_circle_24  ))
                mainAdapter.datas = datas
                mainAdapter.notifyDataSetChanged()

            }




        }

        imageView.setImageBitmap(mutable) // 이미지뷰에 결과 이미지 출력


    }




    private fun initRecycler() {
        mainAdapter = MainAdapter(this)

        rv_profile.adapter = mainAdapter

        rv_profile.addItemDecoration(VerticalItemDecorator(20))
        rv_profile.addItemDecoration(HorizontalItemDecorator(10))


    }
    class VerticalItemDecorator(private val divHeight : Int) : RecyclerView.ItemDecoration() {

        @Override
        override fun getItemOffsets(outRect: Rect, view: View, parent : RecyclerView, state : RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.top = divHeight
            outRect.bottom = divHeight
        }
    }
    class HorizontalItemDecorator(private val divHeight : Int) : RecyclerView.ItemDecoration() {

        @Override
        override fun getItemOffsets(outRect: Rect, view: View, parent : RecyclerView, state : RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.left = divHeight
            outRect.right = divHeight
        }
    }



}
