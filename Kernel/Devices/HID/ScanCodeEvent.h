/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel {

struct ScanCodeEvent {
    u32 scan_code_value { 0 };
    bool pressed { false };
    bool e0_prefix { false };
};

}
