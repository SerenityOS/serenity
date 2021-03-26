/*
 * Copyright (c) 2021, János Tóth <toth-janos@outlook.com>
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

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#include <stdint.h>
#include <stdio.h>
#include <string.h>

const char* usage = "usage:\n"
                    "\tdd <options>\n"
                    "options:\n"
                    "\tif=<file>\tinput file (default: stdin)\n"
                    "\tof=<file>\toutput file (default: stdout)\n"
                    "\tbs=<size>\tblocks size (default: 512)\n"
                    "\tcount=<size>\t<size> blocks to copy (default: 0 (until end-of-file))\n"
                    "\tseek=<size>\tskip <size> blocks at start of output (default: 0)\n"
                    "\tskip=<size>\tskip <size> blocks at start of intput (default: 0)\n"
                    "\tstatus=<level>\tlevel of output (default: default)\n"
                    "\t\t\tdefault - error messages + final statistics\n"
                    "\t\t\tnone - just error messages\n"
                    "\t\t\tnoxfer - no final statistics\n"
                    "\t--help\t\tshows this text\n";

enum Status {
    Default,
    None,
    Noxfer
};

static String split_at_equals(const char* argument)
{
    String string_value(argument);

    auto values = string_value.split('=');
    if (values.size() != 2) {
        fprintf(stderr, "Unable to parse: %s\n", argument);
        return {};
    } else {
        return values[1];
    }
}

static int handle_io_file_arguments(int& fd, int flags, const char* argument)
{
    auto value = split_at_equals(argument);
    if (value.is_empty()) {
        return -1;
    }

    fd = open(value.characters(), flags);
    if (fd == -1) {
        fprintf(stderr, "Unable to open: %s\n", value.characters());
        return -1;
    } else {
        return 0;
    }
}

static int handle_size_arguments(size_t& numeric_value, const char* argument)
{
    auto value = split_at_equals(argument);
    if (value.is_empty()) {
        return -1;
    }

    Optional<unsigned> numeric_optional = value.to_uint();
    if (!numeric_optional.has_value()) {
        fprintf(stderr, "Invalid size-value: %s\n", value.characters());
        return -1;
    }

    numeric_value = numeric_optional.value();
    if (numeric_value < 1) {
        fprintf(stderr, "Invalid size-value: %lu\n", numeric_value);
        return -1;
    } else {
        return 0;
    }
}

static int handle_status_arguments(Status& status, const char* argument)
{
    auto value = split_at_equals(argument);
    if (value.is_empty()) {
        return -1;
    }

    if (value == "default") {
        status = Default;
        return 0;
    } else if (value == "noxfer") {
        status = Noxfer;
        return 0;
    } else if (value == "none") {
        status = None;
        return 0;
    } else {
        fprintf(stderr, "Unknown status: %s\n", value.characters());
        return -1;
    }
}

int main(int argc, char** argv)
{
    int input_fd = 0;
    int input_flags = O_RDONLY;
    int output_fd = 1;
    int output_flags = O_CREAT | O_WRONLY;
    size_t block_size = 512;
    size_t count = 0;
    size_t skip = 0;
    size_t seek = 0;
    Status status = Default;

    size_t total_bytes_copied = 0;
    size_t total_blocks_in = 0, partial_blocks_in = 0;
    size_t total_blocks_out = 0, partial_blocks_out = 0;
    uint8_t* buffer = nullptr;
    ssize_t nread = 0, nwritten = 0;

    for (int a = 1; a < argc; a++) {
        if (!strcmp(argv[a], "--help")) {
            printf("%s", usage);
            return 0;
        } else if (!strncmp(argv[a], "if=", 3)) {
            if (handle_io_file_arguments(input_fd, input_flags, argv[a]) < 0) {
                return 1;
            }
        } else if (!strncmp(argv[a], "of=", 3)) {
            if (handle_io_file_arguments(output_fd, output_flags, argv[a]) < 0) {
                return 1;
            }
        } else if (!strncmp(argv[a], "bs=", 3)) {
            if (handle_size_arguments(block_size, argv[a]) < 0) {
                return 1;
            }
        } else if (!strncmp(argv[a], "count=", 6)) {
            if (handle_size_arguments(count, argv[a]) < 0) {
                return 1;
            }
        } else if (!strncmp(argv[a], "seek=", 5)) {
            if (handle_size_arguments(seek, argv[a]) < 0) {
                return 1;
            }
        } else if (!strncmp(argv[a], "skip=", 5)) {
            if (handle_size_arguments(skip, argv[a]) < 0) {
                return 1;
            }
        } else if (!strncmp(argv[a], "status=", 7)) {
            if (handle_status_arguments(status, argv[a]) < 0) {
                return 1;
            }
        } else {
            fprintf(stderr, "%s", usage);
            return 1;
        }
    }

    if ((buffer = (uint8_t*)malloc(block_size)) == nullptr) {
        fprintf(stderr, "Unable to allocate %lu bytes for the buffer.\n", block_size);
        return -1;
    }

    if (seek > 0) {
        if (lseek(output_fd, seek * block_size, SEEK_SET) < 0) {
            fprintf(stderr, "Unable to seek %lu bytes.\n", seek * block_size);
            return -1;
        }
    }

    while (1) {
        nread = read(input_fd, buffer, block_size);
        if (nread < 0) {
            fprintf(stderr, "Cannot read from the input.\n");
            break;
        } else if (nread == 0) {
            break;
        } else {
            if ((size_t)nread != block_size) {
                partial_blocks_in++;
            } else {
                total_blocks_in++;
            }

            if (partial_blocks_in + total_blocks_in <= skip) {
                continue;
            }

            nwritten = write(output_fd, buffer, nread);
            if (nwritten < 0) {
                fprintf(stderr, "Cannot write to the output.\n");
                break;
            } else if (nwritten == 0) {
                break;
            } else {
                if ((size_t)nwritten < block_size) {
                    partial_blocks_out++;
                } else {
                    total_blocks_out++;
                }

                total_bytes_copied += nwritten;

                if (count > 0 && (partial_blocks_out + total_blocks_out) >= count) {
                    break;
                }
            }
        }
    }

    if (status == Default) {
        fprintf(stderr, "%lu+%lu blocks in\n", total_blocks_in, partial_blocks_in);
        fprintf(stderr, "%lu+%lu blocks out\n", total_blocks_out, partial_blocks_out);
        fprintf(stderr, "%lu bytes copied.\n", total_bytes_copied);
    }

    free(buffer);

    if (input_fd != 0) {
        close(input_fd);
    }

    if (output_fd != 1) {
        close(output_fd);
    }

    return 0;
}
