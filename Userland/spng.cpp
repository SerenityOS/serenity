/* 
 * Copyright (C) 2020 Matthew Graham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
