/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

// The virtio input device uses the linux evdev event format.
// Event types and codes are defined here: https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/tree/include/uapi/linux/input-event-codes.h

enum class EvDevEventType : u16 {
    Syn = 0x00,
    Key = 0x01,
    Rel = 0x02,
    Abs = 0x03,
};

enum class EvDevEventCode : u16 {
    // For EvDevEventType::Syn
    SynReport = 0x00,

    // For EvDevEventType::Key
    ButtonLeft = 0x110,
    ButtonRight = 0x111,
    ButtonMiddle = 0x112,

    KeyLeftControl = 0x1d,
    KeyLeftShift = 0x2a,
    KeyRightShift = 0x36,
    KeyKeypadAsterisk = 0x37,
    KeyLeftAlt = 0x38,
    KeyKeypad7 = 0x47,
    KeyKeypadDot = 0x53,
    KeyKeypadEnter = 0x60,
    KeyRightControl = 0x61,
    KeyKeypadSlash = 0x62,
    KeyRightAlt = 0x64,
    KeyKeypadEqual = 0x75,
    KeyLeftMeta = 0x7d,
    KeyRightMeta = 0x7e,

    // For EvDevEventType::Rel
    RelX = 0x00,
    RelY = 0x01,
    RelWheel = 0x08,

    // For EvDevEventType::Abs
    AbsX = 0x00,
    AbsY = 0x01,
};

}
