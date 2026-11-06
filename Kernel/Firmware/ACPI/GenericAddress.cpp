/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntegralMath.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Firmware/ACPI/GenericAddress.h>
#include <Kernel/Memory/TypedMapping.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/IO.h>
#endif

namespace Kernel::ACPI {

::ACPI::Structures::GenericAddressStructure make_generic_address_from_io_port(u32 io_port)
{
    return ::ACPI::Structures::GenericAddressStructure {
        .address_space = ::ACPI::GenericAddressStructure::AddressSpace::SystemIO,
        .bit_width = ::ACPI::GenericAddressStructure::BitWidth::Word,
        .bit_offset = 0,
        .access_size = ::ACPI::GenericAddressStructure::AccessSize::Word,
        .address = io_port,
    };
}

static ErrorOr<u8> get_bit_width(::ACPI::Structures::GenericAddressStructure const& structure)
{
    // NOTE: We need to do a more complicated algorithm because for legacy reasons there are,
    //       two different ways of specifying the address structure, the FADT register format,
    //       and the APEI region format. Here we follow what both uACPI and ACPICA do.

    bool is_typical_bit_width = false;
    if (structure.bit_width == ::ACPI::GenericAddressStructure::BitWidth::Byte || structure.bit_width == ::ACPI::GenericAddressStructure::BitWidth::Word || structure.bit_width == ::ACPI::GenericAddressStructure::BitWidth::DWord || structure.bit_width == ::ACPI::GenericAddressStructure::BitWidth::QWord)
        is_typical_bit_width = true;

    u8 bit_width;

    if (structure.bit_offset == 0 && is_typical_bit_width) {
        bit_width = to_underlying(structure.bit_width);
    } else if (structure.access_size != ::ACPI::GenericAddressStructure::AccessSize::Undefined) {
        bit_width = 8 << (to_underlying(structure.access_size) - 1);
    } else {
        bit_width = 1 << AK::ceil_log2(structure.bit_offset + to_underlying(structure.bit_width));

        if (bit_width <= 8) {
            bit_width = 8;
        } else {
            while (structure.address % (bit_width / 8) != 0)
                bit_width /= 2;
        }
    }

    return AK::min(bit_width, structure.address_space == ::ACPI::GenericAddressStructure::AddressSpace::SystemIO ? 32 : 64);
}

ErrorOr<u64> read_generic_address(::ACPI::Structures::GenericAddressStructure const& structure)
{
    auto bit_width = TRY(get_bit_width(structure));

    switch (structure.address_space) {
    case ::ACPI::GenericAddressStructure::AddressSpace::SystemIO: {
#if ARCH(X86_64)
        IOAddress address(structure.address);
        dbgln_if(ACPI_DEBUG, "ACPI: Reading from {}", address);

        switch (bit_width) {
        case 8:
            return address.in<u8>();
        case 16:
            return address.in<u16>();
        case 32:
            return address.in<u32>();
        default:
            dbgln("ACPI: Tried to read an unsupported bit width from an IO port");
            return EINVAL;
        }
#else
        dbgln("ACPI: Tried to access SystemIO on a non x86 architecture");
        return EINVAL;
#endif
    }

    case ::ACPI::GenericAddressStructure::AddressSpace::SystemMemory: {
        dbgln_if(ACPI_DEBUG, "ACPI: Reading from {}", PhysicalAddress(structure.address));

        switch (bit_width) {
        case 8:
            return *TRY(Memory::map_typed<u8>(PhysicalAddress(structure.address)));
        case 16:
            return *TRY(Memory::map_typed<u16>(PhysicalAddress(structure.address)));
        case 32:
            return *TRY(Memory::map_typed<u32>(PhysicalAddress(structure.address)));
        case 64:
            return *TRY(Memory::map_typed<u64>(PhysicalAddress(structure.address)));

        default:
            dbgln("ACPI: Tried to read an unsupported bit width from SystemMemory");
            return EINVAL;
        }
    }

    default:
        dbgln("ACPI: Tried to access an unsupported address space");
        return EINVAL;
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<void> write_generic_address(::ACPI::Structures::GenericAddressStructure const& structure, u64 value)
{
    auto bit_width = TRY(get_bit_width(structure));

    switch (structure.address_space) {
    case ::ACPI::GenericAddressStructure::AddressSpace::SystemIO: {
#if ARCH(X86_64)
        IOAddress address(structure.address);
        dbgln_if(ACPI_DEBUG, "ACPI: Sending value {:x} to {}", value, address);

        if (bit_width == 64) {
            dbgln("ACPI: Tried to write a QWord to an IO port");
            return EINVAL;
        }

        address.out(value, bit_width);
        break;
#else
        dbgln("ACPI: Tried to access SystemIO on a non x86 architecture");
        return EINVAL;
#endif
    }

    case ::ACPI::GenericAddressStructure::AddressSpace::SystemMemory: {
        dbgln_if(ACPI_DEBUG, "ACPI: Sending value {:x} to {}", value, PhysicalAddress(structure.address));

        switch (bit_width) {
        case 8:
            *TRY(Memory::map_typed<u8>(PhysicalAddress(structure.address))) = value;
            break;
        case 16:
            *TRY(Memory::map_typed<u16>(PhysicalAddress(structure.address))) = value;
            break;
        case 32:
            *TRY(Memory::map_typed<u32>(PhysicalAddress(structure.address))) = value;
            break;
        case 64:
            *TRY(Memory::map_typed<u64>(PhysicalAddress(structure.address))) = value;
            break;

        default:
            dbgln("ACPI: Tried to write an unsupported bit width to SystemMemory");
            return EINVAL;
        }

        return {};
    }

    case ::ACPI::GenericAddressStructure::AddressSpace::PCIConfigurationSpace: {
        // According to https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#address-space-format,
        // PCI addresses must be confined to devices on Segment group 0, bus 0.
        auto pci_address = PCI::Address(0, 0, ((structure.address >> 24) & 0xFF), ((structure.address >> 16) & 0xFF));
        dbgln_if(ACPI_DEBUG, "ACPI: Sending value {:x} to {}", value, pci_address);

        if (bit_width == 64) {
            dbgln("ACPI: Trying to send QWord to PCI configuration space");
            return EINVAL;
        }

        u32 offset_in_pci_address = structure.address & 0xFFFF;

        auto const& pci_device_identifier = PCI::get_device_identifier(pci_address);
        PCI::raw_access(pci_device_identifier, offset_in_pci_address, bit_width, value);
        return {};
    }

    default:
        dbgln("ACPI: Tried to access an unsupported address space");
        return EINVAL;
    }

    VERIFY_NOT_REACHED();
}

}
