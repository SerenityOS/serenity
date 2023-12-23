/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

enum TruncateOperation {
    OP_Set,
    OP_Grow,
    OP_Shrink,
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    StringView resize;
    StringView reference;
    StringView file;

    Core::ArgsParser args_parser;
    args_parser.add_option(resize, "Resize the target file to (or by) this size. Prefix with + or - to expand or shrink the file, or a bare number to set the size exactly", "size", 's', "size");
    args_parser.add_option(reference, "Resize the target file to match the size of this one", "reference", 'r', "file");
    args_parser.add_positional_argument(file, "File path", "file");
    args_parser.parse(arguments);

    if (resize.is_empty() && reference.is_empty()) {
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (!resize.is_empty() && !reference.is_empty()) {
        args_parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    auto op = OP_Set;
    off_t size = 0;

    if (!resize.is_empty()) {
        switch (resize[0]) {
        case '+':
            op = OP_Grow;
            resize = resize.substring_view(1);
            break;
        case '-':
            op = OP_Shrink;
            resize = resize.substring_view(1);
            break;
        }

        auto suffix = resize[resize.length() - 1];
        i64 multiplier = 1;
        if (!AK::is_ascii_digit(suffix)) {
            switch (to_ascii_lowercase(suffix)) {
            case 'k':
                multiplier = KiB;
                resize = resize.substring_view(0, resize.length() - 1);
                break;
            case 'm':
                multiplier = MiB;
                resize = resize.substring_view(0, resize.length() - 1);
                break;
            case 'g':
                multiplier = GiB;
                resize = resize.substring_view(0, resize.length() - 1);
                break;
            default:
                args_parser.print_usage(stderr, arguments.strings[0]);
                return 1;
            }
        }

        auto size_opt = resize.to_number<off_t>();
        if (!size_opt.has_value() || Checked<off_t>::multiplication_would_overflow(size_opt.value(), multiplier)) {
            args_parser.print_usage(stderr, arguments.strings[0]);
            return 1;
        }
        size = size_opt.value() * multiplier;
    }

    if (!reference.is_empty()) {
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
        size = max(stat.st_size - size, 0);
        break;
    }

    TRY(Core::System::ftruncate(fd, size));
    TRY(Core::System::close(fd));

    return 0;
}
