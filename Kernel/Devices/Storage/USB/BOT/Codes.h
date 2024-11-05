/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>

namespace Kernel::USB::MassStorage {
// https://www.usb.org/sites/default/files/Mass_Storage_Specification_Overview_v1.4_2-19-2010.pdf
// 2
enum class SubclassCode : u8 {
    NotReported = 0x00,
    RBC = 0x01,
    MMC5 = 0x02, // ATAPI
    Obsolete_QIC157 = 0x03,
    UFI = 0x04, // Floppy
    Obsolete_SFF8070i = 0x05,
    SCSI_transparent = 0x06,
    LSD_FS = 0x07,
    IEEE1667 = 0x08,
    // Reserved: 0x09 - 0xFE
    VendorSpecific = 0xFF
};

constexpr StringView subclass_string(SubclassCode code)
{
    switch (code) {
    case SubclassCode::NotReported:
        return "Not Reported"sv;
    case SubclassCode::RBC:
        return "RBC"sv;
    case SubclassCode::MMC5:
        return "MMC-5 (ATAPI)"sv;
    case SubclassCode::Obsolete_QIC157:
        return "QIC157 (Obsolete)"sv;
    case SubclassCode::UFI:
        return "UFI"sv;
    case SubclassCode::Obsolete_SFF8070i:
        return "SFF8070i (Obsolete)"sv;
    case SubclassCode::SCSI_transparent:
        return "SCSI-transparent"sv;
    case SubclassCode::LSD_FS:
        return "LSD FS"sv;
    case SubclassCode::IEEE1667:
        return "IEEE1667"sv;
    case SubclassCode::VendorSpecific:
        return "Vendor Specific"sv;
    }

    return "Reserved"sv;
}

// 3
enum class TransportProtocol : u8 {
    CBI_completion_interrupt = 0x00,    // Control/Bulk/Interrupt
    CBI_no_completion_interrupt = 0x01, // Control/Bulk/Interrupt
    Obsolete = 0x02,
    // Reserved: 0x03 - 0x4F
    BBB = 0x50, // Bulk-only
    // Reserved: 0x51 - 0x61
    UAS = 0x62,
    // Reserved: 0x63 - 0xFE
    VendorSpecific = 0xFF
};

constexpr StringView transport_protocol_string(TransportProtocol protocol)
{
    switch (protocol) {
    case TransportProtocol::CBI_completion_interrupt:
        return "Control/Bulk/Interrupt with completion interrupt"sv;
    case TransportProtocol::CBI_no_completion_interrupt:
        return "Control/Bulk/Interrupt without completion interrupt"sv;
    case TransportProtocol::Obsolete:
        return "Obsolete"sv;
    case TransportProtocol::BBB:
        return "Bulk only"sv;
    case TransportProtocol::UAS:
        return "UAS"sv;
    case TransportProtocol::VendorSpecific:
        return "Vendor Specific"sv;
    }

    return "Reserved"sv;
}

// 4
enum class RequestCodes : u8 {
    ADSC = 0x00, // Accept Device Specific Command (CBI) - also alias USB-request 00h Get Status
    // Reserved/alias USB-bRequest: 0x01 - 0x0D
    // Reserved: 0x0E - 0XFB
    GetRequest = 0xFC,
    PutRequest = 0xFD,
    GetMaxLun = 0xFE,               // GML (BBB)
    BulkOnlyMassStorageReset = 0xFF // BOMSR (BBB)
};

// 5
enum class ClassSpecificDescriptorCodes : u8 {
    // Undefined by Mass Storage: 0x00 - 0x23
    PipeUsageClassSpecific = 0x24 // UAS
    // Undefined by Mass Storage: 0x25 - 0xFF
};

}
