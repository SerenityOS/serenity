/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/StringView.h>
#include <stdio.h>
#include <unistd.h>

struct Options {
    const char* path;
    const char* program { "/bin/Shell" };
    int flags { -1 };
};

void print_usage(const char* argv0)
{
    fprintf(
        stderr,
        "Usage:\n"
        "\t%s <path> [program] [-o options]\n",
        argv0
    );
}

Options parse_options(int argc, char** argv)
{
    Options options;

    if (argc < 2) {
        print_usage(argv[0]);
        exit(1);
    }

    options.path = argv[1];
    int i = 2;
    if (i < argc && argv[i][0] != '-')
        options.program = argv[i++];

    if (i >= argc)
        return options;

    if (strcmp(argv[i], "-o") != 0) {
        print_usage(argv[0]);
        exit(1);
    }
    i++;
    if (i >= argc) {
        print_usage(argv[0]);
        exit(1);
    }

    options.flags = 0;

    StringView arg = argv[i];
    Vector<StringView> parts = arg.split_view(',');
    for (auto& part : parts) {
        if (part == "defaults")
            continue;
        else if (part == "nodev")
            options.flags |= MS_NODEV;
        else if (part == "noexec")
            options.flags |= MS_NOEXEC;
        else if (part == "nosuid")
            options.flags |= MS_NOSUID;
        else if (part == "bind")
            fprintf(stderr, "Ignoring -o bind, as it doesn't make sense for chroot");
        else
            fprintf(stderr, "Ignoring invalid option: %s\n", String(part).characters());
    }

    return options;
}

int main(int argc, char** argv)
{

    Options options = parse_options(argc, argv);

    if (chroot_with_mount_flags(options.path, options.flags) < 0) {
        perror("chroot");
        return 1;
    }

    if (chdir("/") < 0) {
        perror("chdir(/)");
        return 1;
    }

    execl(options.program, options.program, nullptr);
    perror("execl");
    return 1;
}
