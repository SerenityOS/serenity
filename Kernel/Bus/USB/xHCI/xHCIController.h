/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/xHCI/xHCIRootHub.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::USB {

class xHCIPCIInterrupter;
class xHCIDeviceTreeInterrupter;

class xHCIController
    : public USBController {
    friend class xHCIPCIInterrupter;
    friend class xHCIDeviceTreeInterrupter;

public:
    virtual ~xHCIController() override;

    virtual ErrorOr<void> initialize() override;
    virtual ErrorOr<void> reset() override;
    virtual ErrorOr<void> stop() override;
    virtual ErrorOr<void> start() override;

    virtual void cancel_async_transfer(NonnullLockRefPtr<Transfer> transfer) override;
    virtual ErrorOr<size_t> submit_control_transfer(Transfer& transfer) override;
    virtual ErrorOr<size_t> submit_bulk_transfer(Transfer& transfer) override;
    virtual ErrorOr<void> submit_async_interrupt_transfer(NonnullLockRefPtr<Transfer> transfer, u16 ms_interval) override;

    virtual ErrorOr<void> reset_pipe(USB::Device&, USB::Pipe&) override;

    virtual ErrorOr<void> initialize_device(USB::Device&) override;

    ErrorOr<void> initialize_endpoint_if_needed(Pipe const&);

    u8 ports() const { return m_ports; }

    ErrorOr<HubStatus> get_port_status(Badge<xHCIRootHub>, u8 port);
    ErrorOr<void> set_port_feature(Badge<xHCIRootHub>, u8 port, HubFeatureSelector);
    ErrorOr<void> clear_port_feature(Badge<xHCIRootHub>, u8 port, HubFeatureSelector);

protected:
    xHCIController(Memory::TypedMapping<u8> registers_mapping);

    virtual bool using_message_signalled_interrupts() const = 0;
    virtual ErrorOr<NonnullOwnPtr<GenericInterruptHandler>> create_interrupter(u16 interrupter_id) = 0;
    virtual ErrorOr<void> write_dmesgln_prefix(StringBuilder&) const = 0;

private:
    void handle_interrupt(u16 interrupter_id);

    void take_exclusive_control_from_bios();
    ErrorOr<void> find_port_max_speeds();

    void event_handling_thread();
    void hot_plug_thread();

    // Arbitrarily chosen to decrease allocation sizes, can be increased up to 256 if we reach this limit
    static constexpr size_t max_devices = 64;

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

    struct EventRingSegmentTableEntry {
        u32 ring_segment_base_address_low : 32;
        u32 ring_segment_base_address_high : 32;
        u32 ring_segment_size : 16;
        u32 reserved0 : 16;
        u32 reserved1 : 32;
    };
    static_assert(AssertSize<EventRingSegmentTableEntry, 0x10>());

    union TransferRequestBlock {
        enum class TRBType : u32 {
            Normal = 1,
            Setup_Stage = 2,
            Data_Stage = 3,
            Status_Stage = 4,
            Isoch = 5,
            Link = 6,
            Event_Data = 7,
            No_Op = 8,
            Enable_Slot_Command = 9,
            Disable_Slot_Command = 10,
            Address_Device_Command = 11,
            Configure_Endpoint_Command = 12,
            Evaluate_Context_Command = 13,
            Reset_Endpoint_Command = 14,
            Stop_Endpoint_Command = 15,
            Set_TR_Dequeue_Pointer_Command = 16,
            Reset_Device_Command = 17,
            Force_Event_Command = 18,
            Negotiate_Bandwidth_Command = 19,
            Set_Latency_Tolerance_Value_Command = 20,
            Get_Port_Bandwidth_Command = 21,
            Force_Header_Command = 22,
            No_Op_Command = 23,
            Get_Extended_Property_Command = 24,
            Set_Extended_Property_Command = 25,
            Transfer_Event = 32,
            Command_Completion_Event = 33,
            Port_Status_Change_Event = 34,
            Bandwidth_Request_Event = 35,
            Doorbell_Event = 36,
            Host_Controller_Event = 37,
            Device_Notification_Event = 38,
            Microframe_Index_Wrap_Event = 39,
        };
        enum class CompletionCode : u32 {
            Invalid = 0,
            Success = 1,
            Data_Buffer_Error = 2,
            Babble_Detected_Error = 3,
            USB_Transaction_Error = 4,
            TRB_Error = 5,
            Stall_Error = 6,
            Resource_Error = 7,
            Bandwidth_Error = 8,
            No_Slots_Available_Error = 9,
            Invalid_Stream_Type_Error = 10,
            Slot_Not_Enabled_Error = 11,
            Endpoint_Not_Enabled_Error = 12,
            Short_Packet = 13,
            Ring_Underrun = 14,
            Ring_Overrun = 15,
            VF_Event_Ring_Full_Error = 16,
            Parameter_Error = 17,
            Bandwidth_Overrun_Error = 18,
            Context_State_Error = 19,
            No_Ping_Response_Error = 20,
            Event_Ring_Full_Error = 21,
            Incompatible_Device_Error = 22,
            Missed_Service_Error = 23,
            Command_Ring_Stopped = 24,
            Command_Aborted = 25,
            Stopped = 26,
            Stopped_Length_Invalid = 27,
            Stopped_Short_Packet = 28,
            Max_Exit_Latency_Too_Large_Error = 29,
            Isoch_Buffer_Overrun = 31,
            Event_Lost_Error = 32,
            Undefined_Error = 33,
            Invalid_Stream_ID_Error = 34,
            Secondary_Bandwidth_Error = 35,
            Split_Transaction_Error = 36,
        };
        enum class TransferType : u32 {
            No_Data_Stage = 0,
            Reserved = 1,
            OUT_Data_Stage = 2,
            IN_Data_Stage = 3,
        };
        struct {
            u32 parameter0 : 32;
            u32 parameter1 : 32;
            u32 status : 32;
            u32 cycle_bit : 1;                            // C
            u32 evaluate_next_transfer_request_block : 1; // ENT
            u32 reserved0 : 2;
            u32 chain_bit : 1; // CH
            u32 reserved1 : 5;
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 control : 16;
        } generic;
        struct {
            u32 data_buffer_pointer_low : 32;
            u32 data_buffer_pointer_high : 32;
            u32 transfer_request_block_transfer_length : 17;
            u32 transfer_descriptor_size : 5;
            u32 interrupter_target : 10;
            u32 cycle_bit : 1;                            // C
            u32 evaluate_next_transfer_request_block : 1; // ENT
            u32 interrupt_on_short_packet : 1;            // ISP
            u32 no_snoop : 1;                             // NS
            u32 chain_bit : 1;                            // CH
            u32 interrupt_on_completion : 1;              // IOC
            u32 immediate_data : 1;                       // IDT
            u32 reserved0 : 2;                            // RsvdZ
            u32 block_event_interrupt : 1;                // BEI
            TRBType transfer_request_block_type : 6;      // TRB Type
            u32 reserved1 : 16;
        } normal;
        struct {
            u32 request_type : 8;                            // bmRequestType
            u32 request : 8;                                 // bRequest
            u32 value : 16;                                  // wValue
            u32 index : 16;                                  // wIndex
            u32 length : 16;                                 // wLength
            u32 transfer_request_block_transfer_length : 17; // TRB Transfer Length
            u32 reserved0 : 5;                               // RsvdZ
            u32 interrupter_target : 10;                     // Interrupter Target
            u32 cycle_bit : 1;                               // C
            u32 reserved1 : 4;                               // RsvdZ
            u32 interrupt_on_completion : 1;                 // IOC
            u32 immediate_data : 1;                          // IDT
            u32 reserved2 : 3;                               // RsvdZ
            TRBType transfer_request_block_type : 6;         // TRB Type
            TransferType transfer_type : 2;                  // TRT
            u32 reserved3 : 14;                              // RsvdZ
        } setup_stage;
        struct {
            u32 data_buffer_low : 32;
            u32 data_buffer_high : 32;
            u32 transfer_request_block_transfer_length : 17; // TRB Transfer Length
            u32 transfer_descriptor_size : 5;
            u32 interrupter_target : 10;
            u32 cycle_bit : 1;                            // C
            u32 evaluate_next_transfer_request_block : 1; // ENT
            u32 interrupt_on_short_packet : 1;            // ISP
            u32 no_snoop : 1;                             // NS
            u32 chain_bit : 1;                            // CH
            u32 interrupt_on_completion : 1;              // IOC
            u32 immediate_data : 1;                       // IDT
            u32 reserved0 : 3;                            // RsvdZ
            TRBType transfer_request_block_type : 6;      // TRB Type
            u32 direction : 1;                            // DIR
            u32 reserved1 : 15;                           // RsvdZ
        } data_stage;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 22;
            u32 interrupter_target : 10;
            u32 cycle_bit : 1;                            // C
            u32 evaluate_next_transfer_request_block : 1; // ENT
            u32 reserved3 : 2;                            // RsvdZ
            u32 chain_bit : 1;                            // CH
            u32 interrupt_on_completion : 1;              // IOC
            u32 reserved4 : 4;
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 direction : 1;                       // DIR
            u32 reserved5 : 15;                      // RsvdZ
        } status_stage;
        struct {
            u32 data_buffer_pointer_low : 32;
            u32 data_buffer_pointer_high : 32;
            u32 transfer_request_block_transfer_length : 17;
            u32 transfer_descriptor_size_OR_transfer_burst_count : 5; // TD Size/TBC
            u32 interrupter_target : 10;
            u32 cycle_bit : 1;                                                          // C
            u32 evaluate_next_transfer_request_block : 1;                               // ENT
            u32 interrupt_on_short_packet : 1;                                          // ISP
            u32 no_snoop : 1;                                                           // NS
            u32 chain_bit : 1;                                                          // CH
            u32 interrupt_on_completion : 1;                                            // IOC
            u32 immediate_data : 1;                                                     // IDT
            u32 transfer_burst_count_OR_transfer_request_block_status_OR_reserved0 : 2; // TBC/TRBSts/RsvdZ
            u32 block_event_interrupt : 1;                                              // BEI
            TRBType transfer_request_block_type : 6;                                    // TRB Type
            u32 transfer_last_burst_packet_count : 4;                                   // TLBPC
            u32 frame_id : 11;
            u32 start_isoch_as_soon_as_possible : 1; // SIA
        } isoch;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 22;
            u32 interrupter_target : 10;
            u32 cycle_bit : 1;                            // C
            u32 evaluate_next_transfer_request_block : 1; // ENT
            u32 reserved3 : 2;                            // RsvdZ
            u32 chain_bit : 1;                            // CH
            u32 interrupt_on_completion : 1;              // IOC
            u32 reserved4 : 4;                            // RsvdZ
            TRBType transfer_request_block_type : 6;      // TRB Type
            u32 reserved5 : 16;                           // RsvdZ
        } no_op;
        struct {
            u32 transfer_request_block_pointer_low : 32;
            u32 transfer_request_block_pointer_high : 32;
            u32 transfer_request_block_transfer_length : 24;
            CompletionCode completion_code : 8;
            u32 cycle_bit : 1;  // C
            u32 reserved0 : 1;  // RsvdZ
            u32 event_data : 1; // ED
            u32 reserved1 : 7;
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 endpoint_id : 5;
            u32 reserved2 : 3;
            u32 slot_id : 8;
        } transfer_event;
        struct {
            u32 command_transfer_request_block_pointer_low : 32;
            u32 command_transfer_request_block_pointer_high : 32;
            u32 command_completion_parameter : 24;
            CompletionCode completion_code : 8;
            u32 cycle_bit : 1;                       // C
            u32 reserved0 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 vf_id : 8;
            u32 slot_id : 8;
        } command_completion_event;
        struct {
            u32 reserved0 : 24;
            u32 port_id : 8;
            u32 reserved1 : 32;
            u32 reserved2 : 24;
            CompletionCode completion_code : 8;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved4 : 16;
        } port_status_change_event;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 24;
            CompletionCode completion_code : 8;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved4 : 8;
            u32 slot_id : 8;
        } bandwidth_request_event;
        struct {
            u32 doorbell_reason : 5;
            u32 reserved0 : 27;
            u32 reserved1 : 32;
            u32 reserved2 : 24;
            CompletionCode completion_code : 8;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 vf_id : 8;
            u32 slot_id : 8;
        } doorbell_event;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 24;
            CompletionCode completion_code : 8;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved4 : 16;
        } host_controller_event;
        struct {
            u32 reserved0 : 4;
            u32 notification_type : 4;
            u32 device_notification_data_low : 24;
            u32 device_notification_data_high : 32;
            u32 reserved1 : 24;
            CompletionCode completion_code : 8;
            u32 cycle_bit : 1;                       // C
            u32 reserved2 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved3 : 8;
            u32 slot_id : 8;
        } device_notification_event;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 24;
            CompletionCode completion_code : 8;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved4 : 16;
        } microframe_index_wrap_event;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved4 : 16;
        } no_op_command;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 slot_type : 5;
            u32 reserved4 : 11;
        } enable_slot_command;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved4 : 8;
            u32 slot_id : 8;
        } disable_slot_command;
        struct {
            u32 input_context_pointer_low : 32;
            u32 input_context_pointer_high : 32;
            u32 reserved0 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved1 : 8;                       // RsvdZ
            u32 block_set_address_request : 1;       // BSR
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved2 : 8;
            u32 slot_id : 8;
        } address_device_command;
        struct {
            u32 input_context_pointer_low : 32;
            u32 input_context_pointer_high : 32;
            u32 reserved0 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved1 : 8;                       // RsvdZ
            u32 deconfigure : 1;                     // DC
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved2 : 8;
            u32 slot_id : 8;
        } configure_endpoint_command;
        struct {
            u32 input_context_pointer_low : 32;
            u32 input_context_pointer_high : 32;
            u32 reserved0 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved1 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved2 : 8;
            u32 slot_id : 8;
        } evaluate_context_command;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 8;                       // RsvdZ
            u32 transfer_state_preserve : 1;         // TSP
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 endpoint_id : 5;
            u32 reserved4 : 3;
            u32 slot_id : 8;
        } reset_endpoint_command;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 endpoint_id : 5;
            u32 reserved4 : 2;
            u32 suspend : 1; // SP
            u32 slot_id : 8;
        } stop_endpoint_command;
        struct {
            u32 dequeue_cycle_state : 1; // DCS
            u32 stream_context_type : 3; // SCT
            u32 new_tr_dequeue_pointer_low : 28;
            u32 new_tr_dequeue_pointer_high : 32;
            u32 reserved0 : 16;
            u32 stream_id : 16;
            u32 cycle_bit : 1;                       // C
            u32 reserved1 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 endpoint_id : 5;
            u32 reserved2 : 3;
            u32 slot_id : 8;
        } set_tr_dequeue_pointer_command;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved4 : 8;
            u32 slot_id : 8;
        } reset_device_command;
        struct {
            u32 event_transfer_request_block_pointer_low : 32;
            u32 event_transfer_request_block_pointer_high : 32;
            u32 reserved0 : 22;
            u32 vf_interrupter_target : 10;
            u32 cycle_bit : 1;                       // C
            u32 reserved1 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 vf_id : 8;
            u32 reserved4 : 8;
        } force_event_command;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved4 : 8;
            u32 slot_id : 8;
        } negotiate_bandwidth_command;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 reserved2 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 best_effort_latency_tolerance_value : 12;
            u32 reserved4 : 4;
        } set_latency_tolerance_value_command;
        struct {
            u32 port_bandwidth_context_pointer_low : 32;
            u32 port_bandwidth_context_pointer_high : 32;
            u32 reserved0 : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved1 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 device_speed : 4;
            u32 reserved2 : 4;
            u32 hub_slot_id : 8;
        } get_port_bandwidth_command;
        struct {
            u32 packet_type : 5;
            u32 header_info_low : 27;
            u32 header_info_middle : 32;
            u32 header_info_high : 32;
            u32 cycle_bit : 1;                       // C
            u32 reserved0 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved1 : 8;
            u32 root_hub_port_number : 8;
        } force_header_command;
        struct {
            u32 extended_property_context_pointer_low : 32;
            u32 extended_property_context_pointer_high : 32;
            u32 extended_capability_identifier : 16; // ECI
            u32 reserved0 : 16;
            u32 cycle_bit : 1;                       // C
            u32 reserved1 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 command_sub_type : 3;
            u32 endpoint_id : 5;
            u32 slot_id : 8;
        } get_extended_property_command;
        struct {
            u32 reserved0 : 32;
            u32 reserved1 : 32;
            u32 extended_capability_identifier : 16; // ECI
            u32 capability_parameter : 8;
            u32 reserved2 : 8;
            u32 cycle_bit : 1;                       // C
            u32 reserved3 : 9;                       // RsvdZ
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 command_sub_type : 3;
            u32 endpoint_id : 5;
            u32 slot_id : 8;
        } set_extended_property_command;
        struct {
            u32 ring_segment_pointer_low : 32;
            u32 ring_segment_pointer_high : 32;
            u32 reserved0 : 22;
            u32 interrupter_target : 10;
            u32 cycle_bit : 1;               // C
            u32 toggle_cycle : 1;            // TC
            u32 reserved1 : 2;               // RsvdZ
            u32 chain_bit : 1;               // CH
            u32 interrupt_on_completion : 1; // IOC
            u32 reserved2 : 4;
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved3 : 16;
        } link;
        struct {
            u32 event_data_low : 32;
            u32 event_data_high : 32;
            u32 reserved0 : 22;
            u32 interrupter_target : 10;
            u32 cycle_bit : 1;                            // C
            u32 evaluate_next_transfer_request_block : 1; // ENT
            u32 reserved1 : 2;
            u32 chain_bit : 1;                       // CH
            u32 interrupt_on_completion : 1;         // IOC
            u32 reserved2 : 3;                       // RsvdZ
            u32 block_event_interrupt : 1;           // BEI
            TRBType transfer_request_block_type : 6; // TRB Type
            u32 reserved3 : 16;
        } event_data;
    };
    static_assert(AssertSize<TransferRequestBlock, 0x10>());

    static StringView enum_to_string(TransferRequestBlock::TRBType);
    static StringView enum_to_string(TransferRequestBlock::CompletionCode);

    static constexpr size_t command_ring_size = 16;
    // Use up all the space left in the page
    static constexpr size_t event_ring_segment_size = ((PAGE_SIZE - (sizeof(EventRingSegmentTableEntry) + sizeof(TransferRequestBlock) * command_ring_size)) & ~0x3F) / sizeof(TransferRequestBlock);
    // System software is responsible for ensuring the Size of every ERST entry (Event Ring segment) is at least 16.
    static_assert(event_ring_segment_size >= 16);
    // System software shall allocate a buffer for the Event Ring Segment Table that rounds up its size to the nearest 64B boundary to allow full cache-line accesses.
    static_assert((event_ring_segment_size * sizeof(TransferRequestBlock)) % 64 == 0);
    // The command ring and event ring (ERST and the segment itself) are combined to not take 3 pages for something that fits in 1)
    struct CommandAndEventRings {
        TransferRequestBlock command_ring[command_ring_size];
        TransferRequestBlock event_ring_segment[event_ring_segment_size];
        // Software shall allocate a buffer for the Event Ring Segment Table that rounds up its size to the nearest 64B boundary to allow full cache-line accesses.
        [[gnu::aligned(64)]] EventRingSegmentTableEntry event_ring_segment_table_entry;
    };
    static_assert(sizeof(CommandAndEventRings) <= 0x1000);
    // System software shall allocate a buffer for the Event Ring Segment that rounds up its size to the nearest 64B boundary to allow full cache-line accesses
    static_assert(__builtin_offsetof(CommandAndEventRings, command_ring) % 64 == 0);
    static_assert(__builtin_offsetof(CommandAndEventRings, event_ring_segment) % 64 == 0);

    struct InputControlContext {
        u32 reserved0 : 2;
        u32 drop_contexts : 30;
        u32 add_contexts : 32;
        u32 reserved1 : 32;
        u32 reserved2 : 32;
        u32 reserved3 : 32;
        u32 reserved4 : 32;
        u32 reserved5 : 32;
        u32 configuration_value : 8;
        u32 interface_number : 8;
        u32 alternate_setting : 8;
        u32 reserved6 : 8;
    };
    static_assert(AssertSize<InputControlContext, 0x20>());
    struct SlotContext {
        u32 route_string : 20;
        u32 speed : 4;
        u32 reserved0 : 1;
        u32 multi_transaction_translator : 1; // MTT
        u32 hub : 1;
        u32 context_entries : 5;
        u32 max_exit_latency : 16;
        u32 root_hub_port_number : 8;
        u32 number_of_ports : 8;
        u32 parent_hub_slot_id : 8;
        u32 parent_port_number : 8;
        u32 transaction_translator_think_time : 2; // TTT
        u32 reserved1 : 4;
        u32 interrupter_target : 10;
        u32 usb_device_address : 8;
        u32 reserved2 : 19;
        u32 slot_state : 5;
        u32 reserved3 : 32;
        u32 reserved4 : 32;
        u32 reserved5 : 32;
        u32 reserved6 : 32;
    };
    static_assert(AssertSize<SlotContext, 0x20>());
    struct EndpointContext {
        enum class EndpointType : u32 {
            Not_Valid = 0,
            Isoch_Out = 1,
            Bulk_Out = 2,
            Interrupt_Out = 3,
            Control_Bidirectional = 4,
            Isoch_In = 5,
            Bulk_In = 6,
            Interrupt_In = 7
        };
        u32 endpoint_state : 3;
        u32 reserved0 : 5;
        u32 mult : 2;
        u32 max_primary_streams : 5; // MaxPStreams
        u32 linear_stream_array : 1; // LSA
        u32 interval : 8;
        u32 max_endpoint_service_time_interval_payload_high : 8; // Max ESIT Payload Hi
        u32 reserved1 : 1;
        u32 error_count : 2; // CErr
        EndpointType endpoint_type : 3;
        u32 reserved2 : 1;
        u32 host_initiate_disable : 1; // HID
        u32 max_burst_size : 8;
        u32 max_packet_size : 16;
        u32 dequeue_cycle_state : 1;
        u32 reserved3 : 3;
        u32 transfer_ring_dequeue_pointer_low : 28;
        u32 transfer_ring_dequeue_pointer_high : 32;
        u32 average_transfer_request_block : 16;
        u32 max_endpoint_service_time_interval_payload_low : 16; // Max ESIT Payload Lo
        u32 reserved4 : 32;
        u32 reserved5 : 32;
        u32 reserved6 : 32;
    };
    static_assert(AssertSize<EndpointContext, 0x20>());

    struct PendingTransfer {
        IntrusiveListNode<PendingTransfer> endpoint_list_node;
        u32 start_index { 0 };
        u32 end_index { 0 };
    };
    struct SyncPendingTransfer : public PendingTransfer {
        WaitQueue wait_queue;
        TransferRequestBlock::CompletionCode completion_code { TransferRequestBlock::CompletionCode::Invalid };
        u32 remainder { 0 };
    };
    struct PeriodicPendingTransfer : public PendingTransfer {
        Vector<TransferRequestBlock> transfer_request_blocks;
        NonnullLockRefPtr<Transfer> original_transfer;
    };

    struct EndpointRing {
        OwnPtr<Memory::Region> region;
        u32 enqueue_index { 0 };
        u32 free_transfer_request_blocks { endpoint_ring_size - 1 }; // One less, since we use up the last one for the link TRB
        u32 max_burst_payload { 0 };
        Pipe::Type type { Pipe::Type::Control };
        u8 producer_cycle_state { 1 };
        IntrusiveList<&PendingTransfer::endpoint_list_node> pending_transfers;
        TransferRequestBlock* ring_vaddr() const { return reinterpret_cast<TransferRequestBlock*>(region->vaddr().as_ptr()); }
        PhysicalPtr ring_paddr() const { return region->physical_page(0)->paddr().get(); }
    };
    static constexpr size_t max_endpoints = 31;
    static constexpr size_t endpoint_ring_size = PAGE_SIZE / sizeof(TransferRequestBlock);
    struct SlotState {
        RecursiveSpinlock<LockRank::None> lock;
        OwnPtr<Memory::Region> input_context_region;
        OwnPtr<Memory::Region> device_context_region;
        Array<EndpointRing, max_endpoints> endpoint_rings;
    };

    static u8 endpoint_index(u8 endpoint, Pipe::Direction direction)
    {
        if (direction == Pipe::Direction::Bidirectional) {
            VERIFY(endpoint == 0);
            direction = Pipe::Direction::In;
        }
        return endpoint * 2 + to_underlying(direction);
    }

    size_t context_entry_size() const { return m_large_contexts ? 64 : 32; }
    size_t input_context_size() const { return context_entry_size() * 33; }
    u8* input_context(u8 slot, u8 index) const
    {
        auto* base = m_slots_state[slot - 1].input_context_region->vaddr().as_ptr();
        return base + (context_entry_size() * index);
    }
    InputControlContext* input_control_context(u8 slot) const
    {
        return reinterpret_cast<InputControlContext*>(input_context(slot, 0));
    }
    SlotContext* input_slot_context(u8 slot) const
    {
        return reinterpret_cast<SlotContext*>(input_context(slot, 1));
    }
    EndpointContext* input_endpoint_context(u8 slot, u8 endpoint, Pipe::Direction direction) const
    {
        return reinterpret_cast<EndpointContext*>(input_context(slot, endpoint_index(endpoint, direction) + 1));
    }
    size_t device_context_size() const { return context_entry_size() * 32; }
    u8* device_context(u8 slot, u8 index) const
    {
        auto* base = m_slots_state[slot - 1].device_context_region->vaddr().as_ptr();
        return base + (context_entry_size() * index);
    }
    SlotContext* device_slot_context(u8 slot) const
    {
        return reinterpret_cast<SlotContext*>(device_context(slot, 0));
    }
    EndpointContext* device_endpoint_context(u8 slot, u8 endpoint, Pipe::Direction direction) const
    {
        return reinterpret_cast<EndpointContext*>(device_context(slot, endpoint_index(endpoint, direction)));
    }

    void ring_doorbell(u8 doorbell, u8 doorbell_target);
    void ring_endpoint_doorbell(u8 slot, u8 endpoint, Pipe::Direction direction)
    {
        VERIFY(slot > 0);
        ring_doorbell(slot, endpoint_index(endpoint, direction));
    }
    void ring_command_doorbell() { ring_doorbell(0, 0); }
    void enqueue_command(TransferRequestBlock&);
    void execute_command(TransferRequestBlock&);

    ErrorOr<u8> enable_slot();
    ErrorOr<void> address_device(u8 slot, u64 input_context_address);
    ErrorOr<void> evaluate_context(u8 slot, u64 input_context_address);
    ErrorOr<void> configure_endpoint(u8 slot, u64 input_context_address);

    enum class TransferStatePreserve {
        No,
        Yes,
    };
    ErrorOr<void> reset_endpoint(u8 slot, u8 endpoint, TransferStatePreserve);

    ErrorOr<void> set_tr_dequeue_pointer(u8 slot, u8 endpoint, u8 stream_context_type, u16 stream, u64 new_tr_dequeue_pointer, u8 dequeue_cycle_state);

    ErrorOr<void> enqueue_transfer(u8 slot, u8 endpoint, Pipe::Direction direction, Span<TransferRequestBlock>, PendingTransfer&);
    void handle_transfer_event(TransferRequestBlock const&);

    ErrorOr<Vector<TransferRequestBlock>> prepare_normal_transfer(Transfer& transfer);

    Memory::TypedMapping<u8> m_registers_mapping;
    CapabilityRegisters const volatile& m_capability_registers;
    OperationalRegisters volatile& m_operational_registers;
    RuntimeRegisters volatile& m_runtime_registers;
    DoorbellRegisters volatile& m_doorbell_registers;

    RefPtr<Process> m_process;
    WaitQueue m_event_queue;

    bool m_using_message_signalled_interrupts { false };
    bool m_large_contexts { false };
    u8 m_device_slots { 0 };
    Array<SlotState, max_devices> m_slots_state;
    u8 m_ports { 0 };
    Array<USB::Device::DeviceSpeed, 255> m_port_max_speeds {};

    Vector<NonnullOwnPtr<PeriodicPendingTransfer>> m_active_periodic_transfers;

    Spinlock<LockRank::None> m_command_lock;
    WaitQueue m_command_completion_queue;
    TransferRequestBlock m_command_result_transfer_request_block {};
    u32 m_command_ring_enqueue_index { 0 };
    u8 m_command_ring_producer_cycle_state { 1 };
    u32 m_event_ring_dequeue_index { 0 };
    u8 m_event_ring_consumer_cycle_state { 1 };

    OwnPtr<Memory::Region> m_device_context_base_address_array_region;
    u64* m_device_context_base_address_array { nullptr };
    OwnPtr<Memory::Region> m_scratchpad_buffers_array_region;
    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> m_scratchpad_buffers;
    OwnPtr<Memory::Region> m_command_and_event_rings_region;
    TransferRequestBlock* m_command_ring { nullptr };
    TransferRequestBlock* m_event_ring_segment { nullptr };
    PhysicalPtr m_event_ring_segment_pointer { 0 };

    OwnPtr<GenericInterruptHandler> m_interrupter;
    OwnPtr<xHCIRootHub> m_root_hub;

protected:
    template<typename... Parameters>
    void dmesgln_xhci(AK::CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters) const
    {
        StringBuilder builder;

        MUST(write_dmesgln_prefix(builder));

        AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
        MUST(AK::vformat(builder, fmt.view(), variadic_format_params));

        dmesgln("{}", builder.string_view());
    }
};

}
