/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Prekernel {

// Can exchange mailbox messages with the Raspberry Pi's VideoCore chip.
// https://github.com/raspberrypi/firmware/wiki/Mailbox-property-interface
class Mailbox {
public:
    static bool call(u8 channel, u32 volatile* __attribute__((aligned(16))) data);

    static u32 query_firmware_version();

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
    static u32 set_clock_rate(ClockID, u32 rate_hz, bool skip_setting_turbo = true);
};

}
