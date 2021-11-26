/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileStream.h>
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

int main(int argc, char** argv)
{
    Vector<StringView> filenames;
    bool keep_input_files { false };
    bool write_to_stdout { false };

    Core::ArgsParser args_parser;
    args_parser.add_option(keep_input_files, "Keep (don't delete) input files", "keep", 'k');
    args_parser.add_option(write_to_stdout, "Write to stdout, keep original files unchanged", "stdout", 'c');
    args_parser.add_positional_argument(filenames, "File to decompress", "FILE");
    args_parser.parse(argc, argv);

    if (write_to_stdout)
        keep_input_files = true;

    for (auto filename : filenames) {
        if (!filename.ends_with(".gz"))
            filename = String::formatted("{}.gz", filename);

        const auto input_filename = filename;
        const auto output_filename = filename.substring_view(0, filename.length() - 3);

        auto input_stream_result = Core::InputFileStream::open_buffered(input_filename);

        if (input_stream_result.is_error()) {
            warnln("Failed opening input file for reading: {}", input_stream_result.error());
            return 1;
        }

        auto success = false;
        if (write_to_stdout) {
            auto stdout = Core::OutputFileStream::stdout_buffered();
            success = decompress_file(input_stream_result.value(), stdout);
        } else {
            auto output_stream_result = Core::OutputFileStream::open_buffered(output_filename);
            if (output_stream_result.is_error()) {
                warnln("Failed opening output file for writing: {}", output_stream_result.error());
                return 1;
            }
            success = decompress_file(input_stream_result.value(), output_stream_result.value());
        }
        if (!success) {
            warnln("Failed gzip decompressing input file");
            return 1;
        }

        if (!keep_input_files) {
            const auto retval = unlink(String { input_filename }.characters());
            if (retval != 0) {
                warnln("Failed removing input file");
                return 1;
            }
        }
    }
    return 0;
}
