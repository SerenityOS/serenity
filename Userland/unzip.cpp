/*
 * Copyright (c) 2020, Andrés Vieira <anvieiravazquez@gmail.com>
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

#include <AK/MappedFile.h>
#include <AK/NumberFormat.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <string.h>
#include <sys/stat.h>

static const u8 central_directory_file_header_sig[] = "\x50\x4b\x01\x02";

static bool seek_and_read(u8* buffer, const MappedFile& file, off_t seek_to, size_t bytes_to_read)
{
    if (!buffer)
        return false;

    if ((size_t)seek_to >= file.size())
        return false;

    memcpy(buffer, (const char*)file.data() + seek_to, bytes_to_read);

    return true;
}

static bool find_next_central_directory(off_t file_size, const MappedFile& file, off_t current_index, off_t& return_index)
{
    off_t start_index = current_index == 0 ? current_index : current_index + 1;
    for (off_t index = start_index; index < file_size - 4; index++) {
        u8 buffer[4];
        if (!seek_and_read(buffer, file, index, 4))
            return false;

        if (!memcmp(buffer, central_directory_file_header_sig, 4)) {
            return_index = index;
            return true;
        }
    }
    return false;
}

static bool unpack_file_for_central_directory_index(off_t central_directory_index, const MappedFile& file)
{
    enum CentralFileDirectoryHeaderOffsets {
        CFDHCompressionMethodOffset = 10,
        CFDHLocalFileHeaderIndexOffset = 42,
    };
    enum LocalFileHeaderOffsets {
        LFHCompressionMethodOffset = 8,
        LFHCompressedSizeOffset = 18,
        LFHFileNameLengthOffset = 26,
        LFHExtraFieldLengthOffset = 28,
        LFHFileNameBaseOffset = 30,
    };
    enum CompressionMethod {
        None = 0,
        Shrunk = 1,
        Factor1 = 2,
        Factor2 = 3,
        Factor3 = 4,
        Factor4 = 5,
        Implode = 6,
        Deflate = 8,
        EnhancedDeflate = 9,
        PKWareDCLImplode = 10,
        BZIP2 = 12,
        LZMA = 14,
        TERSE = 18,
        LZ77 = 19,
    };

    u8 buffer[4];
    if (!seek_and_read(buffer, file, central_directory_index + CFDHLocalFileHeaderIndexOffset, 4))
        return false;
    off_t local_file_header_index = buffer[3] << 24 | buffer[2] << 16 | buffer[1] << 8 | buffer[0];

    if (!seek_and_read(buffer, file, local_file_header_index + LFHCompressionMethodOffset, 2))
        return false;
    auto compression_method = buffer[1] << 8 | buffer[0];
    // FIXME: Remove once any decompression is supported.
    ASSERT(compression_method == None);

    if (!seek_and_read(buffer, file, local_file_header_index + LFHCompressedSizeOffset, 4))
        return false;
    off_t compressed_file_size = buffer[3] << 24 | buffer[2] << 16 | buffer[1] << 8 | buffer[0];

    if (!seek_and_read(buffer, file, local_file_header_index + LFHFileNameLengthOffset, 2))
        return false;
    off_t file_name_length = buffer[1] << 8 | buffer[0];

    if (!seek_and_read(buffer, file, local_file_header_index + LFHExtraFieldLengthOffset, 2))
        return false;
    off_t extra_field_length = buffer[1] << 8 | buffer[0];

    char file_name[file_name_length + 1];
    if (!seek_and_read((u8*)file_name, file, local_file_header_index + LFHFileNameBaseOffset, file_name_length))
        return false;
    file_name[file_name_length] = '\0';

    if (file_name[file_name_length - 1] == '/') {
        if (mkdir(file_name, 0755) < 0) {
            perror("mkdir");
            return false;
        }
    } else {
        auto new_file = Core::File::construct(String { file_name });
        if (!new_file->open(Core::IODevice::WriteOnly)) {
            fprintf(stderr, "Can't write file %s: %s\n", file_name, new_file->error_string());
            return false;
        }

        printf(" extracting: %s\n", file_name);
        u8 raw_file_contents[compressed_file_size];
        if (!seek_and_read(raw_file_contents, file, local_file_header_index + LFHFileNameBaseOffset + file_name_length + extra_field_length, compressed_file_size))
            return false;

        // FIXME: Try to uncompress data here. We're just ignoring it as no decompression methods are implemented yet.
        if (!new_file->write(raw_file_contents, compressed_file_size)) {
            fprintf(stderr, "Can't write file contents in %s: %s\n", file_name, new_file->error_string());
            return false;
        }

        if (!new_file->close()) {
            fprintf(stderr, "Can't close file %s: %s\n", file_name, new_file->error_string());
            return false;
        }
    }

    return true;
}

int main(int argc, char** argv)
{
    const char* path;
    int map_size_limit = 32 * MiB;

    Core::ArgsParser args_parser;
    args_parser.add_option(map_size_limit, "Maximum chunk size to map", "map-size-limit", 0, "size");
    args_parser.add_positional_argument(path, "File to unzip", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    String zip_file_path { path };

    struct stat st;
    int rc = stat(zip_file_path.characters(), &st);
    if (rc < 0) {
        perror("stat");
        return 1;
    }

    // FIXME: Map file chunk-by-chunk once we have mmap() with offset.
    //        This will require mapping some parts then unmapping them repeatedly,
    //        but it would be significantly faster and less syscall heavy than seek()/read() at every read.
    if (st.st_size >= map_size_limit) {
        fprintf(stderr, "unzip warning: Refusing to map file since it is larger than %s, pass '--map-size-limit %d' to get around this\n",
            human_readable_size(map_size_limit).characters(),
            round_up_to_power_of_two(st.st_size, 16));
        return 1;
    }

    MappedFile mapped_file { zip_file_path };
    if (!mapped_file.is_valid())
        return 1;

    printf("Archive: %s\n", zip_file_path.characters());

    off_t index = 0;
    while (find_next_central_directory(st.st_size, mapped_file, index, index)) {
        bool success = unpack_file_for_central_directory_index(index, mapped_file);
        if (!success) {
            printf("Could not find local file header for a file.\n");
            return 4;
        }
    }

    return 0;
}
