/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/DistinctNumeric.h>
#include <AK/Function.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Debug.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

namespace PCI {

enum class HeaderType {
    Device = 0,
    Bridge = 1,
};

enum class RegisterOffset {
    VENDOR_ID = 0x00,            // word
    DEVICE_ID = 0x02,            // word
    COMMAND = 0x04,              // word
    STATUS = 0x06,               // word
    REVISION_ID = 0x08,          // byte
    PROG_IF = 0x09,              // byte
    SUBCLASS = 0x0a,             // byte
    CLASS = 0x0b,                // byte
    CACHE_LINE_SIZE = 0x0c,      // byte
    LATENCY_TIMER = 0x0d,        // byte
    HEADER_TYPE = 0x0e,          // byte
    BIST = 0x0f,                 // byte
    BAR0 = 0x10,                 // u32
    BAR1 = 0x14,                 // u32
    BAR2 = 0x18,                 // u32
    SECONDARY_BUS = 0x19,        // byte
    BAR3 = 0x1C,                 // u32
    BAR4 = 0x20,                 // u32
    BAR5 = 0x24,                 // u32
    SUBSYSTEM_VENDOR_ID = 0x2C,  // u16
    SUBSYSTEM_ID = 0x2E,         // u16
    CAPABILITIES_POINTER = 0x34, // u8
    INTERRUPT_LINE = 0x3C,       // byte
    INTERRUPT_PIN = 0x3D,        // byte
};

enum class Limits {
    MaxDevicesPerBus = 32,
    MaxBusesPerDomain = 256,
    MaxFunctionsPerDevice = 8,
};

static constexpr u16 address_port = 0xcf8;
static constexpr u16 value_port = 0xcfc;

static constexpr size_t mmio_device_space_size = 4096;
static constexpr u16 none_value = 0xffff;
static constexpr size_t memory_range_per_bus = mmio_device_space_size * to_underlying(Limits::MaxFunctionsPerDevice) * to_underlying(Limits::MaxDevicesPerBus);

// Taken from https://pcisig.com/sites/default/files/files/PCI_Code-ID_r_1_11__v24_Jan_2019.pdf
enum class ClassID {
    MassStorage = 0x1,
    Multimedia = 0x4,
    Bridge = 0x6,
};

namespace MassStorage {

enum class SubclassID {
    IDEController = 0x1,
    SATAController = 0x6,
};
enum class SATAProgIF {
    AHCI = 0x1,
};

}

namespace Multimedia {

enum class SubclassID {
    AudioController = 0x1,
};

}

namespace Bridge {

enum class SubclassID {
    PCI_TO_PCI = 0x4,
};

}

TYPEDEF_DISTINCT_ORDERED_ID(u8, CapabilityID);
namespace Capabilities {
enum ID {
    Null = 0x0,
    MSI = 0x5,
    VendorSpecific = 0x9,
    MSIX = 0x11,
};
}

struct HardwareID {
    u16 vendor_id { 0 };
    u16 device_id { 0 };

    bool is_null() const { return !vendor_id && !device_id; }

    bool operator==(const HardwareID& other) const
    {
        return vendor_id == other.vendor_id && device_id == other.device_id;
    }
    bool operator!=(const HardwareID& other) const
    {
        return vendor_id != other.vendor_id || device_id != other.device_id;
    }
};

class Domain {
public:
    Domain() = delete;
    Domain(PhysicalAddress base_address, u8 start_bus, u8 end_bus)
        : m_base_addr(base_address)
        , m_start_bus(start_bus)
        , m_end_bus(end_bus)
    {
    }
    u8 start_bus() const { return m_start_bus; }
    u8 end_bus() const { return m_end_bus; }
    PhysicalAddress paddr() const { return m_base_addr; }

private:
    PhysicalAddress m_base_addr;
    u8 m_start_bus;
    u8 m_end_bus;
};

struct Address {
public:
    Address() = default;
    Address(u32 domain)
        : m_domain(domain)
        , m_bus(0)
        , m_device(0)
        , m_function(0)
    {
    }
    Address(u32 domain, u8 bus, u8 device, u8 function)
        : m_domain(domain)
        , m_bus(bus)
        , m_device(device)
        , m_function(function)
    {
    }

    Address(const Address& address) = default;

    bool is_null() const { return !m_bus && !m_device && !m_function; }
    operator bool() const { return !is_null(); }

    // Disable default implementations that would use surprising integer promotion.
    bool operator<=(const Address&) const = delete;
    bool operator>=(const Address&) const = delete;
    bool operator<(const Address&) const = delete;
    bool operator>(const Address&) const = delete;

    bool operator==(const Address& other) const
    {
        if (this == &other)
            return true;
        return m_domain == other.m_domain && m_bus == other.m_bus && m_device == other.m_device && m_function == other.m_function;
    }
    bool operator!=(const Address& other) const
    {
        return !(*this == other);
    }

