#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h>
// Datos de la red Wi-Fi
const char* ssid = "NETLIFE-SANCHEZ";  // Cambia por el nombre de tu red Wi-Fi
const char* password = "kd200421";     // Cambia por tu contraseña

// Pin al que está conectada la bomba
const int relePin = 14;  // Módulo relé conectado al pin GPIO 18
const int humedadPin = 34;  // Pin analógico para el sensor de humedad YL-69

// Crear servidor HTTP en el puerto 80
WebServer server(80);

// Variables para el control de la bomba
bool bombaEncendida = false;  // Controla el estado de la bomba

// Configuración inicial
void setup() {
  // Iniciar puerto serie
  Serial.begin(115200);

  // Conectar a la red Wi-Fi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Conectando a Wi-Fi...");
  }
  Serial.println("Conexión Wi-Fi exitosa!");
  Serial.print("Dirección IP: ");
  Serial.println(WiFi.localIP());

  // Configurar el pin del relé como salida
  pinMode(relePin, OUTPUT);
  digitalWrite(relePin, LOW);  // Apagar la bomba por defecto

  // Configurar el pin del sensor de humedad como entrada
  pinMode(humedadPin, INPUT);

  // Definir las rutas del servidor
  server.on("/activar", HTTP_GET, []() {
    activarBomba();
    String jsonResponse = "{\"status\": \"success\", \"message\": \"Bomba activada\"}";
    server.send(200, "application/json", jsonResponse);
  });

  server.on("/apagar", HTTP_GET, []() {
      desactivarBomba();
      String jsonResponse = "{\"status\": \"success\", \"message\": \"Bomba apagada\"}";
      server.send(200, "application/json", jsonResponse);
  });

  server.on("/datos", HTTP_GET, []() {
    // Leer el valor del sensor de humedad (señal analógica)
    int humedad = analogRead(humedadPin);  // Lee el valor de humedad
    int humedadPorcentaje = map(humedad, 370, 4095, 100, 0);  // Mapea a porcentaje (0-100%)

    // Crear la respuesta en formato de texto
     StaticJsonDocument<200> doc;
    doc["humedad"] = humedadPorcentaje;
    String jsonResponse;
    serializeJson(doc, jsonResponse);

    // Enviar los datos como JSON
    server.send(200, "application/json", jsonResponse);  // Enviar la respuesta con los datos
  });

  // Iniciar el servidor
  server.begin();
  Serial.println("Servidor HTTP iniciado");
}

// Función para controlar la bomba automáticamente en función de la humedad
void controlBomba() {
  int humedad = analogRead(humedadPin);  // Lee el valor del sensor de humedad (analógica)
  int humedadPorcentaje = map(humedad, 370, 4095, 100, 0);  // Mapea el valor de 0-4095 a un porcentaje 0-100

  if (humedadPorcentaje > 90 && bombaEncendida==true) {
    desactivarBomba();
  }

  // Enviar alerta si la humedad está fuera de rango
  if (humedadPorcentaje < 30) {
    sendAlert("La humedad está muy baja, activa la bomba de agua!");
  } else if (humedadPorcentaje > 70) {
    sendAlert("La humedad está muy alta, apaga la bomba de agua!");
  }

  delay(1000);  // Pausa de 1 segundo antes de la siguiente lectura
}

// Función para activar la bomba
void activarBomba() {
  digitalWrite(relePin, HIGH);  // Activar el relé (encender la bomba)
  bombaEncendida = true;
  Serial.println("Bomba activada");
}

// Función para desactivar la bomba
void desactivarBomba() {
  digitalWrite(relePin, LOW);  // Desactivar el relé (apagar la bomba)
  bombaEncendida = false;
  Serial.println("Bomba apagada");
}

// Función para enviar una alerta al monitor serial (esto también puede ser enviado a la app si lo necesitas)
void sendAlert(String message) {
  Serial.println(message);  // Muestra la alerta en el Monitor Serial
}

void loop() {
  // El servidor maneja las peticiones HTTP
  server.handleClient();

  // Control automático de la bomba según la humedad
  controlBomba();
}
