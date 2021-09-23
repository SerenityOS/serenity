/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

void write8(Address address, u32 field, u8 value) { Access::the().write8_field(address, field, value); }
void write16(Address address, u32 field, u16 value) { Access::the().write16_field(address, field, value); }
void write32(Address address, u32 field, u32 value) { Access::the().write32_field(address, field, value); }
u8 read8(Address address, u32 field) { return Access::the().read8_field(address, field); }
u16 read16(Address address, u32 field) { return Access::the().read16_field(address, field); }
u32 read32(Address address, u32 field) { return Access::the().read32_field(address, field); }

void enumerate(Function<void(Address, DeviceIdentifier const&)> callback)
{
    Access::the().fast_enumerate(callback);
}

DeviceIdentifier get_device_identifier(Address address)
{
    return Access::the().get_device_identifier(address);
}

HardwareID get_hardware_id(Address address)
{
    return { read16(address, PCI_VENDOR_ID), read16(address, PCI_DEVICE_ID) };
}

void enable_io_space(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) | (1 << 0));
}
void disable_io_space(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) & ~(1 << 0));
}

void enable_memory_space(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) | (1 << 1));
}
void disable_memory_space(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) & ~(1 << 1));
}
bool is_io_space_enabled(Address address)
{
    return (read16(address, PCI_COMMAND) & 1) != 0;
}

void enable_interrupt_line(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) & ~(1 << 10));
}

void disable_interrupt_line(Address address)
{
    write16(address, PCI_COMMAND, read16(address, PCI_COMMAND) | 1 << 10);
}

u8 get_interrupt_line(Address address)
{
    return read8(address, PCI_INTERRUPT_LINE);
}

u32 get_BAR0(Address address)
{
    return read32(address, PCI_BAR0);
}

u32 get_BAR1(Address address)
{
    return read32(address, PCI_BAR1);
}

u32 get_BAR2(Address address)
{
    return read32(address, PCI_BAR2);
}

u32 get_BAR3(Address address)
{
    return read16(address, PCI_BAR3);
}

u32 get_BAR4(Address address)
{
    return read32(address, PCI_BAR4);
}

u32 get_BAR5(Address address)
{
    return read32(address, PCI_BAR5);
}

u32 get_BAR(Address address, u8 bar)
{
    VERIFY(bar <= 5);
    switch (bar) {
    case 0:
        return get_BAR0(address);
    case 1:
        return get_BAR1(address);
    case 2:
        return get_BAR2(address);
    case 3:
        return get_BAR3(address);
    case 4:
        return get_BAR4(address);
    case 5:
        return get_BAR5(address);
    default:
        VERIFY_NOT_REACHED();
    }
}

void enable_bus_mastering(Address address)
{
    auto value = read16(address, PCI_COMMAND);
    value |= (1 << 2);
    value |= (1 << 0);
    write16(address, PCI_COMMAND, value);
}

void disable_bus_mastering(Address address)
{
    auto value = read16(address, PCI_COMMAND);
    value &= ~(1 << 2);
    value |= (1 << 0);
    write16(address, PCI_COMMAND, value);
}

size_t get_BAR_space_size(Address address, u8 bar_number)
{
    // See PCI Spec 2.3, Page 222
    VERIFY(bar_number < 6);
    u8 field = (PCI_BAR0 + (bar_number << 2));
    u32 bar_reserved = read32(address, field);
    write32(address, field, 0xFFFFFFFF);
    u32 space_size = read32(address, field);
    write32(address, field, bar_reserved);
    space_size &= 0xfffffff0;
    space_size = (~space_size) + 1;
    return space_size;
}

void raw_access(Address address, u32 field, size_t access_size, u32 value)
{
    VERIFY(access_size != 0);
    if (access_size == 1) {
        write8(address, field, value);
        return;
    }
    if (access_size == 2) {
        write16(address, field, value);
        return;
    }
    if (access_size == 4) {
        write32(address, field, value);
        return;
    }
    VERIFY_NOT_REACHED();
}

u8 Capability::read8(u32 field) const
{
    return PCI::read8(m_address, m_ptr + field);
}

u16 Capability::read16(u32 field) const
{
    return PCI::read16(m_address, m_ptr + field);
}

u32 Capability::read32(u32 field) const
{
    return PCI::read32(m_address, m_ptr + field);
}

void Capability::write8(u32 field, u8 value)
{
    PCI::write8(m_address, m_ptr + field, value);
}

void Capability::write16(u32 field, u16 value)
{
    PCI::write16(m_address, m_ptr + field, value);
}

void Capability::write32(u32 field, u32 value)
{
    PCI::write32(m_address, m_ptr + field, value);
}

}
