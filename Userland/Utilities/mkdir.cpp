/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FilePermissionsMask.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath rpath"));

    bool create_parents = false;
    bool verbose = false;
    StringView mode_string;
    Vector<StringView> directories;

    Core::ArgsParser args_parser;
    args_parser.add_option(create_parents, "Create parent directories if they don't exist", "parents", 'p');
    args_parser.add_option(mode_string, "Set new directory permissions", "mode", 'm', "mode");
    args_parser.add_option(verbose, "Print a message for each created directory", "verbose", 'v');
    args_parser.add_positional_argument(directories, "Directories to create", "directories");
    args_parser.parse(arguments);

    mode_t const default_mode = 0755;
    mode_t const mask_reference_mode = 0777;

    Core::FilePermissionsMask mask;

    if (mode_string.is_empty()) {
        mask.assign_permissions(default_mode);
    } else {
        mask = TRY(Core::FilePermissionsMask::parse(mode_string));
    }

    bool has_errors = false;

    auto create_directory = [&](StringView path, mode_t mode) {
        auto maybe_error = Core::System::mkdir(path, mode);
        if (maybe_error.is_error()) {
            warnln("mkdir: {}", strerror(maybe_error.error().code()));
            has_errors = true;
            return false;
        }

        if (verbose)
            outln("mkdir: Created directory '{}'", path);

        return true;
    };

    for (auto& directory : directories) {
        LexicalPath lexical_path(directory);
        if (!create_parents) {
            create_directory(lexical_path.string(), mask.apply(mask_reference_mode));
            continue;
        }
        StringBuilder path_builder;
        if (lexical_path.is_absolute())
            path_builder.append('/');

        auto& parts = lexical_path.parts_view();
        size_t num_parts = parts.size();

        for (size_t idx = 0; idx < num_parts; ++idx) {
            auto& part = parts[idx];

            path_builder.append(part);
            auto path = path_builder.string_view();

            auto stat_or_error = Core::System::stat(path);
            if (stat_or_error.is_error()) {
                if (stat_or_error.error().code() != ENOENT) {
                    warnln("mkdir: {}", strerror(stat_or_error.error().code()));
                    has_errors = true;
                    break;
                }

                bool is_final = (idx == (num_parts - 1));
                mode_t mode = is_final ? mask.apply(mask_reference_mode) : default_mode;

                if (!create_directory(path, mode))
                    break;

            } else {
                if (!S_ISDIR(stat_or_error.value().st_mode)) {
                    warnln("mkdir: cannot create directory '{}': not a directory", path);
                    has_errors = true;
                    break;
                }
            }
            path_builder.append('/');
        }
    }
    return has_errors ? 1 : 0;
}
