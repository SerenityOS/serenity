/*
 * Copyright (c) 2020, Matthew Graham <mjg267106@gmail.com>
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

#include <AK/ByteBuffer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio wpath cpath rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto in_file = Core::File::construct("/dev/stdin");
    if (!in_file->open(Core::IODevice::ReadOnly)) {
        perror("open");
        return 1;
    }
    auto output_buffer = in_file->read_all();

    if (pledge("stdio wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool append = false;
    const char* output_file_name = "/dev/stdout";

    Core::ArgsParser parser;
    parser.add_option(append, "Append input to output file", "append", 'a');
    parser.add_positional_argument(output_file_name, "Output file", "file", Core::ArgsParser::Required::No);
    parser.parse(argc, argv);

    auto open_mode = Core::IODevice::WriteOnly;
    if (append)
        open_mode = (Core::IODevice::OpenMode)(Core::IODevice::Append | open_mode);

    auto output_file = Core::File::construct(output_file_name);
    if (!output_file->open(open_mode)) {
        perror("open");
        return 1;
    }
    output_file->write(output_buffer);

    return 0;
}
