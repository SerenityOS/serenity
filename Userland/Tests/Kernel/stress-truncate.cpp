/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <LibCore/ArgsParser.h>
#include <fcntl.h>
#include <inttypes.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    const char* target = nullptr;
    int max_file_size = 1024 * 1024;
    int count = 1024;

    Core::ArgsParser args_parser;
    args_parser.add_option(max_file_size, "Maximum file size to generate", "max-size", 's', "size");
    args_parser.add_option(count, "Number of truncations to run", "number", 'n', "number");
    args_parser.add_positional_argument(target, "Target file path", "target");
    args_parser.parse(argc, argv);

    int fd = creat(target, 0666);
    if (fd < 0) {
        perror("Couldn't create target file");
        return EXIT_FAILURE;
    }
    close(fd);

    for (int i = 0; i < count; i++) {
        auto new_file_size = AK::get_random<uint64_t>() % (max_file_size + 1);
        printf("(%d/%d)\tTruncating to %" PRIu64 " bytes...\n", i + 1, count, new_file_size);
        if (truncate(target, new_file_size) < 0) {
            perror("Couldn't truncate target file");
            return EXIT_FAILURE;
        }
    }

    if (unlink(target) < 0) {
        perror("Couldn't remove target file");
        return EXIT_FAILURE;
    }

    return EXIT_SUCCESS;
}
