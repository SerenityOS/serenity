/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>
#include <Kernel/Devices/Storage/USB/SCSICodes.h>

namespace Kernel::USB {
// According to ISO/IEC 14776-251 or the t10 UAS specification

// 5.3.3.5 Pipe Usage Descriptor
// Note: This should be 5.2.3.5, but the 2014 revision of the spec has a typo

// Table 8 Pipe ID
enum class PipeID : u8 {
    // Reserved: 0x00
    CommandPipe = 0x01,
    StatusPipe = 0x02,
    DataInPipe = 0x03,
    DataOutPipe = 0x04,
    // Reserved: 0x05 - 0xDF
    // Vendor Specific: 0xE0 - 0xEF
    // Reserved: 0xF0 - 0xFF
};
// Table 7
constexpr u8 UAS_PIPE_USAGE_DESCRIPTOR = 0x24;
struct PipeUsageDescriptor {
    u8 descriptor_length;
    u8 descriptor_type;
    PipeID pipe_id;
    u8 reserved;
};

// 6.2 IUs
// Table 9
enum class IUID : u8 {
    // Reserved: 0x00
    Command = 0x01,
    // Reserved: 0x02
    Sense = 0x03,
    Response = 0x04,
    TaskManagement = 0x05,
    ReadReady = 0x06,
    WriteReady = 0x07,
    // Reserved: 0x08 - 0x0F
};

// Table 10
struct InformationUnitHeader {
    IUID iu_id;
    u8 reserved { 0 };
    BigEndian<u16> tag;
};
static_assert(AssertSize<InformationUnitHeader, 4>());

// 6.2.2 Command IU
// Table 12
enum class TaskAttribute : u8 {
    Simple = 0b000,
    HeadOfQueue = 0b001,
    Ordered = 0b010,
    // Reserved: 0b011
    ACA = 0b100,
    // Reserved: 0b101 - 0b111
};

// Table 11
struct CommandIU {
    InformationUnitHeader header;
    struct {
        TaskAttribute attribute : 3;
        u8 priority : 4;
        u8 reserved : 1;
    } task_info;
    u8 reserved_0 { 0 };
    u8 additional_cbd_length; // must be multiple of 4
    u8 reserved_1 { 0 };
    BigEndian<u64> lun;
    u8 cdb[16];
    u8 additional_cbd_bytes[]; // indicated by additional_cbd_length -> multiple dwords

    template<typename Command>
    void set_command(Command const& command)
    {
        // FIXME:
        static_assert(sizeof(command) <= sizeof(cdb), "Command too large for CommandIU without additional_cbd_bytes");
        memcpy(cdb, &command, min(sizeof(command), sizeof(cdb)));
        additional_cbd_length = 0;
    }
};
static_assert(AssertSize<CommandIU, 32>());

// 6.2.3 Read Ready IU
// Table 13
struct ReadReadyIU {
    InformationUnitHeader header;
};
static_assert(AssertSize<ReadReadyIU, 4>());

// 6.2.4 Write Ready IU
// Table 14
struct WriteReadyIU {
    InformationUnitHeader header;
};
static_assert(AssertSize<WriteReadyIU, 4>());

// 6.2.5 Sense IU
// Table 15
struct SenseIU {
    InformationUnitHeader header;
    BigEndian<u16> status_qualifier; // See SAM-4
    SCSI::StatusCode status;
    u8 reserved[7];
    BigEndian<u16> length; // FIXME: The spec does not actually state the endianness of this?
    u8 sense_data[];
};
static_assert(AssertSize<SenseIU, 16>());

// 6.2.6 Response IU
// Table 17
enum class ResponseCode : u8 {
    // TM: On Task Management IUs
    // Command: On Command IUs
    TaskManagementFunctionComplete = 0x00, // TM
    // Reserved: 0x01
    InvalidIU = 0x02, // TM, Command
    // Reserved: 0x03
    TaskManagementFunctionNotSupported = 0x04, // TM
    TaskManagementFunctionFailed = 0x05,       // TM
    // Reserved: 0x06 - 0x07
    TaskManagementFunctionSucceeded = 0x08, // TM
    IncorrectLUN = 0x09,                    // TM
    OverlappedTagAttempted = 0x0A,          // TM, Command
    // Reserved: 0x0B - 0x0F
};

// Table 16
struct ResponseIU {
    InformationUnitHeader header;
    u8 additional_response_info[3];
    ResponseCode response_code;
};
static_assert(AssertSize<ResponseIU, 8>());

// 6.2.7 Task Management IU
// Table 19
enum class TaskManagementFunction : u8 {
    // LUN: lun field used
    // TOTTBM: tag_of_task_to_be_managed field used
    // Reserved: 0x00
    AbortTask = 0x01,    // LUN, TOTTBM
    AbortTaskSet = 0x02, // LUN
    // Reserved: 0x03
    ClearTaskSet = 0x04, // LUN
    // Reserved: 0x05 - 0x07
    LogicalUnitReset = 0x08, // LUN
    // Reserved: 0x09 - 0x0F
    ITNexusReset = 0x10,
    // Reserved: 0x11 - 0x3F
    ClearACA = 0x40, // LUN
    // Reserved: 0x41 - 0x7F
    QueryTask = 0x80,               // LUN, TOTTBM
    QueryTaskSet = 0x81,            // LUN
    QueryAsynchrounousEvent = 0x82, // LUN
    // Reserved: 0x83 - 0xFF
};

// Table 18
struct TaskManagementIU {
    InformationUnitHeader header;
    u8 function;
    u8 reserved { 0 };
    BigEndian<u16> tag_of_task_to_be_managed;
    BigEndian<u64> lun;
};

}
