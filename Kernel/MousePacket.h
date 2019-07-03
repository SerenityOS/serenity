#pragma once

struct MousePacket {
    int dx { 0 };
    int dy { 0 };
    int dz { 0 };
    unsigned char buttons { 0 };
};
