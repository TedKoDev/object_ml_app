package com.example.object_ml_app
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import android.Manifest
import android.app.Activity

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.example.a0413yolov8kotlin.DataProcess1
import com.example.a0413yolov8kotlin.DataProcess2

import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(),TextToSpeech.OnInitListener {


    // 필요한 변수들 미리 선언 (나중에 초기화)
    lateinit var imageView: ImageView  //이미지 뷰
    lateinit var bitmap: Bitmap        //비트맵 - 이미지를 담는 변수
    lateinit var button1: Button       //갤러리 버튼
    lateinit var button2: Button        //카메라 버튼
    lateinit var listbtn: Button        //단어장 버튼


    // 검출결과 출력용 리사이클러뷰 관련 변수
    lateinit var mainAdapter: MainAdapter //리사이클러뷰 어댑터
    lateinit var rv_profile: RecyclerView //리사이클러뷰
    val datas = mutableListOf<MainData>() //리사이클러뷰 데이터 리스트



    //객체인식 관련 파트 객체 생성
    private val dataProcess1 = DataProcess1(context = this)   // yolov8n 모델 DataProcess1 객체 생성
    private val dataProcess2 = DataProcess2(context = this) // v8Fruits 모델 DataProcess2 객체 생성

        //ONNXRuntime관련 객체 생성 (onnxruntime 라이브러리에서 제공)
        // ...1 = yolov8n 모델  , ...2 = v8Fruits 모델
    private lateinit var session1: OrtSession   // ortSession 객체 생성 = OrtSession 클래스는 ONNX 모델을 로드하고 모델을 실행하는 데 사용됩니다.
    private lateinit var ortEnvironment1: OrtEnvironment //OrtEnvironment는 실행 환경의 이름, 로그 출력 및 디버깅 레벨 등의 설정을 포함합니다. ONNX Runtime에서는 여러 개의 실행 환경을 동시에 사용할 수 있으며, OrtEnvironment 클래스를 사용하여 이러한 실행 환경을 구성하고 관리할 수 있습니다.

    private lateinit var session2: OrtSession
    private lateinit var ortEnvironment2: OrtEnvironment

    // Paint 객체 생성
    val paint = Paint()


    // 카메라 및 갤러리 권한 요청 코드
    companion object {
        private const val PICK_IMAGE_REQUEST = 1 // 갤러리 앱 호출 코드
        private const val ACTION_IMAGE_CAPTURE = 102 // 카메라 앱 호출 코드
        private const val CAMERA_PERMISSION_REQUEST_CODE = 103  // 카메라 권한 요청 코드
    }

    // 감지된 객체마다 다른 색상을 설정하기 위한 리스트 생성
    val colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK, Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.LTGRAY
    )


    // TextToSpeech 객체 생성
    private var tts: TextToSpeech? = null


    // 액티비티 생성 시 호출되는 메소드
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity_main.xml을 화면에 표시
        setContentView(R.layout.activity_main)

        //권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA_PERMISSION_REQUEST_CODE)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA_PERMISSION_REQUEST_CODE)
            }
        }



        // 각 버튼 및 뷰 연동 (xml과 연결)
        listbtn = findViewById(R.id.listBtn)
        rv_profile = findViewById(R.id.rv_profile)
        imageView = findViewById(R.id.imageView)
        button1 = findViewById(R.id.loadBtn)
        button2 = findViewById(R.id.takeBtn)





        // 갤러리 버튼 클릭 시
        button1.setOnClickListener {
            openGallery()
        }
        // 카메라 버튼 클릭 시
        button2.setOnClickListener {
            takePhoto()
        }
        // 단어장 버튼 클릭 시
        listbtn.setOnClickListener {
            val intent = Intent(this, WordListActivity::class.java)
            startActivity(intent)
        }

        // 모델 및 텍스트 로드 메소드
        load()

        // 리사이클러뷰 실행 메소드
        initRecycler()

    }
    // 액티비티 생성 시 호출되는 메소드 끝


    private fun load() {
        // 1번모델용 시작
        dataProcess1.loadModel() // onnx 모델 불러오기
        dataProcess1.loadLabel() // coco txt 파일 불러오기
        // 2번모델용 시작
        dataProcess2.loadModel() // onnx 모델 불러오기
        dataProcess2.loadLabel() // coco txt 파일 불러오기

        try {
            // ortEnvironment 변수 초기화
            ortEnvironment1 = OrtEnvironment.getEnvironment()
            session1 = ortEnvironment1.createSession(
                this.filesDir.absolutePath.toString() + "/" + DataProcess1.FILE_NAME,
                OrtSession.SessionOptions()
            )
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
    }
    // 리사이클러뷰 실행 메소드
    private fun initRecycler() {
        mainAdapter = MainAdapter(this)
        rv_profile.adapter = mainAdapter
        rv_profile.addItemDecoration(DividerItemDecorator(this, 20, 10))
    }

    // 리사이클러뷰 Item 간격 설정
    class DividerItemDecorator(private val context: Context, private val verticalHeight: Int, private val horizontalHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.top = verticalHeight
            outRect.bottom = verticalHeight
            outRect.left = horizontalHeight
            outRect.right = horizontalHeight
        }
    }


    //tts
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

    //사진촬영
    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, ACTION_IMAGE_CAPTURE)
    }

    //갤러리
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // 갤러리에서 가져온것 또는 사진촬영 결과값
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {  //갤러리에서 가져온것
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


        }else if (requestCode == ACTION_IMAGE_CAPTURE) {   // 사진 촬영
            // 사진을 찍은 결과값을 bitmap 변수에 저장
            bitmap = data?.extras?.get("data") as Bitmap

            Log.d("bitmap.width", bitmap.width.toString())
            Log.d("bitmap.height", bitmap.height.toString())

            //이미지 퀄리티 높이기 위한 코드 이미지 크기를 줄여서 퀄리티를 높임 (별 효과가 없음)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)

            //이미지를 byte 배열로 변환
            val byteArray = stream.toByteArray()
            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)


