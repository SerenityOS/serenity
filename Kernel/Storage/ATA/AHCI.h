/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::FIS {

enum class Type : u8 {
    RegisterHostToDevice = 0x27,
    RegisterDeviceToHost = 0x34,
    DMAActivate = 0x39,
    DMASetup = 0x41,
    Data = 0x46,
    BISTActivate = 0x58,
    PIOSetup = 0x5F,
    SetDeviceBits = 0xA1
};

enum class DwordCount : size_t {
    RegisterHostToDevice = 5,
    RegisterDeviceToHost = 5,
    DMAActivate = 1,
    DMASetup = 7,
    PIOSetup = 5,
    SetDeviceBits = 2
};

enum HeaderAttributes : u8 {
    C = (1 << 7), /* Updates Command register */
};

struct [[gnu::packed]] Header {
    u8 fis_type;
    u8 port_muliplier;
};

}

namespace Kernel::FIS::HostToDevice {

struct [[gnu::packed]] Register {
    Header header;
    u8 command;
    u8 features_low;
    u8 lba_low[3];
    u8 device;
    u8 lba_high[3];
    u8 features_high;
    u16 count;
    u8 icc; /* Isochronous Command Completion */
    u8 control;
    u32 reserved;
};

};

namespace Kernel::FIS::DeviceToHost {

struct [[gnu::packed]] Register {
    Header header;
    u8 status;
    u8 error;
    u8 lba_low[3];
    u8 device;
    u8 lba_high[3];
    u8 reserved;
    u16 count;
    u8 reserved2[6];
};

struct [[gnu::packed]] SetDeviceBits {
    Header header;
    u8 status;
    u8 error;
    u32 protocol_specific;
};

struct [[gnu::packed]] DMAActivate {
    Header header;
    u16 reserved;
};

struct [[gnu::packed]] PIOSetup {
    Header header;
    u8 status;
    u8 error;
    u8 lba_low[3];
    u8 device;
    u8 lba_high[3];
    u8 reserved;
    u16 count;
    u8 reserved2;
    u8 e_status;
    u16 transfer_count;
    u16 reserved3;
};

}

namespace Kernel::FIS::BiDirectional {

struct [[gnu::packed]] Data {
    Header header;
    u16 reserved;
    u32 data[];
};

struct [[gnu::packed]] BISTActivate {
};
struct [[gnu::packed]] DMASetup {
    Header header;
    u16 reserved;
    u32 dma_buffer_identifier_low;
    u32 dma_buffer_identifier_high;
    u32 reserved2;
    u32 dma_buffer_offset;
    u32 dma_transfer_count;
    u32 reserved3;
};

}

namespace Kernel::AHCI {

class MaskedBitField {

public:
    explicit MaskedBitField(volatile u32& bitfield_register)
        : m_bitfield(bitfield_register)
        , m_bit_mask(0xffffffff)
    {
    }

    MaskedBitField(volatile u32& bitfield_register, u32 bit_mask)
        : m_bitfield(bitfield_register)
        , m_bit_mask(bit_mask)
    {
    }

    void set_at(u8 index) const
    {
        VERIFY(((1u << index) & m_bit_mask) != 0);
        m_bitfield = m_bitfield | ((1u << index) & m_bit_mask);
    }

    void set_all() const
    {
        m_bitfield = m_bitfield | (0xffffffff & m_bit_mask);
    }

    bool is_set_at(u8 port_index) const
    {
        return m_bitfield & ((1u << port_index) & m_bit_mask);
    }

    bool is_zeroed() const
    {
        return (m_bitfield & m_bit_mask) == 0;
    }

    Vector<u8> to_vector() const
    {
        // FIXME: Add a sync mechanism!
        Vector<u8> indices;
        u32 bitfield = m_bitfield & m_bit_mask;
        for (size_t index = 0; index < 32; index++) {
            if (bitfield & 1) {
                indices.append(index);
            }
            bitfield >>= 1;
        }
        return indices;
    }

