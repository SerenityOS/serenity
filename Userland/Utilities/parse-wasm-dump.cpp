/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FileStream.h>
#include <AK/GenericLexer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibMain/Main.h>
#include <LibWasm/Parser/TextFormat.h>

static void print_error(Wasm::TextFormatParseError const& error)
{
    warnln("Error: {} (at {}:{}, generated in {})", error.error, error.line, error.column, error.location);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView input_file_path;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(input_file_path, "Input dump file path", "path");
    args_parser.parse(arguments);

    auto input_file = TRY(Core::MappedFile::map(input_file_path));
    auto lexer = GenericLexer { input_file->bytes() };
    OutputFileStream output_stream { stdout };

    if (auto result = Wasm::parse_and_generate_module_from_text_format(lexer, output_stream); result.is_error()) {
        print_error(result.error());
        return 1;
    }

    return 0;
}
