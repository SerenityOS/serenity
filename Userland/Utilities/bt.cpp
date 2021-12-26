/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/EventLoop.h>
#include <LibSymbolication/Symbolication.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    char hostname[256];
    if (gethostname(hostname, sizeof(hostname)) < 0) {
        perror("gethostname");
        return 1;
    }

    Core::ArgsParser args_parser;
    pid_t pid = 0;
    args_parser.add_positional_argument(pid, "PID", "pid");
    args_parser.parse(argc, argv);
    Core::EventLoop loop;

    Core::DirIterator iterator(String::formatted("/proc/{}/stacks", pid), Core::DirIterator::SkipDots);
    if (iterator.has_error()) {
        warnln("Error: pid '{}' doesn't appear to exist.", pid);
        return 1;
    }

    while (iterator.has_next()) {
        pid_t tid = iterator.next_path().to_int().value();
        outln("thread: {}", tid);
        outln("frames:");
        auto symbols = Symbolication::symbolicate_thread(pid, tid);
        auto frame_number = symbols.size() - 1;
        for (auto& symbol : symbols) {
            // Make kernel stack frames stand out.
            int color = symbol.address < 0xc0000000 ? 35 : 31;
            out("{:3}: \033[{};1m{:p}\033[0m | ", frame_number, color, symbol.address);
            if (!symbol.name.is_empty())
                out("{} ", symbol.name);
            if (!symbol.source_positions.is_empty()) {
                out("(");

                for (size_t i = 0; i < symbol.source_positions.size(); i++) {
                    auto& source_position = symbol.source_positions[i];
                    bool linked = false;

                    // See if we can find the sources in /usr/src
                    // FIXME: I'm sure this can be improved!
                    auto full_path = LexicalPath::canonicalized_path(String::formatted("/usr/src/serenity/dummy/dummy/{}", source_position.file_path));
                    if (access(full_path.characters(), F_OK) == 0) {
                        linked = true;
                        auto url = URL::create_with_file_scheme(full_path, {}, hostname);
                        url.set_query(String::formatted("line_number={}", source_position.line_number));
                        out("\033]8;;{}\033\\", url.serialize());
                    }

                    out("\033[34;1m{}:{}\033[0m", LexicalPath(source_position.file_path).basename(), source_position.line_number);

                    if (linked)
                        out("\033]8;;\033\\");

                    if (i != symbol.source_positions.size() - 1)
                        out(" => ");
                }

                out(")");
            }
            outln("");
            frame_number--;
        }
        outln("");
    }
    return 0;
}
