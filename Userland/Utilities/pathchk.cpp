/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibCore/ArgsParser.h>
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
    Vector<ByteString> paths;

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
        unsigned long path_max = flag_most_posix ? _POSIX_PATH_MAX : pathconf(path.characters(), _PC_PATH_MAX);
        unsigned long name_max = flag_most_posix ? _POSIX_NAME_MAX : pathconf(path.characters(), _PC_NAME_MAX);

        if (path.length() > path_max) {
            warnln("Limit {} exceeded by length {} of filename '{}'", path_max, path.length(), path);
            fail = true;
            continue;
        }

        if (flag_most_posix) {
            // POSIX portable filename character set (a-z A-Z 0-9 . _ -)
            for (long unsigned i = 0; i < path.length(); ++i) {
                auto c = path[i];
                if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '/' && c != '.' && c != '-' && c != '_') {
                    warnln("Non-portable character '{}' in filename '{}'", path[i], path);
                    fail = true;
                    continue;
                }
            }
        } else {
            struct stat st;
            if (lstat(path.characters(), &st) < 0) {
                if (errno != ENOENT) {
                    warnln("Directory is not searchable '{}'", path);
                    fail = true;
                    continue;
                }
            }
        }

        if (flag_empty_name_and_leading_dash) {
            if (path.is_empty()) {
                warnln("Empty filename");
                fail = true;
                continue;
            }
        }

        for (auto& component : path.split('/')) {
            if (flag_empty_name_and_leading_dash) {
                if (component.starts_with('-')) {
                    warnln("Leading '-' in a component of filename '{}'", path);
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
