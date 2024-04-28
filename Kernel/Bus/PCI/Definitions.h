/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/DistinctNumeric.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Debug.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel::PCI {

enum class HeaderType {
    Device = 0,
    Bridge = 1,
};

enum class HeaderType0BaseRegister {
    BAR0 = 0,
    BAR1,
    BAR2,
    BAR3,
    BAR4,
    BAR5,
};

enum class BARSpaceType {
    IOSpace,
    Memory16BitSpace,
    Memory32BitSpace,
    Memory64BitSpace,
};

enum class RegisterOffset : u32 {
    VENDOR_ID = 0x00,                               // word
    DEVICE_ID = 0x02,                               // word
    COMMAND = 0x04,                                 // word
    STATUS = 0x06,                                  // word
    REVISION_ID = 0x08,                             // byte
    PROG_IF = 0x09,                                 // byte
    SUBCLASS = 0x0a,                                // byte
    CLASS = 0x0b,                                   // byte
    CACHE_LINE_SIZE = 0x0c,                         // byte
    LATENCY_TIMER = 0x0d,                           // byte
    HEADER_TYPE = 0x0e,                             // byte
    BIST = 0x0f,                                    // byte
    BAR0 = 0x10,                                    // u32
    BAR1 = 0x14,                                    // u32
    BAR2 = 0x18,                                    // u32
    SECONDARY_BUS = 0x19,                           // byte
    SUBORDINATE_BUS = 0x1A,                         // byte
    BAR3 = 0x1C,                                    // u32
    BAR4 = 0x20,                                    // u32
    MEMORY_BASE = 0x20,                             // u16
    MEMORY_LIMIT = 0x22,                            // u16
    BAR5 = 0x24,                                    // u32
    PREFETCHABLE_MEMORY_BASE = 0x24,                // u16
    PREFETCHABLE_MEMORY_LIMIT = 0x26,               // u16
    PREFETCHABLE_MEMORY_BASE_UPPER_32_BITS = 0x28,  // u32
    PREFETCHABLE_MEMORY_LIMIT_UPPER_32_BITS = 0x2C, // u32
    SUBSYSTEM_VENDOR_ID = 0x2C,                     // u16
    SUBSYSTEM_ID = 0x2E,                            // u16
    EXPANSION_ROM_POINTER = 0x30,                   // u32
    CAPABILITIES_POINTER = 0x34,                    // u8
    INTERRUPT_LINE = 0x3C,                          // byte
    INTERRUPT_PIN = 0x3D,                           // byte
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
static constexpr u64 bar_address_mask = ~0xfull;
static constexpr u8 msi_control_offset = 2;
static constexpr u16 msi_control_enable = 0x0001;
static constexpr u8 msi_address_low_offset = 4;
static constexpr u8 msi_address_high_or_data_offset = 8;
static constexpr u8 msi_data_offset = 0xc;
static constexpr u16 msi_address_format_mask = 0x80;
static constexpr u8 msi_mmc_format_mask = 0xe;
static constexpr u16 msix_control_table_mask = 0x07ff;
static constexpr u8 msix_table_bir_mask = 0x7;
static constexpr u16 msix_table_offset_mask = 0xfff8;
static constexpr u16 msix_control_enable = 0x8000;

union OpenFirmwareAddress {
    enum class SpaceType : u32 {
        ConfigurationSpace = 0,
        IOSpace = 1,
        Memory32BitSpace = 2,
        Memory64BitSpace = 3,
    };
    struct {
        // https://www.devicetree.org/open-firmware/bindings/pci/pci2_1.pdf
        // Chapter: 2.2.1.1
        // phys.hi cell
        u32 register_ : 8;        // r
        u32 function : 3;         // f
        u32 device : 5;           // d
        u32 bus : 8;              // b
        SpaceType space_type : 2; // s
        u32 : 3;                  // 0
        u32 aliased : 1;          // t
        u32 prefetchable : 1;     // p
        u32 relocatable : 1;      // n
    };
    u32 raw;
};
static_assert(AssertSize<OpenFirmwareAddress, 4>());

// Taken from https://pcisig.com/sites/default/files/files/PCI_Code-ID_r_1_11__v24_Jan_2019.pdf
enum class ClassID {
    Legacy = 0x00,
    MassStorage = 0x01,
    Network = 0x02,
    Display = 0x03,
    Multimedia = 0x04,
    Memory = 0x05,
    Bridge = 0x06,
    SimpleCommunication = 0x07,
    Base = 0x08,
    Input = 0x09,
    DockingStation = 0x0A,
    Processor = 0x0B,
    SerialBus = 0x0C,
    Wireless = 0x0D,
    IntelligentIO = 0x0E,
    SatelliteCommunication = 0x0F,
    EncryptionDecryption = 0x10,
    DataAcquisitionAndSignalProcessing = 0x11,
    ProcessingAccelerator = 0x12,
    NonEssentialInstrumentation = 0x13,
};

namespace Legacy {

enum class SubclassID {
    Any = 0x00,
    VGACompatible = 0x01
};

}
namespace MassStorage {

enum class SubclassID {
    SCSIController = 0x00,
    IDEController = 0x01,
    FloppyController = 0x02,
    IPIController = 0x03,
    RAIDController = 0x04,
    ATAController = 0x05,
    SATAController = 0x06,
    SASController = 0x06,
    NVMeController = 0x08 // Technically other non-volatile memory subsystems as well
};

enum class SATAProgIF {
    AHCI = 0x1,
};

}

namespace Network {

enum class SubclassID {
    Ethernet = 0x00,
    TokenRing = 0x01,
    FDD = 0x02,
    ATM = 0x03,
    ISDN = 0x04,
    WorldFlip = 0x05,
    PICMG_2_14_MultiComputing = 0x06,
    InfiniBand = 0x07,
    HostFabric = 0x08,
};

}

namespace Display {

enum class SubclassID {
    VGA = 0x00,
    XGA = 0x01,
    ThreeD = 0x02,
};

}

namespace Multimedia {

enum class SubclassID {
    Video = 0x00,
    Audio = 0x01,
    ComputerTelephony = 0x01,
    HDACompatible = 0x3,
};

}

namespace Bridge {

enum class SubclassID {
    PCI_TO_PCI = 0x4,
};

}

namespace Base {

enum class SubclassID {
    PIC = 0x00,
    DMAController = 0x01,
    Timer = 0x02,
    RTCController = 0x03,
    PCIHotplugController = 0x04,
    SDHostController = 0x5,
    IOMMU = 0x06
};

}

namespace SerialBus {

enum class SubclassID {
    USB = 0x03,
};

enum class USBProgIf {
    UHCI = 0x00,
    OHCI = 0x10,
    EHCI = 0x20,
    xHCI = 0x30,
    None = 0x80,
    Device = 0xFE
};

}

AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, CapabilityID);

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

    bool operator==(HardwareID const& other) const
    {
        return vendor_id == other.vendor_id && device_id == other.device_id;
    }
    bool operator!=(HardwareID const& other) const
    {
        return vendor_id != other.vendor_id || device_id != other.device_id;
    }
};

