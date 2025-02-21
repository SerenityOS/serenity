/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <Kernel/Library/IORegisterMap.h>

namespace Kernel::E1000 {

// https://www.intel.com/content/dam/doc/manual/pci-pci-x-family-gbe-controllers-software-dev-manual.pdf
// Table 13-2
enum class Register {
    // FIXME: There are more registers, allowing some more HW acceleration,
    //        statistics and more. Support these!
    Ctrl = 0x0000,
    Status = 0x0008,
    EEPROMControl = 0x0010,
    EEPROMRead = 0x0014,
    CtrlExt = 0x0018,
    InterruptCauseR = 0x00C0,
    InterruptThrottling = 0x00C4, // ITR, seems to be not present on later NICs
    InterruptMask = 0x00D0,
    InterruptMaskClear = 0x00D8,
    RCtrl = 0x0100,    // RCTL
    TCtrl = 0x0400,    // TCTL
    TCtrlExt = 0x0404, // TCTL_EXT
    TIPG = 0x0410,     // Transmit Inter Packet Gap

    GPIE = 0x1514, // General Purpose Interrupt Enable

    ExtendedInterruptThrottling = 0x1680, // EITR

    RXDescLow = 0x2800,  // Receive Descriptor Base Low (RDBAL)
    RXDescHigh = 0x2804, // Receive Descriptor Base High (RDBAH)
    RXDescLength = 0x2808,
    RXDescHead = 0x2810,
    RXDescTail = 0x2818,
    RDTR = 0x2820,  // RX Delay Timer Register
    RADV = 0x282C,  // RX Int. Absolute Delay Timer
    RSRPD = 0x2C00, // RX Small Packet Detect Interrupt
    TXDescLow = 0x3800,
    TXDescHigh = 0x3804,
    TXDescLength = 0x3808,
    TXDescHead = 0x3810,
    TXDescTail = 0x3818,
    RXDCTL = 0x3828, // RX Descriptor Control

    // 64 Bit IO allowed to load/store both at the same time
    RAL = 0x5400, // Receive Address Low
    RAH = 0x5404, // Receive Address High

    SRRCTL0 = 0xC00C, // Split and Replication Receive Control for Queue 0

    TXDCTL0 = 0x3808, // Transmit Descriptor Control (+0x40 per queue)
};

enum class LinkSpeed : u32 {
    Speed10M = 0b00,
    Speed100M = 0b01,
    Speed1000M_1 = 0b10,
    Speed1000M_2 = 0b11,
};

enum class LoopbackMode : u32 {
    None = 0b00,
    PHY = 0b11,
};

// 13.4.1
// Table 13-3
struct Ctrl {

    enum SDPIODirection : u32 {
        Input = 0,
        Output = 1,
    };

    u32 full_duplex : 1;
    u32 : 2;
    u32 link_reset : 1;
    u32 : 1;
    u32 auto_speed_detection : 1;
    u32 set_link_up : 1;
    u32 invert_loss_of_signal : 1;
    LinkSpeed speed : 2;
    u32 : 1;
    u32 force_speed : 1;
    u32 force_duplex : 1;
    u32 : 5;
    u32 sdp0_data_value : 1;
    u32 sdp1_data_value : 1;
    u32 d3_cold_wakeup_advertisement_enable : 1;
    u32 enable_phy_power_management : 1;
    SDPIODirection sdp0_direction : 1;
    SDPIODirection sdp1_direction : 1;
    u32 : 2;
    u32 reset : 1;
    u32 receive_flow_control_enable : 1;
    u32 transmit_flow_control_enable : 1;
    u32 : 1;
    u32 vlan_mode : 1;
    u32 phy_reset : 1;
};
static_assert(AssertSize<Ctrl, 4>());

// 13.4.2
// Table 13-5
struct Status {
    enum class PCIXSpeed : u32 {
        Speed66MHz = 0b00,
        Speed100MHz = 0b01,
        Speed133MHz = 0b10,
        Reserved = 0b11,
    };

    u32 full_duplex : 1;
    u32 link_up : 1;
    u32 function_id : 2; // 82546GB/EB only
    u32 txoff : 1;
    u32 tbi_mode : 1;
    LinkSpeed speed : 2;
    LinkSpeed auto_speed_detection : 2;
    u32 : 1;
    u32 pci_66mhz : 1;
    u32 pci_64bit_bus : 1;
    u32 pcix_mode : 1;
    PCIXSpeed pcix_speed : 2;
    u32 : 16;
};
static_assert(AssertSize<Status, 4>());

// 13.4.3
// Table 13-6
struct EEPROMControl {
    enum class FlashWriteEnable : u32 {
        Disabled = 0b01,
        Enabled = 0b10,
    };

