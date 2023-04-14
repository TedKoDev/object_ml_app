package com.example.object_ml_app
import android.Manifest

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
import com.example.object_ml_app.ml.Newfruit
import com.example.object_ml_app.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
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

    // Paint 객체 생성
    val paint = Paint()

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


    private var tts: TextToSpeech? = null

    // 이미지 전처리를 위한 ImageProcessor 객체 생성
    // ResizeOp을 적용하여 이미지 크기 조절
    val imageProcessor =
        ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()

    // 감지된 객체마다 다른 색상을 설정하기 위한 리스트 생성
    val colors = listOf<Int>(
        Color.BLUE, Color.GREEN,Color.RED,Color.CYAN,Color.GRAY,Color.BLACK,Color.DKGRAY,Color.MAGENTA,Color.YELLOW,Color.LTGRAY
    )

    // 액티비티 생성 시 호출되는 메소드
    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)
        // activity_main.xml을 화면에 표시
        setContentView(R.layout.activity_main)

        // 카메라 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }

        // 암시적 인텐트 생성
        // 앨범에서 이미지를 가져오기 위해 ACTION_GET_CONTENT 사용
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)

        // labels.txt 파일을 읽어와 labels 변수에 저장
        labels = FileUtil.loadLabels(this, "labels.txt")

        // SsdMobilenetV11Metadata1 모델을 생성하여 model 변수에 저장
        model = SsdMobilenetV11Metadata1.newInstance(this)

        // ImageView와 Button을 id를 이용하여 변수에 할당
        imageView = findViewById(R.id.imageView)
        button1 = findViewById(R.id.loadBtn)
        button2 = findViewById(R.id.takeBtn)
        listbtn = findViewById(R.id.listBtn)



        rv_profile = findViewById(R.id.rv_profile)

        initRecycler()

        // 버튼 클릭 시 startActivityForResult로 암시적 인텐트 실행
        // requestCode 101로 설정
        button1.setOnClickListener {
            startActivityForResult(intent, 101)
        }
        //button2 클릭시 사진기 실행 후 결과값을 받아오는 메소드 실행
        //화질 개선을 위한 경로 전달
        button2.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, 102)
        }

        listbtn.setOnClickListener {
            val intent = Intent(this, WordListActivity::class.java)
            startActivity(intent)
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


    // 암시적 인텐트 실행 후 결과값을 받아오는 메소드
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // requestCode가 101일 경우 실행
        if (requestCode == 101) {
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


            // get_predictions() 함수 실행
            get_predictions()
        } else if (requestCode == 102) {
            // 사진을 찍은 결과값을 bitmap 변수에 저장
            bitmap = data?.extras?.get("data") as Bitmap

            val uri = saveFile(RandomFileName(), "image/jpeg", bitmap)
            // get_predictions() 함수 실행
            get_predictions()
        }




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

    private fun loadModelFile(modelPath: String): ByteBuffer {
        val fileDescriptor = assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    // 객체 감지 및 출력을 위한 함수
    fun get_predictions2() {

        val tflite = Interpreter(loadModelFile("yolov5s.tflite"))
        val input = ByteBuffer.allocateDirect(1 * 640 * 640 * 3 * 4).order(ByteOrder.nativeOrder())
// load input data into input buffer
        val outputLocations = ByteBuffer.allocateDirect(1 * 25200 * 4).order(ByteOrder.nativeOrder())
        val outputClasses = ByteBuffer.allocateDirect(1 * 25200 * 4).order(ByteOrder.nativeOrder())
        val outputScores = ByteBuffer.allocateDirect(1 * 25200 * 4).order(ByteOrder.nativeOrder())
        val outputNumberDetections = ByteBuffer.allocateDirect(1 * 4).order(ByteOrder.nativeOrder())

        tflite.run(input, arrayOf(outputLocations, outputClasses, outputScores, outputNumberDetections))

// process output data
// release resources
        tflite.close()


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
