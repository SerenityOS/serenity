/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Checksum/Adler32.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <LibMain/Main.h>
#include <string.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<const char*> paths;
    const char* opt_algorithm = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(opt_algorithm, "Checksum algorithm (default 'crc32', use 'list' to list available algorithms)", "algorithm", '\0', nullptr);
    args_parser.add_positional_argument(paths, "File", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto algorithm = (opt_algorithm == nullptr) ? "crc32" : String(opt_algorithm).to_lowercase();

    auto available_algorithms = Vector<String> { "crc32", "adler32" };

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
        paths.append("-");

    bool fail = false;
    for (auto& path : paths) {
        auto filepath = (StringView(path) == "-") ? "/dev/stdin" : path;
        auto file = Core::File::construct(filepath);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            warnln("{}: {}: {}", arguments.strings[0], path, file->error_string());
            fail = true;
            continue;
        }
        struct stat st;
        if (fstat(file->fd(), &st) < 0) {
            warnln("{}: Failed to fstat {}: {}", arguments.strings[0], filepath, strerror(errno));
            fail = true;
            continue;
        }
        if (algorithm == "crc32") {
            Crypto::Checksum::CRC32 crc32;
            while (!file->eof() && !file->has_error())
                crc32.update(file->read(PAGE_SIZE));
            if (file->has_error()) {
                warnln("{}: Failed to read {}: {}", arguments.strings[0], filepath, file->error_string());
                fail = true;
                continue;
            }
            outln("{:08x} {} {}", crc32.digest(), st.st_size, path);
        } else if (algorithm == "adler32") {
            Crypto::Checksum::Adler32 adler32;
            while (!file->eof() && !file->has_error())
                adler32.update(file->read(PAGE_SIZE));
            if (file->has_error()) {
                warnln("{}: Failed to read {}: {}", arguments.strings[0], filepath, file->error_string());
                fail = true;
                continue;
            }
            outln("{:08x} {} {}", adler32.digest(), st.st_size, path);
        } else {
            warnln("{}: Unknown checksum algorithm: {}", arguments.strings[0], algorithm);
            exit(1);
        }
    }

    return fail;
}
