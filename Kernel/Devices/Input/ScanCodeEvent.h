/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/Input/Definitions.h>

namespace Kernel {

struct ScanCodeEvent {
    Array<u8, 8> scan_code_bytes;
    ScanCodeSet sent_scan_code_set { ScanCodeSet::Set1 };
    u8 bytes_count { 0 };
};

}
