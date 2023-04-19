package com.example.object_ml_app

import android.app.AlertDialog
import android.app.SearchManager
import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.a0328kotlin_recy.WordData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class WordListAdapter(private val context: Context) : RecyclerView.Adapter<WordListAdapter.ViewHolder>() , TextToSpeech.OnInitListener{
        private var tts: TextToSpeech? = null




    init {
            tts = TextToSpeech(context, this)
        }

        var datas = mutableListOf<WordData>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_recycler_list,parent,false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = datas.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(datas[position])



        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            private val txtName: TextView = itemView.findViewById(R.id.tv_rv_name)
            private val imgUpdate: ImageView = itemView.findViewById(R.id.btn_rv_update)
            private val imgPlay: ImageView = itemView.findViewById(R.id.btn_rv_play)




            fun bind(item: WordData) {
                txtName.text = item.name
                Glide.with(itemView).load(item.play).into(imgPlay)



                imgPlay.setOnClickListener {

                    tts!!.speak(txtName.text, TextToSpeech.QUEUE_FLUSH, null,"")
                }

                imgUpdate.setOnClickListener {
                    // 팝업 메뉴를 생성합니다.
                    val popupMenu = PopupMenu(itemView.context, imgUpdate)
                    popupMenu.menuInflater.inflate(R.menu.word_popup_menu, popupMenu.menu)

                    // 팝업 메뉴를 보여줍니다.
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_delete -> {
                                // "수정" 항목을 선택한 경우 처리할 작업을 여기에 작성합니다.
                                //                    //삭제 할지 물어보는 dialog 띄우기
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("단어 삭제")
                    builder.setMessage("단어를 삭제하시겠습니까?")
                    builder.setPositiveButton("삭제") { dialog, which ->
                        datas.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, datas.size)
                        notifyItemChanged(position)


                        apiRequest(txtName.text)
                        // shared에서 삭제
                        val pref = context.getSharedPreferences("pref", MODE_PRIVATE)
                        val editor = pref.edit()
                        editor.remove("word_"+txtName.text.toString())
                        editor.apply()



                    }
                    builder.setNegativeButton("취소") { dialog, which ->
                                  //취소

                                      dialog.dismiss()

                             }
                               builder.show()
                                true
                            }
                            R.id.menu_search -> {
                                // "검색" 항목을 선택한 경우 처리할 작업을 여기에 작성합니다.
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/search?q=${txtName.text}")
                                )
                                context.startActivity(intent)

                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()

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
            val kind = "delete"
            val word = textName.toString()
            val plus = "null"
            val tickerCall = apiService.getCoinTicker(ApiService.ClassInfoRequest(access_token, last_id, kind, word, plus     )     )

            //4. 네트워크 통신
            tickerCall.enqueue(object : Callback<ApiService.ClassInfoResponse> {
                override fun onResponse(call: Call<ApiService.ClassInfoResponse>, response: Response<ApiService.ClassInfoResponse>) {
                    if (response.isSuccessful) {
                        // Handle successful response
                        val ticker = response.body()


                        //리사이클러뷰 갱신
                        notifyDataSetChanged()




                        //ticker 한번에 보기
                        Log.d(ContentValues.TAG, "ticker: $ticker")


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