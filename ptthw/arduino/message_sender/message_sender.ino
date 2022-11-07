/*
  Copyright (c) 2022 Rally Tactical Systems, Inc.
*/

#define MSG_PTT_UP      "PTT-UP^"
#define MSG_PTT_DOWN    "PTT-DOWN^"

#define ROTARY_ENCODER_A_PIN        D2
#define ROTARY_ENCODER_B_PIN        D0
#define ROTARY_ENCODER_BUTTON_PIN   25
#define ROTARY_ENCODER_VCC_PIN      27

// Rotary Encoder Inputs
#define CLK D2
#define DT D0
#define SW 4

int counter = 0;
int currentStateCLK;
int lastStateCLK;
int currentStateDT;
int lastStateDT;

String currentDir ="";
unsigned long lastButtonPress = 0;

void setup() {
  
  // Set encoder pins as inputs
  pinMode(CLK,INPUT);
  pinMode(DT,INPUT);
  pinMode(SW, INPUT_PULLUP);

  // Setup Serial Monitor
  Serial.begin(115200);

  // Read the initial state of CLK
  lastStateCLK = digitalRead(CLK);
}

void loop() 
{
  #if 0
  currentStateCLK = digitalRead(CLK);
  currentStateDT = digitalRead(DT);

  // If last and current state of CLK are different, then pulse occurred
  // React to only 1 state change to avoid double count
  if (currentStateCLK != lastStateCLK  && currentStateCLK == 1)
  {
    // If the DT state is different than the CLK state then
    // the encoder is rotating CCW so decrement
    if (currentStateDT != currentStateCLK) 
    {
      counter --;
      currentDir ="CCW";
    } 
    else 
    {
      // Encoder is rotating CW so increment
      counter ++;
      currentDir ="CW";
    }
    
    Serial.print("Direction: ");
    Serial.print(currentDir);
    Serial.print(" | Counter: ");
    Serial.println(counter);
  }

  lastStateCLK = currentStateCLK;
  lastStateDT = currentStateDT;
  
  // Read the button state
  int btnState = digitalRead(SW);

  //If we detect LOW signal, button is pressed
  if (btnState == LOW) {
    //if 50ms have passed since last LOW pulse, it means that the
    //button has been pressed, released and pressed again
    if (millis() - lastButtonPress > 50) {
      Serial.println("Button pressed!");
    }

    // Remember last button press event
    lastButtonPress = millis();
  }
  */

  // Put in a slight delay to help debounce the reading
  delay(10);
  #endif

  Serial.print(MSG_PTT_DOWN);
  delay(3000);
  Serial.print(MSG_PTT_UP);
  delay(2000);
}
