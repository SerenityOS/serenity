/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/ByteBuffer.h>
#include <YAK/JsonArray.h>
#include <YAK/JsonObject.h>
#include <YAK/LexicalPath.h>
#include <YAK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibUSBDB/Database.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/sys/bus/usb", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res/usb.ids", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    Core::ArgsParser args;
    args.set_general_help("List USB devices.");
    args.parse(argc, argv);

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
        auto json = JsonValue::from_string(contents);
        VERIFY(json.has_value());

        json.value().as_array().for_each([usb_db](auto& value) {
            auto& device_descriptor = value.as_object();

            auto device_address = device_descriptor.get("device_address").to_u32();
            auto vendor_id = device_descriptor.get("vendor_id").to_u32();
            auto product_id = device_descriptor.get("product_id").to_u32();

            StringView vendor_string = usb_db->get_vendor(vendor_id);
            StringView device_string = usb_db->get_device(vendor_id, product_id);
            if (device_string.is_empty())
                device_string = "Unknown Device";

            outln("Device {}: ID {:04x}:{:04x} {} {}", device_address, vendor_id, product_id, vendor_string, device_string);
        });
    }

    return 0;
}
