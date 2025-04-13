package com.example.notes

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.CompletableFuture
import okhttp3.logging.HttpLoggingInterceptor


class APISender {


    val domain = "http://10.8.0.2:4000"
    val EMPTY_REQUEST = "".toRequestBody("text/plain".toMediaTypeOrNull())


    private val client: OkHttpClient


    init {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)


        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }


    fun get(url: String): CompletableFuture<Response> {
        val future = CompletableFuture<Response>()


        val request = Request.Builder()
            .url("$domain$url")
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }


            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    future.complete(response)
                } else {
                    future.completeExceptionally(IOException("Unexpected code $response"))
                }
            }
        })


        return future
    }


    fun post(url: String, json: String = ""): CompletableFuture<String> {
        val future = CompletableFuture<String>()


        val requestBody = if (json.isNotEmpty()) {
            json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        } else {
            EMPTY_REQUEST
        }


        val request = Request.Builder()
            .url("$domain$url")
            .header("Cache-Control", "no-cache")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "*/*")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Connection", "keep-alive")
            .post(requestBody)
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }


            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val result = response.body?.string() ?: ""
                    future.complete(result)
                    response.close()
                } else {
                    future.completeExceptionally(IOException("Unexpected code $response"))
                }
            }
        })


        return future
    }
}
