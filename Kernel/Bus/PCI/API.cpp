/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

void write8_locked(DeviceIdentifier const& identifier, PCI::RegisterOffset field, u8 value)
{
    VERIFY(identifier.operation_lock().is_locked());
    Access::the().write8_field(identifier, to_underlying(field), value);
}
void write16_locked(DeviceIdentifier const& identifier, PCI::RegisterOffset field, u16 value)
{
    VERIFY(identifier.operation_lock().is_locked());
    Access::the().write16_field(identifier, to_underlying(field), value);
}
void write32_locked(DeviceIdentifier const& identifier, PCI::RegisterOffset field, u32 value)
{
    VERIFY(identifier.operation_lock().is_locked());
    Access::the().write32_field(identifier, to_underlying(field), value);
}

u8 read8_locked(DeviceIdentifier const& identifier, PCI::RegisterOffset field)
{
    VERIFY(identifier.operation_lock().is_locked());
    return Access::the().read8_field(identifier, to_underlying(field));
}
u16 read16_locked(DeviceIdentifier const& identifier, PCI::RegisterOffset field)
{
    VERIFY(identifier.operation_lock().is_locked());
    return Access::the().read16_field(identifier, to_underlying(field));
}
u32 read32_locked(DeviceIdentifier const& identifier, PCI::RegisterOffset field)
{
    VERIFY(identifier.operation_lock().is_locked());
    return Access::the().read32_field(identifier, to_underlying(field));
}

ErrorOr<void> enumerate(Function<void(DeviceIdentifier const&)> callback)
{
    return Access::the().fast_enumerate(callback);
}

HardwareID get_hardware_id(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    return { read16_locked(identifier, PCI::RegisterOffset::VENDOR_ID), read16_locked(identifier, PCI::RegisterOffset::DEVICE_ID) };
}

void enable_io_space(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    write16_locked(identifier, PCI::RegisterOffset::COMMAND, read16_locked(identifier, PCI::RegisterOffset::COMMAND) | (1 << 0));
}
void disable_io_space(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    write16_locked(identifier, PCI::RegisterOffset::COMMAND, read16_locked(identifier, PCI::RegisterOffset::COMMAND) & ~(1 << 0));
}

void enable_memory_space(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    write16_locked(identifier, PCI::RegisterOffset::COMMAND, read16_locked(identifier, PCI::RegisterOffset::COMMAND) | (1 << 1));
}
void disable_memory_space(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    write16_locked(identifier, PCI::RegisterOffset::COMMAND, read16_locked(identifier, PCI::RegisterOffset::COMMAND) & ~(1 << 1));
}

bool is_io_space_enabled(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    return (read16_locked(identifier, PCI::RegisterOffset::COMMAND) & 1) != 0;
}

void enable_interrupt_line(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    write16_locked(identifier, PCI::RegisterOffset::COMMAND, read16_locked(identifier, PCI::RegisterOffset::COMMAND) & ~(1 << 10));
}

void disable_interrupt_line(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    write16_locked(identifier, PCI::RegisterOffset::COMMAND, read16_locked(identifier, PCI::RegisterOffset::COMMAND) | 1 << 10);
}

u32 get_BAR0(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    return read32_locked(identifier, PCI::RegisterOffset::BAR0);
}

u32 get_BAR1(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    return read32_locked(identifier, PCI::RegisterOffset::BAR1);
}

u32 get_BAR2(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    return read32_locked(identifier, PCI::RegisterOffset::BAR2);
}

u32 get_BAR3(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    return read32_locked(identifier, PCI::RegisterOffset::BAR3);
}

u32 get_BAR4(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    return read32_locked(identifier, PCI::RegisterOffset::BAR4);
}

u32 get_BAR5(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    return read32_locked(identifier, PCI::RegisterOffset::BAR5);
}

u32 get_BAR(DeviceIdentifier const& identifier, HeaderType0BaseRegister pci_bar)
{
    VERIFY(to_underlying(pci_bar) <= 5);
    switch (to_underlying(pci_bar)) {
    case 0:
        return get_BAR0(identifier);
    case 1:
        return get_BAR1(identifier);
    case 2:
        return get_BAR2(identifier);
    case 3:
        return get_BAR3(identifier);
    case 4:
        return get_BAR4(identifier);
    case 5:
        return get_BAR5(identifier);
    default:
        VERIFY_NOT_REACHED();
    }
}

BARSpaceType get_BAR_space_type(u32 pci_bar_value)
{
    // Note: For IO space, bit 0 is set to 1.
    if (pci_bar_value & (1 << 0))
        return BARSpaceType::IOSpace;
    auto memory_space_type = (pci_bar_value >> 1) & 0b11;
    switch (memory_space_type) {
    case 0:
        return BARSpaceType::Memory32BitSpace;
    case 1:
        return BARSpaceType::Memory16BitSpace;
    case 2:
        return BARSpaceType::Memory64BitSpace;
    default:
        VERIFY_NOT_REACHED();
    }
}

