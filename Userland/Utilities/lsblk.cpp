/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Hex.h>
#include <AK/String.h>
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
        warnln("Failed to open /sys/devices/storage - {}", di.error());
        return 1;
    }

    outln(format_row, "LUN"sv, "Command set"sv, "Block Size"sv, "Last LBA"sv);

    TRY(Core::System::pledge("stdio rpath"));

    while (di.has_next()) {
        auto dir = di.next_path();
        auto command_set_file = Core::File::construct(String::formatted("/sys/devices/storage/{}/command_set", dir));
        if (!command_set_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", command_set_file->name(), command_set_file->error_string());
            continue;
        }
        auto last_lba_file = Core::File::construct(String::formatted("/sys/devices/storage/{}/last_lba", dir));
        if (!last_lba_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", last_lba_file->name(), last_lba_file->error_string());
            continue;
        }
        auto sector_size_file = Core::File::construct(String::formatted("/sys/devices/storage/{}/sector_size", dir));
        if (!sector_size_file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Error: Could not open {}: {}", sector_size_file->name(), sector_size_file->error_string());
            continue;
        }

        String command_set = StringView(command_set_file->read_all().bytes());
        String last_lba = StringView(last_lba_file->read_all().bytes());
        String sector_size = StringView(sector_size_file->read_all().bytes());

        outln(format_row, dir, command_set, sector_size, last_lba);
    }

    return 0;
}
