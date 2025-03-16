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
};

}
