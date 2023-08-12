/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    Vector<StringView> filenames;
    bool keep_input_files { false };
    bool write_to_stdout { false };

    Core::ArgsParser args_parser;
    // NOTE: If the user run this program via the /bin/zcat symlink,
    // then emulate gzip decompression to stdout.
    if (args.argc > 0 && args.strings[0] == "zcat"sv) {
        write_to_stdout = true;
    } else {
        args_parser.add_option(keep_input_files, "Keep (don't delete) input files", "keep", 'k');
        args_parser.add_option(write_to_stdout, "Write to stdout, keep original files unchanged", "stdout", 'c');
    }
    args_parser.add_positional_argument(filenames, "File to decompress", "FILE");
    args_parser.parse(args);

    if (write_to_stdout)
        keep_input_files = true;

    for (auto filename : filenames) {

        DeprecatedString input_filename;
        DeprecatedString output_filename;
        if (filename.ends_with(".gz"sv)) {
            input_filename = filename;
            output_filename = filename.substring_view(0, filename.length() - 3);
        } else {
            input_filename = DeprecatedString::formatted("{}.gz", filename);
            output_filename = filename;
        }

        auto output_stream = write_to_stdout ? TRY(Core::File::standard_output()) : TRY(Core::File::open(output_filename, Core::File::OpenMode::Write));
        TRY(Compress::GzipDecompressor::decompress_file(input_filename, move(output_stream)));

        if (!keep_input_files)
            TRY(Core::System::unlink(input_filename));
    }
    return 0;
}
