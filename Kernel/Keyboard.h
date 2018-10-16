#pragma once

namespace Keyboard {

enum class LED {
    ScrollLock = 1 << 0,
    NumLock    = 1 << 1,
    CapsLock   = 1 << 2,
};

void initialize();
void setLED(LED);
void unsetLED(LED);
void handleInterrupt();

}
