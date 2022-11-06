/*
  Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

#define MSG_PTT_UP      "PTT-UP^"
#define MSG_PTT_DOWN    "PTT-DOWN^"

void sendPttUp()
{
  Serial.print(MSG_PTT_UP);
}

void sendPttDown()
{
  Serial.print(MSG_PTT_DOWN);
}

void setup() {
  Serial.begin(115200);
}

void loop() {
  sendPttUp();
  delay(1000);
  sendPttDown();
  delay(1000);
}
