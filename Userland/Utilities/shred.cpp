/*
 * Copyright (c) 2024, Romain Chardiny <romain.chardiny@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio unix rpath wpath cpath"));

    Vector<StringView> paths;
    bool remove_file = false;
    bool verbose = false;
    u32 iterations = 3;
    Optional<StringView> random_source;

    Core::ArgsParser args_parser;
    args_parser.add_option(remove_file, "Deallocate and remove file after overwriting", nullptr, 'u');
    args_parser.add_option(verbose, "Show progress", "verbose", 'v');
    args_parser.add_option(iterations, "Overwrite N times instead of the default (3)", "iterations", 'n', "N");
    args_parser.add_option(random_source, "Get random bytes from FILE", "random-source", 0, "FILE");
    args_parser.add_positional_argument(paths, "Path(s) to overwrite", "FILE", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto rng_file = TRY(Core::File::open(random_source.value_or("/dev/random"sv), Core::File::OpenMode::Read));

    for (auto& path : paths) {
        auto file = TRY(Core::File::open(path, Core::File::OpenMode::ReadWrite | Core::File::OpenMode::DontCreate));

        off_t output_file_length = TRY(FileSystem::size_from_fstat(file->fd()));

        for (u32 iter = 0; iter < iterations; iter++) {
            if (verbose)
                outln("shred: {}: pass {}/{} (random)", path, iter + 1, iterations);

            Array<u8, BUFSIZ> buffer;
            off_t total_written = 0;
            ssize_t nread = 0;
            ssize_t nwritten = 0;

            while (total_written < output_file_length) {
                auto buffer_span = buffer.span().trim(output_file_length - total_written);

                nread = TRY(Core::System::read(rng_file->fd(), buffer_span));

                if (nread == 0)
                    break;

                nwritten = TRY(Core::System::write(file->fd(), buffer_span));

                if (nwritten == 0)
                    break;

                total_written += nwritten;
            }

            TRY(Core::System::fsync(file->fd()));
            TRY(file->seek(0, SeekMode::SetPosition));
        }

        if (remove_file) {
            TRY(Core::System::ftruncate(file->fd(), 0));
            TRY(Core::System::unlink(path));
        }
    }

    return 0;
}
