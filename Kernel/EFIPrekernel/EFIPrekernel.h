/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Firmware/EFI/SystemTable.h>

namespace Kernel {

struct EFIMemoryMap {
    EFI::MemoryDescriptor* descriptor_array { nullptr };
    EFI::PhysicalAddress descriptor_array_paddr { 0 };
    FlatPtr descriptor_array_size { 0 };
    FlatPtr descriptor_size { 0 };
    FlatPtr buffer_size { 0 };
    FlatPtr map_key { 0 };
    u32 descriptor_version { 0 };
};

}
