/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SetOnce.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Bus/PCI/DeviceTreeHelpers.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/BusDirectory.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Userland/Libraries/LibDeviceTree/DeviceTree.h>

namespace Kernel::PCI {

SetOnce g_pci_access_io_probe_failed;
SetOnce g_pci_access_is_disabled_from_commandline;

void initialize()
{
    if (kernel_command_line().is_pci_disabled()) {
        g_pci_access_is_disabled_from_commandline.set();
        return;
    }

    new Access();

    // [1]: https://github.com/devicetree-org/devicetree-specification/releases/download/v0.4/devicetree-specification-v0.4.pdf
    // [2]: https://github.com/devicetree-org/dt-schema/blob/main/dtschema/schemas/pci/pci-bus-common.yaml
    // [3]: https://github.com/devicetree-org/dt-schema/blob/main/dtschema/schemas/pci/pci-host-bridge.yaml

    // The pci controllers are usually in /soc/pcie?@XXXXXXXX on RISC-V, and in /pcie?@XXXXXXXX on AArch64
    // FIXME: They can also appear in any simple-bus other than soc
    auto const& device_tree = DeviceTree::get();

    auto maybe_soc = device_tree.get_child("soc"sv);

    auto const& pci_host_controller_node_parent = maybe_soc.has_value() ? maybe_soc.value() : device_tree;

    enum class ControllerCompatible {
        Unknown,
        ECAM,
    };

    // These properties must be present
    auto soc_address_cells = pci_host_controller_node_parent.get_property("#address-cells"sv).value().as<u32>();
    [[maybe_unused]] auto soc_size_cells = pci_host_controller_node_parent.get_property("#size-cells"sv).value().as<u32>();

    bool found_compatible_pci_controller = false;
    for (auto const& [name, node] : pci_host_controller_node_parent.children()) {
        if (!name.starts_with("pci"sv))
            continue;

        if (auto device_type = node.get_property("device_type"sv); !device_type.has_value() || device_type.value().as_string() != "pci"sv) {
            // Technically, the device_type property is deprecated, but if it is present,
            // no harm's done in checking it anyway
            dmesgln("PCI: PCI named devicetree entry {} not a PCI type device, got device type '{}' instead", name, device_type.has_value() ? device_type.value().as_string() : "<None>"sv);
            continue;
        }

        auto maybe_compatible = node.get_property("compatible"sv);
        if (!maybe_compatible.has_value()) {
            dmesgln("PCI: Devicetree node for {} does not have a 'compatible' string, rejecting", name);
            continue;
        }
        auto compatible = maybe_compatible.value();
        auto controller_compatibility = ControllerCompatible::Unknown;
        // Compatible strings are a list of strings;
        // They should be sorted from most specific to least specific,
        // so it's best to take the first one we recognize
        compatible.for_each_string([&controller_compatibility](StringView compatible_string) -> IterationDecision {
            if (compatible_string == "pci-host-ecam-generic"sv) {
                controller_compatibility = ControllerCompatible::ECAM;
                return IterationDecision::Break;
            }
            // FIXME: Implement CAM (pci-host-cam-generic), but maybe it's to old to be relevant

            return IterationDecision::Continue;
        });
        if (controller_compatibility == ControllerCompatible::Unknown) {
            dmesgln("PCI: Devicetree node for {} does not have a known 'compatible' string, rejecting", name);
            dmesgln("PCI: Compatible strings provided: {}", compatible.as_strings());
            continue;
        }

        auto maybe_reg = node.get_property("reg"sv);
        if (!maybe_reg.has_value()) {
            dmesgln("PCI: Devicetree node for {} does not have a physical address assigned to it, rejecting", name);
            continue;
        }
        auto reg = maybe_reg.value();

        auto domain = determine_pci_domain_for_devicetree_node(node, name).release_value_but_fixme_should_propagate_errors();

        switch (controller_compatibility) {
        case ControllerCompatible::ECAM: {
            // FIXME: Make this use a nice helper function
            // FIXME: Use the provided size field
            auto stream = reg.as_stream();
            FlatPtr paddr = MUST(stream.read_cells(soc_address_cells));

            Access::the().add_host_controller(MemoryBackedHostBridge::must_create(domain, PhysicalAddress { paddr }));
            break;
        }
        case ControllerCompatible::Unknown:
            VERIFY_NOT_REACHED(); // This should have been rejected earlier
        }

        found_compatible_pci_controller = true;

        configure_devicetree_host_controller(node).release_value_but_fixme_should_propagate_errors();
    }

    if (!found_compatible_pci_controller) {
        dmesgln("PCI: No compatible controller found");
        g_pci_access_io_probe_failed.set();
        return;
    }

    Access::the().rescan_hardware();

    PCIBusSysFSDirectory::initialize();

    // FIXME: X86_64 Reserves Interrupts here, maybe we need to do something like this here as well

    MUST(PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        dmesgln("{} {}", device_identifier.address(), device_identifier.hardware_id());
    }));
}

}
