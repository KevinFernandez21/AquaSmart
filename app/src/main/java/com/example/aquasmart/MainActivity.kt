package com.example.aquasmart


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.aquasmart.model.HumedadResponse
import com.example.aquasmart.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var textViewHumedad: TextView
    private lateinit var buttonControlBomba: Button
    private lateinit var apiService: ApiService
    private var bombaEncendida = false
    private val intervaloActualizacion: Long = 1000  // Tiempo entre actualizaciones en ms


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        textViewHumedad = findViewById(R.id.textViewHumedad)
        buttonControlBomba = findViewById(R.id.buttonControlBomba)

        // Configuración Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.100.152/")  // Cambia a la IP de tu ESP32
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Configurar botón para controlar la bomba
        buttonControlBomba.setOnClickListener {
            if (bombaEncendida) {
                apagarBomba()
            } else {
                encenderBomba()
            }
        }

        // Iniciar actualizaciones periódicas de la humedad
        iniciarActualizacionHumedad()
    }
    private fun iniciarActualizacionHumedad() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                obtenerDatosHumedad()
                kotlinx.coroutines.delay(intervaloActualizacion)
            }
        }
    }

    private suspend fun obtenerDatosHumedad() {
        try {
            Log.d("MainActivity", "Realizando la solicitud a la API para obtener la humedad")
            val response = withContext(Dispatchers.IO) { apiService.getHumedad().execute() }

            if (response.isSuccessful) {
                val humedad = response.body()?.humedad
                humedad?.let {
                    Log.d("MainActivity", "Humedad recibida: $it%")
                    textViewHumedad.text = "Humedad: $it%"
                }
            } else {
                Log.e("MainActivity", "Error en la respuesta: ${response.code()}")
                Toast.makeText(this, "Error al obtener la humedad", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error de conexión: ${e.message}")
            Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
        }
    }

    private fun encenderBomba() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) { apiService.activarBomba().execute() }

                if (response.isSuccessful) {
                    // Acceder a los datos de la respuesta JSON
                    val bombaResponse = response.body()
                    bombaResponse?.let {
                        // Puedes mostrar el mensaje desde la respuesta
                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                        bombaEncendida = true
                        buttonControlBomba.text = "Apagar Bomba"
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error al encender la bomba", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al encender la bomba: ${e.message}")
                Toast.makeText(this@MainActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun apagarBomba() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) { apiService.apagarBomba().execute() }

                if (response.isSuccessful) {
                    // Acceder a los datos de la respuesta JSON
                    val bombaResponse = response.body()
                    bombaResponse?.let {
                        // Puedes mostrar el mensaje desde la respuesta
                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                        bombaEncendida = false
                        buttonControlBomba.text = "Encender Bomba"
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Error al apagar la bomba", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al apagar la bomba: ${e.message}")
                Toast.makeText(this@MainActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}