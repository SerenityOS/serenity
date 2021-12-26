/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        success = file->open(STDIN_FILENO, Core::IODevice::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescription::No);
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
    dbg() << "Read size " << buffer.size();

    auto input = String::copy(buffer);
    auto document = Markdown::Document::parse(input);

    if (!document) {
        fprintf(stderr, "Error parsing\n");
        return 1;
    }

    String res = html ? document->render_to_html() : document->render_for_terminal(view_width);
    printf("%s", res.characters());
}
