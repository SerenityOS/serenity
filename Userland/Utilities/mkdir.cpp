/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio cpath rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool create_parents = false;
    Vector<const char*> directories;

    Core::ArgsParser args_parser;
    args_parser.add_option(create_parents, "Create parent directories if they don't exist", "parents", 'p');
    args_parser.add_positional_argument(directories, "Directories to create", "directories");
    args_parser.parse(argc, argv);

    // FIXME: Support -m/--mode option
    mode_t mode = 0755;

    bool has_errors = false;

    for (auto& directory : directories) {
        LexicalPath lexical_path(directory);
        if (!create_parents) {
            if (mkdir(lexical_path.string().characters(), mode) < 0) {
                perror("mkdir");
                has_errors = true;
            }
            continue;
        }
        StringBuilder path_builder;
        if (lexical_path.is_absolute())
            path_builder.append("/");
        for (auto& part : lexical_path.parts_view()) {
            path_builder.append(part);
            auto path = path_builder.build();
            struct stat st;
            if (stat(path.characters(), &st) < 0) {
                if (errno != ENOENT) {
                    perror("stat");
                    has_errors = true;
                    break;
                }
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
