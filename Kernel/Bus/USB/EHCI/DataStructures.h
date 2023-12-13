/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel::USB::EHCI {

// https://www.intel.com/content/www/us/en/products/docs/io/universal-serial-bus/ehci-specification-for-usb.html
// Section 3 (32 bit structures)and Appendix B (64 bit structures)

// Table 3-1 Typ Field Value Definitions
enum class Typ : u8 {
    iTD = 0b00,
    QH = 0b01,
    siTD = 0b10,
    FSTN = 0b11
};

struct IsochronousTransferDescriptor;
struct SplitTransactionIsochronousTransferDescriptor;
struct QueueElementTransferDescriptor;
struct QueueHead;
struct FrameSpanTraversalNode;

// 3.1 Periodic Frame List
// Also for "3.3.1 Next Link Pointer" and the like
union FrameListElementPointer {
    u32 link_pointer;
    struct {
        u32 terminate : 1;
        Typ typ : 2;
        u32 zero : 2;
        u32 link_pointer_hi : 27;
    };

    template<typename T>
    static FrameListElementPointer make(PhysicalPtr addr);
    template<>
    FrameListElementPointer make<IsochronousTransferDescriptor>(PhysicalPtr addr)
    {
        VERIFY((addr & 0b11111) == 0);
        VERIFY(addr == (u32)addr);
        return { .terminate = 0, .typ = Typ::iTD, .zero = 0, .link_pointer_hi = (u32)addr >> 5 };
    }
    template<>
    FrameListElementPointer make<SplitTransactionIsochronousTransferDescriptor>(PhysicalPtr addr)
    {
        VERIFY((addr & 0b11111) == 0);
        VERIFY(addr == (u32)addr);
        return { .terminate = 0, .typ = Typ::siTD, .zero = 0, .link_pointer_hi = (u32)addr >> 5 };
    }
    template<>
    FrameListElementPointer make<QueueHead>(PhysicalPtr addr)
    {
        VERIFY((addr & 0b11111) == 0);
        VERIFY(addr == (u32)addr);
        return { .terminate = 0, .typ = Typ::QH, .zero = 0, .link_pointer_hi = (u32)addr >> 5 };
    }
    template<>
    FrameListElementPointer make<FrameSpanTraversalNode>(PhysicalPtr addr)
    {
        VERIFY((addr & 0b11111) == 0);
        VERIFY(addr == (u32)addr);
        return { .terminate = 0, .typ = Typ::FSTN, .zero = 0, .link_pointer_hi = (u32)addr >> 5 };
    }
};

// 3.3 Isochronous (High-Speed) Transfer Descriptor (iTD)
struct IsochronousTransferDescriptor {
    FrameListElementPointer next_link_pointer;
    // 3.3.2 iTD Transaction Status and Control List
    struct TransactionStatusControl {
        u32 transaction_x_offset : 11; // (RW)
        u32 page_select : 3;           // (RW)
        u32 interrupt_on_complete : 1;
        u32 transaction_x_length : 12; // RW
        // Status Bit Field:           // RW
        u32 transaction_error : 1;
        u32 babble_detected : 1;
        u32 data_buffer_error : 1;
        u32 active : 1;
    } transaction_status_and_control[8];

    // 3.3.3 iTD Buffer Page Pointer List (Plus)
    union {
        struct {
            u32 reserved : 12;
            u32 pointer_hi : 20;
        } buffer_pointer_list[7];
        struct {
            u32 device_address : 7;
            u32 : 1;
            u32 endpoint_number : 4;
            u32 : 20;

            u32 maximum_packet_size : 11;
            u32 direction : 1;
            u32 : 20;

            u32 transactions_per_micro_frame : 2; // Multi
            u32 : 10;
            u32 : 20;

            u32 _[4];
        };
    };
};
static_assert(AssertSize<IsochronousTransferDescriptor, 0x40>());

struct IsochronousTransferDescriptor64 : public IsochronousTransferDescriptor {
    u32 extended_buffer_pointer_list[7];
};
static_assert(AssertSize<IsochronousTransferDescriptor64, 0x5C>());

// 3.4 Split Transaction Isochronous Transfer Descriptor (siTD)
struct SplitTransactionIsochronousTransferDescriptor {
    FrameListElementPointer next_link_pointer;
    // 3.4.2 siTD Endpoint Capabilities/Characteristics
    // Table 3-9. Endpoint and Transaction Translator Characteristics
    struct {
        u8 device_address : 6;
        u8 reserved0 : 1 { 0 };
        u8 endpoint_number : 4;
        u8 reserved1 : 4 { 0 };
        u8 hub_address : 7;
        u8 reserved2 : 1 { 0 };
        u8 port_number : 7;
        u8 direction : 1;
    };
    // Table 3-10. Micro-frame Schedule Control
    struct {
        u8 split_start_mask : 8;
        u8 split_completion_mask : 8;
        u16 reserved { 0 };
    } schedule_control;

    // 3.4.3 siTD Transfer State
    struct {
        struct Status {
            u8 reserved : 1 { 0 };
            u8 split_transaction_state : 1;
            u8 missed_micro_frame : 1;
            u8 transaction_error : 1;
            u8 babble_detected : 1;
            u8 data_buffer_error : 1;
            u8 err : 1;
            u8 active : 1;
        } status;                                    // RW
        u8 micro_frame_complete_split_progress_mask; // RW
        u16 total_bytes_to_transfer : 10;            // RW
        u16 reserved : 4;                            // RW
        u16 page_select : 1;                         // RW
        u16 interrupt_on_complete : 1;
    } status_and_control;

    // 3.4.4 siTD Buffer Pointer List (plus)
    enum class TransactionPosition : u32 {
        All = 0b00,
        Begin = 0b01,
        Mid = 0b10,
        End = 0b11
    };
    union {
        struct {
            u32 reserved : 12;
            u32 pointer_hi : 20;
        } buffer_pointer_list[2];
        struct {
            u32 current_offset : 12; // RW
            u32 : 20;
            u32 transaction_count : 3;                    // RW
            TransactionPosition transaction_position : 2; // RW
            u32 reserved : 7 { 0 };
            u32 : 20;
        };
    };
    // 3.4.5 siTD Back Link Pointer
    struct {
        u32 terminate : 1;
        u32 reserved : 4 { 0 };
        u32 back_pointer_hi : 27;
    } back_link_pointer;
};
static_assert(AssertSize<SplitTransactionIsochronousTransferDescriptor, 0x1C>());

struct SplitTransactionIsochronousTransferDescriptor64 : public SplitTransactionIsochronousTransferDescriptor {
    u32 extended_buffer_pointer_list[2];
};
static_assert(AssertSize<SplitTransactionIsochronousTransferDescriptor64, 0x24>());

// 3.5 Queue Element Transfer Descriptor (qTD)
struct QueueElementTransferDescriptor {
    enum class PIDCode : u8 {
        OUT = 0b00,   // generates token (E1H)
        IN = 0b01,    // generates token (69H)
        SETUP = 0b10, // generates token (2DH)
    };
    // 3.5.1 Next qTD Pointer
    // Note: the type field is not evaluated here, as is ignored:
    //       "These bits are reserved and their value has no effect on operation."
    //       ~ Table 3-14. qTD Next Element Transfer Pointer (DWord 0)
    FrameListElementPointer next_qTD_pointer;
    // 3.5.2 Alternate Next qTD Pointer
    FrameListElementPointer alternate_next_qTD_pointer;
    // 3.5.3 qTD Token
    struct Status {
        u8 ping_state : 1;
        u8 split_transaction_state : 1;
        u8 missed_micro_frame : 1;
        u8 transaction_error : 1;
        u8 babble_detected : 1;
        u8 data_buffer_error : 1;
        u8 halted : 1;
        u8 active : 1;
    } status;
    PIDCode pid_code : 2;
    u8 error_counter : 2;
    u8 current_page : 3;
    u8 interrupt_on_complete : 1;
    u16 total_bytes_to_transfer : 15;
    u16 data_toggle : 1;

    // 3.5.4 qTD Buffer Page Pointer List
    union {
        u32 buffer_pointer_list[5];
        struct {
            u32 current_page_offset : 12 { 0 };
            u32 : 20;
            // Table 3-22. Host-Controller Rules for Bits in Overlay (DWords 5, 6, 8 and 9)
            // adds more fields here:
            u32 split_transaction_complete_split_progress : 8; // (C-prog-mask)
            u32 : 4;
            u32 : 20;
            u32 split_transaction_frame_tag : 5;
            u32 s_bytes : 7;
            u32 : 20;
            u32 _[2];
        };
    };
};
static_assert(AssertSize<QueueElementTransferDescriptor, 0x20>());

struct QueueElementTransferDescriptor64 : public QueueElementTransferDescriptor {
    u32 extended_buffer_pointer_list[5];
};
static_assert(AssertSize<QueueElementTransferDescriptor64, 0x34>());

// 3.6 Queue Head
struct QueueHead {
    // 3.6.1 Queue Head Horizontal Link Pointer
    FrameListElementPointer queue_head_horizontal_link_pointer;
    // 3.6.2 Endpoint Capabilities/Characteristics
    struct EndpointCharacteristics {
        u32 device_address : 6;
        u32 inactive_on_next_transaction : 1;
        u32 endpoint_number : 4;
        enum class EndpointSpeed : u32 {
            FullSpeed = 0b00,
            LowSpeed = 0b01,
            HighSpeed = 0b10,
        } endpoint_speed : 2;
        u32 data_toggle_control : 1;
        u32 head_of_reclamation_list_flag : 1;
        u32 maximum_packet_length : 11;
        u32 control_endpoint_flag : 1;
        u32 nak_count_reload : 4;
    } endpoint_characteristics;
    struct EndpointCapabilities {
        u32 interrupt_shedule_mask : 8;
        u32 split_completion_mask : 8;
        u32 hub_address : 7;
        u32 port_number : 7;
        u32 high_bandwidth_multiplier : 2;
    } endpoint_capabilities;

    // 3.6.3 Transfer Overlay
    // Note: The lower bits (T, Typ) are ignored
    FrameListElementPointer current_transaction_pointer;
    // "The DWords 4-11 of a queue head are the transaction overlay area. This area has the same base structure as
    //  a Queue Element Transfer Descriptor, defined in Section 3.5. The queue head utilizes the reserved fields of
    //  the page pointers defined in Figure 3-7 to implement tracking the state of split transactions"
    // Table 3-22. Host-Controller Rules for Bits in Overlay (DWords 5, 6, 8 and 9)
    // FIXME: Do this with less code duplication
    enum class PIDCode : u8 {
        OUT = 0b00,   // generates token (E1H)
        IN = 0b01,    // generates token (69H)
        SETUP = 0b10, // generates token (2DH)
    };
    // 3.5.1 Next qTD Pointer
    // Note: the type field is not evaluated here, as is ignored:
    //       "These bits are reserved and their value has no effect on operation."
    //       ~ Table 3-14. qTD Next Element Transfer Pointer (DWord 0)
    union {
        QueueElementTransferDescriptor overlay;
        struct {
            u32 : 32;
            u32 : 1;

            u32 nak_counter : 4;
            u32 : 27;
            u32 _[sizeof(QueueElementTransferDescriptor) / 4 - 2];
        };
    };
};
static_assert(AssertSize<QueueHead, 0x30>());

struct QueueHead64 : public QueueHead {
    u32 extended_buffer_pointer_list[5];
};
static_assert(AssertSize<QueueHead64, 0x44>());

// 3.7 Periodic Frame Span Traversal Node (FSTN)
struct FrameSpanTraversalNode {
    FrameListElementPointer normal_path_link_pointer;
    FrameListElementPointer back_path_link_pointer;
};

}
