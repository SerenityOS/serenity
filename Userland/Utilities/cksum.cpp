/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Checksum/Adler32.h>
#include <LibCrypto/Checksum/CRC32.h>

int main(int argc, char** argv)
{
    Vector<const char*> paths;
    const char* opt_algorithm = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(opt_algorithm, "Checksum algorithm (default 'crc32', use 'list' to list available algorithms)", "algorithm", '\0', nullptr);
    args_parser.add_positional_argument(paths, "File", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

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
        warnln("{}: Unknown checksum algorithm: {}", argv[0], algorithm);
        exit(1);
    }

    if (paths.is_empty())
        paths.append("-");

    bool fail = false;
    for (auto& path : paths) {
        auto file = Core::File::construct((StringView(path) == "-") ? "/dev/stdin" : path);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            warnln("{}: {}: {}", argv[0], path, file->error_string());
            fail = true;
            continue;
        }
        auto file_buffer = file->read_all();
        auto bytes = file_buffer.bytes().size();
        if (algorithm == "crc32") {
            outln("{} {} {}", Crypto::Checksum::CRC32 { file_buffer.bytes() }.digest(), bytes, path);
        } else if (algorithm == "adler32") {
            outln("{} {} {}", Crypto::Checksum::Adler32 { file_buffer.bytes() }.digest(), bytes, path);
        } else {
            warnln("{}: Unknown checksum algorithm: {}", argv[0], algorithm);
            exit(1);
        }
    }

    return fail;
}
