/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Types.h>

// https://learn.microsoft.com/en-us/previous-versions/windows/hardware/design/dn642101(v=vs.85)

namespace Kernel::I2C {

// 5.1.1 HID Descriptor Format

struct HIDDescriptor {
    LittleEndian<u16> hid_descriptor_length;      // wHIDDescLength:      "The length, in unsigned bytes, of the complete Hid Descriptor"
    LittleEndian<u16> version;                    // bcdVersion:          "The version number, in binary coded decimal (BCD) format. DEVICE should default to 0x0100"
    LittleEndian<u16> report_descriptor_length;   // wReportDescLength:   "The length, in unsigned bytes, of the Report Descriptor."
    LittleEndian<u16> report_descriptor_register; // wReportDescRegister: "The register index containing the Report Descriptor on the DEVICE."
    LittleEndian<u16> input_register;             // wInputRegister:      "This field identifies, in unsigned bytes, the register number to read the input report from the DEVICE."
    LittleEndian<u16> max_input_length;           // wMaxInputLength:     "This field identifies in unsigned bytes the length of the largest Input Report to be read from the Input Register (Complex HID Devices will need various sized reports)."
    LittleEndian<u16> output_register;            // wOutputRegister:     "This field identifies, in unsigned bytes, the register number to send the output report to the DEVICE."
    LittleEndian<u16> max_output_length;          // wMaxOutputLength:    "This field identifies in unsigned bytes the length of the largest output Report to be sent to the Output Register (Complex HID Devices will need various sized reports)."
    LittleEndian<u16> command_register;           // wCommandRegister:    "This field identifies, in unsigned bytes, the register number to send command requests to the DEVICE"
    LittleEndian<u16> data_register;              // wDataRegister:       "This field identifies in unsigned bytes the register number to exchange data with the Command Request"
    LittleEndian<u16> vendor_id;                  // wVendorID:           "This field identifies the DEVICE manufacturers Vendor ID. Must be non-zero."
    LittleEndian<u16> product_id;                 // wProductID:          "This field identifies the DEVICE’s unique model / Product ID."
    LittleEndian<u16> version_id;                 // wVersionID:          "This field identifies the DEVICE’s firmware revision number."
    u8 _[4];                                      // RESERVED:            "This field is reserved and should be set to 0."
};
static_assert(AssertSize<HIDDescriptor, 30>());

}
