/*
 * Copyright (c) 2023-2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/HostController.h>
#include <Kernel/Bus/PCI/DeviceTreeHelpers.h>
#include <LibDeviceTree/DeviceTree.h>

namespace Kernel::PCI {

ErrorOr<void> configure_devicetree_host_controller(::DeviceTree::Node const& node)
{
    FlatPtr pci_32bit_mmio_base = 0;
    u32 pci_32bit_mmio_size = 0;
    FlatPtr pci_64bit_mmio_base = 0;
    u64 pci_64bit_mmio_size = 0;
    HashMap<PCIInterruptSpecifier, u64> masked_interrupt_mapping;
    PCIInterruptSpecifier interrupt_mask;

    auto const& device_tree = DeviceTree::get();

    if (node.parent() == nullptr)
        return EINVAL;

    auto parent_address_cells = node.parent()->address_cells();

    auto maybe_ranges = node.get_property("ranges"sv);
    if (maybe_ranges.has_value()) {
        auto address_cells = node.get_property("#address-cells"sv).value().as<u32>();
        VERIFY(address_cells == 3); // Additional cell for OpenFirmware PCI address metadata
        auto size_cells = node.get_property("#size-cells"sv).value().as<u32>();

        auto stream = maybe_ranges.value().as_stream();
        while (!stream.is_eof()) {
            auto pci_address_metadata = bit_cast<OpenFirmwareAddress>(MUST(stream.read_cell()));
            FlatPtr pci_address = MUST(stream.read_cells(2));

            FlatPtr mmio_address = MUST(stream.read_cells(parent_address_cells));
            u64 mmio_size = MUST(stream.read_cells(size_cells));

            if (pci_address_metadata.space_type != OpenFirmwareAddress::SpaceType::Memory32BitSpace
                && pci_address_metadata.space_type != OpenFirmwareAddress::SpaceType::Memory64BitSpace)
                continue; // We currently only support memory-mapped PCI on RISC-V and AArch64

            // TODO: Support mapped PCI addresses
            VERIFY(pci_address == mmio_address);
            if (pci_address_metadata.space_type == OpenFirmwareAddress::SpaceType::Memory32BitSpace) {
                if (pci_address_metadata.prefetchable)
                    continue; // We currently only use non-prefetchable 32-bit regions, since 64-bit regions are always prefetchable - TODO: Use 32-bit prefetchable regions if only they are available
                if (pci_32bit_mmio_size >= mmio_size)
                    continue; // We currently only use the single largest region - TODO: Use all available regions if needed

                pci_32bit_mmio_base = mmio_address;
                pci_32bit_mmio_size = mmio_size;
            } else {
                if (pci_64bit_mmio_size >= mmio_size)
                    continue; // We currently only use the single largest region - TODO: Use all available regions if needed

                pci_64bit_mmio_base = mmio_address;
                pci_64bit_mmio_size = mmio_size;
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
        VERIFY(node.get_property("#interrupt-cells"sv)->as<u32>() == 1);
        VERIFY(maybe_interrupt_map_mask.value().size() == 4 * sizeof(u32));

        auto mask_stream = maybe_interrupt_map_mask.value().as_stream();
        auto metadata_mask = bit_cast<OpenFirmwareAddress>(MUST(mask_stream.read_cell()));
        u64 phyical_address_mask = MUST(mask_stream.read_cells(2));
        // [2]: phys.mid and phys.lo mask should be 0 -> physical-address-mask = 0
        //      0 < metadata_mask < 0xff00
        VERIFY(phyical_address_mask == 0);
        VERIFY(metadata_mask.raw <= 0xff00);
        // Additionally it would be ludicrous/impossible to differentiate interrupts on registers
        VERIFY(metadata_mask.register_ == 0);

        u32 pin_mask = MUST(mask_stream.read_cell());
        // [2]: The interrupt specifier mask should be between 0 and 7
        VERIFY(pin_mask <= 7);

        interrupt_mask = PCIInterruptSpecifier {
            .interrupt_pin = static_cast<u8>(pin_mask),
            .function = metadata_mask.function,
            .device = metadata_mask.device,
            .bus = metadata_mask.bus,
        };
        auto map_stream = maybe_interrupt_map.value().as_stream();
        while (!map_stream.is_eof()) {
            auto pci_address_metadata = bit_cast<OpenFirmwareAddress>(MUST(map_stream.read_cell()));
            MUST(map_stream.discard(sizeof(u32) * 2)); // Physical Address, the mask for those is guaranteed to be 0
            u32 pin = MUST(map_stream.read_cell());

            u32 interrupt_controller_phandle = MUST(map_stream.read_cell());
            auto const* interrupt_controller = device_tree.phandle(interrupt_controller_phandle);
            VERIFY(interrupt_controller);

            if (!interrupt_controller->has_property("interrupt-controller"sv)) {
                dmesgln("PCI: Implement support for nested interrupt nexuses");
                TODO();
            }

            MUST(map_stream.discard(sizeof(u32) * interrupt_controller->address_cells()));

            auto interrupt_cells = interrupt_controller->get_property("#interrupt-cells"sv)->as<u32>();
#if ARCH(RISCV64)
            VERIFY(interrupt_cells == 1 || interrupt_cells == 2);
            u64 interrupt = MUST(map_stream.read_cells(interrupt_cells));
#elif ARCH(AARCH64)
            // FIXME: Don't depend on a specific interrupt descriptor format.
            auto const& domain_root = *MUST(interrupt_controller->interrupt_domain_root(device_tree));
            if (!domain_root.is_compatible_with("arm,gic-400"sv) && !domain_root.is_compatible_with("arm,cortex-a15-gic"sv))
                TODO();

            VERIFY(interrupt_cells == 3);
            MUST(map_stream.discard(sizeof(u32))); // This is the IRQ type.
            u64 interrupt = MUST(map_stream.read_cell()) + 32;
            MUST(map_stream.discard(sizeof(u32))); // This is the trigger type.
#else
#    error Unknown architecture
#endif

            pin &= pin_mask;
            pci_address_metadata.raw &= metadata_mask.raw;
            masked_interrupt_mapping.set(
                PCIInterruptSpecifier {
                    .interrupt_pin = static_cast<u8>(pin),
                    .function = pci_address_metadata.function,
                    .device = pci_address_metadata.device,
                    .bus = pci_address_metadata.bus,
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
        Access::the().configure_pci_space(config);
    } else {
        dmesgln("PCI: No MMIO ranges found - assuming pre-configured by bootloader");
    }

    return {};
}

}