//SKD 24, 26 버전인 경우 Matrix를 사용하여 세로로 찍은 사진을 가로로 보이게 함
//SDK 28 버전인 경우 ExifInterface를 사용하여 세로로 찍은 사진을 가로로 보이게 함

//이미지의 방향 정보를 읽어옴
            val exif = ExifInterface(ByteArrayInputStream(byteArray))
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

//이미지 회전 방지코드
//Matrix란 이미지를 회전, 이동, 크기 변환 등의 변형을 적용하기 위한 클래스
            val matrix = Matrix()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //SDK 28 버전 이상인 경우 ExifInterface를 사용하여 세로로 찍은 사진을 가로로 보이게 함
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
                }
            } else {
                //SKD 24, 26 버전인 경우 Matrix를 사용하여 세로로 찍은 사진을 가로로 보이게 함

                // 세로로 촬영된 경우에만 우측으로 90도 돌리기
                if (bitmap.width == bitmap.height) {

                    Log.d("동일함", bitmap.width.toString())

                    matrix.setRotate(90f)
                }

            }

            //이미지 회전
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)



                saveFile(RandomFileName(), "image/jpeg", bitmap)

            // 이미지 처리 함수 호출
            imageProcess(bitmap)
        }

    }

    // 이미지 처리 함수
    private fun imageProcess(bitmap0: Bitmap) {

        val predictedClassNames = ArrayList<String>() // ArrayList to store predicted class names

        // 이미지를 bitmap으로 변환
        val bitmap1 = dataProcess1.imageToBitmap(bitmap0)
        val bitmap2 = dataProcess2.imageToBitmap(bitmap0)


        // 이미지를 floatBuffer로 변환
        val floatBuffer1 = dataProcess1.bitmapToFloatBuffer(bitmap1)
        val floatBuffer2 = dataProcess2.bitmapToFloatBuffer(bitmap2)


        val inputName = session1.inputNames.iterator().next() // session 이름
        val inputName2 = session2.inputNames.iterator().next() // session 이름

        // 모델의 요구 입력값 [1 3 640 640] [배치 사이즈, 픽셀(RGB), 너비, 높이], 모델마다 크기는 다를 수 있음.
        val shape = longArrayOf(
            DataProcess1.BATCH_SIZE.toLong(),
            DataProcess1.PIXEL_SIZE.toLong(),
            DataProcess1.INPUT_SIZE.toLong(),
            DataProcess1.INPUT_SIZE.toLong()
        )


        val inputTensor1 = OnnxTensor.createTensor(ortEnvironment1, floatBuffer1, shape)   // 첫 번째 모델의 입력값 생성
        val inputTensor2 = OnnxTensor.createTensor(ortEnvironment2, floatBuffer2, shape)   // 두 번째 모델의 입력값 생성

        val resultTensor1 = session1.run(Collections.singletonMap(inputName, inputTensor1))   // 첫 번째 모델에 대한 예측값 반환
        val resultTensor2 = session2.run(Collections.singletonMap(inputName2, inputTensor2))  // 두 번째 모델에 대한 예측값 반환

        val outputs1 = resultTensor1.get(0).value as Array<*> // 첫 번째 모델에서 반환된 예측값 배열
        val outputs2 = resultTensor2.get(0).value as Array<*> // 두 번째 모델에서 반환된 예측값 배열

        val results1 = dataProcess1.outputsToNPMSPredictions(outputs1)   // 첫 번째 모델의 예측값 후처리
        val results2 = dataProcess2.outputsToNPMSPredictions(outputs2)   // 두 번째 모델의 예측값 후처리

        //위 코드는 두 개의 ONNX 모델에 대해 예측값을 생성하는 코드입니다.
        //
        //inputTensor1과 inputTensor2는 각각 첫 번째와 두 번째 모델에 대한 입력값 텐서를 생성합니다.
        //resultTensor1과 resultTensor2는 각 모델에 대한 입력값을 전달하고 예측 결과값을 반환합니다.
        //outputs1과 outputs2는 각 모델에서 반환된 결과값입니다. resultTensor1.get(0).value와 resultTensor2.get(0).value는 각 모델의 예측 결과값을 ONNX 텐서 객체로 반환하는데, as Array<*>를 사용하여 결과값을 배열로 변환합니다.
        //results1과 results2는 각 모델의 예측 결과값을 후처리하여 최종 예측값을 생성하는 함수입니다. outputs1과 outputs2를 인자로 받아 처리하며, 최종 예측값을 반환합니다.



        Log.d("results1 Class", "results1 : $results1")
        Log.d("results2 Class", "results2 : $results2")


        //배열 비우기 (초기화)
        datas.clear()


        val canvas = Canvas(bitmap1)



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



            if(predictedClassIndex >= 0 && predictedClassIndex < dataProcess1.classes.size ) {
                canvas.drawText(
//                    dataProcess.classes[predictedClassIndex] + " " + result.score,
                    dataProcess1.classes[predictedClassIndex] ,
                    x1,
                    y1 + 30f,
                    paint
                )
            }


            // Retrieving the corresponding class label from the `classes` array
            if (predictedClassIndex >= 0 && predictedClassIndex < dataProcess1.classes.size) {
                val predictedClassName1 = dataProcess1.classes[predictedClassIndex]
                datas.add(MainData( name = dataProcess1.classes[predictedClassIndex] ,  play = R.drawable.ic_baseline_play_arrow_24 , add = R.drawable.ic_baseline_add_circle_24  ))

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

    }

    fun saveFile(fileName:String, mimeType:String, bitmap: Bitmap):Uri?{

        //갤러리 HangeulSquareAPP 폴더에 저장
        val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "HangeulSquareAPP"


        //ContentValues 란? 키와 값으로 이루어진 데이터를 저장하는 객체
        var CV = ContentValues()

        // MediaStore 에 파일명, mimeType 을 지정
        CV.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        CV.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        // 안정성 검사
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            CV.put(MediaStore.Images.Media.RELATIVE_PATH, relativeLocation)
            CV.put(MediaStore.Images.Media.IS_PENDING, 1)
        }else{
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "HangeulSquareAPP")
            if(!dir.exists()){
                dir.mkdir()
            }
            val file = File(dir, fileName)
            CV.put(MediaStore.Images.Media.DATA, file.absolutePath)
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

    // 파일명을 날짜로 저장
    fun RandomFileName() : String{
        val fileName = SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis()) + ".jpg"
        return fileName
    }


    // 촬영한 사진 저장
    fun saveFile2(fileName:String, mimeType:String, bitmap: Bitmap):Uri?{

        //갤러리 HangeulSquareAPP 폴더에 저장
        val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "HangeulSquareAPP"


        //ContentValues 란? 키와 값으로 이루어진 데이터를 저장하는 객체
        var CV = ContentValues()

        // MediaStore 에 파일명, mimeType 을 지정
        CV.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        CV.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        CV.put(MediaStore.Images.Media.RELATIVE_PATH, relativeLocation)

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

    // 파일명을 날짜로 저장
    fun RandomFileName2() : String{
        val fileName = SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis())
        return fileName
    }


    // 액티비티 종료 시 호출되는 메소드
    override fun onDestroy() {
        super.onDestroy()

        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }


}
