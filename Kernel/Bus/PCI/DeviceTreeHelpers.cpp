/*
 * Copyright (c) 2023-2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/DeviceTreeHelpers.h>
#include <Kernel/Library/StdLib.h>
#include <LibDeviceTree/DeviceTree.h>

namespace Kernel::PCI {

// Common properties for PCI host bridge nodes: https://github.com/devicetree-org/dt-schema/blob/main/dtschema/schemas/pci/pci-host-bridge.yaml
// Common properties for PCI bus structure: https://github.com/devicetree-org/dt-schema/blob/main/dtschema/schemas/pci/pci-bus-common.yaml
// PCI Bus Binding to IEEE Std 1275-1994: https://www.devicetree.org/open-firmware/bindings/pci/pci2_1.pdf

static TriState s_linux_pci_domain_property_used = TriState::Unknown;
static u32 s_next_pci_domain_number { 0 };

ErrorOr<Domain> determine_pci_domain_for_devicetree_node(::DeviceTree::Node const& node, StringView node_name)
{
    // PCI Bus Binding to IEEE Std 1275-1994, 3.1.2. Bus-specific Properties for Bus Nodes:
    // ""bus-range" [...] denotes range of bus numbers controlled by this PCI bus."
    Array<u8, 2> bus_range { 0, 255 };
    auto maybe_bus_range = node.get_property("bus-range"sv);
    if (maybe_bus_range.has_value()) {
        if (maybe_bus_range->size() != sizeof(u32) * 2)
            return EINVAL;

        auto provided_bus_range = maybe_bus_range->as<Array<BigEndian<u32>, 2>>();
        if (provided_bus_range[0] > 255 || provided_bus_range[1] > 255 || provided_bus_range[1] < provided_bus_range[0])
            return EINVAL;

        bus_range[0] = provided_bus_range[0];
        bus_range[1] = provided_bus_range[1];
    }

    // https://github.com/devicetree-org/dt-schema/blob/main/dtschema/schemas/pci/pci-host-bridge.yaml:
    // linux,pci-domain:
    // "If present this property assigns a fixed PCI domain number to a host bridge,
    //  otherwise an unstable (across boots) unique number will be assigned.
    //  It is required to either not set this property at all or set it for all
    //  host bridges in the system, otherwise potentially conflicting domain numbers
    //  may be assigned to root buses behind different host bridges.  The domain
    //  number for each host bridge in the system must be unique."
    u32 domain_number = 0;
    auto maybe_domain_number = node.get_property("linux,pci-domain"sv);

    if (s_linux_pci_domain_property_used != TriState::Unknown && static_cast<TriState>(maybe_domain_number.has_value()) != s_linux_pci_domain_property_used) {
        dbgln("PCI: Either all or no PCI host bridge devicetree nodes must have a \"linux,pci-domain\" property");
        return EINVAL;
    }

    if (maybe_domain_number.has_value()) {
        if (maybe_domain_number->size() != sizeof(u32))
            return EINVAL;

        domain_number = maybe_domain_number->as<u32>();
    } else {
        domain_number = s_next_pci_domain_number++;
    }

    s_linux_pci_domain_property_used = static_cast<TriState>(maybe_domain_number.has_value());

    dbgln("PCI: Assigned domain number {} for {}", domain_number, node_name);

    return Domain {
        domain_number,
        bus_range[0],
        bus_range[1],
    };
}

ErrorOr<void> configure_devicetree_host_controller(HostController& host_controller, ::DeviceTree::Node const& node, StringView node_name)
{
    FlatPtr pci_32bit_mmio_base = 0;
    u32 pci_32bit_mmio_size = 0;
    FlatPtr pci_64bit_mmio_base = 0;
    u64 pci_64bit_mmio_size = 0;
    HashMap<PCIInterruptSpecifier, u64> masked_interrupt_mapping;
    PCIInterruptSpecifier interrupt_mask;

    auto const& device_tree = DeviceTree::get();

    auto const* parent = node.parent();

    if (parent == nullptr)
        return EINVAL;

    auto maybe_ranges = node.ranges();
    if (!maybe_ranges.is_error()) {
        dbgln("PCI: Address mapping for {}:", node_name);

        for (auto range : maybe_ranges.release_value()) {
            auto pci_address = TRY(range.child_bus_address().as<OpenFirmwareAddress>());
            auto cpu_physical_address = TRY(TRY(parent->translate_child_bus_address_to_root_address(range.parent_bus_address())).as_flatptr());
            auto range_size = TRY(range.length().as_size_t());

            static constexpr auto space_type_names = Array {
                "Configuration Space"sv,
                "I/O Space"sv,
                "32-bit-address Memory Space"sv,
                "64-bit-address Memory Space"sv
            };

            dbgln("  CPU {:p}-{:p} => PCI {:#016x}-{:#016x} {} prefetchable={} relocatable={}",
                cpu_physical_address,
                cpu_physical_address + range_size,
                pci_address.io_or_memory_space_address,
                pci_address.io_or_memory_space_address + range_size,
                space_type_names[to_underlying(pci_address.space_type)],
                !!pci_address.prefetchable,
                !pci_address.non_relocatable);

            if (pci_address.space_type != OpenFirmwareAddress::SpaceType::Memory32BitSpace
                && pci_address.space_type != OpenFirmwareAddress::SpaceType::Memory64BitSpace)
                continue; // We currently only support memory-mapped PCI on RISC-V and AArch64

            TRY(host_controller.add_memory_space_window(HostController::Window {
                .host_address = PhysicalAddress { cpu_physical_address },
                .bus_address = pci_address.io_or_memory_space_address,
                .size = range_size,
            }));

            if (pci_address.space_type == OpenFirmwareAddress::SpaceType::Memory32BitSpace) {
                if (pci_address.prefetchable)
                    continue; // We currently only use non-prefetchable 32-bit regions, since 64-bit regions are always prefetchable - TODO: Use 32-bit prefetchable regions if only they are available
                if (pci_32bit_mmio_size >= range_size)
                    continue; // We currently only use the single largest region - TODO: Use all available regions if needed

                pci_32bit_mmio_base = pci_address.io_or_memory_space_address;
                pci_32bit_mmio_size = range_size;
            } else {
                if (pci_64bit_mmio_size >= range_size)
                    continue; // We currently only use the single largest region - TODO: Use all available regions if needed

                pci_64bit_mmio_base = pci_address.io_or_memory_space_address;
                pci_64bit_mmio_size = range_size;
            }
        }
    }

    // 2.4.3 Interrupt Nexus Properties
    // #interrupt-cells: [2] `1` for pci busses
    // interrupt-map:
    //  [{
    //     child-unit-address(bus-node/#address-cells|3),
    //     child-interrupt-specifier(#interrupt-cells|1),
    //     interrupt-parent(phandle),
    //     parent-unit-address(interrupt-parent/#address-cells),
    //     parent-interrupt-specifier(interrupt-parent/#interrupt-cells)
    //  }]
    //   Note: The bus-node may be any other bus the child is connected to
    //   FIXME?: Let's just hope this is always this/a PCI bus
    // interrupt-map-mask:
    // > This property specifies a  mask that is ANDed with the incoming
    // > unit interrupt specifier being looked up in the table specified in the
    // > interrupt-map property.
    // Hence this should be of size:
    // pci/#address-cells(3) + #interrupt-cells(1) = 4
    auto maybe_interrupt_map = node.get_property("interrupt-map"sv);
    auto maybe_interrupt_map_mask = node.get_property("interrupt-map-mask"sv);
    if (maybe_interrupt_map.has_value() && maybe_interrupt_map_mask.has_value()) {
        auto maybe_interrupt_cells = node.get_property("#interrupt-cells"sv);
        if (!maybe_interrupt_cells.has_value())
            return EINVAL;

        if (maybe_interrupt_cells->size() != sizeof(u32) || maybe_interrupt_cells->as<u32>() != 1)
            return EINVAL;

        if (maybe_interrupt_map_mask->size() != 4 * sizeof(u32))
            return EINVAL;

        auto mask_stream = maybe_interrupt_map_mask.value().as_stream();
        auto pci_address_mask = TRY(mask_stream.read_value<OpenFirmwareAddress>());

        // The "interrupt-map-mask" constraints from https://github.com/devicetree-org/dt-schema/blob/main/dtschema/schemas/pci/pci-bus-common.yaml
        // only allow the function and device mask fields to be non-zero and the interrupt specifier mask to be between 0 and 7.
        if (pci_address_mask.register_ != 0
            || pci_address_mask.bus != 0
            || pci_address_mask.space_type != static_cast<OpenFirmwareAddress::SpaceType>(0)
            || pci_address_mask.aliased != 0
            || pci_address_mask.prefetchable != 0
            || pci_address_mask.non_relocatable != 0
            || pci_address_mask.io_or_memory_space_address != 0) {
            return EINVAL;
        }

        u32 pin_mask = TRY(mask_stream.read_cell());
        if (pin_mask > 7)
            return EINVAL;

        interrupt_mask = PCIInterruptSpecifier {
            .interrupt_pin = static_cast<u8>(pin_mask),
            .function = pci_address_mask.function,
            .device = pci_address_mask.device,
            .bus = pci_address_mask.bus,
        };
        auto map_stream = maybe_interrupt_map.value().as_stream();
        while (!map_stream.is_eof()) {
            auto pci_address = TRY(map_stream.read_value<OpenFirmwareAddress>());
            u32 pin = TRY(map_stream.read_cell());

            u32 interrupt_controller_phandle = TRY(map_stream.read_cell());
            auto const* interrupt_controller = device_tree.phandle(interrupt_controller_phandle);
            if (interrupt_controller == nullptr)
                return EINVAL;

            if (!interrupt_controller->has_property("interrupt-controller"sv)) {
                dmesgln("PCI: Implement support for nested interrupt nexuses");
                TODO();
            }

            // The devicetree spec says that we should assume `#address-cells = <2>` if the property isn't present.
            // Linux however defaults to `#address-cells = <0>` for interrupt parent nodes.
            // Some devicetrees seem to expect this Linux behavior, so we have to follow it.
            u32 interrupt_controller_address_cells = 0;

            if (auto prop = interrupt_controller->get_property("#address-cells"sv); prop.has_value())
                interrupt_controller_address_cells = prop.release_value().as<u32>();

            TRY(map_stream.discard(sizeof(u32) * interrupt_controller_address_cells));

            auto interrupt_cells = interrupt_controller->get_property("#interrupt-cells"sv)->as<u32>();
#if ARCH(RISCV64)
            if (interrupt_cells != 1 && interrupt_cells != 2)
                return ENOTSUP;

            u64 interrupt = TRY(map_stream.read_cells(interrupt_cells));
#elif ARCH(AARCH64)
            // FIXME: Don't depend on a specific interrupt descriptor format.
            auto const& domain_root = *TRY(interrupt_controller->interrupt_domain_root(device_tree));
            if (!domain_root.is_compatible_with("arm,gic-400"sv) && !domain_root.is_compatible_with("arm,cortex-a15-gic"sv))
                return ENOTSUP;

            if (interrupt_cells != 3)
                return ENOTSUP;

            TRY(map_stream.discard(sizeof(u32))); // This is the IRQ type.
            u64 interrupt = TRY(map_stream.read_cell()) + 32;
            TRY(map_stream.discard(sizeof(u32))); // This is the trigger type.
#else
#    error Unknown architecture
#endif

            pin &= pin_mask;
            pci_address &= pci_address_mask;
            masked_interrupt_mapping.set(
                PCIInterruptSpecifier {
                    .interrupt_pin = static_cast<u8>(pin),
                    .function = pci_address.function,
                    .device = pci_address.device,
                    .bus = pci_address.bus,
                },
                interrupt);
        }
    }

    if (pci_32bit_mmio_size != 0 || pci_64bit_mmio_size != 0) {
        PCIConfiguration config {
            pci_32bit_mmio_base,
            pci_32bit_mmio_base + pci_32bit_mmio_size,
            pci_64bit_mmio_base,
            pci_64bit_mmio_base + pci_64bit_mmio_size,
            move(masked_interrupt_mapping),
            interrupt_mask,
        };
        Access::the().configure_pci_space(host_controller, config);
    } else {
        dmesgln("PCI: No MMIO ranges found - assuming pre-configured by bootloader");
    }

    return {};
}

}
