/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileStream.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<StringView> filenames;
    bool keep_input_files { false };
    bool write_to_stdout { false };
    bool decompress { false };

    Core::ArgsParser args_parser;
    args_parser.add_option(keep_input_files, "Keep (don't delete) input files", "keep", 'k');
    args_parser.add_option(write_to_stdout, "Write to stdout, keep original files unchanged", "stdout", 'c');
    args_parser.add_option(decompress, "Decompress", "decompress", 'd');
    args_parser.add_positional_argument(filenames, "Files", "FILES");
    args_parser.parse(arguments);

    if (write_to_stdout)
        keep_input_files = true;

    for (auto const& input_filename : filenames) {
        DeprecatedString output_filename;
        if (decompress) {
            if (!input_filename.ends_with(".gz"sv)) {
                warnln("unknown suffix for: {}, skipping", input_filename);
                continue;
            }
            output_filename = input_filename.substring_view(0, input_filename.length() - ".gz"sv.length());
        } else {
            output_filename = DeprecatedString::formatted("{}.gz", input_filename);
        }

        // We map the whole file instead of streaming to reduce size overhead (gzip header) and increase the deflate block size (better compression)
        // TODO: automatically fallback to buffered streaming for very large files
        RefPtr<Core::MappedFile> file;
        ReadonlyBytes input_bytes;
        if (TRY(Core::System::stat(input_filename)).st_size > 0) {
            file = TRY(Core::MappedFile::map(input_filename));
            input_bytes = file->bytes();
        }

        ByteBuffer output_bytes;
        if (decompress)
            output_bytes = TRY(Compress::GzipDecompressor::decompress_all(input_bytes));
        else
            output_bytes = TRY(Compress::GzipCompressor::compress_all(input_bytes));

        auto output_stream = write_to_stdout ? TRY(Core::Stream::File::standard_output()) : TRY(Core::Stream::File::open(output_filename, Core::Stream::OpenMode::Write));
        TRY(output_stream->write_entire_buffer(output_bytes));

        if (!keep_input_files) {
            TRY(Core::System::unlink(input_filename));
        }
    }
    return 0;
}