    u16 domain() const { return m_domain; }
    u8 bus() const { return m_bus; }
    u8 device() const { return m_device; }
    u8 function() const { return m_function; }

    u32 io_address_for_field(u8 field) const
    {
        return 0x80000000u | (m_bus << 16u) | (m_device << 11u) | (m_function << 8u) | (field & 0xfc);
    }

private:
    u32 m_domain { 0 };
    u8 m_bus { 0 };
    u8 m_device { 0 };
    u8 m_function { 0 };
};

class Capability {
public:
    Capability(const Address& address, u8 id, u8 ptr)
        : m_address(address)
        , m_id(id)
        , m_ptr(ptr)
    {
    }

    CapabilityID id() const { return m_id; }

    u8 read8(u32) const;
    u16 read16(u32) const;
    u32 read32(u32) const;
    void write8(u32, u8);
    void write16(u32, u16);
    void write32(u32, u32);

private:
    Address m_address;
    const CapabilityID m_id;
    const u8 m_ptr;
};

TYPEDEF_DISTINCT_ORDERED_ID(u8, ClassCode);
TYPEDEF_DISTINCT_ORDERED_ID(u8, SubclassCode);
TYPEDEF_DISTINCT_ORDERED_ID(u8, ProgrammingInterface);
TYPEDEF_DISTINCT_ORDERED_ID(u8, RevisionID);
TYPEDEF_DISTINCT_ORDERED_ID(u16, SubsystemID);
TYPEDEF_DISTINCT_ORDERED_ID(u16, SubsystemVendorID);
TYPEDEF_DISTINCT_ORDERED_ID(u8, InterruptLine);
TYPEDEF_DISTINCT_ORDERED_ID(u8, InterruptPin);

class Access;
class DeviceIdentifier {
public:
    DeviceIdentifier(Address address, HardwareID hardware_id, RevisionID revision_id, ClassCode class_code, SubclassCode subclass_code, ProgrammingInterface prog_if, SubsystemID subsystem_id, SubsystemVendorID subsystem_vendor_id, InterruptLine interrupt_line, InterruptPin interrupt_pin, Vector<Capability> const& capabilities)
        : m_address(address)
        , m_hardware_id(hardware_id)
        , m_revision_id(revision_id)
        , m_class_code(class_code)
        , m_subclass_code(subclass_code)
        , m_prog_if(prog_if)
        , m_subsystem_id(subsystem_id)
        , m_subsystem_vendor_id(subsystem_vendor_id)
        , m_interrupt_line(interrupt_line)
        , m_interrupt_pin(interrupt_pin)
        , m_capabilities(capabilities)
    {
        if constexpr (PCI_DEBUG) {
            for (const auto& capability : capabilities)
                dbgln("{} has capability {}", address, capability.id());
        }
    }

    Vector<Capability> const& capabilities() const { return m_capabilities; }
    HardwareID const& hardware_id() const { return m_hardware_id; }
    Address const& address() const { return m_address; }

    RevisionID revision_id() const { return m_revision_id; }
    ClassCode class_code() const { return m_class_code; }
    SubclassCode subclass_code() const { return m_subclass_code; }
    ProgrammingInterface prog_if() const { return m_prog_if; }
    SubsystemID subsystem_id() const { return m_subsystem_id; }
    SubsystemVendorID subsystem_vendor_id() const { return m_subsystem_vendor_id; }

    InterruptLine interrupt_line() const { return m_interrupt_line; }
    InterruptPin interrupt_pin() const { return m_interrupt_pin; }

    void apply_subclass_code_change(Badge<Access>, SubclassCode new_subclass)
    {
        m_subclass_code = new_subclass;
    }
    void apply_prog_if_change(Badge<Access>, ProgrammingInterface new_progif)
    {
        m_prog_if = new_progif;
    }

private:
    Address m_address;
    HardwareID m_hardware_id;

    RevisionID m_revision_id;
    ClassCode m_class_code;
    SubclassCode m_subclass_code;
    ProgrammingInterface m_prog_if;
    SubsystemID m_subsystem_id;
    SubsystemVendorID m_subsystem_vendor_id;

    InterruptLine m_interrupt_line;
    InterruptPin m_interrupt_pin;

    Vector<Capability> m_capabilities;
};

class Domain;
class Device;
}

}

template<>
struct AK::Formatter<Kernel::PCI::Address> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::PCI::Address value)
    {
        return Formatter<FormatString>::format(
            builder,
            "PCI [{:04x}:{:02x}:{:02x}:{:02x}]", value.domain(), value.bus(), value.device(), value.function());
    }
};

template<>
struct AK::Formatter<Kernel::PCI::HardwareID> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::PCI::HardwareID value)
    {
        return Formatter<FormatString>::format(
            builder,
            "PCI::HardwareID [{:04x}:{:04x}]", value.vendor_id, value.device_id);
    }
};
