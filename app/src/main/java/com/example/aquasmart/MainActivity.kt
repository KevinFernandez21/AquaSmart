package com.example.aquasmart


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import com.example.aquasmart.model.HumedadResponse
import com.example.aquasmart.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.Context
class MainActivity : AppCompatActivity() {

    private lateinit var textViewHumedad: TextView
    private lateinit var controlBombaImageView: ImageView
    private lateinit var apiService: ApiService
    private var bombaEncendida = false
    private val intervaloActualizacion: Long = 1000  // Tiempo entre actualizaciones en ms

    private val CHANNEL_ID = "humidity_channel"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        textViewHumedad = findViewById(R.id.textViewHumedad)
        controlBombaImageView = findViewById(R.id.ControlBomba)

        // Configuración Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.100.152/")  // Cambia a la IP de tu ESP32
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Configurar botón para controlar la bomba
        controlBombaImageView.setOnClickListener {
            if (bombaEncendida) {
                apagarBomba()
            } else {
                encenderBomba()
            }
        }
        createNotificationChannel()
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
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Humedad Channel"
            val descriptionText = "Canal para notificaciones de humedad"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Registrar el canal
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun sendNotification(message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_round) // Usa un ícono adecuado
            .setContentTitle("Alerta de Humedad")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(0, builder.build())
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

                    // Verificar y enviar notificaciones
                    if (it <= 20) {
                        sendNotification("Los niveles de humedad están muy bajos, actívalos por favor.")
                    } else if (it >= 70 && it < 80) {
                        sendNotification("Nivel de humedad suficiente.")
                    } else if (it >= 80) {
                        sendNotification("Humedad por encima, se apagó la bomba automáticamente.")
                        if (bombaEncendida) apagarBomba()
                    }
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
                    // Cambiar la imagen a "encendido"
                    controlBombaImageView.setImageResource(R.drawable.encendido)
                    bombaEncendida = true
                    Toast.makeText(this@MainActivity, "Bomba activada", Toast.LENGTH_SHORT).show()
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
                    // Cambiar la imagen a "apagado"
                    controlBombaImageView.setImageResource(R.drawable.apagado)
                    bombaEncendida = false
                    Toast.makeText(this@MainActivity, "Bomba apagada", Toast.LENGTH_SHORT).show()
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