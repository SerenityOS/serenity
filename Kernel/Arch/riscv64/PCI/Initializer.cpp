/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/CPU.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/BusDirectory.h>
#include <Userland/Libraries/LibDeviceTree/DeviceTree.h>

namespace Kernel::PCI {

bool g_pci_access_io_probe_failed { false };
bool g_pci_access_is_disabled_from_commandline;

void initialize()
{
    g_pci_access_is_disabled_from_commandline = kernel_command_line().is_pci_disabled();
    if (g_pci_access_is_disabled_from_commandline)
        return;

    new Access();

    // https://github.com/devicetree-org/devicetree-specification/releases/download/v0.4/devicetree-specification-v0.4.pdf
    // https://github.com/devicetree-org/dt-schema/blob/main/dtschema/schemas/pci/pci-bus-common.yaml
    // https://github.com/devicetree-org/dt-schema/blob/main/dtschema/schemas/pci/pci-host-bridge.yaml

    // The pci controllers are usually in /soc/pcie?@XXXXXXXX
    // FIXME: They can also appear in the root node, or any simple-bus other than soc
    auto const& device_tree = DeviceTree::get();

    auto maybe_soc = device_tree.get_child("soc"sv);
    if (!maybe_soc.has_value()) {
        dmesgln("PCI: No `soc` node found in the device tree, PCI initialization will be skipped");
        return;
    }

    auto const& soc = maybe_soc.value();

    enum class ControllerCompatible {
        Unknown,
        ECAM,
    };

    // These properties must be present
    auto soc_address_cells = soc.get_property("#address-cells"sv).value().as<u32>();
    [[maybe_unused]] auto soc_size_cells = soc.get_property("#size-cells"sv).value().as<u32>();

    Optional<u32> domain_counter;
    for (auto const& entry : soc.children()) {
        if (!entry.key.starts_with("pci"sv))
            continue;
        auto const& node = entry.value;

        if (auto device_type = node.get_property("device_type"sv); !device_type.has_value() || device_type.value().as_string() != "pci"sv) {
            // Technically, the device_type property is deprecated, but if it is present,
            // no harm's done in checking it anyway
            dmesgln("PCI: PCI named devicetree entry {} not a PCI type device, got device type '{}' instead", entry.key, device_type.has_value() ? device_type.value().as_string() : "<None>"sv);
            continue;
        }

        auto maybe_compatible = node.get_property("compatible"sv);
        if (!maybe_compatible.has_value()) {
            dmesgln("PCI: Devicetree node for {} does not have a 'compatible' string, rejecting", entry.key);
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
            dmesgln("PCI: Devicetree node for {} does not have a known 'compatible' string, rejecting", entry.key);
            dmesgln("PCI: Compatible strings provided: {}", compatible.as_strings());
            continue;
        }

        auto maybe_reg = node.get_property("reg"sv);
        if (!maybe_reg.has_value()) {
            dmesgln("PCI: Devicetree node for {} does not have a physical address assigned to it, rejecting", entry.key);
            continue;
        }
        auto reg = maybe_reg.value();

        Array<u8, 2> bus_range { 0, 255 };
        auto maybe_bus_range = node.get_property("bus-range"sv);
        if (maybe_bus_range.has_value()) {
            auto provided_bus_range = maybe_bus_range.value().as<Array<BigEndian<u32>, 2>>();
            // FIXME: Range check these
            bus_range[0] = provided_bus_range[0];
            bus_range[1] = provided_bus_range[1];
        }

        u32 domain;
        auto maybe_domain = node.get_property("linux,pci-domain"sv);
        if (!maybe_domain.has_value()) {
            // FIXME: Make a check similar to the domain counter check below
            if (!domain_counter.has_value())
                domain_counter = 0;
            domain = domain_counter.value();
            domain_counter = domain_counter.value() + 1;
        } else {
            if (domain_counter.has_value()) {
                dmesgln("PCI: Devicetree node for {} has a PCI-domain assigned, but a previous controller did not have one assigned", entry.key);
                dmesgln("PCI: This could lead to domain collisions if handled improperly");
                dmesgln("PCI: We will for now reject this device for now, further investigation is advised");
                continue;
            }
            domain = maybe_domain.value().as<u32>();
        }

        switch (controller_compatibility) {
        case ControllerCompatible::ECAM: {
            // FIXME: Make this use a nice helper function
            // FIXME: Use the provided size field
            auto stream = reg.as_stream();
            FlatPtr paddr;
            if (soc_address_cells == 1)
                paddr = MUST(stream.read_value<BigEndian<u32>>());
            else
                paddr = MUST(stream.read_value<BigEndian<u64>>());

            Access::the().add_host_controller(
                MemoryBackedHostBridge::must_create(
                    Domain {
                        domain,
                        bus_range[0],
                        bus_range[1],
                    },
                    PhysicalAddress { paddr }));
            break;
        }
        case ControllerCompatible::Unknown:
            VERIFY_NOT_REACHED(); // This should have been rejected earlier
        }
    }

    Access::the().rescan_hardware();

    PCIBusSysFSDirectory::initialize();

    // FIXME: X86_64 Reserves Interrupts here, maybe we need to do something like this here as well

    MUST(PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        dmesgln("{} {}", device_identifier.address(), device_identifier.hardware_id());
    }));
}

}
