/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool ignore_env = false;
    const char* split_string = nullptr;
    Vector<const char*> values;

    Core::ArgsParser args_parser;
    args_parser.add_option(ignore_env, "Start with an empty environment", "ignore-environment", 'i');
    args_parser.add_option(split_string, "Process and split S into separate arguments; used to pass multiple arguments on shebang lines", "split-string", 'S', "S");

    args_parser.add_positional_argument(values, "Environment and commands", "env/command", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (ignore_env)
        clearenv();

    size_t argv_start;
    for (argv_start = 0; argv_start < values.size(); ++argv_start) {
        if (StringView { values[argv_start] }.contains('=')) {
            putenv(const_cast<char*>(values[argv_start]));
        } else {
            break;
        }
    }

    Vector<String> split_string_storage;
    Vector<const char*> new_argv;
    if (split_string) {
        for (auto view : StringView(split_string).split_view(' ')) {
            split_string_storage.append(view);
        }
        for (auto& str : split_string_storage) {
            new_argv.append(str.characters());
        }
    }

    for (size_t i = argv_start; i < values.size(); ++i) {
        new_argv.append(values[i]);
    }

    if (new_argv.size() == 0) {
        for (auto entry = environ; *entry != nullptr; ++entry)
            printf("%s\n", *entry);

        return 0;
    }

    new_argv.append(nullptr);

    const char* executable = new_argv[0];
    char* const* new_argv_ptr = const_cast<char* const*>(&new_argv[0]);

    execvp(executable, new_argv_ptr);
    perror("execvp");
    return 1;
}
