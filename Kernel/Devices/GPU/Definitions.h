/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::Graphics {

// Note: Address 0x50 is expected to be the DDC2 (EDID) i2c address.
static constexpr u8 ddc2_i2c_address = 0x50;

}
