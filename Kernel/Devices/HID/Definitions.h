/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/API/KeyCode.h>

namespace Kernel {

enum class ScanCodeSet {
    Set1 = 1,
    Set2 = 2,
    Set3 = 3,
};

struct KeyCodeEntry {
    KeyCode key_code;
    u8 map_entry_index;
};

}
