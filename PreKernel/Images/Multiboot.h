/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Multiboot.h>

namespace Multiboot {

enum MemoryEntryType {
    Available = 1,
    Reserved = 2,
    ACPIReclaimable = 3,
    ACPINVS = 4,
    FaultyRAM = 5,
};

StringView parse_entry_type(MemoryEntryType);

}
