/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::HID {

// https://usb.org/sites/default/files/hut1_6.pdf#chapter.3
enum class UsagePage : u16 {
    GenericDesktop = 0x01,
    KeyboardOrKeypad = 0x07,
    Button = 0x09,
    Consumer = 0x0c,
};

enum class Usage : u32 {
    // Generic Desktop Page (0x01)
    // https://usb.org/sites/default/files/hut1_6.pdf#chapter.4
    Mouse = 0x0001'0002,
    Keyboard = 0x0001'0006,

    // Keyboard/Keypad Page (0x07)
    // https://usb.org/sites/default/files/hut1_6.pdf#chapter.10
    KeypadNumlock = 0x0007'0054,
    Keypad1 = 0x0007'0059,
    KeypadDot = 0x0007'0063,
    KeyboardLeftControl = 0x0007'00e0,
    KeyboardLeftShift = 0x0007'00e1,
    KeyboardLeftAlt = 0x0007'00e2,
    KeyboardLeftGUI = 0x0007'00e3,
    KeyboardRightControl = 0x0007'00e4,
    KeyboardRightShift = 0x0007'00e5,
    KeyboardRightAlt = 0x0007'00e6,
    KeyboardRightGUI = 0x0007'00e7,
};

}
