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

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <string.h>
#include <sys/stat.h>

static const u8 central_directory_file_header_sig[] = "\x50\x4b\x01\x02";

bool seek_and_read(u8* buffer, Core::File& file, off_t seek_to, size_t bytes_to_read)
{
    if (!buffer)
        return false;

    if (!file.seek(seek_to))
        return false;

    size_t read_bytes = 0;
    while (read_bytes < bytes_to_read) {
        auto nread = file.read(buffer + read_bytes, bytes_to_read - read_bytes);
        if (nread <= 0)
            return false;
        read_bytes += nread;
    }
    
    return true;
}

bool find_next_central_directory(off_t file_size, Core::File& file, off_t current_index, off_t& return_index)
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

bool unpack_file_for_central_directory_index(off_t central_directory_index, Core::File& file)
{
    u8 buffer[4];
    if (!seek_and_read(buffer, file, central_directory_index + 42, 4))
        return false;
    off_t local_file_header_index = buffer[3] << 24 | buffer[2] << 16 | buffer[1] << 8 | buffer[0];

    if (!seek_and_read(buffer, file, local_file_header_index + 18, 4))
        return false;
    off_t compressed_file_size = buffer[3] << 24 | buffer[2] << 16 | buffer[1] << 8 | buffer[0];

    if (!seek_and_read(buffer, file, local_file_header_index + 26, 2))
        return false;
    off_t file_name_length = buffer[1] << 8 | buffer[0];

    if (!seek_and_read(buffer, file, local_file_header_index + 28, 2))
        return false;
    off_t extra_field_length = buffer[1] << 8 | buffer[0];

    if (!seek_and_read(buffer, file, local_file_header_index + 30, file_name_length))
        return false;
    char file_name[file_name_length + 1];
    memcpy(file_name, buffer, file_name_length);
    file_name[file_name_length] = '\0';

    auto new_file = Core::File::construct(String { file_name });
    if (!new_file->open(Core::IODevice::WriteOnly)) {
        fprintf(stderr, "Can't write file %s: %s\n", file_name, file.error_string());
        return false;
    }

    printf(" extracting: %s\n", file_name);
    u8 raw_file_contents[compressed_file_size];
    if (!seek_and_read(raw_file_contents, file, local_file_header_index + 30 + file_name_length + extra_field_length, compressed_file_size))
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

    return true;
}

int main(int argc, char** argv)
{
    const char* path;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "File to unzip", "path", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    String zip_file_path = { path };

    auto file = Core::File::construct(zip_file_path);
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Can't read file %s: %s\n", zip_file_path.characters(), file->error_string());
        return 2;
    }

    struct stat st;
    int rc = fstat(file->fd(), &st);
    if (rc < 0) {
        memset(&st, 0, sizeof(st));
        return 3;
    }

    printf("Archive: %s\n", zip_file_path.characters());

    off_t index = 0;
    while (find_next_central_directory(st.st_size, file, index, index)) {
        bool success = unpack_file_for_central_directory_index(index, file);
        if (!success) {
            printf("Could not find local file header for a file.\n");
            return 4;
        }
    }

    return 0;
}
