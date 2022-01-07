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
    TRY(Core::System::pledge("stdio rpath", nullptr));

    auto program_name = LexicalPath::basename(arguments.strings[0]);
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
        warnln("Error: program must be executed as 'md5sum', 'sha1sum', 'sha256sum' or 'sha512sum'; got '{}'", program_name);
        exit(1);
    }

    auto hash_name = program_name.substring_view(0, program_name.length() - 3).to_string().to_uppercase();
    auto paths_help_string = String::formatted("File(s) to print {} checksum of", hash_name);

    Vector<StringView> paths;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(paths, paths_help_string.characters(), "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (paths.is_empty())
        paths.append("-");

    Crypto::Hash::Manager hash;
    hash.initialize(hash_kind);

    auto has_error = false;

    for (auto const& path : paths) {
        NonnullRefPtr<Core::File> file = Core::File::standard_input();
        if (path != "-"sv) {
            auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
            if (file_or_error.is_error()) {
                warnln("{}: {}: {}", program_name, path, file->error_string());
                has_error = true;
                continue;
            }
            file = file_or_error.release_value();
        }

        while (!file->eof() && !file->has_error())
            hash.update(file->read(PAGE_SIZE));
        outln("{:hex-dump}  {}", hash.digest().bytes(), path);
    }
    return has_error ? 1 : 0;
}
