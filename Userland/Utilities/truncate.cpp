/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>

#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

enum TruncateOperation {
    OP_Set,
    OP_Grow,
    OP_Shrink,
};

int main(int argc, char** argv)
{
    char const* resize = nullptr;
    char const* reference = nullptr;
    char const* file = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(resize, "Resize the target file to (or by) this size. Prefix with + or - to expand or shrink the file, or a bare number to set the size exactly", "size", 's', "size");
    args_parser.add_option(reference, "Resize the target file to match the size of this one", "reference", 'r', "file");
    args_parser.add_positional_argument(file, "File path", "file");
    args_parser.parse(argc, argv);

    if (!resize && !reference) {
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    if (resize && reference) {
        args_parser.print_usage(stderr, argv[0]);
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
            args_parser.print_usage(stderr, argv[0]);
            return 1;
        }
        size = size_opt.value();
    }

    if (reference) {
        struct stat st;
        int rc = stat(reference, &st);
        if (rc < 0) {
            perror("stat");
            return 1;
        }

        size = st.st_size;
    }

    int fd = open(file, O_RDWR | O_CREAT, 0666);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    struct stat st;
    if (fstat(fd, &st) < 0) {
        perror("fstat");
        return 1;
    }

    switch (op) {
    case OP_Set:
        break;
    case OP_Grow:
        size = st.st_size + size;
        break;
    case OP_Shrink:
        size = st.st_size - size;
        break;
    }

    if (ftruncate(fd, size) < 0) {
        perror("ftruncate");
        return 1;
    }

    if (close(fd) < 0) {
        perror("close");
        return 1;
    }

    return 0;
}
