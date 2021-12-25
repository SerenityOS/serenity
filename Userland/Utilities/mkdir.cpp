/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Xavier Defrang <xavier.defrang@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FilePermissionsMask.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <sys/stat.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio cpath rpath"));

    bool create_parents = false;
    String mode_string;
    Vector<const char*> directories;

    Core::ArgsParser args_parser;
    args_parser.add_option(create_parents, "Create parent directories if they don't exist", "parents", 'p');
    args_parser.add_option(mode_string, "Set new directory permissions", "mode", 'm', "mode");
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

    for (auto& directory : directories) {
        LexicalPath lexical_path(directory);
        if (!create_parents) {
            if (mkdir(lexical_path.string().characters(), mask.apply(mask_reference_mode)) < 0) {
                perror("mkdir");
                has_errors = true;
            }
            continue;
        }
        StringBuilder path_builder;
        if (lexical_path.is_absolute())
            path_builder.append("/");

        auto& parts = lexical_path.parts_view();
        size_t num_parts = parts.size();

        for (size_t idx = 0; idx < num_parts; ++idx) {
            auto& part = parts[idx];

            path_builder.append(part);
            auto path = path_builder.build();

            struct stat st;
            if (stat(path.characters(), &st) < 0) {
                if (errno != ENOENT) {
                    perror("stat");
                    has_errors = true;
                    break;
                }

                bool is_final = (idx == (num_parts - 1));
                mode_t mode = is_final ? mask.apply(mask_reference_mode) : default_mode;

                if (mkdir(path.characters(), mode) < 0) {
                    perror("mkdir");
                    has_errors = true;
                    break;
                }
            } else {
                if (!S_ISDIR(st.st_mode)) {
                    warnln("mkdir: cannot create directory '{}': not a directory", path);
                    has_errors = true;
                    break;
                }
            }
            path_builder.append("/");
        }
    }
    return has_errors ? 1 : 0;
}
