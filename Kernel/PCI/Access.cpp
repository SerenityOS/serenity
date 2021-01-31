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

#include <Kernel/Debug.h>
#include <Kernel/IO.h>
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

PhysicalID Access::get_physical_id(Address address) const
{
    for (auto physical_id : m_physical_ids) {
        if (physical_id.address().seg() == address.seg()
            && physical_id.address().bus() == address.bus()
            && physical_id.address().device() == address.device()
            && physical_id.address().function() == address.function()) {
            return physical_id;
        }
    }
    ASSERT_NOT_REACHED();
}

u8 Access::early_read8_field(Address address, u32 field)
{
    dbgln<PCI_DEBUG>("PCI: Early reading 8-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in8(PCI_VALUE_PORT + (field & 3));
}

u16 Access::early_read16_field(Address address, u32 field)
{
    dbgln<PCI_DEBUG>("PCI: Early reading 16-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in16(PCI_VALUE_PORT + (field & 2));
}

u32 Access::early_read32_field(Address address, u32 field)
{
    dbgln<PCI_DEBUG>("PCI: Early reading 32-bit field {:#08x} for {}", field, address);
    IO::out32(PCI_ADDRESS_PORT, address.io_address_for_field(field));
    return IO::in32(PCI_VALUE_PORT);
}

u16 Access::early_read_type(Address address)
{
    dbgln<PCI_DEBUG>("PCI: Early reading type for {}", address);
    return (early_read8_field(address, PCI_CLASS) << 8u) | early_read8_field(address, PCI_SUBCLASS);
}

void Access::enumerate_functions(int type, u8 bus, u8 device, u8 function, Function<void(Address, ID)>& callback, bool recursive)
{
    dbgln<PCI_DEBUG>("PCI: Enumerating function type={}, bus={}, device={}, function={}", type, bus, device, function);
    Address address(0, bus, device, function);
    if (type == -1 || type == early_read_type(address))
        callback(address, { early_read16_field(address, PCI_VENDOR_ID), early_read16_field(address, PCI_DEVICE_ID) });
    if (early_read_type(address) == PCI_TYPE_BRIDGE && recursive) {
        u8 secondary_bus = early_read8_field(address, PCI_SECONDARY_BUS);
#if PCI_DEBUG
        klog() << "PCI: Found secondary bus: " << secondary_bus;
#endif
        ASSERT(secondary_bus != bus);
        enumerate_bus(type, secondary_bus, callback, recursive);
    }
}

void Access::enumerate_device(int type, u8 bus, u8 device, Function<void(Address, ID)>& callback, bool recursive)
{
    dbgln<PCI_DEBUG>("PCI: Enumerating device type={}, bus={}, device={}", type, bus, device);
    Address address(0, bus, device, 0);
    if (early_read16_field(address, PCI_VENDOR_ID) == PCI_NONE)
        return;
    enumerate_functions(type, bus, device, 0, callback, recursive);
    if (!(early_read8_field(address, PCI_HEADER_TYPE) & 0x80))
        return;
    for (u8 function = 1; function < 8; ++function) {
        Address address(0, bus, device, function);
        if (early_read16_field(address, PCI_VENDOR_ID) != PCI_NONE)
            enumerate_functions(type, bus, device, function, callback, recursive);
    }
}

void Access::enumerate_bus(int type, u8 bus, Function<void(Address, ID)>& callback, bool recursive)
{
    dbgln<PCI_DEBUG>("PCI: Enumerating bus type={}, bus={}", type, bus);
    for (u8 device = 0; device < 32; ++device)
        enumerate_device(type, bus, device, callback, recursive);
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

Optional<u8> get_capabilities_pointer(Address address)
{
    dbgln<PCI_DEBUG>("PCI: Getting capabilities pointer for {}", address);
    if (PCI::read16(address, PCI_STATUS) & (1 << 4)) {
        dbgln<PCI_DEBUG>("PCI: Found capabilities pointer for {}", address);
        return PCI::read8(address, PCI_CAPABILITIES_POINTER);
    }
    dbgln<PCI_DEBUG>("PCI: No capabilities pointer for {}", address);
    return {};
}

PhysicalID get_physical_id(Address address)
{
    return Access::the().get_physical_id(address);
}

Vector<Capability> get_capabilities(Address address)
{
    dbgln<PCI_DEBUG>("PCI: Getting capabilities for {}", address);
    auto capabilities_pointer = PCI::get_capabilities_pointer(address);
    if (!capabilities_pointer.has_value()) {
        dbgln<PCI_DEBUG>("PCI: No capabilities for {}", address);
        return {};
    }
    Vector<Capability> capabilities;
    auto capability_pointer = capabilities_pointer.value();
    while (capability_pointer != 0) {
        dbgln<PCI_DEBUG>("PCI: Reading in capability at {:#02x} for {}", capability_pointer, address);
        u16 capability_header = PCI::read16(address, capability_pointer);
        u8 capability_id = capability_header & 0xff;
        capability_pointer = capability_header >> 8;
        capabilities.append({ capability_id, capability_pointer });
    }
    return capabilities;
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
