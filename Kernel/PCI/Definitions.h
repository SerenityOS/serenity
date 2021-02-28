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

#pragma once

#include <AK/Function.h>
#include <AK/LogStream.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Debug.h>

namespace Kernel {

// clang-format off
#define PCI_VENDOR_ID 0x00            // word
#define PCI_DEVICE_ID 0x02            // word
#define PCI_COMMAND 0x04              // word
#define PCI_STATUS 0x06               // word
#define PCI_REVISION_ID 0x08          // byte
#define PCI_PROG_IF 0x09              // byte
#define PCI_SUBCLASS 0x0a             // byte
#define PCI_CLASS 0x0b                // byte
#define PCI_CACHE_LINE_SIZE 0x0c      // byte
#define PCI_LATENCY_TIMER 0x0d        // byte
#define PCI_HEADER_TYPE 0x0e          // byte
#define PCI_BIST 0x0f                 // byte
#define PCI_BAR0 0x10                 // u32
#define PCI_BAR1 0x14                 // u32
#define PCI_BAR2 0x18                 // u32
#define PCI_BAR3 0x1C                 // u32
#define PCI_BAR4 0x20                 // u32
#define PCI_BAR5 0x24                 // u32
#define PCI_SUBSYSTEM_ID 0x2C         // u16
#define PCI_SUBSYSTEM_VENDOR_ID 0x2E  // u16
#define PCI_CAPABILITIES_POINTER 0x34 // u8
#define PCI_INTERRUPT_LINE 0x3C       // byte
#define PCI_SECONDARY_BUS 0x19        // byte
#define PCI_HEADER_TYPE_DEVICE 0
#define PCI_HEADER_TYPE_BRIDGE 1
#define PCI_TYPE_BRIDGE 0x0604
#define PCI_ADDRESS_PORT 0xCF8
#define PCI_VALUE_PORT 0xCFC
#define PCI_NONE 0xFFFF
#define PCI_MAX_DEVICES_PER_BUS 32
#define PCI_MAX_BUSES 256
#define PCI_MAX_FUNCTIONS_PER_DEVICE 8
// clang-format on

namespace PCI {
struct ID {
    u16 vendor_id { 0 };
    u16 device_id { 0 };

    bool is_null() const { return !vendor_id && !device_id; }

    bool operator==(const ID& other) const
    {
        return vendor_id == other.vendor_id && device_id == other.device_id;
    }
    bool operator!=(const ID& other) const
    {
        return vendor_id != other.vendor_id || device_id != other.device_id;
    }
};
inline const LogStream& operator<<(const LogStream& stream, const ID value)
{
    return stream << String::formatted("({:04x}:{:04x})", value.vendor_id, value.device_id);
}
struct Address {
public:
    Address() = default;
    Address(u16 seg)
        : m_seg(seg)
        , m_bus(0)
        , m_device(0)
        , m_function(0)
    {
    }
    Address(u16 seg, u8 bus, u8 device, u8 function)
        : m_seg(seg)
        , m_bus(bus)
        , m_device(device)
        , m_function(function)
    {
    }

    Address(const Address& address)
        : m_seg(address.seg())
        , m_bus(address.bus())
        , m_device(address.device())
        , m_function(address.function())
    {
    }

    bool is_null() const { return !m_bus && !m_device && !m_function; }
    operator bool() const { return !is_null(); }

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(const Address&) const = delete;
    bool operator<=(const Address&) const = delete;
    bool operator>=(const Address&) const = delete;
    bool operator<(const Address&) const = delete;
    bool operator>(const Address&) const = delete;

    u16 seg() const { return m_seg; }
    u8 bus() const { return m_bus; }
    u8 device() const { return m_device; }
    u8 function() const { return m_function; }

    u32 io_address_for_field(u8 field) const
    {
        return 0x80000000u | (m_bus << 16u) | (m_device << 11u) | (m_function << 8u) | (field & 0xfc);
    }

protected:
    u32 m_seg { 0 };
    u8 m_bus { 0 };
    u8 m_device { 0 };
    u8 m_function { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, const Address value)
{
    return stream << "PCI [" << String::formatted("{:04x}:{:02x}:{:02x}:{:02x}", value.seg(), value.bus(), value.device(), value.function()) << "]";
}

struct ChangeableAddress : public Address {
    ChangeableAddress()
        : Address(0)
    {
    }
    explicit ChangeableAddress(u16 seg)
        : Address(seg)
    {
    }
    ChangeableAddress(u16 seg, u8 bus, u8 device, u8 function)
        : Address(seg, bus, device, function)
    {
    }
    void set_seg(u16 seg) { m_seg = seg; }
    void set_bus(u8 bus) { m_bus = bus; }
    void set_device(u8 device) { m_device = device; }
    void set_function(u8 function) { m_function = function; }
    bool operator==(const Address& address)
    {
        if (m_seg == address.seg() && m_bus == address.bus() && m_device == address.device() && m_function == address.function())
            return true;
        else
            return false;
    }
    const ChangeableAddress& operator=(const Address& address)
    {
        set_seg(address.seg());
        set_bus(address.bus());
        set_device(address.device());
        set_function(address.function());
        return *this;
    }
};

struct Capability {
    u8 m_id;
    u8 m_next_pointer;
};

class PhysicalID {
public:
    PhysicalID(Address address, ID id, Vector<Capability> capabilities)
        : m_address(address)
        , m_id(id)
        , m_capabilities(capabilities)
    {
        if constexpr (PCI_DEBUG) {
            for (auto capability : capabilities)
                dbgln("{} has capability {}", address, capability.m_id);
        }
    }

    Vector<Capability> capabilities() const { return m_capabilities; }
    const ID& id() const { return m_id; }
    const Address& address() const { return m_address; }

private:
    Address m_address;
    ID m_id;
    Vector<Capability> m_capabilities;
};

ID get_id(PCI::Address);
void enumerate(Function<void(Address, ID)> callback);
void enable_interrupt_line(Address);
void disable_interrupt_line(Address);
u8 get_interrupt_line(Address);
void raw_access(Address, u32, size_t, u32);
u32 get_BAR0(Address);
u32 get_BAR1(Address);
u32 get_BAR2(Address);
u32 get_BAR3(Address);
u32 get_BAR4(Address);
u32 get_BAR5(Address);
u8 get_revision_id(Address);
u8 get_programming_interface(Address);
u8 get_subclass(Address);
u8 get_class(Address);
u16 get_subsystem_id(Address);
u16 get_subsystem_vendor_id(Address);
size_t get_BAR_space_size(Address, u8);
Optional<u8> get_capabilities_pointer(Address);
Vector<Capability> get_capabilities(Address);
void enable_bus_mastering(Address);
void disable_bus_mastering(Address);
PhysicalID get_physical_id(Address address);

class Access;
class MMIOAccess;
class IOAccess;
class MMIOSegment;
class DeviceController;
class Device;

}

}

template<>
struct AK::Formatter<Kernel::PCI::Address> : Formatter<FormatString> {
    void format(FormatBuilder& builder, Kernel::PCI::Address value)
    {
        return Formatter<FormatString>::format(
            builder,
            "PCI [{:04x}:{:02x}:{:02x}:{:02x}]", value.seg(), value.bus(), value.device(), value.function());
    }
};

template<>
struct AK::Formatter<Kernel::PCI::ID> : Formatter<FormatString> {
    void format(FormatBuilder& builder, Kernel::PCI::ID value)
    {
        return Formatter<FormatString>::format(
            builder,
            "PCI::ID [{:04x}:{:04x}]", value.vendor_id, value.device_id);
    }
};
