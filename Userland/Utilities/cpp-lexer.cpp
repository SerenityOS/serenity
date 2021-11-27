/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCpp/Lexer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    const char* path = nullptr;
    args_parser.add_positional_argument(path, "Cpp File", "cpp-file", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto file = Core::File::construct(path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", path, file->error_string());
        exit(1);
    }
    auto content = file->read_all();
    StringView content_view(content);

    Cpp::Lexer lexer(content);
    lexer.lex_iterable([](auto token) {
        outln("{}", token.to_string());
    });

    return 0;
}
