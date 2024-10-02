/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SCSIVitalProductData.h"

#include <AK/Endian.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

// https://www.seagate.com/files/staticfiles/support/docs/manual/Interface%20manuals/100293068j.pdf

namespace Kernel::SCSI {

// 2.4.1.2
struct FixedFormatSenseData {
    u8 response_code : 7;
    u8 valid : 1;
    u8 obsolete;
    u8 sense_key : 4;
    u8 : 1;
    u8 incorrect_length_indicator : 1; // ILI
    u8 end_of_medium : 1;              // EOM
    u8 file_mark : 1;
    BigEndian<u32> information;
    u8 additional_sense_length;
    BigEndian<u32> command_specific_information;
    u8 additional_sense_code;
    u8 additional_sense_code_qualifier;
    u8 field_replaceable_unit_code;
    u8 sense_key_specific[3];
    u8 additional_sense_bytes[];
};
static_assert(AssertSize<FixedFormatSenseData, 18>());

// 3.6.1
struct Inquiry {
    u8 opcode { 0x12 };
    u8 enable_vital_product_data { 0 }; // EVPD, Also CMDDT in the second bit, but thats obsolete
    VitalProductDataPageCode page_code { VitalProductDataPageCode::SupportedVitalProductDataPages };
    BigEndian<u16> allocation_length;
    u8 control { 0 };
};
static_assert(AssertSize<Inquiry, 6>());

// 3.6.2
struct StandardInquiryData {
    // Table 61
    enum class DeviceType : u8 {
        DirectAccessBlockDevice = 0x00,
        SequentialAccessDevice = 0x01,
        PrinterDevice = 0x02,
        ProcessorDevice = 0x03,
        WriteOnceDevice = 0x04,
        CDDVDDevice = 0x05,
        // 0x06 was Scanner device
        OpticalMemoryDevice = 0x07,
        MediumChangerDevice = 0x08,
        // 0x09 was Communications device
        // 0x0A-0x0B are obsolete
        StorageArrayControllerDevice = 0x0C,
        EnclosureServicesDevice = 0x0D,
        SimplifiedDirectAccessDevice = 0x0E,
        OpticalCardReaderWriterDevice = 0x0F,
        BridgeControllerCommands = 0x10,
        ObjectBasedStorageDevice = 0x11,
        AutomationDriveInterface = 0x12,
        // 0x13-0x1D are reserved
        WellKnownLogicalUnit = 0x1E,
        UnknownOrNoDeviceType = 0x1F,
    };
    struct {
        DeviceType device_type : 5;
        u8 qualifier : 3;
    } peripheral_info;
    u8 removable; // 0x80 for removable, 0x00 for fixed, The remaining bits used to be the device qualifier (SCSI-1)
    u8 version;
    struct {
        u8 response_data_format : 4;
        u8 hierarchical_support : 1; // HISUP
        u8 normal_aca_support : 1;   // NORMACA
        u8 obsolete : 2;
    } response_data;
    u8 additional_length; // N-4
    struct {
        u8 protect : 1;
        u8 : 2;
        u8 third_party_copy : 1;
        u8 target_port_group_support : 2;
        u8 access_control_coordinator : 1;
        u8 scc_support : 1;

        u8 : 4;
        u8 multi_port : 1;
        u8 vendor_specific1 : 1;
        u8 enclosure_services : 1;
        u8 : 1;

        u8 vendor_specific2 : 1;
        u8 command_queueing : 1;
        u8 : 6;
    } capabilities;

    char vendor_id[8]; // These are padded with spaces
    char product_id[16];
    char product_revision_level[4];

