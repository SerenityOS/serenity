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
#include <LibArchive/Zip.h>
#include <LibCompress/Deflate.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <sys/stat.h>

static bool unpack_zip_member(Archive::ZipMember zip_member)
{
    if (zip_member.is_directory) {
        if (mkdir(zip_member.name.characters(), 0755) < 0) {
            perror("mkdir");
            return false;
        }
        outln(" extracting: {}", zip_member.name);
        return true;
    }
    auto new_file = Core::File::construct(zip_member.name);
    if (!new_file->open(Core::IODevice::WriteOnly)) {
        warnln("Can't write file {}: {}", zip_member.name, new_file->error_string());
        return false;
    }

    outln(" extracting: {}", zip_member.name);

    // TODO: verify CRC32s match!
    switch (zip_member.compression_method) {
    case Archive::ZipCompressionMethod::Store: {
        if (!new_file->write(zip_member.compressed_data.data(), zip_member.compressed_data.size())) {
            warnln("Can't write file contents in {}: {}", zip_member.name, new_file->error_string());
            return false;
        }
        break;
    }
    case Archive::ZipCompressionMethod::Deflate: {
        auto decompressed_data = Compress::DeflateDecompressor::decompress_all(zip_member.compressed_data);
        if (!decompressed_data.has_value()) {
            warnln("Failed decompressing file {}", zip_member.name);
            return false;
        }
        if (decompressed_data.value().size() != zip_member.uncompressed_size) {
            warnln("Failed decompressing file {}", zip_member.name);
            return false;
        }
        if (!new_file->write(decompressed_data.value().data(), decompressed_data.value().size())) {
            warnln("Can't write file contents in {}: {}", zip_member.name, new_file->error_string());
            return false;
        }
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    if (!new_file->close()) {
        warnln("Can't close file {}: {}", zip_member.name, new_file->error_string());
        return false;
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
        warnln("unzip warning: Refusing to map file since it is larger than {}, pass '--map-size-limit {}' to get around this",
            human_readable_size(map_size_limit).characters(),
            round_up_to_power_of_two(st.st_size, 16));
        return 1;
    }

    auto file_or_error = MappedFile::map(zip_file_path);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", zip_file_path, file_or_error.error());
        return 1;
    }
    auto& mapped_file = *file_or_error.value();

    warnln("Archive: {}", zip_file_path);

    auto zip_file = Archive::Zip::try_create(mapped_file.bytes());
    if (!zip_file.has_value()) {
        warnln("Invalid zip file {}", zip_file_path);
        return 1;
    }

    auto success = zip_file->for_each_member([&](auto zip_member) {
        return unpack_zip_member(zip_member) ? IterationDecision::Continue : IterationDecision::Break;
    });

    return success ? 0 : 1;
}
