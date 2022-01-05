/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibUSBDB/Database.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/bus/usb", "r"));
    TRY(Core::System::unveil("/res/usb.ids", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    bool print_verbose = false;

    Core::ArgsParser args;
    args.set_general_help("List USB devices.");
    args.add_option(print_verbose, "Print verbose information about each device", "verbose", 'v');
    args.parse(arguments);

    Core::DirIterator usb_devices("/sys/bus/usb", Core::DirIterator::SkipDots);

    RefPtr<USBDB::Database> usb_db = USBDB::Database::open();
    if (!usb_db) {
        warnln("Failed to open usb.ids");
    }

    while (usb_devices.has_next()) {
        auto full_path = LexicalPath(usb_devices.next_full_path());

        auto proc_usb_device = Core::File::construct(full_path.string());
        if (!proc_usb_device->open(Core::OpenMode::ReadOnly)) {
            warnln("Failed to open {}: {}", proc_usb_device->name(), proc_usb_device->error_string());
            continue;
        }

        auto contents = proc_usb_device->read_all();
        auto json_or_error = JsonValue::from_string(contents);
        if (json_or_error.is_error()) {
            warnln("Failed to decode JSON: {}", json_or_error.error());
            continue;
        }
        auto json = json_or_error.release_value();

        json.as_array().for_each([usb_db, print_verbose](auto& value) {
            auto& device_json = value.as_object();

            auto device_address = device_json.get("device_address").to_u32();
            auto vendor_id = device_json.get("vendor_id").to_u32();
            auto product_id = device_json.get("product_id").to_u32();

            StringView vendor_string = usb_db->get_vendor(vendor_id);
            StringView device_string = usb_db->get_device(vendor_id, product_id);
            if (device_string.is_empty())
                device_string = "Unknown Device";

            outln("Device {}: ID {:04x}:{:04x} {} {}", device_address, vendor_id, product_id, vendor_string, device_string);

            if (print_verbose) {
                // Print out lots of information about the device
                outln("Device Descriptor");
                outln(" usb_spec_compliance_bcd: {:>15}", device_json.get("usb_spec_compliance_bcd").to_bcd());
                outln(" device_class: {:>22}", device_json.get("device_class").to_i32());
                outln(" device_sub_class: {:>18}", device_json.get("device_sub_class").to_i32());
                outln(" device_protocol: {:>19}", device_json.get("device_protocol").to_i32());
                outln(" max_packet_size: {:>19}", device_json.get("max_packet_size").to_i32());
                outln(" vendor_id: {:>24}{:04x} {}", "", device_json.get("vendor_id").to_u32(), vendor_string);
                outln(" product_id: {:>23}{:04x} {}", "", device_json.get("product_id").to_u32(), device_string);
                outln(" device_release_bcd: {:>22}", device_json.get("device_release_bcd").to_bcd());
                outln(" manufacturer_id_descriptor_index: {:>2}", device_json.get("manufacturer_id_descriptor_index").to_i32());
                outln(" product_string_descriptor_index: {:>3}", device_json.get("product_string_descriptor_index").to_i32());
                outln(" serial_number_descriptor_index: {:>4}", device_json.get("serial_number_descriptor_index").to_i32());
                outln(" num_configurations: {:>16}", device_json.get("num_configurations").to_i32());

                // Now print out the configuration descriptors
                const auto& configuration_descriptors = device_json.get("configuration_descriptors").as_array();
                configuration_descriptors.for_each([&](const JsonValue& configuration_descriptor) {
                    auto& descriptor_json_object = configuration_descriptor.as_object();

                    outln("Configuration Descriptor");
                    outln(" number_of_interfaces: {:>18}", descriptor_json_object.get("number_of_interfaces").to_i32());
                    outln(" configuration_value: {:>19}", descriptor_json_object.get("configuration_value").to_i32());
                    outln(" configuration_string_descriptor_index: {}", descriptor_json_object.get("configuration_string_descriptor_index"));
                    outln(" attributes_bitmap: {:>20}{:08b}", "", descriptor_json_object.get("attributes_bitmap").to_u32()); // TODO: Decode bitmap ala Linux
                    outln(" max_power_in_ma: {:>24}", descriptor_json_object.get("max_power_in_ma").to_u32() * 2);           // This value is in 2mA steps
                });
            }
        });
    }

    return 0;
}
