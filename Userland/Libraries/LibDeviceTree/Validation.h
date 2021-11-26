/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDeviceTree/FlattenedDeviceTree.h>

namespace DeviceTree {

enum class Verbose {
    No,
    Yes
};

bool validate_flattened_device_tree(FlattenedDeviceTreeHeader const& header, u8 const* blob_start, size_t blob_size, Verbose = Verbose::No);

bool dump(FlattenedDeviceTreeHeader const& header, u8 const* blob_start, size_t blob_size);

}
