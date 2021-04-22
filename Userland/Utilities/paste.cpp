/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <stdlib.h>

int main(int argc, char* argv[])
{
    bool print_type = false;
    bool no_newline = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Paste from the clipboard to stdout.");
    args_parser.add_option(print_type, "Display the copied type", "print-type", 0);
    args_parser.add_option(no_newline, "Do not append a newline", "no-newline", 'n');
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    auto& clipboard = GUI::Clipboard::the();
    auto data_and_type = clipboard.data_and_type();

    if (data_and_type.mime_type.is_null()) {
        warnln("Nothing copied");
        return 1;
    }

    if (!print_type) {
        out("{}", StringView(data_and_type.data));
        // Append a newline to text contents, unless the caller says otherwise.
        if (data_and_type.mime_type.starts_with("text/") && !no_newline)
            outln();
    } else {
        outln("{}", data_and_type.mime_type);
    }

    return 0;
}