class Domain {
public:
    Domain() = delete;
    Domain(u32 domain_number, u8 start_bus, u8 end_bus)
        : m_domain_number(domain_number)
        , m_start_bus(start_bus)
        , m_end_bus(end_bus)
    {
    }
    u8 start_bus() const { return m_start_bus; }
    u8 end_bus() const { return m_end_bus; }
    u32 domain_number() const { return m_domain_number; }

private:
    u32 m_domain_number;
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

    Address(Address const& address) = default;

    bool is_null() const { return !m_bus && !m_device && !m_function; }
    operator bool() const { return !is_null(); }

    // Disable default implementations that would use surprising integer promotion.
    bool operator<=(Address const&) const = delete;
    bool operator>=(Address const&) const = delete;
    bool operator<(Address const&) const = delete;
    bool operator>(Address const&) const = delete;

    bool operator==(Address const& other) const
    {
        if (this == &other)
            return true;
        return m_domain == other.m_domain && m_bus == other.m_bus && m_device == other.m_device && m_function == other.m_function;
    }
    bool operator!=(Address const& other) const
    {
        return !(*this == other);
    }

    u32 domain() const { return m_domain; }
    u8 bus() const { return m_bus; }
    u8 device() const { return m_device; }
    u8 function() const { return m_function; }

private:
    u32 m_domain { 0 };
    u8 m_bus { 0 };
    u8 m_device { 0 };
    u8 m_function { 0 };
};

class Capability {
public:
    Capability(Address address, u8 id, u8 ptr)
        : m_address(address)
        , m_id(id)
        , m_ptr(ptr)
    {
    }

    CapabilityID id() const { return m_id; }

