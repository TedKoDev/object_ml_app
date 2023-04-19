package com.example.a0328kotlin_recy


import android.content.ContentValues
import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.object_ml_app.ApiService
import com.example.object_ml_app.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MainAdapter(private val context: Context) : RecyclerView.Adapter<MainAdapter.ViewHolder>() , TextToSpeech.OnInitListener{
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context, this)
    }

    var datas = mutableListOf<MainData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_recycler_ex,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = datas.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(datas[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val txtName: TextView = itemView.findViewById(R.id.tv_rv_name)
        private val imgAdd: ImageView = itemView.findViewById(R.id.btn_rv_add)
        private val imgPlay: ImageView = itemView.findViewById(R.id.btn_rv_play)




        fun bind(item: MainData) {
            txtName.text = item.name
            Glide.with(itemView).load(item.play).into(imgPlay)
            Glide.with(itemView).load(item.add).into(imgAdd)


            imgPlay.setOnClickListener {

                tts!!.speak(txtName.text, TextToSpeech.QUEUE_FLUSH, null,"")
            }
            imgAdd.setOnClickListener {
                apiRequest(   txtName.text )

                //sharedPreference에 저장
                val pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE)
                val editor = pref.edit()
                editor.putString("word_"+txtName.text, txtName.text.toString())
                editor.apply()


            }

        }

    }

    private fun apiRequest(textName: CharSequence){

        //1. retrofit 객체 생성
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://www.hangeulsquare.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //2. service 객체 생성
        val apiService: ApiService = retrofit.create(ApiService::class.java)

        //3. Call객체 생성
        val access_token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJVc2VyX0lEIjoiMSIsIlVfTmFtZSI6IlRFRChBRE1JTikiLCJVX0VtYWlsIjoiYWRtaW5AaGFuZ2V1bHNxdWFyZS5jb20iLCJleHAiOjE2ODI0ODQxODgsImlhdCI6MTY4MTg3OTM4OH0.RsJjdFRT7svkISnTeQQw_EE46ncIHFVz4cp7WlcaCT0"
        val last_id = "461"
        val kind = "add"
        val plus = "null"
        val word = textName.toString()
        val tickerCall = apiService.getCoinTicker(ApiService.ClassInfoRequest(access_token, last_id, kind, word, plus)     )

        //4. 네트워크 통신
        tickerCall.enqueue(object : Callback<ApiService.ClassInfoResponse> {
            override fun onResponse(call: Call<ApiService.ClassInfoResponse>, response: Response<ApiService.ClassInfoResponse>) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val ticker = response.body()


                    //ticker 한번에 보기
                    Log.d(ContentValues.TAG, "ticker: $ticker")

                    // Toast로 값 확인
//                    Toast.makeText(context, ticker.toString(), Toast.LENGTH_SHORT).show()
                Toast.makeText(context, textName.toString() +" Save", Toast.LENGTH_SHORT).show()



                } else {
                    // Handle error response
                    val errorBody = response.errorBody()?.string()
                    Log.e(ContentValues.TAG, "error response: $errorBody")
                }
            }

            override fun onFailure(call: Call<ApiService.ClassInfoResponse>, t: Throwable) {
                // Handle network error
                Log.e(ContentValues.TAG, "network error: ${t.message}")
            }
        })

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


}