/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace Kernel::USB::xHCI {

// 5.3 Host Controller Capability Registers
struct CapabilityRegisters {
    u32 capability_register_length : 8; // CAPLENGTH

    u32 reserved0 : 8; // Rsvd

    u32 host_controller_interface_version_number : 16; // HCIVERSION

    struct {
        // HCSPARAMS1
        u32 number_of_device_slots : 8;  // MaxSlots
        u32 number_of_interrupters : 11; // MaxIntrs
        u32 reserved0 : 5;               // Rsvd
        u32 number_of_ports : 8;         // MaxPorts
        // HCSPARAMS2
        u32 isochronous_scheduling_threshold : 4; // IST
        u32 event_ring_segment_table_max : 4;     // ERST Max
        u32 reserved1 : 13;                       // Rsvd
        u32 max_scratchpad_buffers_high : 5;      // Max Scratchpad Bufs Hi
        u32 scratchpad_restore : 1;               // SPR
        u32 max_scratchpad_buffers_low : 5;       // Max Scratchpad Bufs Lo
        // HCSPARAMS3
        u32 U1_device_exit_latency : 8;  // U1 Device Exit Latency
        u32 reserved2 : 8;               // Rsvd
        u32 U2_device_exit_latency : 16; // U2 Device Exit Latency
    } structural_parameters;

    struct {
        u32 _64bit_addressing_capability : 1;           // AC64
        u32 bandwidth_negotiation_capability : 1;       // BNC
        u32 context_size : 1;                           // CSZ
        u32 port_power_control : 1;                     // PPC
        u32 port_indicators : 1;                        // PIND
        u32 light_host_controller_reset_capability : 1; // LHRC
        u32 latency_tolerance_messaging_capability : 1; // LTC
        u32 no_secondary_stream_id_support : 1;         // NSS
        u32 parse_all_event_data : 1;                   // PAE
        u32 stopped_short_packet_capability : 1;        // SPC
        u32 stopped_EDTLA_capability : 1;               // SEC
        u32 contiguous_frame_id_capability : 1;         // CFC
        u32 maximum_primary_stream_array_size : 4;      // MaxPSASize
        u32 xHCI_extended_capabilities_pointer : 16;    // xECP
    } capability_parameters_1;                          // HCCPARAMS1

    u32 doorbell_offset; // DBOFF

    u32 runtime_register_space_offset; // RTSOFF

    struct {
        u32 U3_entry_capability : 1;                                              // U3C
        u32 configure_endpoint_command_max_exit_latency_too_large_capability : 1; // CMC
        u32 force_save_context_capability : 1;                                    // FSC
        u32 compliance_transition_capability : 1;                                 // CTC
        u32 large_ESIT_payload_capability : 1;                                    // LEC
        u32 configuration_information_capability : 1;                             // CIC
        u32 extended_TBC_capability : 1;                                          // ETC
        u32 extended_TBC_TRB_status_capability : 1;                               // ETC_TSC
        u32 get_set_extended_property_capability : 1;                             // GSC
        u32 virtualization_based_trusted_io_capability : 1;                       // VTC
        u32 reserved0 : 22;                                                       // Reserved
    } capability_parameters_2;                                                    // HCCPARAMS2
};
static_assert(AssertSize<CapabilityRegisters, 0x20>());

union PortStatusAndControl {
    struct {
        u32 current_connect_status : 1;       // CCS
        u32 port_enabled_disabled : 1;        // PED
        u32 reserved0 : 1;                    // RsvdZ
        u32 over_current_active : 1;          // OCA
        u32 port_reset : 1;                   // PR
        u32 port_link_state : 4;              // PLS
        u32 port_power : 1;                   // PP
        u32 port_speed : 4;                   // Port Speed
        u32 port_indicator_control : 2;       // PIC
        u32 port_link_state_write_strobe : 1; // LWS
        u32 connect_status_change : 1;        // CSC
        u32 port_enabled_disabled_change : 1; // PEC
        u32 warm_port_reset_change : 1;       // WRC
        u32 over_current_change : 1;          // OCC
        u32 port_reset_change : 1;            // PRC
        u32 port_link_state_change : 1;       // PLC
        u32 port_config_error_change : 1;     // CEC
        u32 cold_attach_status : 1;           // CAS
        u32 wake_on_connect_enable : 1;       // WCE
        u32 wake_on_disconnect_enable : 1;    // WDE
        u32 wake_on_over_current_enable : 1;  // WOE
        u32 reserved1 : 2;                    // RsvdZ
        u32 device_removable : 1;             // DR
        u32 warm_port_reset : 1;              // WPR
    };
    u32 raw { 0 }; // RW1CS fields
};
static_assert(AssertSize<PortStatusAndControl, 0x4>());

struct PortRegisters {
    PortStatusAndControl port_status_and_control; // PORTSC

    union {
        struct {
            u32 U1_timeout : 8;                         // U1 Timeout
            u32 U2_timeout : 8;                         // U2 Timeout
            u32 force_link_power_management_accept : 1; // FLA
            u32 reserved0 : 15;                         // RsvdP
        } usb3;
        struct {
            u32 l1_status : 3;                             // L1S
            u32 remote_wake_enable : 1;                    // RWE
            u32 best_effort_service_latency : 4;           // BESL
            u32 l1_device_slot : 8;                        // L1 Device Slot
            u32 hardware_link_power_management_enable : 1; // HLE
            u32 reserved0 : 11;                            // RsvdP
            u32 port_test_control : 4;                     // Test Mode
        } usb2;
    } port_power_management_status_and_control; // PORTPMSC

    union {
        struct {
            u32 link_error_count : 16; // Link Error Count
            u32 rx_lane_count : 4;     // RLC
            u32 tx_lane_count : 4;     // TLC
            u32 reserved0 : 8;         // RsvdP
        } usb3;
        struct {
            u32 reserved0;
        } usb2;
    } port_link_info; // PORTLI

    union {
        struct {
            u32 reserved0;
        } usb3;
        struct {
            u32 host_initiated_resume_duration_mode : 2; // HIRDM
            u32 l1_timeout : 8;                          // L1 Timeout
            u32 best_effort_service_latency_deep : 4;    // BESLD
            u32 reserved0 : 18;                          // RsvdP
        } usb2;
    } port_hardware_link_power_management_control; // PORTHLPMC
};
static_assert(AssertSize<PortRegisters, 0x10>());

union USBStatus {
    struct {
        u32 host_controller_halted : 1; // HCH
        u32 reserved0 : 1;              // RsvdZ
        u32 host_system_error : 1;      // HSE
        u32 event_interrupt : 1;        // EINT
        u32 port_change_detect : 1;     // PCD
        u32 reserved1 : 3;              // RsvdZ
        u32 save_state_status : 1;      // SSS
        u32 restore_status_status : 1;  // RSS
        u32 save_restore_error : 1;     // SRE
        u32 controller_not_ready : 1;   // CNR
        u32 host_controller_error : 1;  // HCE
        u32 reserved2 : 19;             // RsvdZ
    };
    u32 raw { 0 }; // RW1CS fields
};
static_assert(AssertSize<USBStatus, 0x4>());

union CommandRingControlRegister {
    struct {
        u32 ring_cycle_state : 1;           // RCS
        u32 command_stop : 1;               // CS
        u32 command_abort : 1;              // CA
        u32 command_ring_running : 1;       // CRR
        u32 reserved0 : 2;                  // RsvdP
        u32 command_ring_pointer_low : 26;  // Command Ring Pointer Lo
        u32 command_ring_pointer_high : 32; // Command Ring Pointer Hi
    };
    struct {
        u32 raw0 { 0 };
        u32 raw1 { 0 };
    }; // RW1CS fields
};
static_assert(AssertSize<CommandRingControlRegister, 0x8>());

// 5.4 Host Controller Operational Registers
struct OperationalRegisters {
    struct {
        u32 run_stop : 1;                             // R/S
        u32 host_controller_reset : 1;                // HCRST
        u32 interrupter_enable : 1;                   // INTE
        u32 host_system_error_enable : 1;             // HSEE
        u32 reserved0 : 3;                            // RsvdP
        u32 light_host_controller_reset : 1;          // LHCRST
        u32 controller_save_state : 1;                // CSS
        u32 controller_restore_state : 1;             // CRS
        u32 enable_wrap_event : 1;                    // EWE
        u32 enable_U3_microframe_index_stop : 1;      // EU3S
        u32 reserved1 : 1;                            // RsvdP
        u32 CEM_enable : 1;                           // CME
        u32 extended_transfer_burst_count_enable : 1; // ETE
        u32 extended_TBC_TRB_status_enable : 1;       // TSB_EN
        u32 VTIO_enable : 1;                          // VTIOE
        u32 reserved2 : 15;                           // RsvdP
    } usb_command;                                    // USBCMD

    USBStatus usb_status; // USBSTS

    u32 page_size; // PAGESIZE

    u32 reserved0[2]; // RsvdZ

    struct {
        u32 notification_enable_0 : 1;  // N0
        u32 notification_enable_1 : 1;  // N1
        u32 notification_enable_2 : 1;  // N2
        u32 notification_enable_3 : 1;  // N3
        u32 notification_enable_4 : 1;  // N4
        u32 notification_enable_5 : 1;  // N5
        u32 notification_enable_6 : 1;  // N6
        u32 notification_enable_7 : 1;  // N7
        u32 notification_enable_8 : 1;  // N8
        u32 notification_enable_9 : 1;  // N9
        u32 notification_enable_10 : 1; // N10
        u32 notification_enable_11 : 1; // N11
        u32 notification_enable_12 : 1; // N12
        u32 notification_enable_13 : 1; // N13
        u32 notification_enable_14 : 1; // N14
        u32 notification_enable_15 : 1; // N15
        u32 reserved0 : 16;             // RsvdP
    } device_notification_control;      // DNCTRL

    CommandRingControlRegister command_ring_control; // CRCR

    u32 reserved1[4]; // RsvdZ

    struct {
        u32 low;
        u32 high;
    } device_context_base_address_array_pointer; // DCBAAP

    struct {
        u32 max_device_slots_enabled : 8;         // MaxSlotsEn
        u32 U3_entry_enable : 1;                  // U3E
        u32 configuration_information_enable : 1; // CIE
        u32 reserved0 : 22;                       // RsvdP
    } configure;                                  // CONFIG

    u32 reserved2[241]; // RsvdZ

    PortRegisters port_registers[0x100];
};
static_assert(AssertSize<OperationalRegisters, 0x1400>());

struct InterrupterRegisters {
    struct {
        u32 interrupt_pending : 1; // IP
        u32 interrupt_enabled : 1; // IE
        u32 reserved0 : 30;        // RsvdP
    } interrupter_management;      // IMAN

    struct {
        u32 interrupt_moderation_interval : 16; // IMODI
        u32 interrupt_moderation_counter : 16;  // IMODC
    } interrupter_moderation;                   // IMOD

    u32 even_ring_segment_table_size; // ERSTSZ

    u32 reserved0; // RsvdP

    struct {
        u32 low;
        u32 high;
    } event_ring_segment_table_base_address; // ERSTBA

    struct {
        u32 dequeue_ERST_segment_index : 3;       // DESI
        u32 event_handler_busy : 1;               // EHB
        u32 event_ring_dequeue_pointer_low : 28;  // ERDP Lo
        u32 event_ring_dequeue_pointer_high : 32; // ERDP Hi
    } event_ring_dequeue_pointer;                 // ERDP
};
static_assert(AssertSize<InterrupterRegisters, 0x20>());

// 5.5 Host Controller Runtime Registers
struct RuntimeRegisters {
    u32 microframe_index; // MFINDEX

    u32 reserved0[7]; // RsvdZ

    InterrupterRegisters interrupter_registers[0x400];
};
static_assert(AssertSize<RuntimeRegisters, 0x8020>());

// 5.6 Doorbell Registers
union DoorbellRegister {
    struct {
        u32 doorbell_target : 8;     // DB Target
        u32 reserved0 : 8;           // RsvdZ
        u32 doorbell_stream_id : 16; // DB Stream ID
    };
    u32 raw; // All fields must be modified at once
};
static_assert(AssertSize<DoorbellRegister, 0x4>());

struct DoorbellRegisters {
    u32 doorbells[256];
};
static_assert(AssertSize<DoorbellRegisters, 0x400>());

struct ExtendedCapability {
    enum class CapabilityID : u32 {
        USB_Legacy_Support = 1,
        Supported_Protocols = 2,
        Extended_Power_Management = 3,
        IO_Virtualization = 4,
        Message_Interrupt = 5,
        USB_Debug_Capability = 10,
        Extended_Message_Interrupt = 17,
    };
    CapabilityID capability_id : 8;
    u32 next_xHCI_extended_capability_pointer : 8;
    u32 capability_specific : 16;
};
static_assert(AssertSize<ExtendedCapability, 0x4>());

struct USBLegacySupportExtendedCapability {
    struct {
        ExtendedCapability::CapabilityID capability_id : 8;
        u32 next_xHCI_extended_capability_pointer : 8;
        u32 host_controller_bios_owned_semaphore : 1;
        u32 reserved0 : 7;
        u32 host_controller_os_owned_semaphore : 1;
        u32 reserved1 : 7;
    } usb_legacy_support_capability; // USBLEGSUP

    struct {
        u32 usb_smi_enable : 1;
        u32 reserved0 : 3;
        u32 smi_on_host_system_error_enable : 1;
        u32 reserved1 : 8;
        u32 smi_on_os_ownership_enable : 1;
        u32 smi_on_pci_command_enable : 1;
        u32 smi_on_bar_enable : 1;
        u32 smi_on_event_interrupt : 1;
        u32 reserved2 : 3;
        u32 smi_on_host_system_error : 1;
        u32 reserved3 : 8;
        u32 smi_on_os_ownership_change : 1;
        u32 smi_on_pci_command : 1;
        u32 smi_on_bar : 1;
    } usb_legacy_support_control_status; // USBLEGCTLSTS
};
static_assert(AssertSize<USBLegacySupportExtendedCapability, 0x8>());

struct ProtocolSpeedID {
    u32 protocol_speed_id_value : 4;       // PSIV
    u32 protocol_speed_id_exponent : 2;    // PSIE
    u32 protocol_speed_id_type : 2;        // PLT
    u32 protocol_speed_id_full_duplex : 1; // PFD
    u32 reserved0 : 5;                     // RsvdP
    u32 link_protocol : 2;                 // LP
    u32 protocol_speed_id_mantissa : 16;   // PSIM
};
static_assert(AssertSize<ProtocolSpeedID, 0x4>());

struct SupportedProtocolExtendedCapability {
    static constexpr u32 usb_name_string = AK::convert_between_host_and_little_endian(0x20425355);
    ExtendedCapability::CapabilityID capability_id : 8;
    u32 next_xHCI_extended_capability_pointer : 8;
    u32 minor_revision : 8;
    u32 major_revision : 8;
    u32 name_string : 32;
    u32 compatible_port_offset : 8;
    u32 compatible_port_count : 8;
    u32 protocol_defined : 12;
    u32 protocol_speed_id_count : 4; // PSIC
    u32 protocol_slot_type : 5;
    u32 reserved0 : 27;
    ProtocolSpeedID protocol_speed_ids[16];
};
static_assert(AssertSize<SupportedProtocolExtendedCapability, 0x50>());

}
