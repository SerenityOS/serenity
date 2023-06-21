/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/DeviceExpansionROM.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullRefPtr<PCIDeviceExpansionROMSysFSComponent> PCIDeviceExpansionROMSysFSComponent::create(PCIDeviceSysFSDirectory const& device)
{
    auto option_rom_size = PCI::get_expansion_rom_space_size(device.device_identifier());
    return adopt_ref(*new (nothrow) PCIDeviceExpansionROMSysFSComponent(device, option_rom_size));
}

PCIDeviceExpansionROMSysFSComponent::PCIDeviceExpansionROMSysFSComponent(PCIDeviceSysFSDirectory const& device, size_t option_rom_size)
    : SysFSComponent()
    , m_device(device)
    , m_option_rom_size(option_rom_size)
{
}

ErrorOr<size_t> PCIDeviceExpansionROMSysFSComponent::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    // NOTE: It might be that the PCI option ROM size is zero, indicating non-existing ROM.
    if (m_option_rom_size == 0)
        return Error::from_errno(EIO);
    // NOTE: This takes into account that `off_t offset` might be a negative number and
    // there's no meaningful way to handle negative values, so just return with an error.
    if (offset < 0)
        return Error::from_errno(EINVAL);
    auto unsigned_offset = static_cast<size_t>(offset);
    // NOTE: If the offset is beyond the PCI option ROM size, return EOF.
    if (unsigned_offset >= m_option_rom_size)
        return 0;

    ssize_t nread = min(static_cast<off_t>(m_option_rom_size - offset), static_cast<off_t>(count));
    auto blob = TRY(try_to_generate_buffer(unsigned_offset, nread));
    TRY(buffer.write(blob->bytes()));
    return nread;
}

ErrorOr<NonnullOwnPtr<KBuffer>> PCIDeviceExpansionROMSysFSComponent::try_to_generate_buffer(size_t offset_in_rom, size_t count) const
{
    // NOTE: If the offset is beyond the PCI option ROM size, panic!.
    VERIFY(offset_in_rom < m_option_rom_size);
    auto temporary_buffer_size = TRY(Memory::page_round_up(count));
    auto temporary_buffer = TRY(KBuffer::try_create_with_size("SysFS DeviceExpansionROM Device"sv, temporary_buffer_size, Memory::Region::Access::ReadWrite));

    SpinlockLocker locker(m_device->device_identifier().operation_lock());
    // NOTE: These checks takes into account a couple of cases:
    // 1. Option ROM doesn't exist so the value of the ROM physical pointer is 0, and in that case
    // we should not allow mapping.
    // 2. Option ROM exists but for some (odd) reason, it is found in non-reserved (usable) physical memory
    // region, so access to it should be forbidden from this sysfs node.
    auto pci_option_rom_physical_pointer = PCI::read32_locked(m_device->device_identifier(), PCI::RegisterOffset::EXPANSION_ROM_POINTER);
    if (pci_option_rom_physical_pointer == 0)
        return Error::from_errno(EIO);

    if (pci_option_rom_physical_pointer & 1)
        dbgln("SysFS DeviceExpansionROM: Possible firmware bug! PCI option ROM was found already to be enabled.");

    auto offested_option_rom_memory_mapped_start_address = PhysicalAddress(pci_option_rom_physical_pointer + offset_in_rom);
    auto mapping_size = min(static_cast<off_t>(m_option_rom_size - offset_in_rom), static_cast<off_t>(count));
    if (!MM.is_allowed_to_read_physical_memory_for_userspace(offested_option_rom_memory_mapped_start_address, mapping_size))
        return Error::from_errno(EPERM);

    ScopeGuard unmap_option_rom_on_return([&] {
        // NOTE: In general, there's probably nothing wrong in leaving Option ROM being
        // mapped into physical memory.
        // For the sake of completeness, let's ensure we don't leave the Option ROM being
        // mapped into physical memory.
        // NOTE: It might be that in the future some driver will need to have the expansion ROM
        // being present in the physical address space. If that's the case then we should add a flag
        // to the PCI::DeviceIdentifier to indicate this condition!
        PCI::write32_locked(m_device->device_identifier(), PCI::RegisterOffset::EXPANSION_ROM_POINTER, pci_option_rom_physical_pointer);
    });
    // Note: Write the original value ORed with 1 to enable mapping into physical memory
    PCI::write32_locked(m_device->device_identifier(), PCI::RegisterOffset::EXPANSION_ROM_POINTER, pci_option_rom_physical_pointer | 1);

    auto do_io_transaction = [](Bytes bytes_buffer, Memory::TypedMapping<u8> const& mapping, size_t length_to_map) {
        VERIFY(length_to_map <= PAGE_SIZE);
        memcpy(bytes_buffer.data(), mapping.region->vaddr().offset(mapping.offset).as_ptr(), length_to_map);
    };

    size_t remaining_length = count;
    size_t nprocessed = 0;
    while (remaining_length > 0) {
        size_t length_to_map = min<size_t>(PAGE_SIZE, remaining_length);
        auto mapping = TRY(Memory::map_typed<u8>(offested_option_rom_memory_mapped_start_address.offset(nprocessed), length_to_map, Memory::Region::Access::Read));
        do_io_transaction(temporary_buffer->bytes().slice(nprocessed, length_to_map), mapping, length_to_map);
        nprocessed += length_to_map;
        remaining_length -= length_to_map;
    }
    return temporary_buffer;
}
}
