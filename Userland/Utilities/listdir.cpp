/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/DirectoryEntry.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static bool flag_show_unix_posix_file_type = false;
static bool flag_show_total_count = false;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("List Dirent entries in a directory.");
    args_parser.add_option(flag_show_unix_posix_file_type, "Show POSIX names for file types", "posix-names", 'P');
    args_parser.add_option(flag_show_total_count, "Show total count for each directory being iterated", "total-entries-count", 't');
    args_parser.add_positional_argument(paths, "Directory to list", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (paths.is_empty())
        paths.append("."sv);

    for (auto& path : paths) {
        Core::DirIterator di(path, Core::DirIterator::NoStat);
        if (di.has_error()) {
            auto error = di.error();
            warnln("Failed to open {} - {}", path, error);
            return error;
        }

        outln("Traversing {}", path);
        size_t count = 0;

        Function<StringView(Core::DirectoryEntry::Type)> name_from_directory_entry_type;
        if (flag_show_unix_posix_file_type)
            name_from_directory_entry_type = Core::DirectoryEntry::posix_name_from_directory_entry_type;
        else
            name_from_directory_entry_type = Core::DirectoryEntry::representative_name_from_directory_entry_type;

        while (di.has_next()) {
            auto dir_entry = di.next();
            if (dir_entry.has_value()) {
                outln("    {} (Type: {}, Inode number: {})",
                    dir_entry.value().name,
                    name_from_directory_entry_type(dir_entry.value().type),
                    dir_entry.value().inode_number);
                count++;
            }
        }
        if (flag_show_total_count)
            outln("Directory {} has {} which has being listed during the program runtime", path, count);
    }

    return 0;
}
