package com.example.aquasmart


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.aquasmart.api.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var textViewHumedad: TextView
    private lateinit var controlBombaImageView: ImageView
    private lateinit var apiService: ApiService
    private var bombaEncendida = false
    private val intervaloActualizacion: Long = 1000  // Tiempo entre actualizaciones en ms
    // SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences
    private val ipKey = "ESP32_IP"  // Clave para almacenar la IP
    private val CHANNEL_ID = "humidity_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        textViewHumedad = findViewById(R.id.textViewHumedad)
        controlBombaImageView = findViewById(R.id.ControlBomba)

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)

        // Obtener la IP guardada (si existe)
        val ipGuardada = sharedPreferences.getString(ipKey, "http://192.168.100.152") // Valor por defecto
        // Configurar Retrofit con la IP obtenida
        setupRetrofit(ipGuardada)

        // Configurar ImageView para controlar la bomba
        controlBombaImageView.setOnClickListener {
            if (bombaEncendida) {
                apagarBomba()
            } else {
                encenderBomba()
            }
        }

        // Configurar botón para guardar IP
        val buttonGuardarIP = findViewById<Button>(R.id.buttonGuardarIP)
        val editTextIP = findViewById<EditText>(R.id.editTextIP)
        buttonGuardarIP.setOnClickListener {
            val nuevaIP = editTextIP.text.toString()
            if (nuevaIP.isNotEmpty()) {
                // Guardar la IP en SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString(ipKey, nuevaIP)
                editor.apply()
                // Actualizar la conexión con la nueva IP
                setupRetrofit(nuevaIP)
                Toast.makeText(this, "IP guardada: $nuevaIP", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor, ingrese una IP válida", Toast.LENGTH_SHORT).show()
            }
        }

        // Crear canal de notificación
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
    private fun setupRetrofit(ip: String?) {
        if (ip != null) {
            val retrofit = Retrofit.Builder()
                .baseUrl(ip)  // Usar la IP proporcionada
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
    }
    private fun createNotificationChannel() {
        val name = "Humedad Channel"
        val descriptionText = "Canal para notificaciones de humedad"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Registrar el canal
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    private fun sendNotification(message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_round) // Usa un ícono adecuado
            .setContentTitle("Alerta de Humedad")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
            // El permiso ha sido concedido, puedes mostrar la notificación
            with(NotificationManagerCompat.from(this)) {
                notify(0, builder.build())
            }
        } else {
            // El permiso no ha sido concedido, solicita el permiso
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1
            )
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