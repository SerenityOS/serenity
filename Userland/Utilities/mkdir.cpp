/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
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
        for (auto& part : lexical_path.parts()) {
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
                    fprintf(stderr, "mkdir: cannot create directory '%s': not a directory\n", path.characters());
                    has_errors = true;
                    break;
                }
            }
            path_builder.append("/");
        }
    }
    return has_errors ? 1 : 0;
}
