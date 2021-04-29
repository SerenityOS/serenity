/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MappedFile.h>
#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileStream.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    Vector<String> filenames;
    bool keep_input_files { false };
    bool write_to_stdout { false };

    Core::ArgsParser args_parser;
    args_parser.add_option(keep_input_files, "Keep (don't delete) input files", "keep", 'k');
    args_parser.add_option(write_to_stdout, "Write to stdout, keep original files unchanged", "stdout", 'c');
    args_parser.add_positional_argument(filenames, "File to compress", "FILE");
    args_parser.parse(argc, argv);

    if (write_to_stdout)
        keep_input_files = true;

    for (auto const& input_filename : filenames) {
        auto output_filename = String::formatted("{}.gz", input_filename);

        // We map the whole file instead of streaming to reduce size overhead (gzip header) and increase the deflate block size (better compression)
        // TODO: automatically fallback to buffered streaming for very large files
        auto file_or_error = MappedFile::map(input_filename);
        if (file_or_error.is_error()) {
            warnln("Failed opening input file for reading: {}", file_or_error.error());
            return 1;
        }
        auto file = file_or_error.value();

        auto compressed_file = Compress::GzipCompressor::compress_all(file->bytes());
        if (!compressed_file.has_value()) {
            warnln("Failed gzip compressing input file");
            return 1;
        }

        auto success = false;
        if (write_to_stdout) {
            auto stdout = Core::OutputFileStream { Core::File::standard_output() };
            success = stdout.write_or_error(compressed_file.value());
        } else {
            auto output_stream_result = Core::OutputFileStream::open(output_filename);
            if (output_stream_result.is_error()) {
                warnln("Failed opening output file for writing: {}", output_stream_result.error());
                return 1;
            }
            success = output_stream_result.value().write_or_error(compressed_file.value());
        }
        if (!success) {
            warnln("Failed writing compressed file to output");
            return 1;
        }

        if (!keep_input_files) {
            const auto retval = unlink(input_filename.characters());
            if (retval != 0) {
                warnln("Failed removing input file");
                return 1;
            }
        }
    }
    return 0;
}
