/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::USB::xHCI {

// 5.3 Host Controller Capability Registers
struct CapabilityRegisters {
    struct {
        u32 caplength : 8;
        u32 rsvd : 8;
        u32 hci_version_minor : 8;
        u32 hci_version_major : 8;
    };
    // 5.3.3 Structural Parameters 1 (HCSPARAMS1)
    struct {
        u32 max_device_slots : 8;  //  0-7
        u32 max_interrupters : 11; //  8-18
        u32 : 5;                   // 19-23 Rsvd.
        u32 number_of_ports : 8;   // 24-31 MaxPorts
    } structural_parameters1;
    // 5.3.4 Structural Parameters 2 (HCSPARAMS2)
    struct {
        u32 isochronous_scheduling_threshold : 4; //  0- 3 IST
        u32 event_ring_segment_table_max : 4;     //  4- 7 ERST Max
        u32 : 13;                                 //  8-20 Rsvd.
        u32 max_scratchpad_buffers_hi : 5;        // 21-25
        u32 scratchpad_restore : 1;               // 26    SPR
        u32 max_scratchpad_buffers_lo : 5;        // 27-31
    } structural_parameters2;
    // 5.3.5 Structural Parameters 3 (HCSPARAMS3)
    struct {
        // Spec Bug: Figure 5-8: Structural Parameters 3 Register (HCSPARAMS3)
        //           Shows:
        //           31                 16|15     8|7      0|
        //                  Rsvd.         |   u2   |   u1   |
        //           While
        //           Table 5-12: Host Controller Structural Parameters 3 (HCSPARAMS3)
        //           shows the layout below
        u32 u1_device_exit_latency : 8;  //  0- 7
        u32 : 8;                         //  8-15 Rsvd
        u32 u2_device_exit_latency : 16; // 16-31
    } structural_parameters3;
    // 5.3.6 Capability Parameters 1 (HCCPARAMS1)
    struct {
        u32 addressing_capability_64 : 1;               //  0    AC64
        u32 BW_negotiation_capability : 1;              //  1    BNC
        u32 context_size : 1;                           //  2    CSZ
        u32 port_power_control : 1;                     //  3    PPC
        u32 port_indicators : 1;                        //  4    PIND
        u32 light_hc_reset_capability : 1;              //  5    LHRC
        u32 latency_tolerance_messaging_capability : 1; //  6    LTC
        u32 no_secondary_SID_support : 1;               //  7    NSS
        u32 parse_all_event_data : 1;                   //  8    PAE
        u32 stopped_short_packet_capability : 1;        //  9    SPC
        u32 stopped_edtla_capability : 1;               // 10    SEC
        u32 contiguous_frame_id_capability : 1;         // 11    CFC
        u32 maximum_primary_stream_array_size : 4;      // 12-15 MaxPSASize
        u32 xHCI_extended_capabilities_pointer : 16;    // 16-31 xECP
    } capability_parameters1;
    u32 doorbell_offset;
    u32 runtime_register_space_offset;
    struct {
        u32 u3_entry_capability : 1;                                              //  0    U3C
        u32 configure_endpoint_command_max_exit_latency_too_large_capability : 1; //  1    CMC
        u32 force_save_context_capability : 1;                                    //  2    FSC
        u32 compliance_transition_capability : 1;                                 //  3    CTC
        u32 large_ESIT_payload_capability : 1;                                    //  4    LEC
        u32 configuration_information_capability : 1;                             //  5    CIC
        u32 extended_tbc_capability : 1;                                          //  7    ETC_TSC
        u32 get_set_extendet_property_capability : 1;                             //  8    GSC
        u32 virtualization_based_trusted_io_capability : 1;                       //  9    VTC
        u32 : 22;                                                                 // 10-31 Reserved
    } capability_parameters2;
    // Optional: Offset: 0x20 VTIOOFF
};
static_assert(AssertSize<CapabilityRegisters, 0x20>());
// Table 5-9: eXtensible Host Controller Capability Registers
// FIXME: QEMU enforces 32 bit reads on this register,
//        This although does not seem to be demanded by the spec
//        So we cannot static_assert all offsets for now
// static_assert(__builtin_offsetof(CapabilityRegisters, caplength) == 0x00);
// static_assert(__builtin_offsetof(CapabilityRegisters, rsvd) == 0x01);
// static_assert(__builtin_offsetof(CapabilityRegisters, hci_version) == 0x02);
static_assert(__builtin_offsetof(CapabilityRegisters, structural_parameters1) == 0x04);
static_assert(__builtin_offsetof(CapabilityRegisters, structural_parameters2) == 0x08);
static_assert(__builtin_offsetof(CapabilityRegisters, structural_parameters3) == 0x0C);
static_assert(__builtin_offsetof(CapabilityRegisters, capability_parameters1) == 0x10);
static_assert(__builtin_offsetof(CapabilityRegisters, doorbell_offset) == 0x14);
static_assert(__builtin_offsetof(CapabilityRegisters, runtime_register_space_offset) == 0x18);
static_assert(__builtin_offsetof(CapabilityRegisters, capability_parameters2) == 0x1C);

// 5.4 Host Controller Operational Registers
// "Unless otherwise stated, all registers should be accessed as a 32-bit width on
//  reads with an appropriate software mask, if needed. A software
//  read/modify/write mechanism should be invoked for partial writes."
//  Note: This means we can safely use u32 underlying types
struct OperationalRegisters {
    union USBCommand {
        struct {
            // 5.4.1 USB Command Register (USBCMD)
            u32 run_stop : 1;                       //  0    R/S    - RW
            u32 host_controller_reset : 1;          //  1    HCRST  - RW
            u32 interrupt_enable : 1;               //  2    INTE   - RW
            u32 host_system_error_enable : 1;       //  3    HSEE   - RW
            u32 light_host_controller_reset : 1;    //  4    LHCRST - RO/RW
            u32 controller_save_state : 1;          //  8    CSS    - RW
            u32 controller_restore_state : 1;       //  9    CRS    - RW
            u32 enable_warp_event : 1;              // 10    EWE    - RW
            u32 eanble_u3_mfindex_stop : 1;         // 11    EU3S   - RW
            u32 : 1;                                // 12    RsvdP
            u32 CEM_enable : 1;                     // 13    CME    - RW
            u32 extendet_tbc_enable : 1;            // 14    ETE    - ??
            u32 extendet_tbc_trb_status_enable : 1; // 15    TSC_EN - ??
            u32 vtio_enable : 1;                    // 16    VTIOE  - RW
            u32 : 15;                               // 17-31 RsvdP
        };
        u32 raw;
    } usb_command;
    union StatusRegister {
        struct {
            // 5.4.2 USB Status Register (USBSTS)
            u32 const hc_halted : 1;             //  0    HCH   - RO
            u32 : 1;                             //  1    RsvdZ
            u32 host_system_error : 1;           //  2    HSE   - RW1C
            u32 event_interrupt : 1;             //  3    EINT  - RW1C
            u32 port_change_detect : 1;          //  4    PCD   - RW1C
            u32 : 3;                             //  5- 7 RsvdZ
            u32 const save_state_status : 1;     //  8    SSS   - RO
            u32 const restore_state_status : 1;  //  9    RSS   - RO
            u32 save_restore_error : 1;          // 10    SRE   - RW1C
            u32 const controller_not_ready : 1;  // 11    CNR   - RO
            u32 const host_controller_error : 1; // 12    HCE   - RO
            u32 : 19;                            // 13-31 RsvdZ
        } const;
        u32 raw;
    } usb_status;
    // 5.4.3 Page Size Register (PAGESIZE)
    u32 const page_size; //  0-15 Page Size - RO
                         // 16-31 Rsvd
    u32 rsvdZ_1[2];
    u32 device_notification_control; // DNCTRL
    struct {
        u32 ring_cycle_state : 1;           //  0    RCS                  - RW
        u32 command_stop : 1;               //  1    CS                   - RW1S
        u32 command_abort : 1;              //  2    CA                   - RW1S
        u32 const command_ring_running : 1; //  3    CRR                  - RO
        u32 : 2;                            //  4- 5 RsvdP
        u32 addr_lo : 26;                   //  6-53 Command Ring Pointer - RW
        u32 addr_hi;
    } command_ring_control; // CRCR
    u32 rsvdZ_2[4];
    u32 device_context_array_base_pointer[2]; // DCBAAP lo - hi
    struct {
        u32 max_device_slots_enabled : 8;         //  0- 7 MaxSlotsEn - RW
        u32 u3_entry_enable : 1;                  //  8    U3E        - RW
        u32 configuration_information_enable : 1; //  8    CIE        - RW
        u32 : 22;                                 // 10-32 RsvdP
    } configure;                                  // CONFIG
    u32 rsvdZ_3[241];
    struct PortRegister {
        enum class PortLinkState : u32 {
            U0 = 0,
            U1 = 1,
            U2 = 2,
            U3 = 3, // Device Suspended
            Disabled = 4,
            RxDetect = 5,
            Inactive = 6,
            Polling = 7,
            Recovery = 8,
            HotReset = 9,
            ComplianceMode = 10,
            TestMode = 11,
            // 12-14 Reserved
            Resume = 15,
        };
        // 5.4.8 Port Status and Control Register(PORTSC)
        union PortStatusControl {
            struct {
                u32 current_connect_status : 1;       //  0    CCS        - ROS
                u32 port_enabled_disabled : 1;        //  1    PED        - RW1CS
                u32 : 1;                              //  2    RsvdZ
                u32 over_current_active : 1;          //  3    OCA        - RO
                u32 port_reset : 1;                   //  4    PR         - RW1S
                PortLinkState port_link_state : 4;    //  5- 8 PLS        - RWS
                u32 port_power : 1;                   //  9    PP         - RWS
                u32 port_speed : 4;                   // 10-13 Port Speed - ROS
                                                      //     -> xHCISupportedProtocolCapability::PSIV
                u32 port_indicator_control : 2;       // 14-15 PIC        - RWS // FIXME: Enum
                u32 port_link_state_write_strobe : 1; // 16    LWS        - RW
                u32 connect_status_change : 1;        // 17    CSC        - RW1CS
                u32 port_enabled_disabled_change : 1; // 18    PEC        - RW1CS
                u32 warm_port_reset_change : 1;       // 19    WRC        - RW1CS/RsvdZ
                u32 over_current_change : 1;          // 20    OCC        - RW1CS
                u32 port_reset_change : 1;            // 21    PRC        - RW1CS
                u32 port_link_state_change : 1;       // 22    PLC        - RW1CS
                u32 port_config_error_change : 1;     // 23    PEC        - RW1CS/RsvdZ
                u32 cold_attach_status : 1;           // 24    CAS        - RO
                u32 wake_on_connect_enable : 1;       // 25    WCE        - RWS
                u32 wake_on_disconnect_enable : 1;    // 26    WDE        - RWS
                u32 wake_on_over_current_enable : 1;  // 26    WOE        - RWS
                u32 : 2;                              // 28-29 RsvdZ
                u32 device_removable : 1;             // 30    DR         - RO
                u32 warm_port_reset : 1;              // 31    WPR        - RW1S/RsvdZ
            };
            u32 raw;
        } status_control;
        union {
            // 5.4.9 Port PM Status and Control Register (PORTPMSC)
            struct {
                // 5.4.9.1 USB3 Protocol PORTPMSC Definition
                u32 u1_timeout : 8;           //  0- 7 U1 Timeout - RWS
                u32 u2_timeout : 8;           //  8-15 U2 Timeout - RWS
                u32 force_link_pm_accept : 1; // 16    FLA        - RW
                u32 : 15;                     // 17-31 RsvdP
            } usb3;
            struct {
                // 5.4.9.2 USB2 Protocol PORTPMSC Definition
                u32 l1_status : 3;                   //  0- 2 L1S            - RO // FIXME: enum
                u32 remote_wake_enable : 1;          //  3    RWE            - RW
                u32 best_effort_service_latency : 4; //  4- 7 BESL           - RW
                u32 l1_device_slot : 8;              //  8-15 L1 Device Slot - RW
                u32 hardware_lpm_enable : 1;         // 16    HLE            - RW
                u32 : 11;                            // 17-27 RsvdP
                u32 port_test_control : 4;           // 28-31 Test Mode      - RW // FIXME: enum
            } usb2;
        } power_management;
        struct {
            // 5.4.10.1 USB3 Protocol PORTLI Definition
            u32 link_error_count : 16;   //  0-15 Link Error Count - RW
            u32 const rx_lane_count : 4; // 16-19 RLC              - RO
            u32 const tx_lane_count : 4; // 20-23 TLC              - RO
            u32 : 8;                     // 24-31 RsvdP
            // 5.4.10.2 USB2 Protocol PORTLI Definition
            // "The USB2 Port Link Info register is reserved and shall be treated
            //  as RsvdP by software."
        } link_info;
        struct {
            // 5.4.11 Port Hardware LPM Control Register (PORTHLPMC)
            // FIXME: The spec now defines PORTEXSC?
            // -> extended status and control
            u32 : 32; // FIXME!
        } hw_lpm_control;
    } port_register_set[];
};

// Table 5-18: Host Controller Operational Registers
static_assert(__builtin_offsetof(OperationalRegisters, usb_command) == 0x00);
static_assert(__builtin_offsetof(OperationalRegisters, usb_status) == 0x04);
static_assert(__builtin_offsetof(OperationalRegisters, page_size) == 0x08);
static_assert(__builtin_offsetof(OperationalRegisters, device_notification_control) == 0x14);
static_assert(__builtin_offsetof(OperationalRegisters, command_ring_control) == 0x18);
static_assert(__builtin_offsetof(OperationalRegisters, device_context_array_base_pointer) == 0x30);
static_assert(__builtin_offsetof(OperationalRegisters, configure) == 0x38);
static_assert(__builtin_offsetof(OperationalRegisters, port_register_set) == 0x400);

// Table 5-19: Host Controller USB Port Register Set
static_assert(AssertSize<OperationalRegisters::PortRegister, 0x10>());
static_assert(__builtin_offsetof(OperationalRegisters::PortRegister, status_control) == 0x00);
static_assert(__builtin_offsetof(OperationalRegisters::PortRegister, power_management) == 0x04);
static_assert(__builtin_offsetof(OperationalRegisters::PortRegister, link_info) == 0x08);
static_assert(__builtin_offsetof(OperationalRegisters::PortRegister, hw_lpm_control) == 0x0C);

// 5.5 Host Controller Runtime Registers
// "Unless otherwise stated, all registers should be accessed with Dword references
//  on reads, with an appropriate software mask if needed. A software
//  read/modify/write mechanism should be invoked for partial writes.
//  Software should write registers containing a Qword address field using only
//
//  Qword references. If a system is incapable of issuing Qword references , then
//  writes to the Qword address fields shall be performed using 2 Dword references;
//  low Dword-first, high-Dword second."
//  Note: As we only service 64 bit systems, this means we can safely use u32 and u64 underlying types
struct RuntimeRegisters {
    // 5.5.1 Microframe Index Register (MFINDEX)
    u32 const micro_frame_index; //  0-13 Microframe Index - RO
                                 // 14-31 RsvdZ
    u32 rsvdZ[7];
    // 5.5.2 Interrupter Register Set
    // FIXME: Spec says: "Up to 1024 interrupters are supported"
    //        Figure out if and how we can limit this if we want to save some memory
    //        These also map to MSI-X vectors
    struct InterrupterRegisters {
        union {
            // 5.5.2.1 Interrupter Management Register (IMAN)
            struct {
                u32 interrupt_pending : 1; //  0    IP - RW1C
                u32 interrupt_enable : 1;  //  1    IE - RW
                u32 : 30;                  //  2-31 RsvdP
            };
            u32 raw;
        } interrupter_management;
        struct {
            // 5.5.2.2 Interrupter Moderation Register(IMOD)
            u32 interval : 16; //  0-15 IMODI - RW
            u32 counter : 16;  // 16-31 IMODC - RW
        } interrupter_moderation;
        struct {
            // 5.5.2.3 Event Ring Registers
            // 5.5.2.3.1 Event Ring Segment Table Size Register (ERSTSZ)
            u32 segment_table_size : 16; //  0-15 Event Ring Segment Table Size - RW
            u32 : 16;                    // 16-31 RsvdP
            // 5.5.2.3.2 Event Ring Segment Table Base Address Register (ERSTBA)
            // Note: Bits 0-5 are RsvdP, but this essentially enforces alignments
            u64 segment_table_address; //  6-63 Event Ring Segment Table Base Address Register - RW
            // 5.5.2.3.3 Event Ring Dequeue Pointer Register (ERDP)
            union {
                struct {
                    u64 dequeue_erst_segment_index : 3; //  0- 2 Dequeue ERST Segment Index (DESI) - RW
                    u64 event_handler_busy : 1;         //  3    Event Handler Busy (EHB)          - RW1C
                    u64 : 60;
                };
                u64 event_ring_deque_pointer; //  4-63 Event Ring Dequeue Pointer - RW
            } event_ring_deque_pointer;
        } event_ring;
    } interrupt_set[1024];
};

// Table 5-35: Host Controller Runtime Registers
static_assert(__builtin_offsetof(RuntimeRegisters, micro_frame_index) == 0x00);
static_assert(__builtin_offsetof(RuntimeRegisters, interrupt_set) == 0x20);
static_assert(__builtin_offsetof(RuntimeRegisters, interrupt_set[1023]) == 0x8000);
// Table 5-37: Interrupter Registers
static_assert(AssertSize<RuntimeRegisters::InterrupterRegisters, 32>());
static_assert(__builtin_offsetof(RuntimeRegisters::InterrupterRegisters, interrupter_management) == 0x00);
static_assert(__builtin_offsetof(RuntimeRegisters::InterrupterRegisters, interrupter_moderation) == 0x04);

// 5.6 Doorbell Registers
// All registers are 32 bits in length. Software should read and write these registers
// using only Dword accesses.
struct DoorbellRegister {
    u32 target : 8;       //  0- 7 DB Target - RW
    u32 rsvd_Z : 8 { 0 }; //  8-15 RsvdZ
    u32 stream_id : 16;   // 16-31 DB Stream ID - RW
};
static_assert(AssertSize<DoorbellRegister, 4>());

// FIXME: VTIO Registers
}
