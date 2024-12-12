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

    Optional<u32> domain_counter;
    FlatPtr pci_32bit_mmio_base = 0;
    u32 pci_32bit_mmio_size = 0;
    FlatPtr pci_64bit_mmio_base = 0;
    u64 pci_64bit_mmio_size = 0;
    HashMap<PCIInterruptSpecifier, u64> masked_interrupt_mapping;
    PCIInterruptSpecifier interrupt_mask;
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
                dmesgln("PCI: Devicetree node for {} has a PCI-domain assigned, but a previous controller did not have one assigned", name);
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
            FlatPtr paddr = MUST(stream.read_cells(soc_address_cells));

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

        found_compatible_pci_controller = true;

        auto maybe_ranges = node.get_property("ranges"sv);
        if (maybe_ranges.has_value()) {
            auto address_cells = node.get_property("#address-cells"sv).value().as<u32>();
            VERIFY(address_cells == 3); // Additional cell for OpenFirmware PCI address metadata
            auto size_cells = node.get_property("#size-cells"sv).value().as<u32>();

            auto stream = maybe_ranges.value().as_stream();
            while (!stream.is_eof()) {
                auto pci_address_metadata = bit_cast<OpenFirmwareAddress>(MUST(stream.read_cell()));
                FlatPtr pci_address = MUST(stream.read_cells(2));

                FlatPtr mmio_address = MUST(stream.read_cells(soc_address_cells));
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
    }

    if (!found_compatible_pci_controller) {
        dmesgln("PCI: No compatible controller found");
        g_pci_access_io_probe_failed.set();
        return;
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
    Access::the().rescan_hardware();

    PCIBusSysFSDirectory::initialize();

    // FIXME: X86_64 Reserves Interrupts here, maybe we need to do something like this here as well

    MUST(PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        dmesgln("{} {}", device_identifier.address(), device_identifier.hardware_id());
    }));
}

}