    u8 read8(size_t offset) const;
    u16 read16(size_t offset) const;
    u32 read32(size_t offset) const;
    void write8(size_t offset, u8 value) const;
    void write16(size_t offset, u16 value) const;
    void write32(size_t offset, u32 value) const;

private:
    Address const m_address;
    CapabilityID const m_id;
    u8 const m_ptr;
};

AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, ClassCode);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(ClassCode, ClassID)

AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, SubclassCode);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(SubclassCode, Legacy::SubclassID);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(SubclassCode, MassStorage::SubclassID);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(SubclassCode, Network::SubclassID);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(SubclassCode, Display::SubclassID);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(SubclassCode, Multimedia::SubclassID);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(SubclassCode, Bridge::SubclassID);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(SubclassCode, Base::SubclassID);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(SubclassCode, SerialBus::SubclassID);

AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, ProgrammingInterface);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(ProgrammingInterface, MassStorage::SATAProgIF);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(ProgrammingInterface, SerialBus::USBProgIf);

AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, RevisionID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u16, SubsystemID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u16, SubsystemVendorID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, InterruptLine);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u8, InterruptPin);

class Access;
class EnumerableDeviceIdentifier {
public:
    EnumerableDeviceIdentifier(Address address, HardwareID hardware_id, RevisionID revision_id, ClassCode class_code, SubclassCode subclass_code, ProgrammingInterface prog_if, SubsystemID subsystem_id, SubsystemVendorID subsystem_vendor_id, InterruptLine interrupt_line, InterruptPin interrupt_pin, Vector<Capability> const& capabilities)
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
            for (auto const& capability : capabilities)
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

protected:
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

class MSIxInfo {
public:
    MSIxInfo(u16 table_size, u8 table_bar, u32 table_offset)
        : table_size(table_size)
        , table_bar(table_bar)
        , table_offset(table_offset)
    {
    }

    MSIxInfo() = default;

    u16 table_size {};
    u8 table_bar {};
    u32 table_offset {};
};

class MSIInfo {
public:
    MSIInfo(bool message_address_64_bit_support, u8 count)
        : message_address_64_bit_format(message_address_64_bit_support)
        , count(count)
    {
    }

    MSIInfo() = default;

    bool message_address_64_bit_format { false };
    u8 count {};
};

class DeviceIdentifier
    : public RefCounted<DeviceIdentifier>
    , public EnumerableDeviceIdentifier {
    AK_MAKE_NONCOPYABLE(DeviceIdentifier);

public:
    static ErrorOr<NonnullRefPtr<DeviceIdentifier>> from_enumerable_identifier(EnumerableDeviceIdentifier const& other_identifier);

    void initialize();
    bool is_msix_capable() const { return m_msix_info.table_size > 0; }
    u8 get_msix_table_bar() const { return m_msix_info.table_bar; }
    u32 get_msix_table_offset() const { return m_msix_info.table_offset; }

    bool is_msi_capable() const { return m_msi_info.count > 0; }
    bool is_msi_64bit_address_format() { return m_msi_info.message_address_64_bit_format; }

    Spinlock<LockRank::None>& operation_lock() { return m_operation_lock; }
    Spinlock<LockRank::None>& operation_lock() const { return m_operation_lock; }

    virtual ~DeviceIdentifier() = default;

private:
    DeviceIdentifier(EnumerableDeviceIdentifier const& other_identifier)
        : EnumerableDeviceIdentifier(other_identifier.address(),
              other_identifier.hardware_id(),
              other_identifier.revision_id(),
              other_identifier.class_code(),
              other_identifier.subclass_code(),
              other_identifier.prog_if(),
              other_identifier.subsystem_id(),
              other_identifier.subsystem_vendor_id(),
              other_identifier.interrupt_line(),
              other_identifier.interrupt_pin(),
              other_identifier.capabilities())
    {
    }

    mutable Spinlock<LockRank::None> m_operation_lock;
    MSIxInfo m_msix_info {};
    MSIInfo m_msi_info {};
};

class Domain;
class Device;
}

template<>
struct AK::Formatter<Kernel::PCI::Address> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::PCI::Address value)
    {
        return Formatter<FormatString>::format(
            builder,
            "PCI [{:04x}:{:02x}:{:02x}:{:02x}]"sv, value.domain(), value.bus(), value.device(), value.function());
    }
};

template<>
struct AK::Formatter<Kernel::PCI::HardwareID> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::PCI::HardwareID value)
    {
        return Formatter<FormatString>::format(
            builder,
            "PCI::HardwareID [{:04x}:{:04x}]"sv, value.vendor_id, value.device_id);
    }
};
