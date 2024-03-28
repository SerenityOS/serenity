/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Memory/PhysicalAddress.h>
#include <LibDeviceTree/DeviceTree.h>

namespace Kernel {
constexpr size_t fdt_storage_size = 2 * MiB;
extern u8 s_fdt_storage[fdt_storage_size];

ErrorOr<void> unflatten_fdt();
bool verify_fdt();
void dump_fdt();
ErrorOr<StringView> get_command_line_from_fdt();
}

namespace DeviceTree {
DeviceTree const& get();
}
