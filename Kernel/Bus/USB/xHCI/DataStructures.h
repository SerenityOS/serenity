/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace Kernel::USB::xHCI {

union TransferRequestBlock {
    void dump(StringView prefix) const;

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

StringView enum_to_string(TransferRequestBlock::TRBType);
StringView enum_to_string(TransferRequestBlock::CompletionCode);

struct EventRingSegmentTableEntry {
    u32 ring_segment_base_address_low : 32;
    u32 ring_segment_base_address_high : 32;
    u32 ring_segment_size : 16;
    u32 reserved0 : 16;
    u32 reserved1 : 32;
};
static_assert(AssertSize<EventRingSegmentTableEntry, 0x10>());

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

}
