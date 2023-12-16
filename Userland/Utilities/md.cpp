/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibMarkdown/Document.h>
#include <sys/ioctl.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty"));

    StringView filename;
    bool html = false;
    int view_width = 0;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Render Markdown to some other format.");
    args_parser.add_option(html, "Render to HTML rather than for the terminal", "html", 'H');
    args_parser.add_option(view_width, "Viewport width for the terminal (defaults to current terminal width)", "view-width", 0, "width");
    args_parser.add_positional_argument(filename, "Path to Markdown file", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!html && view_width == 0) {
        if (isatty(STDOUT_FILENO)) {
            struct winsize ws;
            if (ioctl(STDERR_FILENO, TIOCGWINSZ, &ws) < 0)
                view_width = 80;
            else
                view_width = ws.ws_col;

        } else {
            view_width = 80;
        }
    }

    auto file = TRY(Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read));

    TRY(Core::System::pledge("stdio"));

    auto buffer = TRY(file->read_until_eof());
    dbgln("Read size {}", buffer.size());

    auto input = ByteString::copy(buffer);
    auto document = Markdown::Document::parse(input);

    if (!document) {
        warnln("Error parsing Markdown document");
        return 1;
    }

    if (html) {
        out("{}", document->render_to_html());
    } else {
        out("{}", TRY(document->render_for_terminal(view_width)));
    }

    return 0;
}
