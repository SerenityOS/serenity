/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/Bus/USB/xHCI/DataStructures.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel::USB::xHCI {

StringView enum_to_string(TransferRequestBlock::CompletionCode completion_code)
{
    switch (completion_code) {
    case TransferRequestBlock::CompletionCode::Invalid:
        return "Invalid"sv;
    case TransferRequestBlock::CompletionCode::Success:
        return "Success"sv;
    case TransferRequestBlock::CompletionCode::Data_Buffer_Error:
        return "Data Buffer Error"sv;
    case TransferRequestBlock::CompletionCode::Babble_Detected_Error:
        return "Babble Detected Error"sv;
    case TransferRequestBlock::CompletionCode::USB_Transaction_Error:
        return "USB Transaction Error"sv;
    case TransferRequestBlock::CompletionCode::TRB_Error:
        return "TRB Error"sv;
    case TransferRequestBlock::CompletionCode::Stall_Error:
        return "Stall Error"sv;
    case TransferRequestBlock::CompletionCode::Resource_Error:
        return "Resource Error"sv;
    case TransferRequestBlock::CompletionCode::Bandwidth_Error:
        return "Bandwidth Error"sv;
    case TransferRequestBlock::CompletionCode::No_Slots_Available_Error:
        return "No Slots Available Error"sv;
    case TransferRequestBlock::CompletionCode::Invalid_Stream_Type_Error:
        return "Invalid Stream Type Error"sv;
    case TransferRequestBlock::CompletionCode::Slot_Not_Enabled_Error:
        return "Slot Not Enabled Error"sv;
    case TransferRequestBlock::CompletionCode::Endpoint_Not_Enabled_Error:
        return "Endpoint Not Enabled Error"sv;
    case TransferRequestBlock::CompletionCode::Short_Packet:
        return "Short Packet"sv;
    case TransferRequestBlock::CompletionCode::Ring_Underrun:
        return "Ring Underrun"sv;
    case TransferRequestBlock::CompletionCode::Ring_Overrun:
        return "Ring Overrun"sv;
    case TransferRequestBlock::CompletionCode::VF_Event_Ring_Full_Error:
        return "VF Event Ring Full Error"sv;
    case TransferRequestBlock::CompletionCode::Parameter_Error:
        return "Parameter Error"sv;
    case TransferRequestBlock::CompletionCode::Bandwidth_Overrun_Error:
        return "Bandwidth Overrun Error"sv;
    case TransferRequestBlock::CompletionCode::Context_State_Error:
        return "Context State Error"sv;
    case TransferRequestBlock::CompletionCode::No_Ping_Response_Error:
        return "No Ping Response Error"sv;
    case TransferRequestBlock::CompletionCode::Event_Ring_Full_Error:
        return "Event Ring Full Error"sv;
    case TransferRequestBlock::CompletionCode::Incompatible_Device_Error:
        return "Incompatible Device Error"sv;
    case TransferRequestBlock::CompletionCode::Missed_Service_Error:
        return "Missed Service Error"sv;
    case TransferRequestBlock::CompletionCode::Command_Ring_Stopped:
        return "Command Ring Stopped"sv;
    case TransferRequestBlock::CompletionCode::Command_Aborted:
        return "Command Aborted"sv;
    case TransferRequestBlock::CompletionCode::Stopped:
        return "Stopped"sv;
    case TransferRequestBlock::CompletionCode::Stopped_Length_Invalid:
        return "Stopped Length Invalid"sv;
    case TransferRequestBlock::CompletionCode::Stopped_Short_Packet:
        return "Stopped Short Packet"sv;
    case TransferRequestBlock::CompletionCode::Max_Exit_Latency_Too_Large_Error:
        return "Max Exit Latency Too Large Error"sv;
    case TransferRequestBlock::CompletionCode::Isoch_Buffer_Overrun:
        return "Isoch Buffer Overrun"sv;
    case TransferRequestBlock::CompletionCode::Event_Lost_Error:
        return "Event Lost Error"sv;
    case TransferRequestBlock::CompletionCode::Undefined_Error:
        return "Undefined Error"sv;
    case TransferRequestBlock::CompletionCode::Invalid_Stream_ID_Error:
        return "Invalid Stream ID Error"sv;
    case TransferRequestBlock::CompletionCode::Secondary_Bandwidth_Error:
        return "Secondary Bandwidth Error"sv;
    case TransferRequestBlock::CompletionCode::Split_Transaction_Error:
        return "Split Transaction Error"sv;
    default:
        return "Unknown"sv;
    }
}

StringView enum_to_string(TransferRequestBlock::TRBType trb_type)
{
    switch (trb_type) {
    case TransferRequestBlock::TRBType::Normal:
        return "Normal"sv;
    case TransferRequestBlock::TRBType::Setup_Stage:
        return "Setup Stage"sv;
    case TransferRequestBlock::TRBType::Data_Stage:
        return "Data Stage"sv;
    case TransferRequestBlock::TRBType::Status_Stage:
        return "Status Stage"sv;
    case TransferRequestBlock::TRBType::Isoch:
        return "Isoch"sv;
    case TransferRequestBlock::TRBType::Link:
        return "Link"sv;
    case TransferRequestBlock::TRBType::Event_Data:
        return "Event Data"sv;
    case TransferRequestBlock::TRBType::No_Op:
        return "No Op"sv;
    case TransferRequestBlock::TRBType::Enable_Slot_Command:
        return "Enable Slot Command"sv;
    case TransferRequestBlock::TRBType::Disable_Slot_Command:
        return "Disable Slot Command"sv;
    case TransferRequestBlock::TRBType::Address_Device_Command:
        return "Address Device Command"sv;
    case TransferRequestBlock::TRBType::Configure_Endpoint_Command:
        return "Configure Endpoint Command"sv;
    case TransferRequestBlock::TRBType::Evaluate_Context_Command:
        return "Evaluate Context Command"sv;
    case TransferRequestBlock::TRBType::Reset_Endpoint_Command:
        return "Reset Endpoint Command"sv;
    case TransferRequestBlock::TRBType::Stop_Endpoint_Command:
        return "Stop Endpoint Command"sv;
    case TransferRequestBlock::TRBType::Set_TR_Dequeue_Pointer_Command:
        return "Set TR Dequeue Pointer Command"sv;
    case TransferRequestBlock::TRBType::Reset_Device_Command:
        return "Reset Device Command"sv;
    case TransferRequestBlock::TRBType::Force_Event_Command:
        return "Force Event Command"sv;
    case TransferRequestBlock::TRBType::Negotiate_Bandwidth_Command:
        return "Negotiate Bandwidth Command"sv;
    case TransferRequestBlock::TRBType::Set_Latency_Tolerance_Value_Command:
        return "Set Latency Tolerance Value Command"sv;
    case TransferRequestBlock::TRBType::Get_Port_Bandwidth_Command:
        return "Get Port Bandwidth Command"sv;
    case TransferRequestBlock::TRBType::Force_Header_Command:
        return "Force Header Command"sv;
    case TransferRequestBlock::TRBType::No_Op_Command:
        return "No Op Command"sv;
    case TransferRequestBlock::TRBType::Get_Extended_Property_Command:
        return "Get Extended Property Command"sv;
    case TransferRequestBlock::TRBType::Set_Extended_Property_Command:
        return "Set Extended Property Command"sv;
    case TransferRequestBlock::TRBType::Transfer_Event:
        return "Transfer Event"sv;
    case TransferRequestBlock::TRBType::Command_Completion_Event:
        return "Command Completion Event"sv;
    case TransferRequestBlock::TRBType::Port_Status_Change_Event:
        return "Port Status Change Event"sv;
    case TransferRequestBlock::TRBType::Bandwidth_Request_Event:
        return "Bandwidth Request Event"sv;
    case TransferRequestBlock::TRBType::Doorbell_Event:
        return "Doorbell Event"sv;
    case TransferRequestBlock::TRBType::Host_Controller_Event:
        return "Host Controller Event"sv;
    case TransferRequestBlock::TRBType::Device_Notification_Event:
        return "Device Notification Event"sv;
    case TransferRequestBlock::TRBType::Microframe_Index_Wrap_Event:
        return "Microframe Index Wrap Event"sv;
    default:
        return "Unknown"sv;
    }
}

void TransferRequestBlock::dump(StringView prefix) const
{
    auto print_line = [prefix]<typename... Parameters>(CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters) {
        AK::StringBuilder builder;

        builder.append(prefix);

        AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
        AK::vformat(builder, fmt.view(), variadic_format_params).release_value_but_fixme_should_propagate_errors();

        dbgln("{}", builder.string_view());
    };

    switch (generic.transfer_request_block_type) {
        using enum TransferRequestBlock::TRBType;

    case Normal: {
        auto data_buffer = PhysicalAddress { normal.data_buffer_pointer_low | static_cast<u64>(normal.data_buffer_pointer_high) << 32 };
        print_line("Data Buffer: {}", data_buffer);
        print_line("TRB Transfer Length: {:#x}", normal.transfer_request_block_transfer_length);
        print_line("TD Size: {:#x}", normal.transfer_descriptor_size);
        print_line("Interrupter Target: {:#x}", normal.interrupter_target);
        print_line("Cycle bit: {}", normal.cycle_bit);
        print_line("Evaluate Next TRB: {}", normal.evaluate_next_transfer_request_block);
        print_line("Interrupt-on Short Packet: {}", normal.interrupt_on_short_packet);
        print_line("No Snoop: {}", normal.no_snoop);
        print_line("Chain bit: {}", normal.chain_bit);
        print_line("Interrupt On Completion: {}", normal.interrupt_on_completion);
        print_line("Immediate Data: {}", normal.immediate_data);
        print_line("Block Event Interrupt: {}", normal.block_event_interrupt);
        break;
    }
    case Setup_Stage:
        print_line("bmRequestType: {:#x}", setup_stage.request_type);
        print_line("bRequest: {:#x}", setup_stage.request);
        print_line("wValue: {:#x}", setup_stage.value);
        print_line("wIndex: {:#x}", setup_stage.index);
        print_line("wLength: {:#x}", setup_stage.length);
        print_line("TRB Transfer Length: {:#x}", setup_stage.transfer_request_block_transfer_length);
        print_line("Interrupter Target: {:#x}", setup_stage.interrupter_target);
        print_line("Cycle bit: {}", setup_stage.cycle_bit);
        print_line("Interrupt On Completion: {}", setup_stage.interrupt_on_completion);
        print_line("Immediate Data: {}", setup_stage.immediate_data);
        print_line("Transfer Type: {:#x}", to_underlying(setup_stage.transfer_type));
        break;
    case Data_Stage: {
        auto data_buffer = PhysicalAddress { data_stage.data_buffer_low | static_cast<u64>(data_stage.data_buffer_high) << 32 };
        print_line("Data Buffer: {}", data_buffer);
        print_line("TRB Transfer Length: {:#x}", data_stage.transfer_request_block_transfer_length);
        print_line("TD Size: {:#x}", data_stage.transfer_descriptor_size);
        print_line("Interrupter Target: {:#x}", data_stage.interrupter_target);
        print_line("Cycle bit: {}", data_stage.cycle_bit);
        print_line("Evaluate Next TRB: {}", data_stage.evaluate_next_transfer_request_block);
        print_line("Interrupt-on Short Packet: {}", data_stage.interrupt_on_short_packet);
        print_line("No Snoop: {}", data_stage.no_snoop);
        print_line("Chain bit: {}", data_stage.chain_bit);
        print_line("Interrupt On Completion: {}", data_stage.interrupt_on_completion);
        print_line("Immediate Data: {}", data_stage.immediate_data);
        print_line("Direction: {}", data_stage.direction);
        break;
    }
    case Status_Stage:
        print_line("Interrupter Target: {:#x}", status_stage.interrupter_target);
        print_line("Cycle bit: {}", status_stage.cycle_bit);
        print_line("Evaluate Next TRB: {}", status_stage.evaluate_next_transfer_request_block);
        print_line("Chain bit: {}", status_stage.chain_bit);
        print_line("Interrupt On Completion: {}", setup_stage.interrupt_on_completion);
        print_line("Direction: {}", status_stage.direction);
        break;
    case Isoch: {
        auto data_buffer = PhysicalAddress { isoch.data_buffer_pointer_low | static_cast<u64>(isoch.data_buffer_pointer_high) << 32 };
        print_line("Data Buffer: {}", data_buffer);
        print_line("TRB Transfer Length: {:#x}", isoch.transfer_request_block_transfer_length);
        print_line("TD Size/TBC: {:#x}", isoch.transfer_descriptor_size_OR_transfer_burst_count);
        print_line("Interrupter Target: {:#x}", isoch.interrupter_target);
        print_line("Cycle bit: {}", isoch.cycle_bit);
        print_line("Evaluate Next TRB: {}", isoch.evaluate_next_transfer_request_block);
        print_line("Interrupt-on Short Packet: {}", isoch.interrupt_on_short_packet);
        print_line("No Snoop: {}", isoch.no_snoop);
        print_line("Chain bit: {}", isoch.chain_bit);
        print_line("Interrupt On Completion: {}", isoch.interrupt_on_completion);
        print_line("Immediate Data: {}", isoch.immediate_data);
        print_line("Transfer Burst Count/TRB Status: {:#x}", isoch.transfer_burst_count_OR_transfer_request_block_status_OR_reserved0);
        print_line("Block Event Interrupt: {}", isoch.block_event_interrupt);
        print_line("Transfer Last Burst Packet Count: {:#x}", isoch.transfer_last_burst_packet_count);
        print_line("Frame ID: {:#x}", isoch.frame_id);
        print_line("Start Isoch ASAP: {}", isoch.start_isoch_as_soon_as_possible);
        break;
    }
    case Link: {
        auto ring_segment_pointer = PhysicalAddress { link.ring_segment_pointer_low | static_cast<u64>(link.ring_segment_pointer_high) << 32 };
        print_line("Ring Segment Pointer: {}", ring_segment_pointer);
        print_line("Interrupter Target: {:#x}", link.interrupter_target);
        print_line("Cycle bit: {}", link.cycle_bit);
        print_line("Toggle Cycle: {}", link.toggle_cycle);
        print_line("Chain bit: {}", link.chain_bit);
        print_line("Interrupt On Completion: {}", link.interrupt_on_completion);
        break;
    }
    case Event_Data: {
        auto event_data_ptr = PhysicalAddress { event_data.event_data_low | static_cast<u64>(event_data.event_data_high) << 32 };
        print_line("Event Data: {}", event_data_ptr);
        print_line("Interrupter Target: {:#x}", event_data.interrupter_target);
        print_line("Cycle bit: {}", event_data.cycle_bit);
        print_line("Evaluate Next TRB: {}", event_data.evaluate_next_transfer_request_block);
        print_line("Chain bit: {}", event_data.chain_bit);
        print_line("Interrupt On Completion: {}", event_data.interrupt_on_completion);
        print_line("Block Event Interrupt: {}", event_data.block_event_interrupt);
        break;
    }
    case No_Op:
        print_line("Interrupter Target: {:#x}", no_op.interrupter_target);
        print_line("Cycle bit: {}", no_op.cycle_bit);
        print_line("Evaluate Next TRB: {}", no_op.evaluate_next_transfer_request_block);
        print_line("Chain bit: {}", no_op.chain_bit);
        print_line("Interrupt On Completion: {}", no_op.interrupt_on_completion);
        break;
    case Enable_Slot_Command:
        print_line("Cycle bit: {}", enable_slot_command.cycle_bit);
        print_line("Slot Type: {:#x}", enable_slot_command.slot_type);
        break;
    case Disable_Slot_Command:
        print_line("Cycle bit: {}", disable_slot_command.cycle_bit);
        print_line("Slot ID: {}", disable_slot_command.slot_id);
        break;
    case Address_Device_Command: {
        auto input_context_pointer = PhysicalAddress { address_device_command.input_context_pointer_low | static_cast<u64>(address_device_command.input_context_pointer_high) << 32 };
        print_line("Input Context Pointer: {}", input_context_pointer);
        print_line("Cycle bit: {}", address_device_command.cycle_bit);
        print_line("Block Set Address Request: {}", address_device_command.block_set_address_request);
        print_line("Slot ID: {}", address_device_command.slot_id);
        break;
    }
    case Configure_Endpoint_Command: {
        auto input_context_pointer = PhysicalAddress { configure_endpoint_command.input_context_pointer_low | static_cast<u64>(configure_endpoint_command.input_context_pointer_high) << 32 };
        print_line("Input Context Pointer: {}", input_context_pointer);
        print_line("Cycle bit: {}", configure_endpoint_command.cycle_bit);
        print_line("Deconfigure: {}", configure_endpoint_command.deconfigure);
        print_line("Slot ID: {}", configure_endpoint_command.slot_id);
        break;
    }
    case Evaluate_Context_Command: {
        auto input_context_pointer = PhysicalAddress { evaluate_context_command.input_context_pointer_low | static_cast<u64>(evaluate_context_command.input_context_pointer_high) << 32 };
        print_line("Input Context Pointer: {}", input_context_pointer);
        print_line("Cycle bit: {}", evaluate_context_command.cycle_bit);
        print_line("Slot ID: {}", evaluate_context_command.slot_id);
        break;
    }
    case Reset_Endpoint_Command:
        print_line("Cycle bit: {}", reset_endpoint_command.cycle_bit);
        print_line("Transfer State Preserve: {}", reset_endpoint_command.transfer_state_preserve);
        print_line("Endpoint ID: {}", reset_endpoint_command.endpoint_id);
        print_line("Slot ID: {}", reset_endpoint_command.slot_id);
        break;
    case Stop_Endpoint_Command:
        print_line("Cycle bit: {}", stop_endpoint_command.cycle_bit);
        print_line("Endpoint ID: {}", stop_endpoint_command.endpoint_id);
        print_line("Suspend: {}", stop_endpoint_command.suspend);
        print_line("Slot ID: {}", stop_endpoint_command.slot_id);
        break;
    case Set_TR_Dequeue_Pointer_Command: {
        auto new_tr_dequeue_pointer = PhysicalAddress { set_tr_dequeue_pointer_command.new_tr_dequeue_pointer_low | static_cast<u64>(set_tr_dequeue_pointer_command.new_tr_dequeue_pointer_high) << 32 };
        print_line("Dequeue Cycle State: {}", set_tr_dequeue_pointer_command.dequeue_cycle_state);
        print_line("Stream Context Type: {:#x}", set_tr_dequeue_pointer_command.stream_context_type);
        print_line("New TR Dequeue Pointer: {}", new_tr_dequeue_pointer);
        print_line("Stream ID: {:#x}", set_tr_dequeue_pointer_command.stream_id);
        print_line("Cycle bit: {}", set_tr_dequeue_pointer_command.cycle_bit);
        print_line("Endpoint ID: {}", set_tr_dequeue_pointer_command.endpoint_id);
        print_line("Slot ID: {}", set_tr_dequeue_pointer_command.slot_id);
        break;
    }
    case Reset_Device_Command:
        print_line("Cycle bit: {}", reset_device_command.cycle_bit);
        print_line("Slot ID: {}", reset_device_command.slot_id);
        break;
    case Force_Event_Command: {
        auto event_trb_pointer = PhysicalAddress { force_event_command.event_transfer_request_block_pointer_low | static_cast<u64>(force_event_command.event_transfer_request_block_pointer_high) << 32 };
        print_line("Event TRB Pointer: {}", event_trb_pointer);
        print_line("VF Interrupter Target: {:#x}", force_event_command.vf_interrupter_target);
        print_line("Cycle bit: {}", force_event_command.cycle_bit);
        print_line("VF ID: {:#x}", force_event_command.vf_id);
        break;
    }
    case Negotiate_Bandwidth_Command:
        print_line("Cycle bit: {}", negotiate_bandwidth_command.cycle_bit);
        print_line("Slot ID: {}", negotiate_bandwidth_command.slot_id);
        break;
    case Set_Latency_Tolerance_Value_Command:
        print_line("Cycle bit: {}", set_latency_tolerance_value_command.cycle_bit);
        print_line("Best Effort Latency Tolerance Value: {:#x}", set_latency_tolerance_value_command.best_effort_latency_tolerance_value);
        break;
    case Get_Port_Bandwidth_Command: {
        auto port_bandwidth_context_pointer = PhysicalAddress { get_port_bandwidth_command.port_bandwidth_context_pointer_low | static_cast<u64>(get_port_bandwidth_command.port_bandwidth_context_pointer_high) << 32 };
        print_line("Port Bandwidth Context Pointer: {}", port_bandwidth_context_pointer);
        print_line("Cycle bit: {}", get_port_bandwidth_command.cycle_bit);
        print_line("Device Speed: {:#x}", get_port_bandwidth_command.device_speed);
        print_line("Hub Slot ID: {}", get_port_bandwidth_command.hub_slot_id);
        break;
    }
    case Force_Header_Command: {
        auto header_info = PhysicalAddress { force_header_command.header_info_low | static_cast<u64>(force_header_command.header_info_high) << 32 };
        print_line("Packet Type: {:#x}", force_header_command.packet_type);
        print_line("Header Info: {:#x}", header_info);
        print_line("Cycle bit: {}", force_header_command.cycle_bit);
        print_line("Root Hub Port Number: {:#x}", force_header_command.root_hub_port_number);
        break;
    }
    case No_Op_Command:
        print_line("Cycle bit: {}", no_op_command.cycle_bit);
        break;
    case Get_Extended_Property_Command: {
        auto extended_property_context_pointer = PhysicalAddress { get_extended_property_command.extended_property_context_pointer_low | static_cast<u64>(get_extended_property_command.extended_property_context_pointer_high) << 32 };
        print_line("Extended Property Context Pointer: {}", extended_property_context_pointer);
        print_line("Extended Capability Identifier: {:#x}", get_extended_property_command.extended_capability_identifier);
        print_line("Cycle bit: {}", get_extended_property_command.cycle_bit);
        print_line("Command SubType: {:#x}", get_extended_property_command.command_sub_type);
        print_line("Endpoint ID: {}", get_extended_property_command.endpoint_id);
        print_line("Slot ID: {}", get_extended_property_command.slot_id);
        break;
    }
    case Set_Extended_Property_Command:
        print_line("Extended Capability Identifier: {:#x}", set_extended_property_command.extended_capability_identifier);
        print_line("Capability Parameter: {:#x}", set_extended_property_command.capability_parameter);
        print_line("Cycle bit: {}", set_extended_property_command.cycle_bit);
        print_line("Command SubType: {:#x}", set_extended_property_command.command_sub_type);
        print_line("Endpoint ID: {}", set_extended_property_command.endpoint_id);
        print_line("Slot ID: {}", set_extended_property_command.slot_id);
        break;
    case Transfer_Event: {
        auto trb_pointer = PhysicalAddress { transfer_event.transfer_request_block_pointer_low | static_cast<u64>(transfer_event.transfer_request_block_pointer_high) << 32 };
        print_line("TRB Pointer: {}", trb_pointer);
        print_line("TRB Transfer Length: {:#x}", transfer_event.transfer_request_block_transfer_length);
        print_line("Completion Code: {}", enum_to_string(transfer_event.completion_code));
        print_line("Cycle bit: {}", transfer_event.cycle_bit);
        print_line("Event Data: {}", transfer_event.event_data);
        print_line("Endpoint ID: {}", transfer_event.endpoint_id);
        print_line("Slot ID: {}", transfer_event.slot_id);
        break;
    }
    case Command_Completion_Event: {
        auto command_trb_pointer = PhysicalAddress { command_completion_event.command_transfer_request_block_pointer_low | static_cast<u64>(command_completion_event.command_transfer_request_block_pointer_high) << 32 };
        print_line("Command TRB Pointer: {}", command_trb_pointer);
        print_line("Command Completion Parameter: {:#x}", command_completion_event.command_completion_parameter);
        print_line("Completion Code: {}", enum_to_string(command_completion_event.completion_code));
        print_line("Cycle bit: {}", command_completion_event.cycle_bit);
        print_line("VF ID: {}", command_completion_event.vf_id);
        print_line("Slot ID: {}", command_completion_event.slot_id);
        break;
    }
    case Port_Status_Change_Event:
        print_line("Port ID: {}", port_status_change_event.port_id);
        print_line("Completion Code: {}", enum_to_string(port_status_change_event.completion_code));
        print_line("Cycle bit: {}", port_status_change_event.cycle_bit);
        break;
    case Bandwidth_Request_Event:
        print_line("Completion Code: {}", enum_to_string(bandwidth_request_event.completion_code));
        print_line("Cycle bit: {}", bandwidth_request_event.cycle_bit);
        print_line("Slot ID: {}", bandwidth_request_event.slot_id);
        break;
    case Doorbell_Event:
        print_line("DB Reason: {:#x}", doorbell_event.doorbell_reason);
        print_line("Completion Code: {}", enum_to_string(doorbell_event.completion_code));
        print_line("Cycle bit: {}", doorbell_event.cycle_bit);
        print_line("VF ID: {}", doorbell_event.vf_id);
        print_line("Slot ID: {}", doorbell_event.slot_id);
        break;
    case Host_Controller_Event:
        print_line("Completion Code: {}", enum_to_string(host_controller_event.completion_code));
        print_line("Cycle bit: {}", host_controller_event.cycle_bit);
        break;
    case Device_Notification_Event: {
        auto device_notification_data = PhysicalAddress { device_notification_event.device_notification_data_low | static_cast<u64>(device_notification_event.device_notification_data_high) << 32 };
        print_line("Notification Type: {:#x}", device_notification_event.notification_type);
        print_line("Device Notification Data: {:#x}", device_notification_data);
        print_line("Completion Code: {}", enum_to_string(device_notification_event.completion_code));
        print_line("Cycle bit: {}", device_notification_event.cycle_bit);
        print_line("Slot ID: {}", device_notification_event.slot_id);
        break;
    }
    case Microframe_Index_Wrap_Event:
        print_line("Completion Code: {}", enum_to_string(microframe_index_wrap_event.completion_code));
        print_line("Cycle bit: {}", microframe_index_wrap_event.cycle_bit);
        break;
    default:
        print_line("-- Unknown --");
        break;
    }
}

}