    u32 bit_mask() const { return m_bit_mask; };

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(const MaskedBitField&) const = delete;
    bool operator<=(const MaskedBitField&) const = delete;
    bool operator>=(const MaskedBitField&) const = delete;
    bool operator<(const MaskedBitField&) const = delete;
    bool operator>(const MaskedBitField&) const = delete;

private:
    volatile u32& m_bitfield;
    const u32 m_bit_mask;
};

enum Limits : u16 {
    MaxPorts = 32,
    MaxCommands = 32,
    MaxMultiplierConnectedPorts = 16,
};

enum CommandHeaderAttributes : u16 {
    C = (1 << 10), /* Clear Busy upon R_OK */
    P = (1 << 7),  /* Prefetchable */
    W = (1 << 6),  /* Write */
    A = (1 << 5),  /* ATAPI */
    R = (1 << 8)   /* Reset */
};

enum HBACapabilities : u32 {
    S64A = (u32)1 << 31, /* Supports 64-bit Addressing */
    SNCQ = 1 << 30,      /* Supports Native Command Queuing */
    SSNTF = 1 << 29,     /* Supports SNotification Register */
    SMPS = 1 << 28,      /* Supports Mechanical Presence Switch */
    SSS = 1 << 27,       /* Supports Staggered Spin-up */
    SALP = 1 << 26,      /* Supports Aggressive Link Power Management */
    SAL = 1 << 25,       /* Supports Activity LED */
    SCLO = 1 << 24,      /* Supports Command List Override */
    SAM = 1 << 18,       /* Supports AHCI mode only */
    SPM = 1 << 17,       /* Supports Port Multiplier */
    FBSS = 1 << 16,      /* FIS-based Switching Supported */
    PMD = 1 << 15,       /* PIO Multiple DRQ Block */
    SSC = 1 << 14,       /* Slumber State Capable */
    PSC = 1 << 13,       /* Partial State Capable */
    CCCS = 1 << 7,       /* Command Completion Coalescing Supported */
    EMS = 1 << 6,        /* Enclosure Management Supported */
    SXS = 1 << 5         /* Supports External SATA */
};

enum HBACapabilitiesExtended : u32 {
    DESO = 1 << 5, /* DevSleep Entrance from Slumber Only */
    SADM = 1 << 4, /* Supports Aggressive Device Sleep Management */
    SDS = 1 << 3,  /* Supports Device Sleep */
    APST = 1 << 2, /* Automatic Partial to Slumber Transitions */
    NVMP = 1 << 1, /* NVMHCI Present */
    BOH = 1 << 0,  /* BIOS/OS Handoff */
};

// This structure is not defined by the AHCI spec, but is used within the code
struct [[gnu::packed]] HBADefinedCapabilities {
    size_t ports_count { 1 };
    size_t max_command_list_entries_count { 1 };
    u8 interface_speed_generation { 1 };
    bool external_sata_supported : 1 { false };
    bool enclosure_management_supported : 1 { false };
    bool command_completion_coalescing_supported : 1 { false };
    bool partial_state_capable : 1 { false };
    bool slumber_state_capable : 1 { false };
    bool pio_multiple_drq_block : 1 { false };
    bool fis_based_switching_supported : 1 { false };
    bool port_multiplier_supported : 1 { false };
    bool ahci_mode_only : 1 { true };
    bool command_list_override_supported : 1 { false };
    bool activity_led_supported : 1 { false };
    bool aggressive_link_power_management_supported : 1 { false };
    bool staggered_spin_up_supported : 1 { false };
    bool mechanical_presence_switch_supported : 1 { false };
    bool snotification_register_supported : 1 { false };
    bool native_command_queuing_supported : 1 { false };
    bool addressing_64_bit_supported : 1 { false };
    bool bios_os_handoff : 1 { false };
    bool nvmhci_present : 1 { false };
    bool automatic_partial_to_slumber_transitions : 1 { false };
    bool device_sleep_supported : 1 { false };
    bool aggressive_device_sleep_management_supported : 1 { false };
    bool devsleep_entrance_from_slumber_only : 1 { false };
};

enum DeviceSignature : u32 {
    ATA = 0x00000101,
    ATAPI = 0xEB140101,
    EnclosureManagementBridge = 0xC33C0101,
    PortMultiplier = 0x96690101,
    Unconnected = 0xFFFFFFFF
};

enum class DeviceDetectionInitialization {
    NoActionRequested,
    PerformInterfaceInitializationSequence,
    DisableInterface
};

enum PortInterruptFlag : u32 {
    CPD = (u32)1 << 31, /* Cold Port Detect */
    TFE = 1 << 30,      /* Task File Error */
    HBF = 1 << 29,      /* Host Bus Fatal Error */
    HBD = 1 << 28,      /* Host Bus Data Error */
    IF = 1 << 27,       /* Interface Fatal Error */
    INF = 1 << 26,      /* Interface Non-fatal Error */
    OF = 1 << 24,       /* Overflow */
    IPM = 1 << 23,      /* Incorrect Port Multiplier */
    PRC = 1 << 22,      /* PhyRdy Change */
    DMP = 1 << 7,       /* Device Mechanical Presence */
    PC = 1 << 6,        /* Port Connect Change */
    DP = 1 << 5,        /* Descriptor Processed */
    UF = 1 << 4,        /* Unknown FIS */
    SDB = 1 << 3,       /* Set Device FIS */
    DS = 1 << 2,        /* DMA Setup FIS */
    PS = 1 << 1,        /* PIO Setup FIS */
    DHR = 1 << 0        /* Device to Host Register FIS */
};

enum SErr : u32 {
    DIAG_X = 1 << 26, /* Exchanged */
    DIAG_F = 1 << 25, /* Unknown FIS Type */
    DIAG_T = 1 << 24, /* Transport state transition error */
    DIAG_S = 1 << 23, /* Link sequence error */
    DIAG_H = 1 << 22, /* Handshake error */
    DIAG_C = 1 << 21, /* CRC error */
    DIAG_D = 1 << 20, /* Disparity error */
    DIAG_B = 1 << 19, /* 10B to 8B decode error */
    DIAG_W = 1 << 18, /* Comm Wake */
    DIAG_I = 1 << 17, /* Phy Internal Error */
    DIAG_N = 1 << 16, /* PhyRdy Change */
    ERR_E = 1 << 11,  /* Internal error */
    ERR_P = 1 << 10,  /* Protocol error */
    ERR_C = 1 << 9,   /* Persistent communication or data integrity error */
    ERR_T = 1 << 8,   /* Transient data integrity error */
    ERR_M = 1 << 1,   /* Received communications error */
    ERR_I = 1 << 0,   /* Recovered data integrity error */
};

class PortInterruptStatusBitField {

public:
    explicit PortInterruptStatusBitField(volatile u32& bitfield_register)
        : m_bitfield(bitfield_register)
    {
    }

