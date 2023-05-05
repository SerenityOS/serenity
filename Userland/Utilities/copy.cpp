/*
 * Copyright (c) 2019-2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibMain/Main.h>
#include <unistd.h>

struct Options {
    String data;
    StringView type;
    bool clear;
};

static ErrorOr<Options> parse_options(Main::Arguments arguments)
{
    auto type = "text/plain"sv;
    Vector<StringView> text;
    bool clear = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Copy text from stdin or the command-line to the clipboard.");
    args_parser.add_option(type, "Pick a type", "type", 't', "type");
    args_parser.add_option(clear, "Instead of copying, clear the clipboard", "clear", 'c');
    args_parser.add_positional_argument(text, "Text to copy", "text", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    Options options;
    options.type = type;
    options.clear = clear;

    if (clear) {
        // We're not copying anything.
    } else if (text.is_empty()) {
        // Copy our stdin.
        auto c_stdin = TRY(Core::File::standard_input());
        auto buffer = TRY(c_stdin->read_until_eof());
        dbgln("Read size {}", buffer.size());
        dbgln("Read data: `{}`", StringView(buffer.bytes()));
        options.data = TRY(String::from_utf8(StringView(buffer.bytes())));
    } else {
        // Copy the rest of our command-line args.
        StringBuilder builder;
        builder.join(' ', text);
        options.data = TRY(builder.to_string());
    }

    return options;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    Options options = TRY(parse_options(arguments));

    auto& clipboard = GUI::Clipboard::the();
    if (options.clear)
        clipboard.clear();
    else
        clipboard.set_data(options.data.bytes(), options.type);

    return 0;
}
