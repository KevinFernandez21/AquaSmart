#include <SoftwareSerial.h>

SoftwareSerial bluetooth(10, 11); // RX, TX para HC-06
int humedadPin = A0; // Sensor YL-69 conectado al pin analógico A0
int relePin = 7; // Módulo relé conectado al pin digital 8

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);  // Inicia la comunicación Bluetooth
  pinMode(relePin, OUTPUT); // Configura el pin del relé como salida
  digitalWrite(relePin, LOW); // Apaga la bomba al inicio
}

void loop() {
  // Lee la humedad del sensor YL-69
  int humedad = analogRead(humedadPin);
  int humedadPorcentaje = map(humedad, 0, 1023, 0, 100); // Mapea la lectura a un porcentaje

  // Simula la temperatura (puedes agregar un sensor de temperatura si lo deseas)
  int temperatura = 25; // Asumiendo una temperatura constante

  // Envía los datos de humedad y temperatura al dispositivo Android
  bluetooth.print("humedad=");
  bluetooth.print(humedadPorcentaje);
  bluetooth.print(",temperatura=");
  bluetooth.println(temperatura);

  // Espera por comandos del teléfono
  if (bluetooth.available()) {
    String command = bluetooth.readString();
    
    // Control de la bomba desde el teléfono
    if (command == "ACTIVAR_BOMBA") {
      digitalWrite(relePin, HIGH); // Activa la bomba
    } else if (command == "APAGAR_BOMBA") {
      digitalWrite(relePin, LOW); // Apaga la bomba
    }
  }

  // Control automático de la bomba en base a la humedad
  if (humedadPorcentaje < 30) {
    bluetooth.println("La humedad está muy baja, activa la bomba de agua!");
  } else if (humedadPorcentaje > 70) {
    bluetooth.println("La humedad está muy alta, apaga la bomba de agua!");
    if (digitalRead(relePin) == HIGH) {
      digitalWrite(relePin, LOW); // Apaga la bomba automáticamente si la humedad es muy alta
    }
  }

  delay(1000); // Pausa de 1 segundo antes de la siguiente lectura
}

