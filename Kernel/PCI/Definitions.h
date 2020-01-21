/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/Function.h>
#include <AK/Types.h>

#define PCI_VENDOR_ID 0x00           // word
#define PCI_DEVICE_ID 0x02           // word
#define PCI_COMMAND 0x04             // word
#define PCI_STATUS 0x06              // word
#define PCI_REVISION_ID 0x08         // byte
#define PCI_PROG_IF 0x09             // byte
#define PCI_SUBCLASS 0x0a            // byte
#define PCI_CLASS 0x0b               // byte
#define PCI_CACHE_LINE_SIZE 0x0c     // byte
#define PCI_LATENCY_TIMER 0x0d       // byte
#define PCI_HEADER_TYPE 0x0e         // byte
#define PCI_BIST 0x0f                // byte
#define PCI_BAR0 0x10                // u32
#define PCI_BAR1 0x14                // u32
#define PCI_BAR2 0x18                // u32
#define PCI_BAR3 0x1C                // u32
#define PCI_BAR4 0x20                // u32
#define PCI_BAR5 0x24                // u32
#define PCI_SUBSYSTEM_ID 0x2C        // u16
#define PCI_SUBSYSTEM_VENDOR_ID 0x2E // u16
#define PCI_INTERRUPT_LINE 0x3C      // byte
#define PCI_SECONDARY_BUS 0x19       // byte
#define PCI_HEADER_TYPE_DEVICE 0
#define PCI_HEADER_TYPE_BRIDGE 1
#define PCI_TYPE_BRIDGE 0x0604
#define PCI_ADDRESS_PORT 0xCF8
#define PCI_VALUE_PORT 0xCFC
#define PCI_NONE 0xFFFF
#define PCI_MAX_DEVICES_PER_BUS 32
#define PCI_MAX_BUSES 256
#define PCI_MAX_FUNCTIONS_PER_DEVICE 8

//#define PCI_DEBUG 1

namespace PCI {
struct ID {
    u16 vendor_id { 0 };
    u16 device_id { 0 };

    bool is_null() const { return !vendor_id && !device_id; }

    bool operator==(const ID& other) const
    {
        return vendor_id == other.vendor_id && device_id == other.device_id;
    }
};

struct Address {
public:
    Address() {}
    Address(u16 seg)
        : m_seg(seg)
        , m_bus(0)
        , m_slot(0)
        , m_function(0)
    {
    }
    Address(u16 seg, u8 bus, u8 slot, u8 function)
        : m_seg(seg)
        , m_bus(bus)
        , m_slot(slot)
        , m_function(function)
    {
    }

    Address(const Address& address)
        : m_seg(address.seg())
        , m_bus(address.bus())
        , m_slot(address.slot())
        , m_function(address.function())
    {
    }

    bool is_null() const { return !m_bus && !m_slot && !m_function; }
    operator bool() const { return !is_null(); }

    u16 seg() const { return m_seg; }
    u8 bus() const { return m_bus; }
    u8 slot() const { return m_slot; }
    u8 function() const { return m_function; }

    u32 io_address_for_field(u8 field) const
    {
        return 0x80000000u | (m_bus << 16u) | (m_slot << 11u) | (m_function << 8u) | (field & 0xfc);
    }

protected:
    u32 m_seg { 0 };
    u8 m_bus { 0 };
    u8 m_slot { 0 };
    u8 m_function { 0 };
};

struct ChangeableAddress : public Address {
    ChangeableAddress()
        : Address(0)
    {
    }
    explicit ChangeableAddress(u16 seg)
        : Address(seg)
    {
    }
    ChangeableAddress(u16 seg, u8 bus, u8 slot, u8 function)
        : Address(seg, bus, slot, function)
    {
    }
    void set_seg(u16 seg) { m_seg = seg; }
    void set_bus(u8 bus) { m_bus = bus; }
    void set_slot(u8 slot) { m_slot = slot; }
    void set_function(u8 function) { m_function = function; }
    bool operator==(const Address& address)
    {
        if (m_seg == address.seg() && m_bus == address.bus() && m_slot == address.slot() && m_function == address.function())
            return true;
        else
            return false;
    }
    const ChangeableAddress& operator=(const Address& address)
    {
        set_seg(address.seg());
        set_bus(address.bus());
        set_slot(address.slot());
        set_function(address.function());
        return *this;
    }
};

void enumerate_all(Function<void(Address, ID)> callback);
u8 get_interrupt_line(Address);
u32 get_BAR0(Address);
u32 get_BAR1(Address);
u32 get_BAR2(Address);
u32 get_BAR3(Address);
u32 get_BAR4(Address);
u32 get_BAR5(Address);
u8 get_revision_id(Address);
u8 get_subclass(Address);
u8 get_class(Address);
u16 get_subsystem_id(Address);
u16 get_subsystem_vendor_id(Address);
size_t get_BAR_Space_Size(Address, u8);
void enable_bus_mastering(Address);
void disable_bus_mastering(Address);

class Initializer;
class Access;
class MMIOAccess;
class IOAccess;
class MMIOSegment;
class Device;

}
