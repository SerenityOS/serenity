/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <bits/posix1_lim.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    bool fail = false;
    static bool flag_most_posix = false;
    static bool flag_portability = false;
    static bool flag_empty_name_and_leading_dash = false;
    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_most_posix, "Check for most POSIX systems", nullptr, 'p');
    args_parser.add_option(flag_empty_name_and_leading_dash, "Check for empty names and leading dash", nullptr, 'P');
    args_parser.add_option(flag_portability, "Check portability (equivalent to -p and -P)", "portability", '\0');
    args_parser.add_positional_argument(paths, "Path to check", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    if (flag_portability) {
        flag_most_posix = true;
        flag_empty_name_and_leading_dash = true;
    }

    for (auto& path : paths) {
        auto str_path = String(path);
        unsigned long path_max = flag_most_posix ? _POSIX_PATH_MAX : pathconf(str_path.characters(), _PC_PATH_MAX);
        unsigned long name_max = flag_most_posix ? _POSIX_NAME_MAX : pathconf(str_path.characters(), _PC_NAME_MAX);

        if (str_path.length() > path_max) {
            warnln("Limit {} exceeded by length {} of filename '{}'", path_max, str_path.length(), str_path);
            fail = true;
            continue;
        }

        if (flag_most_posix) {
            // POSIX portable filename character set (a-z A-Z 0-9 . _ -)
            for (long unsigned i = 0; i < str_path.length(); ++i) {
                auto c = path[i];
                if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '/' && c != '.' && c != '-' && c != '_') {
                    warnln("Non-portable character '{}' in filename '{}'", path[i], str_path);
                    fail = true;
                    continue;
                }
            }
        } else {
            struct stat st;
            if (lstat(str_path.characters(), &st) < 0) {
                if (errno != ENOENT) {
                    warnln("Directory is not searchable '{}'", str_path);
                    fail = true;
                    continue;
                }
            }
        }

        if (flag_empty_name_and_leading_dash) {
            if (str_path.is_empty()) {
                warnln("Empty filename");
                fail = true;
                continue;
            }
        }

        for (auto& component : str_path.split('/')) {
            if (flag_empty_name_and_leading_dash) {
                if (component.starts_with('-')) {
                    warnln("Leading '-' in a component of filename '{}'", str_path);
                    fail = true;
                    break;
                }
            }
            if (component.length() > name_max) {
                warnln("Limit {} exceeded by length {} of filename component '{}'", name_max, component.length(), component.characters());
                fail = true;
                break;
            }
        }
    }

    return fail;
}
