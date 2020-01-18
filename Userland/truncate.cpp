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

#include <LibCore/CArgsParser.h>

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
    CArgsParser args_parser("truncate");

    args_parser.add_arg("s", "size", "Resize the target file to (or by) this size. Prefix with + or - to expand or shrink the file, or a bare number to set the size exactly.");
    args_parser.add_arg("r", "reference", "Resize the target file to match the size of this one.");
    args_parser.add_required_single_value("file");

    CArgsParserResult args = args_parser.parse(argc, argv);

    if (!args.is_present("s") && !args.is_present("r")) {
        args_parser.print_usage();
        return -1;
    }

    if (args.is_present("s") && args.is_present("r")) {
        args_parser.print_usage();
        return -1;
    }

    auto op = OP_Set;
    int size = 0;

    if (args.is_present("s")) {
        auto str = args.get("s");

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

        bool ok;
        size = str.to_int(ok);
        if (!ok) {
            args_parser.print_usage();
            return -1;
        }
    }

    if (args.is_present("r")) {
        struct stat st;
        int rc = stat(args.get("r").characters(), &st);
        if (rc < 0) {
            perror("stat");
            return -1;
        }

        op = OP_Set;
        size = st.st_size;
    }

    auto name = args.get_single_values()[0];

    int fd = open(name.characters(), O_RDWR | O_CREAT, 0666);
    if (fd < 0) {
        perror("open");
        return -1;
    }

    struct stat st;
    if (fstat(fd, &st) < 0) {
        perror("fstat");
        return -1;
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
        return -1;
    }

    if (close(fd) < 0) {
        perror("close");
        return -1;
    }

    return 0;
}
