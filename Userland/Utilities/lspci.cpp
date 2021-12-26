/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibPCIDB/Database.h>
#include <stdio.h>
#include <unistd.h>

static bool flag_show_numerical = false;

static const char* format_numerical = "{:04x}:{:02x}:{:02x}.{} {}: {}:{} (rev {:02x})";
static const char* format_textual = "{:04x}:{:02x}:{:02x}.{} {}: {} {} (rev {:02x})";

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res/pci.ids", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/proc/pci", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    Core::ArgsParser args_parser;
    args_parser.set_general_help("List PCI devices.");
    args_parser.add_option(flag_show_numerical, "Show numerical IDs", "numerical", 'n');
    args_parser.parse(argc, argv);

    const char* format = flag_show_numerical ? format_numerical : format_textual;

    RefPtr<PCIDB::Database> db;
    if (!flag_show_numerical) {
        db = PCIDB::Database::open();
        if (!db) {
            warnln("Couldn't open PCI ID database");
            flag_show_numerical = true;
        }
    }

    auto proc_pci = Core::File::construct("/proc/pci");
    if (!proc_pci->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", proc_pci->name(), proc_pci->error_string());
        return 1;
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto file_contents = proc_pci->read_all();
    auto json = JsonValue::from_string(file_contents);
    VERIFY(json.has_value());
    json.value().as_array().for_each([db, format](auto& value) {
        auto& dev = value.as_object();
        auto seg = dev.get("seg").to_u32();
        auto bus = dev.get("bus").to_u32();
        auto device = dev.get("device").to_u32();
        auto function = dev.get("function").to_u32();
        auto vendor_id = dev.get("vendor_id").to_u32();
        auto device_id = dev.get("device_id").to_u32();
        auto revision_id = dev.get("revision_id").to_u32();
        auto class_id = dev.get("class").to_u32();
        auto subclass_id = dev.get("subclass").to_u32();

        String vendor_name;
        String device_name;
        String class_name;

        if (db) {
            vendor_name = db->get_vendor(vendor_id);
            device_name = db->get_device(vendor_id, device_id);
            class_name = db->get_class(class_id);
        }

        if (vendor_name.is_empty())
            vendor_name = String::formatted("{:04x}", vendor_id);
        if (device_name.is_empty())
            device_name = String::formatted("{:04x}", device_id);
        if (class_name.is_empty())
            class_name = String::formatted("{:02x}{:02x}", class_id, subclass_id);

        outln(format, seg, bus, device, function, class_name, vendor_name, device_name, revision_id);
    });

    return 0;
}
