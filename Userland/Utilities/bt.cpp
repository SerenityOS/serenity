/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Kenneth Myhra <kennethmyhra@gmail.com>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibCoredump/Backtrace.h>
#include <LibMain/Main.h>
#include <LibSymbolication/Symbolication.h>
#include <unistd.h>

static ErrorOr<int> symbolicate_process(pid_t pid)
{
    auto hostname = TRY(Core::System::gethostname());
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
                    auto full_path = LexicalPath::canonicalized_path(String::formatted("/usr/src/serenity/dummy/dummy/{}", source_position.file_path));
                    if (access(full_path.characters(), F_OK) == 0) {
                        linked = true;
                        auto url = URL::create_with_file_scheme(full_path, {}, hostname);
                        url.set_query(String::formatted("line_number={}", source_position.line_number));
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

static ErrorOr<int> symbolicate_coredump(String path)
{
    auto coredump = Coredump::Reader::create(path);
    if (!coredump) {
        warnln("Could not open coredump '{}'", path);
        return 1;
    }

    auto const& metadata = coredump->metadata();
    if (auto assert_message = metadata.get("assertion"); assert_message.has_value())
        outln("ASSERTION FAILED: {}\n", *assert_message);
    else if (auto pledge_violation = metadata.get("pledge_violation"); pledge_violation.has_value())
        outln("Has not pledged {}\n", *pledge_violation);

    auto fault_address = metadata.get("fault_address");
    auto fault_type = metadata.get("fault_type");
    auto fault_access = metadata.get("fault_access");
    if (fault_address.has_value() && fault_type.has_value() && fault_access.has_value())
        outln("{} fault on {} at address {}\n", *fault_address, *fault_type, *fault_access);

    bool is_tty = isatty(STDOUT_FILENO);
    size_t thread_index = 0;
    coredump->for_each_thread_info([&](auto const& thread_info) {
        Coredump::Backtrace backtrace(*coredump, thread_info, [&](size_t frame_index, size_t frame_count) {
            if (is_tty)
                warn("\033]9;{};{};\033\\", frame_index, frame_count);
        });
        if (is_tty)
            warn("\033]9;-1;\033\\");

        if (thread_index != 0)
            outln("");
        outln("--- Backtrace for thread #{} (TID {}) ---", thread_index++, thread_info.tid);
        for (auto const& entry : backtrace.entries())
            outln("{}", entry.to_string(is_tty));

        return IterationDecision::Continue;
    });
    return 0;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;
    String pid_or_coredump;
    args_parser.set_general_help("View the backtrace of a specified process");
    args_parser.add_positional_argument(pid_or_coredump, "PID or path to coredump", "pid-or-coredump");
    args_parser.parse(arguments);

    if (auto maybe_pid = pid_or_coredump.to_int<pid_t>(); maybe_pid.has_value())
        return symbolicate_process(*maybe_pid);
    else
        return symbolicate_coredump(move(pid_or_coredump));
}
