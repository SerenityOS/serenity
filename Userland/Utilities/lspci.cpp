/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibPCIDB/Database.h>

static bool flag_show_numerical = false;

static const char* format_numerical = "{:04x}:{:02x}:{:02x}.{} {}: {}:{} (rev {:02x})";
static const char* format_textual = "{:04x}:{:02x}:{:02x}.{} {}: {} {} (rev {:02x})";

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/res/pci.ids", "r"));
    TRY(Core::System::unveil("/proc/pci", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::ArgsParser args_parser;
    args_parser.set_general_help("List PCI devices.");
    args_parser.add_option(flag_show_numerical, "Show numerical IDs", "numerical", 'n');
    args_parser.parse(arguments);

    const char* format = flag_show_numerical ? format_numerical : format_textual;

    RefPtr<PCIDB::Database> db;
    if (!flag_show_numerical) {
        db = PCIDB::Database::open();
        if (!db) {
            warnln("Couldn't open PCI ID database");
            flag_show_numerical = true;
        }
    }

    auto proc_pci = TRY(Core::File::open("/proc/pci", Core::OpenMode::ReadOnly));

    TRY(Core::System::pledge("stdio"));

    auto file_contents = proc_pci->read_all();
    auto json = TRY(JsonValue::from_string(file_contents));
    json.as_array().for_each([db, format](auto& value) {
        auto& dev = value.as_object();
        auto domain = dev.get("domain").to_u32();
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

        outln(format, domain, bus, device, function, class_name, vendor_name, device_name, revision_id);
    });

    return 0;
}
