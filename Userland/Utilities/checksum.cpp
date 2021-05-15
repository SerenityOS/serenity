/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Hash/HashManager.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto program_name = StringView { argv[0] };
    auto hash_kind = Crypto::Hash::HashKind::None;

    if (program_name == "md5sum")
        hash_kind = Crypto::Hash::HashKind::MD5;
    else if (program_name == "sha1sum")
        hash_kind = Crypto::Hash::HashKind::SHA1;
    else if (program_name == "sha256sum")
        hash_kind = Crypto::Hash::HashKind::SHA256;
    else if (program_name == "sha512sum")
        hash_kind = Crypto::Hash::HashKind::SHA512;

    if (hash_kind == Crypto::Hash::HashKind::None) {
        warnln("Error: program must be executed as 'md5sum', 'sha1sum', 'sha256sum' or 'sha512sum'; got '{}'", argv[0]);
        exit(1);
    }

    auto hash_name = program_name.substring_view(0, program_name.length() - 3).to_string().to_uppercase();
    auto paths_help_string = String::formatted("File(s) to print {} checksum of", hash_name);

    Vector<String> paths;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(paths, paths_help_string.characters(), "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (paths.is_empty())
        paths.append("-");

    Crypto::Hash::Manager hash;
    hash.initialize(hash_kind);

    bool success;
    auto has_error = false;
    auto file = Core::File::construct();

    for (auto const& path : paths) {
        if (path == "-") {
            success = file->open(STDIN_FILENO, Core::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescriptor::No);
        } else {
            file->set_filename(path);
            success = file->open(Core::OpenMode::ReadOnly);
        }
        if (!success) {
            warnln("{}: {}: {}", argv[0], path, file->error_string());
            has_error = true;
            continue;
        }

        while (!file->eof() && !file->has_error())
            hash.update(file->read(PAGE_SIZE));
        auto digest = hash.digest();
        auto digest_data = digest.immutable_data();
        StringBuilder builder;
        for (size_t i = 0; i < hash.digest_size(); ++i)
            builder.appendff("{:02x}", digest_data[i]);
        auto hash_sum_hex = builder.build();
        outln("{}  {}", hash_sum_hex, path);
    }
    return has_error ? 1 : 0;
}
