/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace Kernel::DeviceTree {

template<typename DevicePtr>
struct DeviceRecipe {
    StringView driver_name;
    StringView node_name;
    Function<ErrorOr<DevicePtr>()> create_device;
};

}
