/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Random.h>
#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

bool verify_block(int fd, int seed, off_t block, AK::ByteBuffer& buffer);
bool write_block(int fd, int seed, off_t block, AK::ByteBuffer& buffer);

bool verify_block(int fd, int seed, off_t block, AK::ByteBuffer& buffer)
{
    auto offset = block * buffer.size();
    auto rs = lseek(fd, offset, SEEK_SET);
    if (rs < 0) {
        fprintf(stderr, "Couldn't seek to block %" PRIi64 " (offset %" PRIi64 ") while verifying: %s\n", block, offset, strerror(errno));
        return false;
    }
    auto rw = read(fd, buffer.data(), buffer.size());
    if (rw != static_cast<int>(buffer.size())) {
        fprintf(stderr, "Failure to read block %" PRIi64 ": %s\n", block, strerror(errno));
        return false;
    }
    srand((seed + 1) * (block + 1));
    for (size_t i = 0; i < buffer.size(); i++) {
        if (buffer[i] != rand() % 256) {
            fprintf(stderr, "Discrepancy detected at block %" PRIi64 " offset %zd\n", block, i);
            return false;
        }
    }
    return true;
}

bool write_block(int fd, int seed, off_t block, AK::ByteBuffer& buffer)
{
    auto offset = block * buffer.size();
    auto rs = lseek(fd, offset, SEEK_SET);
    if (rs < 0) {
        fprintf(stderr, "Couldn't seek to block %" PRIi64 " (offset %" PRIi64 ") while verifying: %s\n", block, offset, strerror(errno));
        return false;
    }
    srand((seed + 1) * (block + 1));
    for (size_t i = 0; i < buffer.size(); i++)
        buffer[i] = rand();
    auto rw = write(fd, buffer.data(), buffer.size());
    if (rw != static_cast<int>(buffer.size())) {
        fprintf(stderr, "Failure to write block %" PRIi64 ": %s\n", block, strerror(errno));
        return false;
    }
    return true;
}

int main(int argc, char** argv)
{
    Vector<StringView> arguments;
    arguments.ensure_capacity(argc);
    for (auto i = 0; i < argc; ++i)
        arguments.append({ argv[i], strlen(argv[i]) });

    ByteString target;
    int min_block_offset = 0;
    int block_length = 2048;
    int block_size = 512;
    int count = 1024;
    int rng_seed = 0;
    bool paranoid_mode = false;
    bool random_mode = false;
    bool stop_mode = false;
    bool uninitialized_mode = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(min_block_offset, "Minimum block offset to consider", "min-offset", 'o', "size");
    args_parser.add_option(block_length, "Number of blocks to consider", "length", 's', "size");
    args_parser.add_option(block_size, "Block size", "block-size", 'b', "size");
    args_parser.add_option(count, "Number of write/read cycles to run", "number", 'n', "number");
    args_parser.add_option(rng_seed, "Random number generator seed", "seed", 'S', "number");
    args_parser.add_option(paranoid_mode, "Check entire range for consistency after each write", "paranoid", 'p');
    args_parser.add_option(random_mode, "Write one block inside range at random", "random", 'r');
    args_parser.add_option(stop_mode, "Stop after first error", "abort-on-error", 'a');
    args_parser.add_option(uninitialized_mode, "Don't pre-initialize block range", "uninitialized", 'u');
    args_parser.add_positional_argument(target, "Target device/file path", "target");
    args_parser.parse(arguments);

    auto buffer_result = AK::ByteBuffer::create_zeroed(block_size);
    if (buffer_result.is_error()) {
        warnln("Failed to allocate a buffer of {} bytes", block_size);
        return EXIT_FAILURE;
    }
    auto buffer = buffer_result.release_value();

    int fd = open(target.characters(), O_CREAT | O_RDWR, 0666);
    if (fd < 0) {
        perror("Couldn't create target file");
        return EXIT_FAILURE;
    }

    if (!uninitialized_mode) {
        int old_percent = -100;
        for (int i = min_block_offset; i < min_block_offset + block_length; i++) {
            int percent;
            if (block_length <= 1)
                percent = 100;
            else
                percent = 100 * (i - min_block_offset) / (block_length - 1);
            if (old_percent != percent) {
                printf("Pre-initializing entire block range (%3d%%)...\n", percent);
                old_percent = percent;
            }

            if (!write_block(fd, rng_seed, i, buffer))
                return EXIT_FAILURE;
        }
    }

    int r = EXIT_SUCCESS;
    for (int i = 0; i < count; i++) {
        printf("(%d/%d)\tPass %d...\n", i + 1, count, i + 1);

        for (int j = min_block_offset; j < min_block_offset + block_length; j++) {
            off_t block;
            if (random_mode)
                while ((block = AK::get_random<off_t>()) < 0)
                    ;
            else
                block = j;
            block = min_block_offset + block % block_length;

            if (paranoid_mode) {
                for (int k = min_block_offset; j < min_block_offset + block_length; j++) {
                    if (!verify_block(fd, rng_seed, k, buffer)) {
                        if (stop_mode)
                            return EXIT_FAILURE;
                        else
                            r = EXIT_FAILURE;
                    }
                }
            } else {
                if (!verify_block(fd, rng_seed, block, buffer)) {
                    if (stop_mode)
                        return EXIT_FAILURE;
                    else
                        r = EXIT_FAILURE;
                }
            }

            if (!write_block(fd, rng_seed, block, buffer)) {
                if (stop_mode)
                    return EXIT_FAILURE;
                else
                    r = EXIT_FAILURE;
            }
        }
    }

    close(fd);
    return r;
}
