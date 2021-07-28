/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCpp/Parser.h>

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    const char* path = nullptr;
    bool tokens_mode = false;
    bool preprocess_mode = false;
    args_parser.add_option(tokens_mode, "Print Tokens", "tokens", 'T');
    args_parser.add_option(preprocess_mode, "Print Preprocessed source", "preprocessed", 'P');
    args_parser.add_positional_argument(path, "Cpp File", "cpp-file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!path)
        path = "Source/little/main.cpp";
    auto file = Core::File::construct(path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        perror("open");
        exit(1);
    }
    auto content = file->read_all();
    StringView content_view(content);

    ::Cpp::Preprocessor processor(path, content_view);
    auto preprocessed_content = processor.process();
    if (preprocess_mode) {
        outln(preprocessed_content);
        return 0;
    }

    ::Cpp::Parser parser(preprocessed_content, path);
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
}
