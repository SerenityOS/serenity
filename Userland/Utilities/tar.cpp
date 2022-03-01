/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/LexicalPath.h>
#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibArchive/TarStream.h>
#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

constexpr size_t buffer_size = 4096;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool create = false;
    bool extract = false;
    bool list = false;
    bool verbose = false;
    bool gzip = false;
    bool no_auto_compress = false;
    StringView archive_file;
    const char* directory = nullptr;
    Vector<const char*> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(create, "Create archive", "create", 'c');
    args_parser.add_option(extract, "Extract archive", "extract", 'x');
    args_parser.add_option(list, "List contents", "list", 't');
    args_parser.add_option(verbose, "Print paths", "verbose", 'v');
    args_parser.add_option(gzip, "Compress or decompress file using gzip", "gzip", 'z');
    args_parser.add_option(no_auto_compress, "Do not use the archive suffix to select the compression algorithm", "no-auto-compress", 0);
    args_parser.add_option(directory, "Directory to extract to/create from", "directory", 'C', "DIRECTORY");
    args_parser.add_option(archive_file, "Archive file", "file", 'f', "FILE");
    args_parser.add_positional_argument(paths, "Paths", "PATHS", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (create + extract + list != 1) {
        warnln("exactly one of -c, -x, and -t can be used");
        return 1;
    }

    if (!no_auto_compress && !archive_file.is_empty()) {
        if (archive_file.ends_with(".gz"sv) || archive_file.ends_with(".tgz"sv))
            gzip = true;
    }

    if (list || extract) {
        auto file = Core::File::standard_input();

        if (!archive_file.is_empty())
            file = TRY(Core::File::open(archive_file, Core::OpenMode::ReadOnly));

        if (directory)
            TRY(Core::System::chdir(directory));

        Core::InputFileStream file_stream(file);
        Compress::GzipDecompressor gzip_stream(file_stream);

        InputStream& file_input_stream = file_stream;
        InputStream& gzip_input_stream = gzip_stream;
        Archive::TarInputStream tar_stream((gzip) ? gzip_input_stream : file_input_stream);
        // FIXME: implement ErrorOr<TarInputStream>?
        if (!tar_stream.valid()) {
            warnln("the provided file is not a well-formatted ustar file");
            return 1;
        }
        for (; !tar_stream.finished(); tar_stream.advance()) {
            if (list || verbose)
                outln("{}", tar_stream.header().filename());

            if (extract) {
                Archive::TarFileStream file_stream = tar_stream.file_contents();

                const Archive::TarFileHeader& header = tar_stream.header();

                LexicalPath path = LexicalPath(header.filename());
                if (!header.prefix().is_empty())
                    path = path.prepend(header.prefix());

                String absolute_path = Core::File::absolute_path(path.string());

                switch (header.type_flag()) {
                case Archive::TarFileType::NormalFile:
                case Archive::TarFileType::AlternateNormalFile: {
                    Core::File::ensure_parent_directories(absolute_path);

                    int fd = TRY(Core::System::open(absolute_path, O_CREAT | O_WRONLY, header.mode()));

                    Array<u8, buffer_size> buffer;
                    while (file_stream.read(buffer) > 0)
                        TRY(Core::System::write(fd, buffer.span()));

                    TRY(Core::System::close(fd));
                    break;
                }
                case Archive::TarFileType::SymLink: {
                    Core::File::ensure_parent_directories(absolute_path);

                    TRY(Core::System::symlink(header.link_name(), absolute_path));
                    break;
                }
                case Archive::TarFileType::Directory: {
                    Core::File::ensure_parent_directories(absolute_path);

                    auto result_or_error = Core::System::mkdir(absolute_path, header.mode());
                    if (result_or_error.is_error() && result_or_error.error().code() != EEXIST)
                        return result_or_error.error();
                    break;
                }
                case Archive::TarFileType::GlobalExtendedHeader: {
                    dbgln("ignoring global extended header: {}", header.filename());
                    break;
                }
                case Archive::TarFileType::ExtendedHeader: {
                    dbgln("ignoring extended header: {}", header.filename());
                    break;
                }
                default:
                    // FIXME: Implement other file types
                    warnln("file type '{}' of {} is not yet supported", (char)header.type_flag(), header.filename());
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

        if (!archive_file.is_empty())
            file = TRY(Core::File::open(archive_file, Core::OpenMode::WriteOnly));

        if (directory)
            TRY(Core::System::chdir(directory));

        Core::OutputFileStream file_stream(file);
        Compress::GzipCompressor gzip_stream(file_stream);

        OutputStream& file_output_stream = file_stream;
        OutputStream& gzip_output_stream = gzip_stream;
        Archive::TarOutputStream tar_stream((gzip) ? gzip_output_stream : file_output_stream);

        auto add_file = [&](String path) -> ErrorOr<void> {
            auto file = Core::File::construct(path);
            if (!file->open(Core::OpenMode::ReadOnly)) {
                warnln("Failed to open {}: {}", path, file->error_string());
                return {};
            }

            auto statbuf_or_error = Core::System::lstat(path);
            if (statbuf_or_error.is_error())
                return statbuf_or_error.error();

            auto statbuf = statbuf_or_error.value();
            auto canonicalized_path = LexicalPath::canonicalized_path(path);
            tar_stream.add_file(canonicalized_path, statbuf.st_mode, file->read_all());
            if (verbose)
                outln("{}", canonicalized_path);

            return {};
        };

        auto add_directory = [&](String path, auto handle_directory) -> ErrorOr<void> {
            auto statbuf_or_error = Core::System::lstat(path);
            if (statbuf_or_error.is_error())
                return statbuf_or_error.error();

            auto statbuf = statbuf_or_error.value();
            auto canonicalized_path = LexicalPath::canonicalized_path(path);
            tar_stream.add_directory(canonicalized_path, statbuf.st_mode);
            if (verbose)
                outln("{}", canonicalized_path);

            Core::DirIterator it(path, Core::DirIterator::Flags::SkipParentAndBaseDir);
            while (it.has_next()) {
                auto child_path = it.next_full_path();
                if (!Core::File::is_directory(child_path)) {
                    TRY(add_file(child_path));
                } else {
                    TRY(handle_directory(child_path, handle_directory));
                }
            }

            return {};
        };

        for (auto const& path : paths) {
            if (Core::File::is_directory(path)) {
                TRY(add_directory(path, add_directory));
            } else {
                TRY(add_file(path));
            }
        }

        tar_stream.finish();

        return 0;
    }

    return 0;
}
