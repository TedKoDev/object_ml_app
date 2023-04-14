package com.example.object_ml_app


import android.content.ContentValues
import android.graphics.Rect
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.a0328kotlin_recy.WordData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WordListActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    lateinit var wordAdapter: WordListAdapter
    lateinit var tv_count: TextView
    var page: Int? = 0



    val datas = mutableListOf<WordData>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_list)


        recyclerView = findViewById(R.id.recyclerView)
        tv_count = findViewById(R.id.tv_count)






        //인터넷이 동작하면 데이터를 받아오고, 동작하지 않으면 데이터를 받아오지 않는다.
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            //인터넷이 동작하면 데이터를 받아온다.


            apiRequest(page)
            initRecycler()

        } else {
            initRecycler()

            val pref = getSharedPreferences("pref", MODE_PRIVATE)
            val wordList = mutableListOf<String>()
            val map = pref.all
            for ((key, value) in map) {
                if (key.startsWith("word_")) {
                    wordList.add(value.toString())
                }
            }
            val wordListArray = wordList.toTypedArray()
            val wordListArraySize = wordListArray.size
            tv_count.text = wordListArraySize.toString()

            for (i in 0 until wordListArraySize) {
                datas.add(WordData(name = wordListArray[i], play = R.drawable.ic_baseline_play_arrow_24, update = R.drawable.ic_baseline_density_medium_24))
            }
            wordAdapter.datas = datas
            wordAdapter.notifyDataSetChanged()




        }




    }


    private fun apiRequest(page: Int?) {



        //1. retrofit 객체 생성
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://www.hangeulsquare.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //2. service 객체 생성
        val apiService: ApiService = retrofit.create(ApiService::class.java)

        //3. Call객체 생성
        val access_token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJVc2VyX0lEIjoiNDY3IiwiVV9OYW1lIjoiXHVkNjRkIiwiVV9FbWFpbCI6ImdoZHhvZG1sQG5hdmVyLmNvbSIsImV4cCI6MTY4MDY3NDU1OCwiaWF0IjoxNjgwMDY5NzU4fQ." +
                "lbY_0SLC0tMK05t6aaGPuYzkRNaawKQ_ahK9T5NvV_Q"
        val last_id = "435"
        val kind = "read"
        val word = "null"
        val plus = page.toString()
        val tickerCall = apiService.getCoinTicker(ApiService.ClassInfoRequest(access_token, last_id, kind, word, plus)     )

        //4. 네트워크 통신
        tickerCall.enqueue(object : Callback<ApiService.ClassInfoResponse> {
            override fun onResponse(call: Call<ApiService.ClassInfoResponse>, response: Response<ApiService.ClassInfoResponse>) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val ticker = response.body()

                    tv_count.text = ticker?.count.toString()


                    //result 값 반복해서 출력
                    for (i in 0 until ticker?.result?.size!!) {
                        Log.d(ContentValues.TAG, "ticker: ${ticker.result[i]}")

                        datas.add(WordData( name = ticker.result[i] , play =  R.drawable.ic_baseline_play_arrow_24 ,update = R.drawable.ic_baseline_density_medium_24  ))
                        wordAdapter.datas = datas
                        wordAdapter.notifyDataSetChanged()
                    }

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

    private fun initRecycler() {
        wordAdapter = WordListAdapter(this)
        recyclerView.adapter = wordAdapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        recyclerView.addItemDecoration(VerticalItemDecorator(20))
        recyclerView.addItemDecoration(HorizontalItemDecorator(10))

        //kotlin recyclerview pagination



        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val lastVisibleItemPosition = (recyclerView.layoutManager as LinearLayoutManager?)!!.findLastCompletelyVisibleItemPosition()


                val itemTotalCount = recyclerView.adapter!!.itemCount - 1

                if (!recyclerView.canScrollVertically(1) && lastVisibleItemPosition == itemTotalCount) {

                    page = page?.plus(1);

                    apiRequest(page)

                }
            }
        })


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