/*
 * Copyright (c) 2023-2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>

namespace Kernel::PCI {

ErrorOr<void> configure_devicetree_host_controller(::DeviceTree::Node const& node);

}
