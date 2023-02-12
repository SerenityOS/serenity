/*
 * Copyright (c) 2021-2023, Andrew Kaster <akaster@serenityos.org>
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

bool validate_flattened_device_tree(FlattenedDeviceTreeHeader const& header, ReadonlyBytes raw_device_tree, Verbose = Verbose::No);

ErrorOr<void> dump(FlattenedDeviceTreeHeader const& header, ReadonlyBytes raw_device_tree);
ErrorOr<void> dump_flattened_device_tree_structure(FlattenedDeviceTreeHeader const& header, ReadonlyBytes raw_device_tree);

}
