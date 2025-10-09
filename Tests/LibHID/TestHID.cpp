/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibHID/ReportDescriptorParser.h>
#include <LibHID/ReportParser.h>
#include <LibTest/TestCase.h>

TEST_CASE(boot_protocol_keyboard)
{
    // HID Class Definition 1.11: B.1 Protocol 1 (Keyboard)
    static constexpr auto report_descriptor = to_array<u8>({
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x06,       // Usage (Keyboard)
        0xa1, 0x01,       // Collection (Application)
        0x75, 0x01,       //   Report Size (1)
        0x95, 0x08,       //   Report Count (8)
        0x05, 0x07,       //   Usage Page (Keyboard)        // The HID spec refers to this Usage Page as "Key Codes"
        0x19, 0xe0,       //   Usage Minimum (224)
        0x29, 0xe7,       //   Usage Maximum (231)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x01,       //   Logical Maximum (1)
        0x81, 0x02,       //   Input (Data, Variable, Absolute)
        0x95, 0x01,       //   Report Count (1)
        0x75, 0x08,       //   Report Size (8)
        0x81, 0x01,       //   Input (Constant)
        0x95, 0x05,       //   Report Count (5)
        0x75, 0x01,       //   Report Size (1)
        0x05, 0x08,       //   Usage Page (LEDs)
        0x19, 0x01,       //   Usage Minimum (1)
        0x29, 0x05,       //   Usage Maximum (5)
        0x91, 0x02,       //   Output (Data, Variable, Absolute)
        0x95, 0x01,       //   Report Count (1)
        0x75, 0x03,       //   Report Size (3)
        0x91, 0x01,       //   Output (Constant)
        0x95, 0x06,       //   Report Count (6)
        0x75, 0x08,       //   Report Size (8)
        0x15, 0x00,       //   Logical Minimum (0)
        0x26, 0xff, 0x00, //   Logical Maximum (255)
        0x05, 0x07,       //   Usage Page (Keyboard)        // The HID spec refers to this Usage Page as "Key Codes"
        0x19, 0x00,       //   Usage Minimum (0)
        0x29, 0xff,       //   Usage Maximum (255)
        0x81, 0x00,       //   Input (Data, Array)
        0xc0,             // End Collection
    });

    // HID Usage Tables 1.6: 10 Keyboard/Keypad Page (0x07)
    static constexpr Array<u8, 8> report = {
        0b1010'0101, // Modifier keys: LeftControl, LeftAlt, RightShift, Right GUI
        0x00,        // Reserved
        0x1a,        // Keycode 1: Keyboard w and W
        0x0b,        // Keycode 2: Keyboard h and H
        0x09,        // Keycode 3: Keyboard f and F
        0x1e,        // Keycode 4: Keyboard 1 and !
        0x43,        // Keycode 5: Keyboard F10
        0x00,        // Keycode 6: Reserved (no key pressed)
    };

    struct ExpectedFieldValue {
        u32 usage;
        i64 value;
    };

    static constexpr auto expected_field_values = to_array<ExpectedFieldValue>({
        { 0x0007'00e0, 1 }, // Keyboard LeftControl:      1
        { 0x0007'00e1, 0 }, // Keyboard LeftShift:        0
        { 0x0007'00e2, 1 }, // Keyboard LeftAlt:          1
        { 0x0007'00e3, 0 }, // Keyboard Left GUI:         0
        { 0x0007'00e4, 0 }, // Keyboard RightControl:     0
        { 0x0007'00e5, 1 }, // Keyboard RightShift:       1
        { 0x0007'00e6, 0 }, // Keyboard RightAlt:         0
        { 0x0007'00e7, 1 }, // Keyboard Right GUI:        1
        { 0x0007'001a, 1 }, // Keyboard w and W:          1
        { 0x0007'000b, 1 }, // Keyboard h and H:          1
        { 0x0007'0009, 1 }, // Keyboard f and F:          1
        { 0x0007'001e, 1 }, // Keyboard 1 and !:          1
        { 0x0007'0043, 1 }, // Keyboard F10:              1
        { 0x0007'0000, 1 }, // Reserved (no key pressed): 1
    });

    HID::ReportDescriptorParser parser { report_descriptor };
    auto parsed_descriptor = TRY_OR_FAIL(parser.parse());

    EXPECT_EQ(parsed_descriptor.application_collections.size(), 1uz);
    auto keyboard_application_collection = parsed_descriptor.application_collections[0];

    TRY_OR_FAIL(HID::parse_input_report(parsed_descriptor, keyboard_application_collection, report, [field_index = 0](HID::Field const& field, i64 value) mutable -> ErrorOr<IterationDecision> {
        u32 usage = 0;
        if (field.is_array) {
            EXPECT(field.usage_minimum.has_value());
            usage = value + field.usage_minimum.value();
            value = 1;
        } else {
            usage = field.usage.value();
        }

        EXPECT_EQ(usage, expected_field_values[field_index].usage);
        EXPECT_EQ(value, expected_field_values[field_index].value);

        field_index++;

        return IterationDecision::Continue;
    }));
}