    u32 raw_value() const { return m_bitfield; }
    bool is_set(PortInterruptFlag flag) const { return m_bitfield & (u32)flag; }
    void clear() { m_bitfield = 0xffffffff; }

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(const MaskedBitField&) const = delete;
    bool operator<=(const MaskedBitField&) const = delete;
    bool operator>=(const MaskedBitField&) const = delete;
    bool operator<(const MaskedBitField&) const = delete;
    bool operator>(const MaskedBitField&) const = delete;

private:
    volatile u32& m_bitfield;
};

class PortInterruptEnableBitField {

public:
    explicit PortInterruptEnableBitField(volatile u32& bitfield_register)
        : m_bitfield(bitfield_register)
    {
    }

    u32 raw_value() const { return m_bitfield; }
    bool is_set(PortInterruptFlag flag) { return m_bitfield & (u32)flag; }
    void set_at(PortInterruptFlag flag) { m_bitfield = m_bitfield | static_cast<u32>(flag); }
    void clear() { m_bitfield = 0; }
    bool is_cleared() const { return m_bitfield == 0; }
    void set_all() { m_bitfield = 0xffffffff; }

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(const MaskedBitField&) const = delete;
    bool operator<=(const MaskedBitField&) const = delete;
    bool operator>=(const MaskedBitField&) const = delete;
    bool operator<(const MaskedBitField&) const = delete;
    bool operator>(const MaskedBitField&) const = delete;

private:
    volatile u32& m_bitfield;
};

struct [[gnu::packed]] PortRegisters {
    u32 clb;  /* Port x Command List Base Address */
    u32 clbu; /* Port x Command List Base Address Upper 32-Bits */
    u32 fb;   /* Port x FIS Base Address */
    u32 fbu;  /* Port x FIS Base Address Upper 32-Bits */
    u32 is;   /* Port x Interrupt Status */
    u32 ie;   /* Port x Interrupt Enable */
    u32 cmd;  /* Port x Command and Status */
    u32 reserved;
    u32 tfd;    /* Port x Task File Data */
    u32 sig;    /* Port x Signature */
    u32 ssts;   /* Port x Serial ATA Status (SCR0: SStatus) */
    u32 sctl;   /* Port x Serial ATA Control (SCR2: SControl) */
    u32 serr;   /* Port x Serial ATA Error (SCR1: SError) */
    u32 sact;   /* Port x Serial ATA Active (SCR3: SActive) */
    u32 ci;     /* Port x Command Issue */
    u32 sntf;   /* Port x Serial ATA Notification (SCR4: SNotification) */
    u32 fbs;    /* Port x FIS-based Switching Control */
    u32 devslp; /* Port x Device Sleep */
    u8 reserved2[0x70 - 0x48];
    u8 vs[16]; /* Port x Vendor Specific */
};

struct [[gnu::packed]] GenericHostControl {
    u32 cap; /* Host Capabilities */
    u32 ghc; /* Global Host Control */
    u32 is;  /* Interrupt Status */
    u32 pi;  /* Ports Implemented */
    u32 version;
    u32 ccc_ctl;   /* Command Completion Coalescing Control */
    u32 ccc_ports; /* Command Completion Coalsecing Ports */
    u32 em_loc;    /* Enclosure Management Location */
    u32 em_ctl;    /* Enclosure Management Control */
    u32 cap2;      /* Host Capabilities Extended */
    u32 bohc;      /* BIOS/OS Handoff Control and Status */
};

struct [[gnu::packed]] HBA {
    GenericHostControl control_regs;
    u8 reserved[52];
    u8 nvmhci[64];
    u8 vendor_specific[96];
    PortRegisters port_regs[32];
};

struct [[gnu::packed]] CommandHeader {
    u16 attributes;
    u16 prdtl; /* Physical Region Descriptor Table Length */
    u32 prdbc; /* Physical Region Descriptor Byte Count */
    u32 ctba;  /* Command Table Descriptor Base Address */
    u32 ctbau; /* Command Table Descriptor Base Address Upper 32-bits */
    u32 reserved[4];
};

struct [[gnu::packed]] PhysicalRegionDescriptor {
    u32 base_low;
    u32 base_high;
    u32 reserved;
    u32 byte_count; /* Bit 31 - Interrupt completion, Bit 0 to 21 - Data Byte Count */
};

struct [[gnu::packed]] CommandTable {
    u8 command_fis[64];
    u8 atapi_command[32];
    u8 reserved[32];
    PhysicalRegionDescriptor descriptors[];
};
}
