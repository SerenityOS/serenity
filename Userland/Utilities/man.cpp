/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibMarkdown/Document.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

int main(int argc, char* argv[])
{
    int view_width = 0;
    if (isatty(STDOUT_FILENO)) {
        struct winsize ws;
        if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &ws) == 0)
            view_width = ws.ws_col;
    }

    if (view_width == 0)
        view_width = 80;

    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/usr/share/man", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    const char* section = nullptr;
    const char* name = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Read manual pages. Try 'man man' to get started.");
    args_parser.add_positional_argument(section, "Section of the man page", "section", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(name, "Name of the man page", "name");

    args_parser.parse(argc, argv);

    auto make_path = [name](const char* section) {
        return String::formatted("/usr/share/man/man{}/{}.md", section, name);
    };
    if (!section) {
        const char* sections[] = {
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
        };
        for (auto s : sections) {
            String path = make_path(s);
            if (access(path.characters(), R_OK) == 0) {
                section = s;
                break;
            }
        }
        if (!section) {
            warnln("No man page for {}", name);
            exit(1);
        }
    }

    auto file = Core::File::construct();
    file->set_filename(make_path(section));

    if (!file->open(Core::OpenMode::ReadOnly)) {
        perror("Failed to open man page file");
        exit(1);
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    dbgln("Loading man page from {}", file->filename());
    auto buffer = file->read_all();
    auto source = String::copy(buffer);

    outln("{}({})\t\tSerenityOS manual", name, section);

    auto document = Markdown::Document::parse(source);
    VERIFY(document);

    String rendered = document->render_for_terminal(view_width);
    out("{}", rendered);
}