    StringView device_type_string() const
    {
        switch (peripheral_info.device_type) {
        case DeviceType::DirectAccessBlockDevice:
            return "Direct Access Block Device"sv;
        case DeviceType::SequentialAccessDevice:
            return "Sequential Access Device"sv;
        case DeviceType::PrinterDevice:
            return "Printer Device"sv;
        case DeviceType::ProcessorDevice:
            return "Processor Device"sv;
        case DeviceType::WriteOnceDevice:
            return "Write Once Device"sv;
        case DeviceType::CDDVDDevice:
            return "CD/DVD Device"sv;
        case DeviceType::OpticalMemoryDevice:
            return "Optical Memory Device"sv;
        case DeviceType::MediumChangerDevice:
            return "Medium Changer Device"sv;
        case DeviceType::StorageArrayControllerDevice:
            return "Storage Array Controller Device"sv;
        case DeviceType::EnclosureServicesDevice:
            return "Enclosure Services Device"sv;
        case DeviceType::SimplifiedDirectAccessDevice:
            return "Simplified Direct Access Device"sv;
        case DeviceType::OpticalCardReaderWriterDevice:
            return "Optical Card Reader/Writer Device"sv;
        case DeviceType::BridgeControllerCommands:
            return "Bridge Controller Commands"sv;
        case DeviceType::ObjectBasedStorageDevice:
            return "Object Based Storage Device"sv;
        case DeviceType::AutomationDriveInterface:
            return "Automation Drive Interface"sv;
        case DeviceType::WellKnownLogicalUnit:
            return "Well Known Logical Unit"sv;
        case DeviceType::UnknownOrNoDeviceType:
            return "Canonical Unknown or No Device Type"sv;
        default:
            return "Unknown Device Type"sv;
        }
    }
};
static_assert(AssertSize<StandardInquiryData, 36>());

// 3.16
struct Read10 {
    u8 operation_code { 0x28 };
    union {
        u8 settings { 0 };
        struct {
            u8 obsolete : 2;
            u8 rarc : 1;
            u8 fua : 1;
            u8 dpo : 1;
            u8 rdprotect : 3;
        };
    };
    BigEndian<u32> logical_block_address;
    u8 group_number { 0 }; // only bottom 5 bits
    BigEndian<u16> transfer_length;
    u8 control { 0 };
};
static_assert(AssertSize<Read10, 10>());
// 3.22.1
struct ReadCapacity10 {
    u8 opcode { 0x25 };
    u8 reserved1 { 0 };
    BigEndian<u32> obsolete_logical_block_address { 0 };
    u16 reserved2 { 0 };
    u8 reserved3 { 0 };
    u8 control { 0 };
};
static_assert(AssertSize<ReadCapacity10, 10>());
// 3.22.2
struct ReadCapacity10Parameters {
    BigEndian<u32> block_count;
    BigEndian<u32> block_size;
};
static_assert(AssertSize<ReadCapacity10Parameters, 8>());

// 3.33
struct ReportLUNs {
    u8 opcode { 0xA0 };
    u8 reserved { 0 };
    u8 select_report { 0 }; // FIXME: Support this
    u8 reserved2[3] { 0 };
    BigEndian<u32> allocation_length;
    u8 reserved3 { 0 };
    u8 control { 0 };
};
static_assert(AssertSize<ReportLUNs, 12>());

struct ReportLUNsParameterData {
    BigEndian<u32> lun_list_length;
    u8 reserved[4] { 0 };
    BigEndian<u64> lun_list[];
};

// 3.37
struct RequestSense {
    u8 opcode { 0x03 };
    u8 descriptor_format { 0 }; // 0 for fixed format, 1 for descriptor format
    u8 reserved[2] { 0 };
    u8 allocation_length;
    u8 control { 0 };
};
static_assert(AssertSize<RequestSense, 6>());

// 3.53
struct TestUnitReady {
    u8 opcode { 0x00 };
    u8 reserved[4] { 0 };
    u8 control { 0 };
};
static_assert(AssertSize<TestUnitReady, 6>());

// 3.60
struct Write10 {
    u8 operation_code { 0x2A };
    union {
        u8 settings { 0 };
        struct {
            u8 obsolete : 2;
            u8 reserved : 1;
            u8 fua : 1;
            u8 dpo : 1;
            u8 wrprotect : 3;
        };
    };
    BigEndian<u32> logical_block_address;
    u8 group_number { 0 }; // only bottom 5 bits
    BigEndian<u16> transfer_length;
    u8 control { 0 };
};
static_assert(AssertSize<Read10, 10>());
}
