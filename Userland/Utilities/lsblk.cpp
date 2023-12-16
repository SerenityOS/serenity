/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Hex.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static constexpr StringView format_row = "{:10s}\t{:10s}\t{:10s}\t{:10s}"sv;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/devices/storage", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::ArgsParser args_parser;
    args_parser.set_general_help("List Storage (Block) devices.");
    args_parser.parse(arguments);

    Core::DirIterator di("/sys/devices/storage/", Core::DirIterator::SkipParentAndBaseDir);
    if (di.has_error()) {
        auto error = di.error();
        warnln("Failed to open /sys/devices/storage - {}", error);
        return error;
    }

    outln(format_row, "LUN"sv, "Command set"sv, "Block Size"sv, "Last LBA"sv);

    TRY(Core::System::pledge("stdio rpath"));

    while (di.has_next()) {
        auto dir = di.next_path();
        auto command_set_filename = ByteString::formatted("/sys/devices/storage/{}/command_set", dir);
        auto command_set_file = Core::File::open(command_set_filename, Core::File::OpenMode::Read);
        if (command_set_file.is_error()) {
            dbgln("Error: Could not open {}: {}", command_set_filename, command_set_file.error());
            continue;
        }
        auto last_lba_filename = ByteString::formatted("/sys/devices/storage/{}/last_lba", dir);
        auto last_lba_file = Core::File::open(last_lba_filename, Core::File::OpenMode::Read);
        if (last_lba_file.is_error()) {
            dbgln("Error: Could not open {}: {}", last_lba_filename, last_lba_file.error());
            continue;
        }
        auto sector_size_filename = ByteString::formatted("/sys/devices/storage/{}/sector_size", dir);
        auto sector_size_file = Core::File::open(sector_size_filename, Core::File::OpenMode::Read);
        if (sector_size_file.is_error()) {
            dbgln("Error: Could not open {}: {}", sector_size_filename, sector_size_file.error());
            continue;
        }

        auto maybe_command_set = command_set_file.value()->read_until_eof();
        if (maybe_command_set.is_error()) {
            dbgln("Error: Could not read {}: {}", command_set_filename, maybe_command_set.error());
            continue;
        }
        ByteString command_set = StringView(maybe_command_set.value().bytes());

        auto maybe_last_lba = last_lba_file.value()->read_until_eof();
        if (maybe_last_lba.is_error()) {
            dbgln("Error: Could not read {}: {}", last_lba_filename, maybe_last_lba.error());
            continue;
        }
        ByteString last_lba = StringView(maybe_last_lba.value().bytes());

        auto maybe_sector_size = sector_size_file.value()->read_until_eof();
        if (maybe_sector_size.is_error()) {
            dbgln("Error: Could not read {}: {}", sector_size_filename, maybe_sector_size.error());
            continue;
        }
        ByteString sector_size = StringView(maybe_sector_size.value().bytes());

        outln(format_row, dir, command_set, sector_size, last_lba);
    }

    return 0;
}
