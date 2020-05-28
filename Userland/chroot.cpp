/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    const char* path = nullptr;
    const char* program = "/bin/Shell";
    int flags = -1;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "New root directory", "path");
    args_parser.add_positional_argument(program, "Program to run", "program", Core::ArgsParser::Required::No);

    Core::ArgsParser::Option option {
        true,
        "Mount options",
        "options",
        'o',
        "options",
        [&flags](const char* s) {
            flags = 0;
            Vector<StringView> parts = StringView(s).split_view(',');
            for (auto& part : parts) {
                if (part == "defaults")
                    continue;
                else if (part == "nodev")
                    flags |= MS_NODEV;
                else if (part == "noexec")
                    flags |= MS_NOEXEC;
                else if (part == "nosuid")
                    flags |= MS_NOSUID;
                else if (part == "ro")
                    flags |= MS_RDONLY;
                else if (part == "remount")
                    flags |= MS_REMOUNT;
                else if (part == "bind")
                    fprintf(stderr, "Ignoring -o bind, as it doesn't make sense for chroot\n");
                else
                    return false;
            }
            return true;
        }
    };
    args_parser.add_option(move(option));
    args_parser.parse(argc, argv);

    if (chroot_with_mount_flags(path, flags) < 0) {
        perror("chroot");
        return 1;
    }

    if (chdir("/") < 0) {
        perror("chdir(/)");
        return 1;
    }

    execl(program, program, nullptr);
    perror("execl");
    return 1;
}
