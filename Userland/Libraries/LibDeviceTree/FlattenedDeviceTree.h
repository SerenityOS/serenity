/*
 * Copyright (c) 2021-2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/IterationDecision.h>
#include <AK/StringView.h>
#include <AK/Types.h>

#include "DeviceTree.h"

namespace DeviceTree {

// https://devicetree-specification.readthedocs.io/en/v0.3/flattened-format.html
struct FlattenedDeviceTreeHeader {
    BigEndian<u32> magic;             // 0xD00DFEED (BE)
    BigEndian<u32> totalsize;         // Total size of entire blob including padding b/w and after fields
    BigEndian<u32> off_dt_struct;     // Offset of StructureBlock from beginning of header
                                      // https://devicetree-specification.readthedocs.io/en/v0.3/flattened-format.html#sect-fdt-structure-block
    BigEndian<u32> off_dt_strings;    // Offset of StringsBlock from beginning of header
                                      // https://devicetree-specification.readthedocs.io/en/v0.3/flattened-format.html#sect-fdt-strings-block
    BigEndian<u32> off_mem_rsvmap;    // Offset of MemoryReservationBlock from beginning of header
    BigEndian<u32> version;           // 0.3 spec defines version 17
    BigEndian<u32> last_comp_version; // 0.3 spec back-compatible with version 16, mandated to be 16
    BigEndian<u32> boot_cpuid_phys;   // Physical ID given in `reg` property of CPU node of boot cpu
    BigEndian<u32> size_dt_strings;   // Length in bytes of StringsBlock
    BigEndian<u32> size_dt_struct;    // Length in bytes of StructureBlock
};
static_assert(sizeof(FlattenedDeviceTreeHeader) == 40, "FDT Header size must match specification");

// https://devicetree-specification.readthedocs.io/en/v0.3/flattened-format.html#format
struct FlattenedDeviceTreeReserveEntry {
    BigEndian<u64> address;
    BigEndian<u64> size;

    bool operator==(FlattenedDeviceTreeReserveEntry const& other) const { return other.address == address && other.size == size; }
};
static_assert(sizeof(FlattenedDeviceTreeReserveEntry) == 16, "FDT Memory Reservation entry size must match specification");

// https://devicetree-specification.readthedocs.io/en/v0.3/flattened-format.html#lexical-structure
enum class FlattenedDeviceTreeTokenType : u32 {
    BeginNode = 1,
    EndNode = 2,
    Property = 3,
    NoOp = 4,
    End = 9
};

struct DeviceTreeCallbacks {
    Function<ErrorOr<IterationDecision>(StringView)> on_node_begin;
    Function<ErrorOr<IterationDecision>(StringView)> on_node_end;
    Function<ErrorOr<IterationDecision>(StringView, ReadonlyBytes)> on_property;
    Function<ErrorOr<IterationDecision>()> on_noop;
    Function<ErrorOr<void>()> on_end;
};

ErrorOr<void> walk_device_tree(FlattenedDeviceTreeHeader const&, ReadonlyBytes raw_device_tree, DeviceTreeCallbacks);

ErrorOr<Property> slow_get_property(StringView name, FlattenedDeviceTreeHeader const&, ReadonlyBytes raw_device_tree);

} // namespace DeviceTree
