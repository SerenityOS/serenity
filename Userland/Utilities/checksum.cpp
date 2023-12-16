/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibCrypto/Hash/HashManager.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    auto program_name = LexicalPath::basename(arguments.strings[0]);
    auto hash_kind = Crypto::Hash::HashKind::None;

    if (program_name == "b2sum")
        hash_kind = Crypto::Hash::HashKind::BLAKE2b;
    else if (program_name == "md5sum")
        hash_kind = Crypto::Hash::HashKind::MD5;
    else if (program_name == "sha1sum")
        hash_kind = Crypto::Hash::HashKind::SHA1;
    else if (program_name == "sha256sum")
        hash_kind = Crypto::Hash::HashKind::SHA256;
    else if (program_name == "sha512sum")
        hash_kind = Crypto::Hash::HashKind::SHA512;

    if (hash_kind == Crypto::Hash::HashKind::None) {
        warnln("Error: program must be executed as 'b2sum', 'md5sum', 'sha1sum', 'sha256sum' or 'sha512sum'; got '{}'", program_name);
        exit(1);
    }

    auto hash_name = program_name.substring_view(0, program_name.length() - 3).to_byte_string().to_uppercase();
    auto paths_help_string = ByteString::formatted("File(s) to print {} checksum of", hash_name);

    bool verify_from_paths = false;
    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(verify_from_paths, "Verify checksums from file(s)", "check", 'c');
    args_parser.add_positional_argument(paths, paths_help_string.characters(), "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (paths.is_empty())
        paths.append("-"sv);

    Crypto::Hash::Manager hash;
    hash.initialize(hash_kind);

    bool has_error = false;
    int read_fail_count = 0;
    int failed_verification_count = 0;

    for (auto const& path : paths) {
        auto file_or_error = Core::File::open_file_or_standard_stream(path, Core::File::OpenMode::Read);
        if (file_or_error.is_error()) {
            ++read_fail_count;
            has_error = true;
            warnln("{}: {}", path, file_or_error.release_error());
            continue;
        }
        auto file = file_or_error.release_value();
        Array<u8, PAGE_SIZE> buffer;
        if (!verify_from_paths) {
            while (!file->is_eof())
                hash.update(TRY(file->read_some(buffer)));
            outln("{:hex-dump}  {}", hash.digest().bytes(), path);
        } else {
            StringBuilder checksum_list_contents;
            Array<u8, 1> checksum_list_buffer;
            while (!file->is_eof())
                checksum_list_contents.append(TRY(file->read_some(checksum_list_buffer)).data()[0]);
            Vector<StringView> const lines = checksum_list_contents.string_view().split_view("\n"sv);

            for (size_t i = 0; i < lines.size(); ++i) {
                Vector<StringView> const line = lines[i].split_view("  "sv);
                if (line.size() != 2) {
                    ++read_fail_count;
                    // The real line number is greater than the iterator.
                    warnln("{}: {}: Failed to parse line {}", program_name, path, i + 1);
                    continue;
                }

                // line[0] = checksum
                // line[1] = filename
                StringView const filename = line[1];
                auto file_from_filename_or_error = Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read);
                if (file_from_filename_or_error.is_error()) {
                    ++read_fail_count;
                    warnln("{}: {}", filename, file_from_filename_or_error.release_error());
                    continue;
                }
                auto file_from_filename = file_from_filename_or_error.release_value();
                hash.reset();
                while (!file_from_filename->is_eof())
                    hash.update(TRY(file_from_filename->read_some(buffer)));
                if (ByteString::formatted("{:hex-dump}", hash.digest().bytes()) == line[0])
                    outln("{}: OK", filename);
                else {
                    ++failed_verification_count;
                    warnln("{}: FAILED", filename);
                }
            }
        }
    }
    // Print the warnings here in order to only print them once.
    if (verify_from_paths) {
        if (read_fail_count) {
            if (read_fail_count == 1)
                warnln("WARNING: 1 file could not be read");
            else
                warnln("WARNING: {} files could not be read", read_fail_count);
            has_error = true;
        }

        if (failed_verification_count) {
            if (failed_verification_count == 1)
                warnln("WARNING: 1 checksum did NOT match");
            else
                warnln("WARNING: {} checksums did NOT match", failed_verification_count);
            has_error = true;
        }
    }
    return has_error ? 1 : 0;
}
