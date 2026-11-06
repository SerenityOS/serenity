/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Firmware/ACPI/GenericAddress.h>
#include <Kernel/Memory/TypedMapping.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/IO.h>
#endif

namespace Kernel::ACPI {

ErrorOr<void> access_generic_address(::ACPI::Structures::GenericAddressStructure const& structure, u32 value)
{
    switch ((::ACPI::GenericAddressStructure::AddressSpace)structure.address_space) {
    case ::ACPI::GenericAddressStructure::AddressSpace::SystemIO: {
#if ARCH(X86_64)
        IOAddress address(structure.address);
        dbgln("ACPI: Sending value {:x} to {}", value, address);
        switch (structure.access_size) {
        case ::ACPI::GenericAddressStructure::AccessSize::QWord: {
            dbgln("Trying to send QWord to IO port");
            return EINVAL;
        }
        case ::ACPI::GenericAddressStructure::AccessSize::Undefined: {
            dbgln("ACPI Warning: Unknown access size {}", to_underlying(structure.access_size));
            if (structure.bit_width == ::ACPI::GenericAddressStructure::BitWidth::QWord || structure.bit_width == ::ACPI::GenericAddressStructure::BitWidth::Undefined) {
                return EINVAL;
            }

            dbgln("ACPI: Bit Width - {} bits", to_underlying(structure.bit_width));
            address.out(value, to_underlying(structure.bit_width));
            break;
        }
        default:
            address.out(value, (8 << (to_underlying(structure.access_size) - 1)));
            break;
        }
#endif
        return {};
    }
    case ::ACPI::GenericAddressStructure::AddressSpace::SystemMemory: {
        dbgln("ACPI: Sending value {:x} to {}", value, PhysicalAddress(structure.address));
        switch ((::ACPI::GenericAddressStructure::AccessSize)structure.access_size) {
        case ::ACPI::GenericAddressStructure::AccessSize::Byte:
            *TRY(Memory::map_typed<u8>(PhysicalAddress(structure.address))) = value;
            break;
        case ::ACPI::GenericAddressStructure::AccessSize::Word:
            *TRY(Memory::map_typed<u16>(PhysicalAddress(structure.address))) = value;
            break;
        case ::ACPI::GenericAddressStructure::AccessSize::DWord:
            *TRY(Memory::map_typed<u32>(PhysicalAddress(structure.address))) = value;
            break;
        case ::ACPI::GenericAddressStructure::AccessSize::QWord: {
            *TRY(Memory::map_typed<u64>(PhysicalAddress(structure.address))) = value;
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }
        return {};
    }
    case ::ACPI::GenericAddressStructure::AddressSpace::PCIConfigurationSpace: {
        // According to https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#address-space-format,
        // PCI addresses must be confined to devices on Segment group 0, bus 0.
        auto pci_address = PCI::Address(0, 0, ((structure.address >> 24) & 0xFF), ((structure.address >> 16) & 0xFF));
        dbgln("ACPI: Sending value {:x} to {}", value, pci_address);

        u32 offset_in_pci_address = structure.address & 0xFFFF;
        if (structure.access_size == ::ACPI::GenericAddressStructure::AccessSize::QWord || structure.access_size == ::ACPI::GenericAddressStructure::AccessSize::Undefined) {
            dbgln("Trying to send QWord or Undefined to PCI configuration space");
            return EINVAL;
        }

        auto& pci_device_identifier = PCI::get_device_identifier(pci_address);
        PCI::raw_access(pci_device_identifier, offset_in_pci_address, (1 << (to_underlying(structure.access_size) - 1)), value);
        return {};
    }
    default:
        dbgln("ACPI: Tried to access an unsupported address space");
        return EINVAL;
    }

    VERIFY_NOT_REACHED();
}

}
