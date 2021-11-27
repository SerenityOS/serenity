/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCpp/Parser.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    const char* path = nullptr;
    bool tokens_mode = false;
    args_parser.add_option(tokens_mode, "Print Tokens", "tokens", 'T');
    args_parser.add_positional_argument(path, "Cpp File", "cpp-file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!path)
        path = "Source/little/main.cpp";
    auto file = Core::File::construct(path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", path, file->error_string());
        exit(1);
    }
    auto content = file->read_all();
    StringView content_view(content);

    ::Cpp::Preprocessor processor(path, content_view);
    auto tokens = processor.process_and_lex();

    ::Cpp::Parser parser(tokens, path);
    if (tokens_mode) {
        parser.print_tokens();
        return 0;
    }
    auto root = parser.parse();

    dbgln("Parser errors:");
    for (auto& error : parser.errors()) {
        dbgln("{}", error);
    }

    root->dump();

    return 0;
}
