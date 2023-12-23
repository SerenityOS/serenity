/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/StringUtils.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

static bool try_set_offset_and_length_parameters(ByteString const& arg_offset, ByteString const& arg_length, u64& offset, u64& length)
{
    // TODO: Add support for hex values
    auto possible_offset = arg_offset.to_number<u64>();
    if (!possible_offset.has_value())
        return false;
    auto possible_length = arg_length.to_number<u64>();
    if (!possible_length.has_value())
        return false;
    offset = possible_offset.value();
    length = possible_length.value();
    return true;
}

static void try_to_dump_with_memory_mapping(int fd, u64 offset, u64 length)
{
    VERIFY(fd >= 0);
    u64 mmoffset = offset % sysconf(_SC_PAGESIZE);
    void* mmp = mmap(NULL, mmoffset + length, PROT_READ, MAP_SHARED, fd, offset - mmoffset);
    if (mmp == MAP_FAILED) {
        perror("mmap");
        return;
    }

    size_t ncomplete = 0;
    while (ncomplete < length) {
        ssize_t nwritten = write(STDOUT_FILENO, static_cast<u8*>(mmp) + ncomplete, length - ncomplete);
        if (nwritten < 0) {
            perror("write");
            return;
        }
        ncomplete += nwritten;
    }
    if (munmap(mmp, mmoffset + length) < 0) {
        perror("munmap");
    }
}

static void try_to_dump_with_read(int fd, u64 offset, u64 length)
{
    VERIFY(fd >= 0);
    auto rs = lseek(fd, offset, SEEK_SET);
    if (rs < 0) {
        fprintf(stderr, "Couldn't seek to offset %" PRIi64 " while verifying: %s\n", offset, strerror(errno));
        return;
    }
    u8 buf[4096];
    size_t ncomplete = 0;
    while (ncomplete < length) {
        size_t length_to_be_read = min<size_t>((length - ncomplete), sizeof(buf));
        if (read(fd, buf, length_to_be_read) < 0) {
            perror("read");
            return;
        }
        ssize_t nwritten = write(STDOUT_FILENO, buf, length_to_be_read);
        if (nwritten < 0) {
            perror("write");
            return;
        }
        ncomplete += nwritten;
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView arg_offset;
    StringView arg_length;
    bool use_read_instead_of_mmap = false;
    Core::ArgsParser args;
    args.add_positional_argument(arg_offset, "Physical Address (Offset)", "offset", Core::ArgsParser::Required::Yes);
    args.add_positional_argument(arg_length, "Length of that region", "length", Core::ArgsParser::Required::Yes);
    args.add_option(use_read_instead_of_mmap, "Read /dev/mem instead of try to map it", nullptr, 'r');

    args.parse(arguments);

    u64 offset = 0;
    u64 length = 0;
    if (!try_set_offset_and_length_parameters(arg_offset, arg_length, offset, length)) {
        warnln("pmemdump: Invalid length or offset parameters\n");
        return 1;
    }

    int fd = open("/dev/mem", O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }
    if (use_read_instead_of_mmap)
        try_to_dump_with_read(fd, offset, length);
    else
        try_to_dump_with_memory_mapping(fd, offset, length);

    close(fd);

    return 0;
}
