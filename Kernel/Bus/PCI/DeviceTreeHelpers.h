/*
 * Copyright (c) 2023-2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>

namespace Kernel::PCI {

ErrorOr<Domain> determine_pci_domain_for_devicetree_node(::DeviceTree::Node const&, StringView node_name);
ErrorOr<void> configure_devicetree_host_controller(HostController&, ::DeviceTree::Node const&, StringView node_name);

}
