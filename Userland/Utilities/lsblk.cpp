/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Hex.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static constexpr StringView format_row = "{:10s}\t{:10s}\t{:10s}\t{:10s}\t{:10s}"sv;
static constexpr StringView format_partition_row = "{:10s}\t{:10s}\t{:10s}\t{:10s}\t{:10s}\t{}"sv;

static constexpr StringView format_uuid_partition_row = "{:10s}\t{:10s}\t{:37s}\t{:10s}\t{:10s}\t{:37s}"sv;
static constexpr StringView format_uuid_partition_sub_row = "├ {:10s}\t{:10s}\t{:37s}\t{:10s}\t{:10s}\t{:37s}"sv;
static constexpr StringView format_uuid_end_partition_sub_row = "└ {:10s}\t{:10s}\t{:37s}\t{:10s}\t{:10s}\t{:37s}"sv;

static ErrorOr<String> resolve_symlink(String symlink_path)
{
    String symlink_target;
    LexicalPath lexical_symlink_path(symlink_path);

    symlink_target = TRY(Core::File::read_link(symlink_path));
    return LexicalPath::absolute_path(lexical_symlink_path.parent().string(), symlink_target);
}

static ErrorOr<String> resolve_storage_device_partition_symlink_to_directory_path(StringView storage_device_directory, StringView partition_dir)
{
    auto storage_device_partition_symlink_path = TRY(resolve_symlink(String::formatted("/sys/devices/storage/physical/{}/partitions/{}", storage_device_directory, partition_dir)));
    if (storage_device_partition_symlink_path.is_null()) {
        return Error::from_string_view("readlink failed"sv);
    }

    auto storage_device_partition_path = TRY(resolve_symlink(storage_device_partition_symlink_path));
    if (storage_device_partition_path.is_null()) {
        return Error::from_string_view("readlink failed"sv);
    }
    return storage_device_partition_path;
}

static bool guid_partitions_for_storage_device(StringView storage_device_directory)
{
    Core::DirIterator partitions_di(String::formatted("/sys/devices/storage/physical/{}/partitions", storage_device_directory), Core::DirIterator::SkipParentAndBaseDir);
    if (partitions_di.has_error()) {
        warnln("Failed to open /sys/devices/storage/physical/{}/partitions - {}", storage_device_directory, partitions_di.error());
        return false;
    }
    if (partitions_di.has_next()) {
        auto partition_dir = partitions_di.next_path();
        auto storage_device_partition_path_or_error = resolve_storage_device_partition_symlink_to_directory_path(storage_device_directory, partition_dir);
        if (storage_device_partition_path_or_error.is_error())
            return false;
        auto storage_device_partition_path = storage_device_partition_path_or_error.release_value();

        auto uuid_file = Core::File::construct(LexicalPath::absolute_path(storage_device_partition_path, "uuid"));
        if (uuid_file->open(Core::OpenMode::ReadOnly)) {
            return true;
        }
    }
    return false;
}

