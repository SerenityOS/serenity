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
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibMain/Main.h>
#include <unistd.h>

struct Options {
    String data;
    StringView type;
    bool clear;
};

static Options parse_options(Main::Arguments arguments)
{
    const char* type = "text/plain";
    Vector<const char*> text;
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
        auto c_stdin = Core::File::construct();
        bool success = c_stdin->open(
            STDIN_FILENO,
            Core::OpenMode::ReadOnly,
            Core::File::ShouldCloseFileDescriptor::No);
        VERIFY(success);
        auto buffer = c_stdin->read_all();
        dbgln("Read size {}", buffer.size());
        options.data = String((char*)buffer.data(), buffer.size());
    } else {
        // Copy the rest of our command-line args.
        StringBuilder builder;
        builder.join(' ', text);
        options.data = builder.to_string();
    }

    return options;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = GUI::Application::construct(arguments);

    Options options = parse_options(arguments);

    auto& clipboard = GUI::Clipboard::the();
    if (options.clear)
        clipboard.clear();
    else
        clipboard.set_data(options.data.bytes(), options.type);

    return 0;
}
