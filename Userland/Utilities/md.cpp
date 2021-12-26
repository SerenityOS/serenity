/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMarkdown/Document.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* file_name = nullptr;
    bool html = false;
    int view_width = 0;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Render Markdown to some other format.");
    args_parser.add_option(html, "Render to HTML rather than for the terminal", "html", 'H');
    args_parser.add_option(view_width, "Viewport width for the terminal (defaults to current terminal width)", "view-width", 0, "width");
    args_parser.add_positional_argument(file_name, "Path to Markdown file", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

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
    auto file = Core::File::construct();
    bool success;
    if (file_name == nullptr) {
        success = file->open(STDIN_FILENO, Core::IODevice::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescriptor::No);
    } else {
        file->set_filename(file_name);
        success = file->open(Core::IODevice::OpenMode::ReadOnly);
    }
    if (!success) {
        fprintf(stderr, "Error: %s\n", file->error_string());
        return 1;
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto buffer = file->read_all();
    dbgln("Read size {}", buffer.size());

    auto input = String::copy(buffer);
    auto document = Markdown::Document::parse(input);

    if (!document) {
        fprintf(stderr, "Error parsing\n");
        return 1;
    }

    String res = html ? document->render_to_html() : document->render_for_terminal(view_width);
    printf("%s", res.characters());
}
