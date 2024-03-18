/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibSymbolication/Symbolication.h>
#include <LibURL/URL.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));
    auto hostname = TRY(Core::System::gethostname());

    Core::ArgsParser args_parser;
    pid_t pid = 0;
    args_parser.add_positional_argument(pid, "PID", "pid");
    args_parser.parse(arguments);
    Core::EventLoop loop;

    Core::DirIterator iterator(ByteString::formatted("/proc/{}/stacks", pid), Core::DirIterator::SkipDots);
    if (iterator.has_error()) {
        warnln("Error: pid '{}' doesn't appear to exist.", pid);
        return 1;
    }

    while (iterator.has_next()) {
        pid_t tid = iterator.next_path().to_number<pid_t>().value();
        outln("thread: {}", tid);
        outln("frames:");
        auto symbols = Symbolication::symbolicate_thread(pid, tid);
        auto frame_number = symbols.size() - 1;
        for (auto& symbol : symbols) {
            // Make kernel stack frames stand out.
            auto maybe_kernel_base = Symbolication::kernel_base();
            int color = maybe_kernel_base.has_value() && symbol.address < maybe_kernel_base.value() ? 35 : 31;
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
                    auto full_path = LexicalPath::canonicalized_path(ByteString::formatted("/usr/src/serenity/dummy/dummy/{}", source_position.file_path));
                    if (access(full_path.characters(), F_OK) == 0) {
                        linked = true;
                        auto url = URL::create_with_file_scheme(full_path, {}, hostname);
                        url.set_query(TRY(String::formatted("line_number={}", source_position.line_number)));
                        out("\033]8;;{}\033\\", url.serialize());
                    }

                    out("\033[34;1m{}:{}\033[0m", LexicalPath::basename(source_position.file_path), source_position.line_number);

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