    u32 clock_input : 1;                     // SK
    u32 chip_select : 1;                     // CS
    u32 data_in : 1;                         // DI
    u32 data_out : 1;                        // DO
    FlashWriteEnable flash_write_enable : 2; // FW
    // Not on 82544GC/EI:
    u32 direct_eeprom_access_request : 1; // EE_REQ
    u32 direct_eeprom_access_grant : 1;   // EE_GNT
    u32 eeprom_present : 1;               // EE_PRES // always 0 on 82541xx and 82547GI/EI
    // 82541xx and 82547GI/EI:
    u32 eeprom_size : 2; // EE_SIZE
    u32 : 2;
    u32 eeprom_type : 1; // EE_TYPE
    u32 : 18;
};
static_assert(AssertSize<EEPROMControl, 4>());

// 13.4.4
// Table 13-7
union EEPROMRead {
    struct {
        u32 start : 1;
        u32 : 3;
        u32 done : 1;
        u32 : 3;
        u32 address : 8;
        u32 data : 16;
    } address_8;
    struct {
        u32 start : 1 = 0;
        u32 done : 1 = 0;
        u32 address : 14 = 0;
        u32 data : 16 = 0;
    } address_14;
    u32 raw = 0;
};

// 13.4.17
// Table 13-63
// 13.4.20
// Table 13-65
enum class Interrupt {
    TXDW = 1 << 0,   // Transmit Descriptor Written Back
    TXQE = 1 << 1,   // Transmit Queue Empty, until 82576
    LSC = 1 << 2,    // Link Status Change
    RXSEQ = 1 << 3,  // Receive Sequence Error (not on 82541xx, 82547GI/EI)
    RXDMT0 = 1 << 4, // Receive Descriptor Minimum Threshold hit
    MACSec = 1 << 5, // MAC Security (since 82576)
    RXO = 1 << 6,    // Receiver FIFO Overrun

    RXT0 = 1 << 7, // Receive Timer Interrupt       (until 82576)
    RXDW = 1 << 7, // Receive Descriptor Write Back (since 82576)
    // 8 Reserved (VMMB)
    MDAC = 1 << 9,   // MDIO Access Complete        (until 82576)
    RXCFG = 1 << 10, // Receiving /C/ Ordered Sets  (until 82576)
    //
    PHYINT = 1 << 12, // PHY Interrupt (not on 82544GI/EI)
    // 82576 and later have GPI_SDPx (here)
    // 11-12 General Purpose Interrupts (82544GI/EI only)
    //       otherwise Reserved
    GPI1 = 1 << 13,
    GPI2 = 1 << 14,
    TXD_LOW = 1 << 15, // Transmit Descriptor Low Threshold Hit (not on 82544GC/EI)
    SRPD = 1 << 16,    // Small Receive Packet Detection (not on 82544GC/EI, until 82576)
    // 17-31 Reserved

    // FIXME: Newer NICs have more interrupts

    InterruptClear = ~0,
    None = 0

};
AK_ENUM_BITWISE_OPERATORS(Interrupt);

// I211
// 8.7.17 General Purpose Interrupt Enable - GPIE
struct GeneralPurposeInterruptEnable {
    u32 non_selective_interrupt_clear_on_read : 1; // NSICR
    u32 : 3;
    u32 multiple_msix : 1; // Multiple MSI-X vectors
    u32 : 2;
    u32 ll_interval : 5;
    u32 : 18;
    u32 extended_interrupt_auto_mask_enable : 1; // EIAME
    u32 pba_support : 1;
};

// I211
// 8.7.14 Interrupt Throttle - EITR
struct ExtendedInterruptThrottling {
    u32 : 2;
    u32 interval : 13;  // in µs intervals
    u32 lli_enable : 1; // Low Latency Interrupt Enable
    // Already present on 82574, but there the interval is in 256ns intervals, 15 bit,
    // ie. it also spans the enable bit, rest is reserved
    // and only is used for MSI-X interrupts
    u32 ll_counter : 5;
    u32 moderation_counter : 10;
    u32 counter_integrity : 1; // Counter Integrity Enable, don't set counters
};

// 13.4.22
// Table 13-67
struct ReceiveControl {
    enum class FreeBufferThreshold : u32 {
        Half = 0b00,
        Quarter = 0b01,
        Eighth = 0b10,
        Reserved = 0b11,
    };
    enum class BufferSize : u32 {
        Size2048 = 0b00,
        Size1024 = 0b01,
        Size512 = 0b10,
        Size256 = 0b11,
        // With RCTL.BSEX set to 1
        // Do not use 0b00
        Size16384 = 0b01,
        Size8192 = 0b10,
        Size4096 = 0b11,
    };

