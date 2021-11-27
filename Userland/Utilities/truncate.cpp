/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

enum TruncateOperation {
    OP_Set,
    OP_Grow,
    OP_Shrink,
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    const char* resize = nullptr;
    const char* reference = nullptr;
    const char* file = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(resize, "Resize the target file to (or by) this size. Prefix with + or - to expand or shrink the file, or a bare number to set the size exactly", "size", 's', "size");
    args_parser.add_option(reference, "Resize the target file to match the size of this one", "reference", 'r', "file");
    args_parser.add_positional_argument(file, "File path", "file");
    args_parser.parse(arguments);

    if (!resize && !reference) {
        args_parser.print_usage(stderr, arguments.argv[0]);
        return 1;
    }

    if (resize && reference) {
        args_parser.print_usage(stderr, arguments.argv[0]);
        return 1;
    }

    auto op = OP_Set;
    off_t size = 0;

    if (resize) {
        String str = resize;

        switch (str[0]) {
        case '+':
            op = OP_Grow;
            str = str.substring(1, str.length() - 1);
            break;
        case '-':
            op = OP_Shrink;
            str = str.substring(1, str.length() - 1);
            break;
        }

        auto size_opt = str.to_int<off_t>();
        if (!size_opt.has_value()) {
            args_parser.print_usage(stderr, arguments.argv[0]);
            return 1;
        }
        size = size_opt.value();
    }

    if (reference) {
        auto stat = TRY(Core::System::stat(reference));
        size = stat.st_size;
    }

    auto fd = TRY(Core::System::open(file, O_RDWR | O_CREAT, 0666));
    auto stat = TRY(Core::System::fstat(fd));

    switch (op) {
    case OP_Set:
        break;
    case OP_Grow:
        size = stat.st_size + size;
        break;
    case OP_Shrink:
        size = stat.st_size - size;
        break;
    }

    TRY(Core::System::ftruncate(fd, size));
    TRY(Core::System::close(fd));

    return 0;
}
