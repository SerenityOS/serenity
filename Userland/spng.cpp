/*
 * Copyright (c) 2020, Matthew Graham <mjg267106@gmail.com>
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
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

void print_error_then_exit_on_fail(int result, const char* function_name)
{
    if (result < 0) {
        perror(function_name);
        exit(1);
    }
}

void copy_over_text(int from_file_descriptor, int to_file_descriptor)
{
    for (;;) {
        char buffer[32768];
        int count = read(from_file_descriptor, buffer, sizeof(buffer));
        print_error_then_exit_on_fail(count, "read");
        if (count == 0)
            break;
        print_error_then_exit_on_fail(write(to_file_descriptor, buffer, count), "write");
    }
}

int main(int argc, char** argv)
{
    print_error_then_exit_on_fail(pledge("stdio wpath cpath rpath fattr chown", nullptr), "pledge");

    bool append = false;
    const char* output_file_name = nullptr;

    Core::ArgsParser parser;
    parser.add_option(append, "Append input to output file", "append", 'a');
    parser.add_positional_argument(output_file_name, "Output file", "file", Core::ArgsParser::Required::No);
    parser.parse(argc, argv);

    if (!output_file_name) {
        copy_over_text(0, 1);
        return 1;
    }

    const char* temp_file_name = "..spng_temp.Uav78GHg";
    int open_mode = O_WRONLY | O_CREAT;
    if (append)
        open_mode |= O_APPEND;
    int temp_file_descriptor = open(temp_file_name, open_mode);
    print_error_then_exit_on_fail(temp_file_descriptor, "open");

    if (append) {
        int output_file_descriptor = open(output_file_name, O_RDONLY);
        print_error_then_exit_on_fail(output_file_descriptor, "open");
        copy_over_text(output_file_descriptor, temp_file_descriptor);
        close(output_file_descriptor);
    }
    copy_over_text(0, temp_file_descriptor);

    print_error_then_exit_on_fail(pledge("stdio cpath rpath fattr chown", nullptr), "pledge");
    struct stat st;
    print_error_then_exit_on_fail(stat(output_file_name, &st), "stat");
    print_error_then_exit_on_fail(pledge("stdio cpath fattr chown", nullptr), "pledge");
    print_error_then_exit_on_fail(chown(temp_file_name, st.st_uid, st.st_gid), "chown");
    print_error_then_exit_on_fail(pledge("stdio cpath fattr", nullptr), "pledge");
    print_error_then_exit_on_fail(chmod(temp_file_name, st.st_mode), "chmod");
    print_error_then_exit_on_fail(pledge("stdio cpath", nullptr), "pledge");
    print_error_then_exit_on_fail(rename(temp_file_name, output_file_name), "rename");

    return 0;
}
