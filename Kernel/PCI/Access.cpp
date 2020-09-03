/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/PCI/Access.h>
#include <Kernel/PCI/IOAccess.h>

namespace Kernel {
namespace PCI {

static Access* s_access;

inline void write8(Address address, u32 field, u8 value) { Access::the().write8_field(address, field, value); }
inline void write16(Address address, u32 field, u16 value) { Access::the().write16_field(address, field, value); }
inline void write32(Address address, u32 field, u32 value) { Access::the().write32_field(address, field, value); }
inline u8 read8(Address address, u32 field) { return Access::the().read8_field(address, field); }
inline u16 read16(Address address, u32 field) { return Access::the().read16_field(address, field); }
inline u32 read32(Address address, u32 field) { return Access::the().read32_field(address, field); }

Access& Access::the()
{
    if (s_access == nullptr) {
        ASSERT_NOT_REACHED(); // We failed to initialize the PCI subsystem, so stop here!
    }
    return *s_access;
}

bool Access::is_initialized()
{
    return (s_access != nullptr);
}

Access::Access()
{
    s_access = this;
}

static u16 read_type(Address address)
{
    return (read8(address, PCI_CLASS) << 8u) | read8(address, PCI_SUBCLASS);
}

void Access::enumerate_functions(int type, u8 bus, u8 slot, u8 function, Function<void(Address, ID)>& callback)
{
    Address address(0, bus, slot, function);
    if (type == -1 || type == read_type(address))
        callback(address, { read16_field(address, PCI_VENDOR_ID), read16_field(address, PCI_DEVICE_ID) });
    if (read_type(address) == PCI_TYPE_BRIDGE) {
        u8 secondary_bus = read8_field(address, PCI_SECONDARY_BUS);
#ifdef PCI_DEBUG
        klog() << "PCI: Found secondary bus: " << secondary_bus;
#endif
        ASSERT(secondary_bus != bus);
        enumerate_bus(type, secondary_bus, callback);
    }
}

void Access::enumerate_slot(int type, u8 bus, u8 slot, Function<void(Address, ID)>& callback)
{
    Address address(0, bus, slot, 0);
    if (read16_field(address, PCI_VENDOR_ID) == PCI_NONE)
        return;
    enumerate_functions(type, bus, slot, 0, callback);
    if (!(read8_field(address, PCI_HEADER_TYPE) & 0x80))
        return;
    for (u8 function = 1; function < 8; ++function) {
        Address address(0, bus, slot, function);
        if (read16_field(address, PCI_VENDOR_ID) != PCI_NONE)
            enumerate_functions(type, bus, slot, function, callback);
    }
}

void Access::enumerate_bus(int type, u8 bus, Function<void(Address, ID)>& callback)
{
    for (u8 slot = 0; slot < 32; ++slot)
        enumerate_slot(type, bus, slot, callback);
}

void Access::enumerate(Function<void(Address, ID)>& callback) const
{
    for (auto& physical_id : m_physical_ids) {
        callback(physical_id.address(), physical_id.id());
    }
}

void enumerate(Function<void(Address, ID)> callback)
{
    Access::the().enumerate(callback);
}

void raw_access(Address address, u32 field, size_t access_size, u32 value)
{
    ASSERT(access_size != 0);
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
    ASSERT_NOT_REACHED();
}

ID get_id(Address address)
{
    return { read16(address, PCI_VENDOR_ID), read16(address, PCI_DEVICE_ID) };
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

u8 get_revision_id(Address address)
{
    return read8(address, PCI_REVISION_ID);
}

u8 get_subclass(Address address)
{
    return read8(address, PCI_SUBCLASS);
}

u8 get_class(Address address)
{
    return read8(address, PCI_CLASS);
}

u8 get_programming_interface(Address address)
{
    return read8(address, PCI_PROG_IF);
}

u16 get_subsystem_id(Address address)
{
    return read16(address, PCI_SUBSYSTEM_ID);
}

u16 get_subsystem_vendor_id(Address address)
{
    return read16(address, PCI_SUBSYSTEM_VENDOR_ID);
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
    ASSERT(bar_number < 6);
    u8 field = (PCI_BAR0 + (bar_number << 2));
    u32 bar_reserved = read32(address, field);
    write32(address, field, 0xFFFFFFFF);
    u32 space_size = read32(address, field);
    write32(address, field, bar_reserved);
    space_size &= 0xfffffff0;
    space_size = (~space_size) + 1;
    return space_size;
}

}
}
