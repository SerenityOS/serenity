/*
 * Copyright (c) 2023-2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Bus/PCI/DeviceTreeHelpers.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::PCI {

class GenericDeviceTreeECAMHostController final : public MemoryBackedHostBridge {
public:
    static ErrorOr<NonnullOwnPtr<GenericDeviceTreeECAMHostController>> create(DeviceTree::Device const& device)
    {
        auto domain = TRY(determine_pci_domain_for_devicetree_node(device.node(), device.node_name()));
        auto configuration_space = TRY(device.get_resource(0));

        if (configuration_space.size < memory_range_per_bus * (domain.end_bus() - domain.start_bus() + 1))
            return ERANGE;

        return adopt_nonnull_own_or_enomem(new (nothrow) GenericDeviceTreeECAMHostController(domain, configuration_space.paddr));
    }

protected:
    GenericDeviceTreeECAMHostController(Domain const& domain, PhysicalAddress physical_address)
        : MemoryBackedHostBridge(domain, physical_address)
    {
    }
};

static constinit Array const compatibles_array = {
    "pci-host-ecam-generic"sv,
};

DEVICETREE_DRIVER(GenericECAMPCIHostControllerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/pci/host-generic-pci.yaml
ErrorOr<void> GenericECAMPCIHostControllerDriver::probe(DeviceTree::Device const& device, StringView) const
{
    if (kernel_command_line().is_pci_disabled())
        return {};

    auto host_controller = TRY(GenericDeviceTreeECAMHostController::create(device));

    TRY(configure_devicetree_host_controller(*host_controller, device.node(), device.node_name()));
    Access::the().add_host_controller(move(host_controller));

    return {};
}

}
