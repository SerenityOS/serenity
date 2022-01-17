/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileStream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

static bool decompress_file(Buffered<Core::InputFileStream>& input_stream, Buffered<Core::OutputFileStream>& output_stream)
{
    auto gzip_stream = Compress::GzipDecompressor { input_stream };

    u8 buffer[4096];

    while (!gzip_stream.has_any_error() && !gzip_stream.unreliable_eof()) {
        const auto nread = gzip_stream.read({ buffer, sizeof(buffer) });
        output_stream.write_or_error({ buffer, nread });
    }

    return !gzip_stream.handle_any_error();
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    Vector<StringView> filenames;
    bool keep_input_files { false };
    bool write_to_stdout { false };

    Core::ArgsParser args_parser;
    args_parser.add_option(keep_input_files, "Keep (don't delete) input files", "keep", 'k');
    args_parser.add_option(write_to_stdout, "Write to stdout, keep original files unchanged", "stdout", 'c');
    args_parser.add_positional_argument(filenames, "File to decompress", "FILE");
    args_parser.parse(args);

    if (write_to_stdout)
        keep_input_files = true;

    for (auto filename : filenames) {

        String input_filename;
        String output_filename;
        if (filename.ends_with(".gz")) {
            input_filename = filename;
            output_filename = filename.substring_view(0, filename.length() - 3);
        } else {
            input_filename = String::formatted("{}.gz", filename);
            output_filename = filename;
        }

        auto input_stream_result = TRY(Core::InputFileStream::open_buffered(input_filename));

        auto success = false;
        if (write_to_stdout) {
            auto stdout = Core::OutputFileStream::stdout_buffered();
            success = decompress_file(input_stream_result, stdout);
        } else {
            auto output_stream_result = TRY(Core::OutputFileStream::open_buffered(output_filename));

            success = decompress_file(input_stream_result, output_stream_result);
        }
        if (!success) {
            warnln("Failed gzip decompressing input file");
            return 1;
        }

        if (!keep_input_files)
            TRY(Core::System::unlink(input_filename));
    }
    return 0;
}
