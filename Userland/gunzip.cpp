/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibCompress/Gzip.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileStream.h>

static void decompress_file(Buffered<Core::InputFileStream>& input_stream, Buffered<Core::OutputFileStream>& output_stream)
{
    auto gzip_stream = Compress::GzipDecompressor { input_stream };

    u8 buffer[4096];

    while (!gzip_stream.unreliable_eof()) {
        const auto nread = gzip_stream.read({ buffer, sizeof(buffer) });
        output_stream.write_or_error({ buffer, nread });
    }
}

int main(int argc, char** argv)
{
    Vector<const char*> filenames;
    bool keep_input_files { false };
    bool write_to_stdout { false };

    Core::ArgsParser args_parser;
    args_parser.add_option(keep_input_files, "Keep (don't delete) input files", "keep", 'k');
    args_parser.add_option(write_to_stdout, "Write to stdout, keep original files unchanged", "stdout", 'c');
    args_parser.add_positional_argument(filenames, "File to decompress", "FILE");
    args_parser.parse(argc, argv);

    if (write_to_stdout)
        keep_input_files = true;

    for (String filename : filenames) {
        if (!filename.ends_with(".gz"))
            filename = String::format("%s.gz", filename);

        const auto input_filename = filename;
        const auto output_filename = filename.substring_view(0, filename.length() - 3);

        auto input_stream_result = Core::InputFileStream::open_buffered(input_filename);

        if (write_to_stdout) {
            auto stdout = Core::OutputFileStream::stdout_buffered();
            decompress_file(input_stream_result.value(), stdout);
        } else {
            auto output_stream_result = Core::OutputFileStream::open_buffered(output_filename);
            decompress_file(input_stream_result.value(), output_stream_result.value());
        }

        if (!keep_input_files) {
            const auto retval = unlink(String { input_filename }.characters());
            ASSERT(retval == 0);
        }
    }
}
