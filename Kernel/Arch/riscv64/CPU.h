/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <LibDeviceTree/DeviceTree.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

constexpr size_t fdt_storage_size = 2 * MiB;
extern u8 s_fdt_storage[fdt_storage_size];

// FIXME: These should move to an architecture independent location,
//        once we need device tree parsing in other architectures, like aarch64
extern BootInfo s_boot_info;

ErrorOr<void> unflatten_fdt();
void dump_fdt();
ErrorOr<StringView> get_command_line_from_fdt();
}

namespace DeviceTree {
DeviceTree const& get();
}
