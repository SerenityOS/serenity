/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCpp/Lexer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    StringView path;
    args_parser.add_positional_argument(path, "Cpp File", "cpp-file", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
    auto content = TRY(file->read_until_eof());
    StringView content_view(content);

    Cpp::Lexer lexer(content);
    lexer.lex_iterable([](auto token) {
        outln("{}", token.to_byte_string());
    });

    return 0;
}
