/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGLSL/Parser.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    String path;
    bool print_tokens = false;
    args_parser.add_option(print_tokens, "Print Tokens", "tokens", 't');
    args_parser.add_positional_argument(path, "Input file", "input-file", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    if (path.is_empty())
        return 1;

    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
    auto content = TRY(file->read_until_eof());
    String content_view = TRY(String::from_utf8(content));

    GLSL::Preprocessor preprocessor(path, content_view);
    auto tokens = TRY(preprocessor.process_and_lex());

    GLSL::Parser parser(tokens, path);
    if (print_tokens)
        parser.print_tokens();
    auto root = TRY(parser.parse());

    dbgln("Parser errors:");
    for (auto& error : parser.errors()) {
        dbgln("{}", error);
    }

    auto standard_out = TRY(Core::File::standard_output());
    TRY(root->dump(*standard_out));

    return 0;
}
