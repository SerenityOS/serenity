/*
 * Copyright (c) 2021, the SerenityOS developers.
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
        fprintf(stderr, "Couldn't seek to block %lld (offset %lld) while verifying: %s\n", block, offset, strerror(errno));
        return false;
    }
    auto rw = read(fd, buffer.data(), buffer.size());
    if (rw != static_cast<int>(buffer.size())) {
        fprintf(stderr, "Failure to read block %lld: %s\n", block, strerror(errno));
        return false;
    }
    srand((seed + 1) * (block + 1));
    for (size_t i = 0; i < buffer.size(); i++) {
        if (buffer[i] != rand() % 256) {
            fprintf(stderr, "Discrepancy detected at block %lld offset %zd\n", block, i);
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
        fprintf(stderr, "Couldn't seek to block %lld (offset %lld) while verifying: %s\n", block, offset, strerror(errno));
        return false;
    }
    srand((seed + 1) * (block + 1));
    for (size_t i = 0; i < buffer.size(); i++)
        buffer[i] = rand();
    auto rw = write(fd, buffer.data(), buffer.size());
    if (rw != static_cast<int>(buffer.size())) {
        fprintf(stderr, "Failure to write block %lld: %s\n", block, strerror(errno));
        return false;
    }
    return true;
}

int main(int argc, char** argv)
{
    const char* target = nullptr;
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
    args_parser.parse(argc, argv);

    auto buffer = AK::ByteBuffer::create_zeroed(block_size);

    int fd = open(target, O_CREAT | O_RDWR, 0666);
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
