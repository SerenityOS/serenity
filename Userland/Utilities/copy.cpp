/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <unistd.h>

struct Options {
    String data;
    StringView type;
};

static Options parse_options(int argc, char* argv[])
{
    const char* type = "text/plain";
    Vector<const char*> text;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Copy text from stdin or the command-line to the clipboard.");
    args_parser.add_option(type, "Pick a type", "type", 't', "type");
    args_parser.add_positional_argument(text, "Text to copy", "text", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    Options options;
    options.type = type;

    if (text.is_empty()) {
        // Copy our stdin.
        auto c_stdin = Core::File::construct();
        bool success = c_stdin->open(
            STDIN_FILENO,
            Core::IODevice::OpenMode::ReadOnly,
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

int main(int argc, char* argv[])
{
    auto app = GUI::Application::construct(argc, argv);

    Options options = parse_options(argc, argv);

    auto& clipboard = GUI::Clipboard::the();
    clipboard.set_data(options.data.bytes(), options.type);

    return 0;
}