    u32 : 1;
    u32 enable : 1;
    u32 store_bad_frames : 1;
    u32 unicast_promiscuous_enable : 1;
    u32 multicast_promiscuous_enable : 1;
    u32 long_packet_enable : 1;
    LoopbackMode loopback_mode : 2;
    FreeBufferThreshold read_descriptor_minimum_threshold_size : 2; // Reserved on 82576 and later
    u32 : 2;
    u32 multicast_offset : 2;
    u32 : 1;
    u32 broadcast_accept_mode : 1;
    BufferSize buffer_size : 2;
    u32 vlan_filter_enable : 1;
    u32 canonical_form_indicator_enable : 1;
    u32 canonical_form_indicator_value : 1;
    u32 pad_small_packets : 1; // 82576 and later
    u32 discard_pause_frames : 1;
    u32 pass_mac_control_frames : 1;
    u32 : 1;
    u32 buffer_size_extension : 1; // BSEX, reserved on 82576 and later
    u32 strip_ethernet_crc : 1;
    u32 : 5;
};
static_assert(AssertSize<ReceiveControl, 4>());

// 13.4.33
// Table 13-76
union TransmitControl {
    struct {
        u32 : 1;
        u32 enable : 1;
        u32 : 1;
        u32 pad_short_packets : 1;
        u32 collision_threshold : 8;
        u32 : 10;
        u32 software_xoff_transmit : 1;
        u32 : 1;
        u32 retransmit_on_late_collision : 1;
        u32 no_retransmit_on_underrun : 1; // 82544GC/EI only
        u32 : 6;
    };
    struct {
        u32 : 12;
        u32 collision_distance : 10;
        u32 : 10;
    } until_82576; // Moved to TCTL_EXT(19:10)
    struct {
        u32 : 10;
        u32 back_off_slot_time : 10;
        u32 : 12;
    } from_82576;
};
static_assert(AssertSize<TransmitControl, 4>());

struct TransmitControlExtended {
    u32 reserved_x40 : 10;
    u32 collision_distance : 10;
    u32 : 12;
};
static_assert(AssertSize<TransmitControlExtended, 4>());

struct TransmitInterPacketGap {
    u32 ipgt : 10;
    u32 ipgr1 : 10;
    u32 ipgr : 10;
    u32 : 2;
};
static_assert(AssertSize<TransmitInterPacketGap, 4>());

// https://www.intel.com/content/dam/www/public/us/en/documents/datasheets/82576eb-gigabit-ethernet-controller-datasheet.pdf
// 8.10.2 Split and Replication Receive Control (SRRCTL)
struct SplitAndReplicationReceiveControl {
    enum class DescriptorType : u32 {
        Legacy = 0b000,
        AdvancedOneBuffer = 0b001,
        AdvancedHeaderSplit = 0b010,
        AdvancedHeaderReplicationAlways = 0b011,
        AdvancedHeaderReplicationOnLargePacket = 0b100,
        // 0b101, 0b111 reserved
        // 0b110 not mentioned, reserved?
    };

    u32 bsize_packet : 7; // Receive Buffer size for packet buffer, in 1KB units, 0 means use RCTL.BSIZE
    u32 : 1;
    u32 bsize_header : 4; // Receive Buffer size for header buffer, in 64B units, must be >0 if DESCTYPE>2
    u32 : 2;
    u32 : 6;
    u32 read_descriptor_minimum_threshold_size : 5; // in multiples of 16
    DescriptorType descriptor_type : 3;
    u32 : 3;
    u32 drop_enable : 1;
};
static_assert(AssertSize<SplitAndReplicationReceiveControl, 4>());

// I211
// 8.11.15 Transmit Descriptor Control (TXDCTL)
struct TransmitDescriptorControl {
    u32 prefetch_threshold : 5;
    u32 : 3;
    u32 host_threshold : 5;
    u32 : 3;
    u32 writeback_threshold : 5;
    u32 : 3;
    u32 : 1;
    u32 queue_enable : 1;
    u32 software_flush : 1;
    u32 priority : 1;
    u32 head_writeback_threshold : 4;
};
static_assert(AssertSize<TransmitDescriptorControl, 4>());

using RegisterMap = IORegisterMap<Register,
    IOReg<Register, Register::Ctrl, Ctrl>,
    IOReg<Register, Register::Status, Status>,
    IOReg<Register, Register::EEPROMControl, EEPROMControl>,
    IOReg<Register, Register::EEPROMRead, EEPROMRead>,

    IOReg<Register, Register::InterruptCauseR, Interrupt>,
    IOReg<Register, Register::InterruptThrottling, u32>,
    IOReg<Register, Register::InterruptMask, Interrupt>,

    IOReg<Register, Register::RCtrl, ReceiveControl>,

    IOReg<Register, Register::TCtrl, TransmitControl>,
    IOReg<Register, Register::TCtrlExt, TransmitControlExtended>,
    IOReg<Register, Register::TIPG, TransmitInterPacketGap>,

    IOReg<Register, Register::ExtendedInterruptThrottling, ExtendedInterruptThrottling>,

    IOReg<Register, Register::RXDescLow, u32>,
    IOReg<Register, Register::RXDescHigh, u32>,
    IOReg<Register, Register::RXDescLength, u32>,
    IOReg<Register, Register::RXDescHead, u32>,
    IOReg<Register, Register::RXDescTail, u32>,

    IOReg<Register, Register::TXDescLow, u32>,
    IOReg<Register, Register::TXDescHigh, u32>,
    IOReg<Register, Register::TXDescLength, u32>,
    IOReg<Register, Register::TXDescHead, u32>,
    IOReg<Register, Register::TXDescTail, u32>,

    IORegArray<Register, Register::RAL, u32, 16, 8, true>,
    IORegArray<Register, Register::RAH, u32, 16, 8, true>,

    IOReg<Register, Register::SRRCTL0, SplitAndReplicationReceiveControl>,

    IOReg<Register, Register::TXDCTL0, TransmitDescriptorControl>>;
}
