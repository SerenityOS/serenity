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
#include <AK/Types.h>

namespace Kernel {

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
    bool operator!=(const ID& other) const
    {
        return vendor_id != other.vendor_id || device_id != other.device_id;
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

inline const LogStream& operator<<(const LogStream& stream, const Address value)
{
    return stream << "PCI [" << String::format("%w", value.seg()) << ":" << String::format("%b", value.bus()) << ":" << String::format("%b", value.slot()) << "." << String::format("%b", value.function()) << "]";
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

struct [[gnu::packed]] Capability
{
    u8 capbility_id;
    u8 next_capability_pointer;
};

enum class CapabilityID {
    Reserved = 0x0,
    PowerManagementInterface = 0x1,
    AGP = 0x2,
    VPD = 0x3,
    SlotIdentification = 4,
    MSI = 0x5,
    CompactPCIHotSwap = 0x6,
    PCI_X = 0x7,
    HyperTransport = 0x8,
    VendorSpecific = 0x9,
    DebugPort = 0xA,
    CompactPCI = 0xB,
    PCIHotPlug = 0xC,
    AGP_8x = 0xE,
    SecureDevice = 0xF,
    PCIe = 0x10,
    MSIx = 0x11
};

struct [[gnu::packed]] MessageCapability : public Capability
{
    u16 message_control;
    u32 message_address;
    u16 message_data;
};

struct [[gnu::packed]] Message64BitCapability : public Capability
{
    u16 message_control;
    u32 message_address;
    u32 message_upper_address;
    u16 message_data;
};

ID get_id(PCI::Address);
void enumerate_all(Function<void(Address, ID)> callback);
void enable_interrupt_line(Address);
void disable_interrupt_line(Address);
bool support_capability_list(Address address);
u8 get_interrupt_line(Address);
void raw_access(Address, u32, size_t, u32);
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
namespace PCIExpress {
struct [[gnu::packed]] Capbility
{
    PCI::Capability header;
    u16 pcie_capabilities_register;
    u32 device_capabilities;
    u16 device_control;
    u16 device_status;
    u32 link_capabilities;
    u16 link_control;
    u16 link_status;
    u32 slot_capabilities;
    u16 slot_control;
    u16 slot_status;
    u16 root_control;
    u16 root_capabilities;
    u32 root_status;
    u32 device_capabilities2;
    u16 device_control2;
    u16 device_status2;
    u32 link_capabilities2;
    u16 link_control2;
    u16 link_status2;
    u32 slot_capabilities2;
    u16 slot_control2;
    u16 slot_status2;
};

struct [[gnu::packed]] ExtendedCapabilityHeader
{
    u16 pcie_extended_capability_id;
    u16 attributes; /* bits 0-3: Capability Version, bits 4-15: Next Capability offset */
};

enum ExtendedCapabilityID {
    AER = 0x1,
    VirtualChannel = 0x2,
    VirtualChannel2 = 0x9,
    DeviceSerialNumber = 0x3,
    RootComplexLinkDeclaration = 0x5,
    RootComplexInternalLinkControl = 0x6,
    PowerBudgeting = 0x4,
    ACS = 0xD,
    RootComplexEventCollectorEndpointAssociation = 0x7,
    MFVC = 0x8, /* Multi-Function Virtual Channel */
    VendorSpecific = 0xB,
    RCRB = 0xA,
    Multicast = 0x12,
    ResizableBAR = 0x15,
};

struct [[gnu::packed]] VirtualChannelResource
{
    u32 capability; /* VC Resource Capability Register */
    u32 control;    /* VC Resource Control Register */
    u16 reserved;
    u16 status; /* VC Resource Status Register */
};

struct [[gnu::packed]] ElementSelfDescription
{
    u8 reserved;
    u8 link_entries_count; /* Number of Link Entries */
    u8 component_id;
    u8 port_number;
};

struct [[gnu::packed]] LinkDescription
{
    u16 attributes;
    u8 target_component_id;
    u8 target_port_id;
};

struct [[gnu::packed]] LinkEntry
{
    LinkDescription description;
    u32 reserved;
    u64 link_address;
};

namespace Capability {
struct [[gnu::packed]] AdvancedErrorReporting
{
    ExtendedCapabilityHeader header;
    u32 uncorrectable_error_status;   /* Uncorrectable Error Status Register */
    u32 uncorrectable_error_mask;     /* Uncorrectable Error Mask Register */
    u32 uncorrectable_error_severity; /* Uncorrectable Error Severity Register */
    u32 correctable_error_status;     /* Correctable Error Status Register */
    u32 correctable_error_mask;       /* Correctable Error Mask Register */
    u32 aecc;                         /* Advanced Error Capabilities and Control Register */
    u32 header_log[4];                /* Header Log Register */
    u32 root_error_command;
    u32 root_error_status;
    u16 correctable_error_source_identification; /* Correctable Error Source Identification Register */
    u16 error_source_identification;             /* Error Source Identification Register */
    u32 tlp_prefix_log[4];                       /* TLP Prefix Log Register */
};

struct [[gnu::packed]] VirtualChannel
{
    ExtendedCapabilityHeader header;
    u32 port_vc_capability1; /* Port VC Capability Register 1  */
    u32 port_vc_capability2; /* Port VC Capability Register 2 */
    u16 port_vc_control;     /* Port VC Control Register */
    u16 port_vc_status;      /* Port VC Status Register */
    VirtualChannelResource resources[];
};

struct [[gnu::packed]] RootComplexLinkDeclaration
{
    ExtendedCapabilityHeader header;
    ElementSelfDescription description;
    u32 reserved;
    LinkEntry link1;
    LinkEntry optional_links[];
};

struct [[gnu::packed]] RootComplexInternalLinkControl
{
    ExtendedCapabilityHeader header;
    u32 root_complex_link_capabilities;
    u16 root_complex_link_control;
    u16 root_complex_link_status;
};

struct [[gnu::packed]] PowerBudgeting
{
    ExtendedCapabilityHeader header;
    u8 data_select;
    u8 reserved[3];
    u32 data;                   /* Data Register */
    u8 power_budget_capability; /* Power Budget Capability Register */
    u8 reserved2[3];
};

struct [[gnu::packed]] ACS
{
    ExtendedCapabilityHeader header;
    u16 acs_capability;           /* ACS Capability Register */
    u16 acs_control;              /* ACS Control Register */
    u32 egress_control_vector;    /* Egress Control Vector */
    u32 egress_control_vectors[]; /* (additional Egress Control Vector DWORDs if required) */
};

struct [[gnu::packed]] RootComplexEventCollectorEndpointAssociation
{
    ExtendedCapabilityHeader header;
    u32 association_bitmap; /* Association Bitmap for Root Complex Integrated Devices */
};

struct [[gnu::packed]] MFVC /* Multi-Function Virtual Channel */
{
    ExtendedCapabilityHeader header;
    u32 port_vc_capability1; /* Port VC Capability Register 1  */
    u32 port_vc_capability2; /* Port VC Capability Register 2 */
    u16 port_vc_control;     /* Port VC Control Register */
    u16 port_vc_status;      /* Port VC Status Register */
    VirtualChannelResource resources[];
};

struct [[gnu::packed]] VendorSpecific
{
    ExtendedCapabilityHeader header;
    u32 vendor_specific_header;
    u8 vendor_specific_registers[];
};

struct [[gnu::packed]] RCRB
{
    ExtendedCapabilityHeader header;
    u16 vendor_id;
    u16 device_id;
    u32 rcrb_capabilites;
    u32 rcrb_control;
    u32 reserved;
};

struct [[gnu::packed]] Multicast
{
    ExtendedCapabilityHeader header;
    u16 capability_register; /* Multicast Capability Register */
    u16 control_register;    /* Multicast Control Register */
    u32 base_address;        /* MC_Base_Address Register */
    u32 receive;             /* MC_Receive Register */
    u32 block_all;           /* MC_Block_All Register */
    u32 block_untranslated;  /* MC_Block_Untranslated Register */
    u32 overlay_bar;         /* MC_Overlay_BAR */
};

struct [[gnu::packed]] ResizableBAR
{
    ExtendedCapabilityHeader header;
};
}

}
}
