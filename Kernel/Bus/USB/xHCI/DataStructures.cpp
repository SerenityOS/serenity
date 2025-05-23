/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Bus/USB/xHCI/DataStructures.h>

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

}
