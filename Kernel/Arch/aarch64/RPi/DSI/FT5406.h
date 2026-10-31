/*
 * Copyright (c) 2026, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::RPi {

// This is the first part of the register space
// The data sheet can be found here
// <https://support.newhavendisplay.com/hc/en-us/articles/4410042626839-FT5x06-Registers>
struct FT5406TouchBuf {
    // The actual max points are device dependent.
    // The namesake FT5406 has only 5, but some compatible ones go up to 10.
    constexpr static size_t max_points = 10;
    enum class EventFlag : u8 {
        PutDown = 0b00,
        PutUp = 0b01,
        Contact = 0b10,
        NoEvent = 0b11,
    };
    enum class GestureID : u8 {
        MoveUp = 0x10,
        MoveLeft = 0x14, // Some versions of the manual disagree which way this is
        MoveDown = 0x18,
        MoveRight = 0x1C, // Some versions of the manual disagree which way this is
        ZoomIn = 0x48,
        ZoomOut = 0x49,
        NoGesture = 0x00,
    };

    struct TouchPoint {
        u8 xh : 4;
        u8 _ : 2; // unused
        EventFlag event : 2;
        u8 xl;

        u8 yh : 4;
        u8 touch_id : 4;
        u8 yl;
        // The following are reserved on the 5X06,
        // But other related panels may expose them.
        u8 touch_weight;
        u8 : 4;
        u8 touch_area : 4;
    };

    u8 device_mode; // bits 6:4 --> b000 for normal operation
    GestureID gesture_id;
    u8 num_points; // This might be only 4 bit
    TouchPoint touch_points[max_points];
};

}
