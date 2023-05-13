/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Types.h>

namespace Kernel {

AK_TYPEDEF_DISTINCT_ORDERED_ID(size_t, PS2PortIndex);

// NOTE: This list is derived from https://wiki.osdev.org/%228042%22_PS/2_Controller#Detecting_PS.2F2_Device_Types
enum class PS2DeviceType {
    Unknown,
    ATKeyboard,
    StandardMouse,
    ScrollWheelMouse,
    MouseWith5Buttons,
    MF2Keyboard,
    ThinkPadKeyboard,
    NCDKeyboard,
    HostConnected122KeysKeyboard,
    KeyboardWith122Keys,
    JapaneseGKeyboard,
    JapanesePKeyboard,
    JapaneseAKeyboard,
    NCDSunKeyboard,
};

}
