/*
 * Copyright (c) 2021-2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Checksum/Adler32.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibCrypto/Checksum/cksum.h>
#include <LibMain/Main.h>
#include <string.h>

struct Data {
    u32 checksum { 0 };
    size_t file_size { 0 };
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<StringView> paths;
    StringView opt_algorithm;

    Core::ArgsParser args_parser;
    args_parser.add_option(opt_algorithm, "Checksum algorithm (default 'cksum', use 'list' to list available algorithms)", "algorithm", '\0', nullptr);
    args_parser.add_positional_argument(paths, "File", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto algorithm = opt_algorithm.is_empty() ? "cksum"sv : opt_algorithm;

    auto available_algorithms = Vector<StringView> { "cksum"sv, "crc32"sv, "adler32"sv };

    if (algorithm == "list") {
        outln("Available algorithms:");
        for (auto& available_algorithm : available_algorithms) {
            outln(available_algorithm);
        }
        exit(0);
    }

    Array<u8, PAGE_SIZE> buffer;
    bool fail = false;
    Function<Data(Core::File*, StringView path)> build_checksum_data_using_file;
    if (algorithm == "cksum") {
        build_checksum_data_using_file = [&buffer, &arguments, &fail](Core::File* file, StringView path) {
            Crypto::Checksum::cksum cksum;
            size_t file_size = 0;
            while (!file->is_eof()) {
                auto data_or_error = file->read_some(buffer);
                if (data_or_error.is_error()) {
                    warnln("{}: Failed to read {}: {}", arguments.strings[0], path, data_or_error.error());
                    fail = true;
                    continue;
                }
                file_size += data_or_error.value().size();
                cksum.update(data_or_error.value());
            }
            return Data { .checksum = cksum.digest(), .file_size = file_size };
        };
    } else if (algorithm == "crc32") {
        build_checksum_data_using_file = [&buffer, &arguments, &fail](Core::File* file, StringView path) {
            Crypto::Checksum::CRC32 crc32;
            size_t file_size = 0;
            while (!file->is_eof()) {
                auto data_or_error = file->read_some(buffer);
                if (data_or_error.is_error()) {
                    warnln("{}: Failed to read {}: {}", arguments.strings[0], path, data_or_error.error());
                    fail = true;
                    continue;
                }
                file_size += data_or_error.value().size();
                crc32.update(data_or_error.value());
            }
            return Data { .checksum = crc32.digest(), .file_size = file_size };
        };
    } else if (algorithm == "adler32") {
        build_checksum_data_using_file = [&buffer, &arguments, &fail](Core::File* file, StringView path) {
            Crypto::Checksum::Adler32 adler32;
            size_t file_size = 0;
            while (!file->is_eof()) {
                auto data_or_error = file->read_some(buffer);
                if (data_or_error.is_error()) {
                    warnln("{}: Failed to read {}: {}", arguments.strings[0], path, data_or_error.error());
                    fail = true;
                    continue;
                }
                file_size += data_or_error.value().size();
                adler32.update(data_or_error.value());
            }
            return Data { .checksum = adler32.digest(), .file_size = file_size };
        };
    } else {
        warnln("{}: Unknown checksum algorithm: {}", arguments.strings[0], algorithm);
        exit(1);
    }

    if (paths.is_empty()) {
        // The POSIX spec explains that when given no file operands, we should read from stdin and only print the checksum and file size. So let's do
        // this here.
        auto file_or_error = Core::File::open_file_or_standard_stream("-"sv, Core::File::OpenMode::Read);
        auto filepath = "/dev/stdin"sv;
        if (file_or_error.is_error()) {
            warnln("{}: {}: {}", arguments.strings[0], filepath, file_or_error.error());
            exit(1);
        }

        auto file = file_or_error.release_value();
        auto data = build_checksum_data_using_file(file.ptr(), filepath);

        outln("{} {}", data.checksum, data.file_size);

        // We return fail here since build_checksum_data_using_file() may set it to true, indicating problems have occurred.
        return fail;
    }

    for (auto& path : paths) {
        auto file_or_error = Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read);
        auto filepath = (path == "-"sv) ? "/dev/stdin"sv : path;
        if (file_or_error.is_error()) {
            warnln("{}: {}: {}", arguments.strings[0], filepath, file_or_error.error());
            fail = true;
            continue;
        }

        auto file = file_or_error.release_value();
        auto data = build_checksum_data_using_file(file.ptr(), path);

        outln("{} {} {}", data.checksum, data.file_size, path);
    }

    return fail;
}
