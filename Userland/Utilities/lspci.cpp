/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Hex.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
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
        ByteString(MUST(buf.slice(2, buf.size() - 2)).bytes()))
        .release_value();
}

static u32 convert_sysfs_value_to_uint(ByteString const& value)
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
        auto error = di.error();
        warnln("Failed to open /sys/bus/pci - {}", error);
        return error;
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

        auto vendor_id_filename = ByteString::formatted("/sys/bus/pci/{}/vendor", dir);
        auto vendor_id_file = Core::File::open(vendor_id_filename, Core::File::OpenMode::Read);
        if (vendor_id_file.is_error()) {
            dbgln("Error: Could not open {}: {}", vendor_id_filename, vendor_id_file.error());
            continue;
        }
        auto device_id_filename = ByteString::formatted("/sys/bus/pci/{}/device_id", dir);
        auto device_id_file = Core::File::open(device_id_filename, Core::File::OpenMode::Read);
        if (device_id_file.is_error()) {
            dbgln("Error: Could not open {}: {}", device_id_filename, device_id_file.error());
            continue;
        }
        auto class_id_filename = ByteString::formatted("/sys/bus/pci/{}/class", dir);
        auto class_id_file = Core::File::open(class_id_filename, Core::File::OpenMode::Read);
        if (class_id_file.is_error()) {
            dbgln("Error: Could not open {}: {}", class_id_filename, class_id_file.error());
            continue;
        }
        auto subclass_id_filename = ByteString::formatted("/sys/bus/pci/{}/subclass", dir);
        auto subclass_id_file = Core::File::open(subclass_id_filename, Core::File::OpenMode::Read);
        if (subclass_id_file.is_error()) {
            dbgln("Error: Could not open {}: {}", subclass_id_filename, subclass_id_file.error());
            continue;
        }
        auto revision_id_filename = ByteString::formatted("/sys/bus/pci/{}/revision", dir);
        auto revision_id_file = Core::File::open(revision_id_filename, Core::File::OpenMode::Read);
        if (revision_id_file.is_error()) {
            dbgln("Error: Could not open {}: {}", revision_id_filename, revision_id_file.error());
            continue;
        }

        auto vendor_id_contents = vendor_id_file.value()->read_until_eof();
        if (vendor_id_contents.is_error()) {
            dbgln("Error: Could not read {}: {}", vendor_id_filename, vendor_id_contents.error());
            continue;
        }
        u32 vendor_id = read_hex_string_from_bytebuffer(vendor_id_contents.value());

        auto device_id_contents = device_id_file.value()->read_until_eof();
        if (device_id_contents.is_error()) {
            dbgln("Error: Could not read {}: {}", device_id_filename, device_id_contents.error());
            continue;
        }
        u32 device_id = read_hex_string_from_bytebuffer(device_id_contents.value());

        auto revision_id_contents = revision_id_file.value()->read_until_eof();
        if (revision_id_contents.is_error()) {
            dbgln("Error: Could not read {}: {}", revision_id_filename, revision_id_contents.error());
            continue;
        }
        u32 revision_id = read_hex_string_from_bytebuffer(revision_id_contents.value());

        auto class_id_contents = class_id_file.value()->read_until_eof();
        if (class_id_contents.is_error()) {
            dbgln("Error: Could not read {}: {}", class_id_filename, class_id_contents.error());
            continue;
        }
        u32 class_id = read_hex_string_from_bytebuffer(class_id_contents.value());

        auto subclass_id_contents = subclass_id_file.value()->read_until_eof();
        if (subclass_id_contents.is_error()) {
            dbgln("Error: Could not read {}: {}", subclass_id_filename, subclass_id_contents.error());
            continue;
        }
        u32 subclass_id = read_hex_string_from_bytebuffer(subclass_id_contents.value());

        ByteString vendor_name;
        ByteString device_name;
        ByteString class_name;

        if (db) {
            vendor_name = db->get_vendor(vendor_id);
            device_name = db->get_device(vendor_id, device_id);
            class_name = db->get_class(class_id);
        }

        if (vendor_name.is_empty())
            vendor_name = ByteString::formatted("{:04x}", vendor_id);
        if (device_name.is_empty())
            device_name = ByteString::formatted("{:04x}", device_id);
        if (class_name.is_empty())
            class_name = ByteString::formatted("{:02x}{:02x}", class_id, subclass_id);

        outln(format, domain, bus, device, function, class_name, vendor_name, device_name, revision_id);

        if (!flag_verbose)
            continue;
        for (size_t bar_index = 0; bar_index <= 5; bar_index++) {
            auto bar_value_filename = ByteString::formatted("/sys/bus/pci/{}/bar{}", dir, bar_index);
            auto bar_value_file = Core::File::open(bar_value_filename, Core::File::OpenMode::Read);
            if (bar_value_file.is_error()) {
                dbgln("Error: Could not open {}: {}", bar_value_filename, bar_value_file.error());
                continue;
            }

            auto bar_value_contents = bar_value_file.value()->read_until_eof();
            if (bar_value_contents.is_error()) {
                dbgln("Error: Could not read {}: {}", bar_value_filename, bar_value_contents.error());
                continue;
            }

            u32 bar_value = read_hex_string_from_bytebuffer(bar_value_contents.value());
            if (bar_value == 0)
                continue;
            bool memory_region = ((bar_value & 1) == 0);
            outln(format_region, bar_index, memory_region ? "Memory" : "IO", bar_value);
        }
    }

    return 0;
}
