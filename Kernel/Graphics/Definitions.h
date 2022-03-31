/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::Graphics {

struct Timings {
    size_t blanking_start() const
    {
        return active;
    }
    size_t blanking_end() const
    {
        return total;
    }

    size_t active;
    size_t sync_start;
    size_t sync_end;
    size_t total;
};

struct Modesetting {
    size_t pixel_clock_in_khz;
    Timings horizontal;
    Timings vertical;
};
// Note: Address 0x50 is expected to be the DDC2 (EDID) i2c address.
static constexpr u8 ddc2_i2c_address = 0x50;

}
