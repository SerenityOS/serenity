/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibWasm/Types.h>

int main(int argc, char* argv[])
{
    const char* filename = nullptr;

    Core::ArgsParser parser;
    parser.add_positional_argument(filename, "File name to parse", "file");
    parser.parse(argc, argv);

    auto result = Core::File::open(filename, Core::IODevice::OpenMode::ReadOnly);
    if (result.is_error()) {
        warnln("Failed to open {}: {}", filename, result.error());
        return 1;
    }

    auto stream = Core::InputFileStream(result.release_value());
    auto parse_result = Wasm::Module::parse(stream);
    if (parse_result.is_error()) {
        warnln("Something went wrong, either the file is invalid, or there's a bug with LibWasm!");
        return 2;
    }

    return 0;
}
