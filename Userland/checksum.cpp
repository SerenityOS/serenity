/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Hash/HashManager.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto program_name = StringView { argv[0] };
    auto hash_kind { Crypto::Hash::HashKind::None };

    if (program_name == "md5sum")
        hash_kind = Crypto::Hash::HashKind::MD5;
    else if (program_name == "sha1sum")
        hash_kind = Crypto::Hash::HashKind::SHA1;
    else if (program_name == "sha256sum")
        hash_kind = Crypto::Hash::HashKind::SHA256;
    else if (program_name == "sha512sum")
        hash_kind = Crypto::Hash::HashKind::SHA512;

    if (hash_kind == Crypto::Hash::HashKind::None) {
        fprintf(stderr, "Error: program must be executed as 'md5sum', 'sha1sum', 'sha256sum' or 'sha512sum'; got '%s'\n", argv[0]);
        exit(1);
    }

    auto hash_name = program_name.substring_view(0, program_name.length() - 3).to_string().to_uppercase();
    auto paths_help_string = String::format("File(s) to print %s checksum of", hash_name.characters());

    Vector<const char*> paths;

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

    for (auto path : paths) {
        if (StringView { path } == "-") {
            success = file->open(STDIN_FILENO, Core::IODevice::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescription::No);
        } else {
            file->set_filename(path);
            success = file->open(Core::IODevice::OpenMode::ReadOnly);
        }
        if (!success) {
            fprintf(stderr, "%s: %s: %s\n", argv[0], path, file->error_string());
            has_error = true;
            continue;
        }
        hash.update(file->read_all());
        auto digest = hash.digest();
        auto digest_data = digest.immutable_data();
        StringBuilder builder;
        for (size_t i = 0; i < hash.digest_size(); ++i)
            builder.appendf("%02x", digest_data[i]);
        auto hash_sum_hex = builder.build();
        printf("%s  %s\n", hash_sum_hex.characters(), path);
    }
    return has_error ? 1 : 0;
}