static void print_storage_device_partitions(StringView storage_device_directory)
{
    Core::DirIterator partitions_di(String::formatted("/sys/devices/storage/physical/{}/partitions", storage_device_directory), Core::DirIterator::SkipParentAndBaseDir);
    if (partitions_di.has_error()) {
        warnln("Failed to open /sys/devices/storage/physical/{}/partitions - {}", storage_device_directory, partitions_di.error());
        return;
    }
    while (partitions_di.has_next()) {
        auto partition_dir = partitions_di.next_path();

        auto storage_device_partition_path_or_error = resolve_storage_device_partition_symlink_to_directory_path(storage_device_directory, partition_dir);
        if (storage_device_partition_path_or_error.is_error())
            continue;
        auto storage_device_partition_path = storage_device_partition_path_or_error.release_value();

        auto attributes_file = Core::File::construct(LexicalPath::absolute_path(storage_device_partition_path, "attributes"));
        if (!attributes_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", attributes_file->name(), attributes_file->error_string());
            continue;
        }
        auto partition_type_file = Core::File::construct(LexicalPath::absolute_path(storage_device_partition_path, "partition_type"));
        if (!partition_type_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", partition_type_file->name(), partition_type_file->error_string());
            continue;
        }
        auto end_lba_file = Core::File::construct(LexicalPath::absolute_path(storage_device_partition_path, "end_lba"));
        if (!end_lba_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", end_lba_file->name(), end_lba_file->error_string());
            continue;
        }
        auto start_lba_file = Core::File::construct(LexicalPath::absolute_path(storage_device_partition_path, "start_lba"));
        if (!start_lba_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", start_lba_file->name(), start_lba_file->error_string());
            continue;
        }

        String attributes = StringView(attributes_file->read_all().bytes());
        String partition_type = StringView(partition_type_file->read_all().bytes());
        String end_lba = StringView(end_lba_file->read_all().bytes());
        String start_lba = StringView(start_lba_file->read_all().bytes());

        auto uuid_file = Core::File::construct(LexicalPath::absolute_path(storage_device_partition_path, "uuid"));
        if (!uuid_file->open(Core::OpenMode::ReadOnly)) {
            if (partitions_di.has_next())
                outln(format_uuid_partition_sub_row, partition_dir, attributes, partition_type, start_lba, end_lba, "n/a"sv);
            else
                outln(format_uuid_end_partition_sub_row, partition_dir, attributes, partition_type, start_lba, end_lba, "n/a"sv);
        } else {
            String uuid = StringView(uuid_file->read_all().bytes());
            if (partitions_di.has_next())
                outln(format_uuid_partition_sub_row, partition_dir, attributes, partition_type, start_lba, end_lba, uuid);
            else
                outln(format_uuid_end_partition_sub_row, partition_dir, attributes, partition_type, start_lba, end_lba, uuid);
        }
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/dev/block", "r"));
    TRY(Core::System::unveil("/sys/devices/storage/physical", "r"));
    TRY(Core::System::unveil("/sys/devices/storage/logical", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::ArgsParser args_parser;
    args_parser.set_general_help("List Storage (Block) devices.");
    args_parser.parse(arguments);

    {
        Core::DirIterator di("/sys/devices/storage/physical/", Core::DirIterator::SkipParentAndBaseDir);
        if (di.has_error()) {
            warnln("Failed to open /sys/devices/storage/physical/ - {}", di.error());
            return 1;
        }

        outln(format_row, "LUN"sv, "Interface type"sv, "Command set"sv, "Block Size"sv, "Last LBA"sv);

        TRY(Core::System::pledge("stdio rpath"));

        while (di.has_next()) {
            auto dir = di.next_path();
            auto command_set_file = Core::File::construct(String::formatted("/sys/devices/storage/physical/{}/command_set", dir));
            if (!command_set_file->open(Core::OpenMode::ReadOnly)) {
                dbgln("Error: Could not open {}: {}", command_set_file->name(), command_set_file->error_string());
                continue;
            }
            auto interface_type_file = Core::File::construct(String::formatted("/sys/devices/storage/physical/{}/interface_type", dir));
            if (!interface_type_file->open(Core::OpenMode::ReadOnly)) {
                dbgln("Error: Could not open {}: {}", interface_type_file->name(), interface_type_file->error_string());
                continue;
            }
            auto last_lba_file = Core::File::construct(String::formatted("/sys/devices/storage/physical/{}/last_lba", dir));
            if (!last_lba_file->open(Core::OpenMode::ReadOnly)) {
                dbgln("Error: Could not open {}: {}", last_lba_file->name(), last_lba_file->error_string());
                continue;
            }
            auto sector_size_file = Core::File::construct(String::formatted("/sys/devices/storage/physical/{}/sector_size", dir));
            if (!sector_size_file->open(Core::OpenMode::ReadOnly)) {
                dbgln("Error: Could not open {}: {}", sector_size_file->name(), sector_size_file->error_string());
                continue;
            }

            String command_set = StringView(command_set_file->read_all().bytes());
            String interface_type = StringView(interface_type_file->read_all().bytes());
            String last_lba = StringView(last_lba_file->read_all().bytes());
            String sector_size = StringView(sector_size_file->read_all().bytes());

            outln(format_row, dir, interface_type, command_set, sector_size, last_lba);
        }
    }

    outln(""sv);

    {
        Core::DirIterator di("/sys/devices/storage/physical/", Core::DirIterator::SkipParentAndBaseDir);
        if (di.has_error()) {
            warnln("Failed to open /sys/devices/storage/physical/ - {}", di.error());
            return 1;
        }

        while (di.has_next()) {
            auto dir = di.next_path();
            outln("Device LUN: {}", dir);
            if (guid_partitions_for_storage_device(dir))
                outln(format_uuid_partition_row, "Index", "Attributes"sv, "Partition Type"sv, "Start LBA"sv, "Last LBA"sv, "UUID"sv);
            else
                outln(format_partition_row, "Index", "Attributes"sv, "Partition Type"sv, "Start LBA"sv, "Last LBA"sv, "UUID"sv);
            print_storage_device_partitions(dir);
            if (di.has_next())
                outln(""sv);
        }
    }

    return 0;
}
