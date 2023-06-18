/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::HardwareClocks {

enum class ClockID {
    Reserved = 0,
    EMMC = 1,
    UART = 2,
    ARM = 3,
    CORE = 4,
    V3D = 5,
    H264 = 6,
    ISP = 7,
    SDRAM = 8,
    PIXEL = 9,
    PWM = 10,
    HEVC = 11,
    EMMC2 = 12,
    M2MC = 13,
    PIXEL_BVB = 14,
};

u32 set_clock_rate(ClockID, u32 rate_hz, bool skip_setting_turbo = true);
u32 get_clock_rate(ClockID);

}
