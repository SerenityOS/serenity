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
};

}
