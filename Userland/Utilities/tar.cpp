/*
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
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

#include <AK/LexicalPath.h>
#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/FileStream.h>
#include <LibTar/TarStream.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

constexpr size_t buffer_size = 4096;

int main(int argc, char** argv)
{
    bool create = false;
    bool extract = false;
    bool list = false;
    bool verbose = false;
    bool gzip = false;
    const char* archive_file = nullptr;
    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(create, "Create archive", "create", 'c');
    args_parser.add_option(extract, "Extract archive", "extract", 'x');
    args_parser.add_option(list, "List contents", "list", 't');
    args_parser.add_option(verbose, "Print paths", "verbose", 'v');
    args_parser.add_option(gzip, "compress or uncompress file using gzip", "gzip", 'z');
    args_parser.add_option(archive_file, "Archive file", "file", 'f', "FILE");
    args_parser.add_positional_argument(paths, "Paths", "PATHS", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (create + extract + list != 1) {
        warnln("exactly one of -c, -x, and -t can be used");
        return 1;
    }

    if (list || extract) {
        auto file = Core::File::standard_input();

        if (archive_file) {
            auto maybe_file = Core::File::open(archive_file, Core::IODevice::OpenMode::ReadOnly);
            if (maybe_file.is_error()) {
                warnln("Core::File::open: {}", maybe_file.error());
                return 1;
            }
            file = maybe_file.value();
        }

        Core::InputFileStream file_stream(file);
        Compress::GzipDecompressor gzip_stream(file_stream);

        InputStream& file_input_stream = file_stream;
        InputStream& gzip_input_stream = gzip_stream;
        Tar::TarInputStream tar_stream((gzip) ? gzip_input_stream : file_input_stream);
        if (!tar_stream.valid()) {
            warnln("the provided file is not a well-formatted ustar file");
            return 1;
        }
        for (; !tar_stream.finished(); tar_stream.advance()) {
            if (list || verbose)
                outln("{}", tar_stream.header().file_name());

            if (extract) {
                Tar::TarFileStream file_stream = tar_stream.file_contents();

                const Tar::Header& header = tar_stream.header();
                switch (header.type_flag()) {
                case Tar::NormalFile:
                case Tar::AlternateNormalFile: {
                    int fd = open(String(header.file_name()).characters(), O_CREAT | O_WRONLY, header.mode());
                    if (fd < 0) {
                        perror("open");
                        return 1;
                    }

                    Array<u8, buffer_size> buffer;
                    size_t nread;
                    while ((nread = file_stream.read(buffer)) > 0) {
                        if (write(fd, buffer.data(), nread) < 0) {
                            perror("write");
                            return 1;
                        }
                    }
                    close(fd);
                    break;
                }
                case Tar::Directory: {
                    if (mkdir(String(header.file_name()).characters(), header.mode())) {
                        perror("mkdir");
                        return 1;
                    }
                    break;
                }
                default:
                    // FIXME: Implement other file types
                    VERIFY_NOT_REACHED();
                }
            }
        }
        file_stream.close();
        return 0;
    }

    if (create) {
        if (paths.size() == 0) {
            warnln("you must provide at least one path to be archived");
            return 1;
        }

        auto file = Core::File::standard_output();

        if (archive_file) {
            auto maybe_file = Core::File::open(archive_file, Core::IODevice::OpenMode::WriteOnly);
            if (maybe_file.is_error()) {
                warnln("Core::File::open: {}", maybe_file.error());
                return 1;
            }
            file = maybe_file.value();
        }

        Core::OutputFileStream file_stream(file);
        Compress::GzipCompressor gzip_stream(file_stream);

        OutputStream& file_output_stream = file_stream;
        OutputStream& gzip_output_stream = gzip_stream;
        Tar::TarOutputStream tar_stream((gzip) ? gzip_output_stream : file_output_stream);

        auto add_file = [&](String path) {
            auto file = Core::File::construct(path);
            if (!file->open(Core::IODevice::ReadOnly)) {
                warnln("Failed to open {}: {}", path, file->error_string());
                return;
            }

            struct stat statbuf;
            if (lstat(path.characters(), &statbuf) < 0) {
                warnln("Failed stating {}", path);
                return;
            }
            auto canonicalized_path = LexicalPath::canonicalized_path(path);
            tar_stream.add_file(canonicalized_path, statbuf.st_mode, file->read_all());
            if (verbose)
                outln("{}", canonicalized_path);
        };

        auto add_directory = [&](String path, auto handle_directory) -> void {
            struct stat statbuf;
            if (lstat(path.characters(), &statbuf) < 0) {
                warnln("Failed stating {}", path);
                return;
            }
            auto canonicalized_path = LexicalPath::canonicalized_path(path);
            tar_stream.add_directory(canonicalized_path, statbuf.st_mode);
            if (verbose)
                outln("{}", canonicalized_path);

            Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);
            while (it.has_next()) {
                auto child_path = it.next_full_path();
                if (!Core::File::is_directory(child_path)) {
                    add_file(child_path);
                } else {
                    handle_directory(child_path, handle_directory);
                }
            }
        };

        for (const String& path : paths) {
            if (Core::File::is_directory(path)) {
                add_directory(path, add_directory);
            } else {
                add_file(path);
            }
        }

        tar_stream.finish();

        return 0;
    }

    return 0;
}
