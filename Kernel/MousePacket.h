#pragma once

struct MousePacket {
    int dx { 0 };
    int dy { 0 };
    byte buttons;
};
