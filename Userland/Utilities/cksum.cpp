/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Checksum/Adler32.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibMain/Main.h>
#include <string.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<StringView> paths;
    StringView opt_algorithm;

    Core::ArgsParser args_parser;
    args_parser.add_option(opt_algorithm, "Checksum algorithm (default 'crc32', use 'list' to list available algorithms)", "algorithm", '\0', nullptr);
    args_parser.add_positional_argument(paths, "File", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto algorithm = opt_algorithm.is_empty() ? "crc32"sv : opt_algorithm;

    auto available_algorithms = Vector<StringView> { "crc32"sv, "adler32"sv };

    if (algorithm == "list") {
        outln("Available algorithms:");
        for (auto& available_algorithm : available_algorithms) {
            outln(available_algorithm);
        }
        exit(0);
    }

    if (!available_algorithms.contains_slow(algorithm)) {
        warnln("{}: Unknown checksum algorithm: {}", arguments.strings[0], algorithm);
        exit(1);
    }

    if (paths.is_empty())
        paths.append("-"sv);

    bool fail = false;
    Array<u8, PAGE_SIZE> buffer;

    for (auto& path : paths) {
        auto file_or_error = Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read);
        auto filepath = (path == "-"sv) ? "/dev/stdin"sv : path;
        if (file_or_error.is_error()) {
            warnln("{}: {}: {}", arguments.strings[0], filepath, file_or_error.error());
            fail = true;
            continue;
        }
        auto file = file_or_error.release_value();
        size_t file_size = 0;

        if (algorithm == "crc32"sv) {
            Crypto::Checksum::CRC32 crc32;
            while (!file->is_eof()) {
                auto data_or_error = file->read_some(buffer);
                if (data_or_error.is_error()) {
                    warnln("{}: Failed to read {}: {}", arguments.strings[0], filepath, data_or_error.error());
                    fail = true;
                    continue;
                }
                file_size += data_or_error.value().size();
                crc32.update(data_or_error.value());
            }
            outln("{:08x} {} {}", crc32.digest(), file_size, path);
        } else if (algorithm == "adler32"sv) {
            Crypto::Checksum::Adler32 adler32;
            while (!file->is_eof()) {
                auto data_or_error = file->read_some(buffer);
                if (data_or_error.is_error()) {
                    warnln("{}: Failed to read {}: {}", arguments.strings[0], filepath, data_or_error.error());
                    fail = true;
                    continue;
                }
                file_size += data_or_error.value().size();
                adler32.update(data_or_error.value());
            }
            outln("{:08x} {} {}", adler32.digest(), file_size, path);
        } else {
            warnln("{}: Unknown checksum algorithm: {}", arguments.strings[0], algorithm);
            exit(1);
        }
    }

    return fail;
}
