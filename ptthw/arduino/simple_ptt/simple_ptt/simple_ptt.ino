/*
  Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

/*
  - Momentary button/switch on a breadboard connected to ESP32.
  - First button leg connected to PWR
  - Second button leg connected to GND via 10K Ohm resistor
  - Third button leg connect to D0 pin
 */

// Messages recognized by the intent producer app on Android
#define MSG_PTT_UP      "PTT-UP^"
#define MSG_PTT_DOWN    "PTT-DOWN^"

// We're on PIN D0 for input
#define PTT_BUTTON_PIN  D0

#define CYCLE_SAVER_MS  50

int g_lastButtonState = -1;
int g_buttonState = -1;

void setup()
{
  g_lastButtonState = -1;
  g_buttonState = -1;
  
  Serial.begin(115200);
  pinMode(PTT_BUTTON_PIN, INPUT);  
}

void loop()
{
  // What's the pin state?
  g_buttonState = digitalRead(PTT_BUTTON_PIN);

  // Only do something if it's state has changed since the last time we checked
  if(g_buttonState != g_lastButtonState)
  {
    // HIGH sends MSG_PTT_DOWN, LOW send MSG_PTT_UP 
    if(g_buttonState == HIGH)
    {
      Serial.print(MSG_PTT_DOWN);
    }
    else
    {
      Serial.print(MSG_PTT_UP);
    }

    // Remember the state
    g_lastButtonState = g_buttonState;
  }

  // No sense in wasting cycles if we don't need to
  delay(CYCLE_SAVER_MS);
}
