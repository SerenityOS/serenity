/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

static ErrorOr<void> decompress_file(NonnullOwnPtr<Core::Stream::File> input_stream, Core::Stream::Stream& output_stream)
{
    auto gzip_stream = Compress::GzipDecompressor { move(input_stream) };

    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));
    while (!gzip_stream.is_eof()) {
        auto span = TRY(gzip_stream.read(buffer));
        TRY(output_stream.write_entire_buffer(span));
    }

    return {};
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

        DeprecatedString input_filename;
        DeprecatedString output_filename;
        if (filename.ends_with(".gz"sv)) {
            input_filename = filename;
            output_filename = filename.substring_view(0, filename.length() - 3);
        } else {
            input_filename = DeprecatedString::formatted("{}.gz", filename);
            output_filename = filename;
        }

        auto input_stream_result = TRY(Core::Stream::File::open(input_filename, Core::Stream::OpenMode::Read));
        auto output_stream = write_to_stdout ? TRY(Core::Stream::File::standard_output()) : TRY(Core::Stream::File::open(output_filename, Core::Stream::OpenMode::Write));

        TRY(decompress_file(move(input_stream_result), *output_stream));

        if (!keep_input_files)
            TRY(Core::System::unlink(input_filename));
    }
    return 0;
}
