/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@gmail.com>
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

#include <AK/MappedFile.h>
#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileStream.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    Vector<const char*> filenames;
    bool keep_input_files { false };
    bool write_to_stdout { false };

    Core::ArgsParser args_parser;
    args_parser.add_option(keep_input_files, "Keep (don't delete) input files", "keep", 'k');
    args_parser.add_option(write_to_stdout, "Write to stdout, keep original files unchanged", "stdout", 'c');
    args_parser.add_positional_argument(filenames, "File to compress", "FILE");
    args_parser.parse(argc, argv);

    if (write_to_stdout)
        keep_input_files = true;

    for (const String& input_filename : filenames) {
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
