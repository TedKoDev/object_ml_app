package com.example.object_ml_app

import retrofit2.Call
import retrofit2.http.*

interface ApiService {


    @POST("restapi/word.php")
    fun getCoinTicker(
        @Body
        request: ClassInfoRequest)
    : Call<ClassInfoResponse>



    data class ClassInfoRequest(
        val access_token: String?,
        val last_id: String?,
        val kind: String?,
        val word: String?,
        val plus: String?)
    

    data class ClassInfoResponse(
//        val result: MutableList<result>,
        val result: MutableList<String>,
        val length: String,
        val page: String,
        val tokenupdate: String,
        val count: String,
        val access_token: String,
        val message: String,
        val last_id: String,
        val login: String,
        val success: String)

    data class result(
        val word: String?

    )
}
