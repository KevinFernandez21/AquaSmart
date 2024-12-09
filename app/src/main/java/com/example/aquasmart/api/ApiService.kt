package com.example.aquasmart.api

import com.example.aquasmart.model.BombaResponse
import retrofit2.Call
import com.example.aquasmart.model.HumedadResponse
import retrofit2.http.GET
interface ApiService {
    @GET("/datos")
    fun getHumedad(): Call<HumedadResponse>

    @GET("/activar")
    fun activarBomba(): Call<BombaResponse>  // Cambiar a BombaResponse

    @GET("/apagar")
    fun apagarBomba(): Call<BombaResponse>
}