/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::USB::xHCI {

// FIXME: Take alignments from
//        Table 6-1: Data Structure Max Size, Boundary, and Alignment Requirement Summary
//        This messes with some AssertSize's in the 32 byte cases

// 6.4.6 TRB Types
enum class TRBType : u32 {
    //                Allowed In:    // CR, ER, TR
    Reserved = 0,                    //
    Normal = 1,                      //         TR (Isoch, Interrupt, Control, Bulk)
    SetupStage = 2,                  //         TR (                  Control      )
    DataStage = 3,                   //         TR (                  Control      )
    StatusStage = 4,                 //         TR (                  Control      )
    Isoch = 5,                       //         TR (Isoch                          )
    Link = 6,                        // CR,     TR (Isoch, Interrupt, Control, Bulk)
    EventData = 7,                   //         TR (Isoch, Interrupt, Control, Bulk)
    NoOp = 8,                        //         TR (Isoch, Interrupt, Control, Bulk)
    EnableSlotCommand = 9,           // CR
    DisableSlotCommand = 10,         // CR
    AddressDeviceCommand = 11,       // CR
    ConfigureEndpointCommand = 12,   // CR
    EvaluateContextCommand = 13,     // CR
    ResetEndpointCommand = 14,       // CR
    StopEndpointCommand = 15,        // CR
    SetTRDequePointerCommand = 16,   // CR
    ResetDeviceCommand = 17,         // CR
    ForceEventCommand = 18,          // CR         (Opt., Virtualization only)
    NegotiateBandwidthCommand = 19,  // CR         (Opt.)
    SetLatencyToleranceCommand = 20, // CR         (Opt.)
    GetPortBandwidthCommand = 21,    // CR         (Opt.)
    ForceHeaderCommand = 22,         // CR
    NoOpCommand = 23,                // CR
    GetExtendedPropertyCommand = 24, // CR         (Opt.)
    SetExtendedPropertyCommand = 25, // CR         (Opt.)
                                     // 26-31 Reserved
    TransferEvent = 32,              //     ER
    CommandCompletionEvent = 33,     //     ER
    PortStatusChangeEvent = 34,      //     ER
    BandwidthRequestEvent = 35,      //     ER    (Opt.)
    DoorbellEvent = 36,              //     ER    (Opt., Virtualization only)
    HostControllerEvent = 37,        //     ER
    DeviceNotificationEvent = 38,    //     ER
    MFINDEXWrapEvent = 39,           //     ER
                                     // 40-47 Reserved
                                     // 48-63 Vendor Defined
};
enum class TRBTransferType : u32 {
    NoDataStage = 0,
    Reserved = 1,
    OutDataStage = 2,
    InDataStage = 3,
};

// 3.2.7 Transfer Request Block
union TRBControl {
    // Table 6-22: Offset 0Ch - Normal TRB Field Definitions
    // Table 6-26: Offset 0Ch - Setup Stage TRB Field Definitions
    struct {
        u32 cycle : 1;                      // C: 0
        u32 evaluate_next_trb : 1;          // ENT: 1
        u32 interrupt_on_short_package : 1; // ISP: 2
        u32 no_snoop : 1;                   // NS: 3
        u32 chain_bit : 1;                  // CH: 4
        u32 interrupt_on_complete : 1;      // IOC: 5
        u32 immediate_data : 1;             // IDT: 6
        u32 resvd : 2;                      // RsvdZ: 7-8
        u32 block_event_interrupt : 1;      // BEI: 9
        TRBType trb_type : 6;               // TRB Type: 10-15
        u32 transfer_type : 2;              // TRT: 16-17
        u32 : 14;                           // RsvdZ: 18-31
    };
    // Table 6-29: Offset 0Ch - Data Stage TRB Field Definitions
    struct {
        u32 : 16;
        u32 direction : 1; // DIR: 16 (0: Out, 1: In)
        u32 : 15;
    };
    // Table 6-34: Offset 0Ch - Isoch TRB Field Definitions
    struct {
        u32 : 7;
        u32 transfer_burst_count : 2;             // TBC/TRBSts: 7-8
        u32 : 7;                                  //
        u32 transfer_last_burst_packet_count : 4; // TLBPC: 16-19
        u32 frame_id : 11;                        // Frame ID: 20-30
        u32 start_asap : 1;                       // SIA: 31
    };

    u32 raw { 0 };
};
union TRBStatus {
    struct {
        // Table 6-21: Offset 08h - Normal TRB Field Definitions
        // Table 6-25: Offset 08h - Setup Stage TRB Field Definitions
        // Table 6-28: Offset 08h - Data Stage TRB Field Definitions
        u32 length : 17;           // 0-16
        u32 TD_size : 5;           // 17-21
        u32 interrupt_target : 10; // 22-31
    } transfer_status;
};

struct TransferRequestBlock {
    // 6.4.1 Transfer TRBs
    // 6.4.2 Event TRBs
    union {
        // 6.4.1.1 Normal TRB
        // 6.4.1.3 Isoch TRB
        // 6.4.1.2.1 Setup Stage TRB
        // 6.4.1.2.2 Data Stage TRB
        void* data_buffer_pointer;

        struct {
            // Table 6-23: Offset 00h - Setup Stage TRB Field Definitions
            u8 bm_request_type; // bmRequestType: 0-7
            u8 b_request;       // bRequest : 8-15
            u16 w_value;        // wValue : 16-31
            // Table 6-24: Offset 04h - Setup Stage TRB Field Definitions
            u16 w_index;  // wIndex : 0-15
            u16 w_length; // wIndex : 0-15
        };
        // 6.4.1.2.3 Status Stage TRB
        // 6.4.1.4 No Op TRB
        u32 resvd[2];

        //
    };

    TRBStatus status;
    TRBControl control;
};
static_assert(AssertSize<TransferRequestBlock, 128 / 8>());

// 6.2.2 Slot Context
struct SlotContext {
    // Table 6-4: Offset 00h - Slot Context Field Definitions
    u32 route_string : 20;   //  0-19
    u32 speed : 4;           // 20-23 Deprecated
    u32 rsvdZ_1 : 1;         // 24
    u32 multi_tt : 1;        // 25    MTT
    u32 hub : 1;             // 26
    u32 context_entries : 5; // 27-31
    // Table 6-5: Offset 04h - Slot Context Field Definitions
    u32 max_exit_latency : 16;    //  0-15
    u32 root_hub_port_number : 8; // 16-23
    u32 number_of_ports : 8;      // 24-31
    // Table 6-6: Offset 08h - Slot Context Field Definitions
    u32 parent_hub_slot_id : 8; //  0- 7
    u32 parent_port_number : 8; //  8-15
    u32 tt_think_time : 2;      // 16-17
    u32 rsvdZ_2 : 4;            // 18-21
    u32 interrupt_target : 10;  // 22-31
    // Table 6-7: Offset 0Ch - Slot Context Field Definitions
    enum class SlotState : u32 {
        Disabled_Enabled = 0,
        Default = 1,
        Addressed = 2,
        Configured = 3,
        // Reserved : 31-4
    };
    u32 usb_device_address : 8; //  0- 7
    u32 rsvdZ_3 : 19;           //  8-26
    SlotState slot_state : 5;   // 27-31

    u32 rsvdO_1[4];
};
static_assert(AssertSize<SlotContext, 32>());

struct SlotContext64 : SlotContext {
    // "Note: [...] If the Context Size (CSZ) field = ‘1’ then each Slot
    // Context data structure consumes 64 bytes, where bytes 32 to 63 are also xHCI
    // Reserved (RsvdO)."
    // ~ Note below Table 6-7
    u32 rsvdO_2[8];
};
static_assert(AssertSize<SlotContext64, 64>());

// 6.2.3 Endpoint Context
struct EndpointContext {
    // Table 6-8: Offset 00h - Endpoint Context Field Definitions
    enum class EndpointState : u32 {
        Disabled = 0,
        Running = 1,
        Halted = 2,
        Stopped = 3,
        Error = 4,
        // 5-7 Reserved
    };
    EndpointState endpoint_state : 3;                      //  0- 2
    u32 rsvdZ_1 : 5;                                       //  3- 7
    u32 mult : 2;                                          //  8- 9
    u32 max_primary_streams : 5;                           // 10-14 MaxPStreams
    u32 linear_stream_array : 1;                           // 15
    u32 interval : 8;                                      // 16-23 See: 6.2.3.6 Interval
                                                           //            125µs units
    u32 max_endpoint_service_time_interval_payload_hi : 8; // 24-31 Max ESIT Payload Hi
    // Table 6-9: Offset 04h - Endpoint Context Field Definitions
    enum class EndpointType : u32 {
        NotValid = 0, // Direction: N/A
        IochOut = 1,
        BulkOut = 2,
        InterruptOut = 3,
        Control = 4, // Direction: BiDi
        IsochIn = 5,
        BulkIn = 6,
        InterruptIn = 7,
    };
    u32 rsvdZ_2 : 1;                 //  0
    u32 error_count : 2;             //  1- 2 CErr
    EndpointType endpoint_type : 3;  //  3- 5
    u32 rsvdZ_3 : 1;                 //  6
    u32 host_instatiate_disable : 1; //  7    HID
    u32 max_burst_size : 8;          //  8-15 low/full speed : 0
                                     //       high speed control/bulk : 0
                                     //       high speed iso/interrupt: bits 12:11 USB2 EndpointDescriptor::wMaxPacketSize
                                     //       enhanced super speed: USB3 SuperSpeedEndpointCompanionDescriptor::bMaxBurst
    u32 max_packet_size : 16;        // 16-31 bits 10:0 wMaxPacketSize, but linear byte count - not base 2 multiple
    // Table 6-10: Offset 08h - Endpoint Context Field Definitions
    union {
        struct {
            u32 dequeue_cycle_state : 1; //  0    DCS
            u32 rsvdZ_4 : 3;             //  1- 3
            u32 : 28;
            u32 : 32;
        };
        u64 TR_dequeue_pointer; //  4-63 If MaxPStreams == 0 -> Transfer Ring
                                //       Else                -> Stream Context Array
    };
    // Table 6-11 : Offset 10h - Endpoint Context Field Definition
    u32 average_trb_length : 16;                            //  0-15
    u32 max_endpoint_service_time_interval_payload_lo : 16; // 16-31

    u32 rsvdO_1[3];
};
static_assert(AssertSize<EndpointContext, 32>());

struct EndpointContext64 : EndpointContext {
    u32 rsvdO_2[8];
};
static_assert(AssertSize<EndpointContext64, 64>());

// FIXME: 6.2.4 Stream Context Array

// 6.2.1 Device Context
struct DeviceContext {
    SlotContext slot_context;
    EndpointContext endpoint_contexts[]; // max size: 31
};
static_assert(AssertSize<DeviceContext, 32>());
static_assert(__builtin_offsetof(DeviceContext, slot_context) == 0);
static_assert(__builtin_offsetof(DeviceContext, endpoint_contexts) == 0x20);
static_assert(__builtin_offsetof(DeviceContext, endpoint_contexts[1]) == 0x40);

struct DeviceContext64 {
    SlotContext64 slot_context;
    EndpointContext64 endpoint_contexts[];
};
static_assert(AssertSize<DeviceContext64, 64>());
static_assert(__builtin_offsetof(DeviceContext64, slot_context) == 0);
static_assert(__builtin_offsetof(DeviceContext64, endpoint_contexts) == 0x40);
static_assert(__builtin_offsetof(DeviceContext64, endpoint_contexts[1]) == 0x80);

// 6.2.5 Input Context
// 6.2.5.1 Input Control Context
struct InputControlContext {
    // Table 6-15: Offset 00h - Input Control Context Field Definitions
    u32 drop_context_flags; //  0- 1 RsvdZ
                            //  2-31 D2-D31
    // Table 6-16: Offset 04h - Input Control Context Field Definitions
    u32 add_contexts_flags; //  0-31 A0-A31

    u32 rsvdZ_1[5];
    // Table 6-17: Offset 1Ch - Input Control Context Field Definitions
    u32 configuration_value : 8; //  0- 7
    u32 interface_number : 8;    //  8-15
    u32 alternate_setting : 8;   // 16-23
    u32 rsvdZ_2 : 8;             // 24-31
};
static_assert(AssertSize<InputControlContext, 32>());

struct InputControlContext64 : InputControlContext {
    u32 rsvdZ_3[8];
};
static_assert(AssertSize<InputControlContext64, 64>());

struct InputContext {
    InputControlContext control_context;
    DeviceContext device_context;
};
static_assert(__builtin_offsetof(InputContext, control_context) == 0);
static_assert(__builtin_offsetof(InputContext, device_context.slot_context) == 0x20);
static_assert(__builtin_offsetof(InputContext, device_context.endpoint_contexts) == 0x40);
static_assert(__builtin_offsetof(InputContext, device_context.endpoint_contexts[1]) == 0x60);

struct InputContext64 {
    InputControlContext64 control_context;
    DeviceContext64 device_context;
};
static_assert(__builtin_offsetof(InputContext64, control_context) == 0);
static_assert(__builtin_offsetof(InputContext64, device_context.slot_context) == 0x40);
static_assert(__builtin_offsetof(InputContext64, device_context.endpoint_contexts) == 0x80);
static_assert(__builtin_offsetof(InputContext64, device_context.endpoint_contexts[1]) == 0xC0);

// FIXME: 6.2.6 Port Bandwidth Context

}
