/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Hex.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibPCIDB/Database.h>

static bool flag_show_numerical = false;
static bool flag_verbose = false;

static constexpr StringView format_numerical = "{:04x}:{:02x}:{:02x}.{} {}: {}:{} (rev {:02x})"sv;
static constexpr StringView format_textual = "{:04x}:{:02x}:{:02x}.{} {}: {} {} (rev {:02x})"sv;
static constexpr StringView format_region = "\tBAR {}: {} region @ {:#x}"sv;

static u32 read_hex_string_from_bytebuffer(ByteBuffer const& buf)
{
    // FIXME: Propagate errors.
    return AK::StringUtils::convert_to_uint_from_hex(
        String(MUST(buf.slice(2, buf.size() - 2)).bytes()))
        .release_value();
}

static u32 convert_sysfs_value_to_uint(String const& value)
{
    if (auto result = AK::StringUtils::convert_to_uint_from_hex(value); result.has_value())
        return result.release_value();
    if (auto result = AK::StringUtils::convert_to_uint(value); result.has_value())
        return result.release_value();
    VERIFY_NOT_REACHED();
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;
    args_parser.set_general_help("List PCI devices.");
    args_parser.add_option(flag_show_numerical, "Show numerical IDs", "numerical", 'n');
    args_parser.add_option(flag_verbose, "Show verbose info on devices", "verbose", 'v');
    args_parser.parse(arguments);

    if (!flag_show_numerical)
        TRY(Core::System::unveil("/res/pci.ids", "r"));
    TRY(Core::System::unveil("/sys/bus/pci", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto const format = flag_show_numerical ? format_numerical : format_textual;

    RefPtr<PCIDB::Database> db;
    if (!flag_show_numerical) {
        db = PCIDB::Database::open();
        if (!db) {
            warnln("Couldn't open PCI ID database");
            flag_show_numerical = true;
        }
    }

    Core::DirIterator di("/sys/bus/pci/", Core::DirIterator::SkipParentAndBaseDir);
    if (di.has_error()) {
        warnln("Failed to open /sys/bus/pci - {}", di.error());
        return 1;
    }

    TRY(Core::System::pledge("stdio rpath"));

    while (di.has_next()) {
        auto dir = di.next_path();
        auto domain_bus_device_parts = dir.split(':');
        VERIFY(domain_bus_device_parts.size() == 3);
        auto domain = convert_sysfs_value_to_uint(domain_bus_device_parts[0]);
        auto bus = convert_sysfs_value_to_uint(domain_bus_device_parts[1]);
        auto device = convert_sysfs_value_to_uint(domain_bus_device_parts[2].split('.')[0]);

        auto function_parts = dir.split('.');
        VERIFY(function_parts.size() == 2);
        auto function = convert_sysfs_value_to_uint(function_parts[1]);

        auto vendor_id_file = Core::File::construct(String::formatted("/sys/bus/pci/{}/vendor", dir));
        if (!vendor_id_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", vendor_id_file->name(), vendor_id_file->error_string());
            continue;
        }
        auto device_id_file = Core::File::construct(String::formatted("/sys/bus/pci/{}/device_id", dir));
        if (!device_id_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", device_id_file->name(), device_id_file->error_string());
            continue;
        }
        auto class_id_file = Core::File::construct(String::formatted("/sys/bus/pci/{}/class", dir));
        if (!class_id_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", class_id_file->name(), class_id_file->error_string());
            continue;
        }
        auto subclass_id_file = Core::File::construct(String::formatted("/sys/bus/pci/{}/subclass", dir));
        if (!subclass_id_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", subclass_id_file->name(), subclass_id_file->error_string());
            continue;
        }
        auto revision_id_file = Core::File::construct(String::formatted("/sys/bus/pci/{}/revision", dir));
        if (!revision_id_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", revision_id_file->name(), revision_id_file->error_string());
            continue;
        }

        u32 vendor_id = read_hex_string_from_bytebuffer(vendor_id_file->read_all());
        u32 device_id = read_hex_string_from_bytebuffer(device_id_file->read_all());
        u32 revision_id = read_hex_string_from_bytebuffer(revision_id_file->read_all());
        u32 class_id = read_hex_string_from_bytebuffer(class_id_file->read_all());
        u32 subclass_id = read_hex_string_from_bytebuffer(subclass_id_file->read_all());

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

        if (!flag_verbose)
            continue;
        for (size_t bar_index = 0; bar_index <= 5; bar_index++) {
            auto bar_value_file = Core::File::construct(String::formatted("/sys/bus/pci/{}/bar{}", dir, bar_index));
            if (!bar_value_file->open(Core::OpenMode::ReadOnly)) {
                dbgln("Error: Could not open {}: {}", bar_value_file->name(), bar_value_file->error_string());
                continue;
            }
            u32 bar_value = read_hex_string_from_bytebuffer(bar_value_file->read_all());
            if (bar_value == 0)
                continue;
            bool memory_region = ((bar_value & 1) == 0);
            outln(format_region, bar_index, memory_region ? "Memory" : "IO", bar_value);
        }
    }

    return 0;
}