void enable_bus_mastering(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    auto value = read16_locked(identifier, PCI::RegisterOffset::COMMAND);
    value |= (1 << 2);
    value |= (1 << 0);
    write16_locked(identifier, PCI::RegisterOffset::COMMAND, value);
}

void disable_bus_mastering(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    auto value = read16_locked(identifier, PCI::RegisterOffset::COMMAND);
    value &= ~(1 << 2);
    value |= (1 << 0);
    write16_locked(identifier, PCI::RegisterOffset::COMMAND, value);
}

static void write8_offsetted(DeviceIdentifier const& identifier, u32 field, u8 value)
{
    VERIFY(identifier.operation_lock().is_locked());
    Access::the().write8_field(identifier, field, value);
}
static void write16_offsetted(DeviceIdentifier const& identifier, u32 field, u16 value)
{
    VERIFY(identifier.operation_lock().is_locked());
    Access::the().write16_field(identifier, field, value);
}
static void write32_offsetted(DeviceIdentifier const& identifier, u32 field, u32 value)
{
    VERIFY(identifier.operation_lock().is_locked());
    Access::the().write32_field(identifier, field, value);
}
static u8 read8_offsetted(DeviceIdentifier const& identifier, u32 field)
{
    VERIFY(identifier.operation_lock().is_locked());
    return Access::the().read8_field(identifier, field);
}
static u16 read16_offsetted(DeviceIdentifier const& identifier, u32 field)
{
    VERIFY(identifier.operation_lock().is_locked());
    return Access::the().read16_field(identifier, field);
}
static u32 read32_offsetted(DeviceIdentifier const& identifier, u32 field)
{
    VERIFY(identifier.operation_lock().is_locked());
    return Access::the().read32_field(identifier, field);
}

size_t get_BAR_space_size(DeviceIdentifier const& identifier, HeaderType0BaseRegister pci_bar)
{
    SpinlockLocker locker(identifier.operation_lock());
    // See PCI Spec 2.3, Page 222
    VERIFY(to_underlying(pci_bar) < 6);
    u8 field = to_underlying(PCI::RegisterOffset::BAR0) + (to_underlying(pci_bar) << 2);
    u32 bar_reserved = read32_offsetted(identifier, field);
    write32_offsetted(identifier, field, 0xFFFFFFFF);
    u32 space_size = read32_offsetted(identifier, field);
    write32_offsetted(identifier, field, bar_reserved);
    space_size &= bar_address_mask;
    space_size = (~space_size) + 1;
    return space_size;
}

size_t get_expansion_rom_space_size(DeviceIdentifier const& identifier)
{
    SpinlockLocker locker(identifier.operation_lock());
    u8 field = to_underlying(PCI::RegisterOffset::EXPANSION_ROM_POINTER);
    u32 bar_reserved = read32_offsetted(identifier, field);
    write32_offsetted(identifier, field, 0xFFFFFFFF);
    u32 space_size = read32_offsetted(identifier, field);
    write32_offsetted(identifier, field, bar_reserved);
    space_size &= bar_address_mask;
    space_size = (~space_size) + 1;
    return space_size;
}

void raw_access(DeviceIdentifier const& identifier, u32 field, size_t access_size, u32 value)
{
    SpinlockLocker locker(identifier.operation_lock());
    VERIFY(access_size != 0);
    if (access_size == 1) {
        write8_offsetted(identifier, field, value);
        return;
    }
    if (access_size == 2) {
        write16_offsetted(identifier, field, value);
        return;
    }
    if (access_size == 4) {
        write32_offsetted(identifier, field, value);
        return;
    }
    VERIFY_NOT_REACHED();
}

u8 Capability::read8(size_t offset) const
{
    auto& identifier = get_device_identifier(m_address);
    SpinlockLocker locker(identifier.operation_lock());
    return read8_offsetted(identifier, m_ptr + offset);
}

u16 Capability::read16(size_t offset) const
{
    auto& identifier = get_device_identifier(m_address);
    SpinlockLocker locker(identifier.operation_lock());
    return read16_offsetted(identifier, m_ptr + offset);
}

u32 Capability::read32(size_t offset) const
{
    auto& identifier = get_device_identifier(m_address);
    SpinlockLocker locker(identifier.operation_lock());
    return read32_offsetted(identifier, m_ptr + offset);
}

void Capability::write8(size_t offset, u8 value) const
{
    auto& identifier = get_device_identifier(m_address);
    SpinlockLocker locker(identifier.operation_lock());
    write8_offsetted(identifier, m_ptr + offset, value);
}

void Capability::write16(size_t offset, u16 value) const
{
    auto& identifier = get_device_identifier(m_address);
    SpinlockLocker locker(identifier.operation_lock());
    write16_offsetted(identifier, m_ptr + offset, value);
}

void Capability::write32(size_t offset, u32 value) const
{
    auto& identifier = get_device_identifier(m_address);
    SpinlockLocker locker(identifier.operation_lock());
    write32_offsetted(identifier, m_ptr + offset, value);
}

DeviceIdentifier const& get_device_identifier(Address address)
{
    return Access::the().get_device_identifier(address);
}

}
