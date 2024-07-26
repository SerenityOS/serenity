/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibShell/Shell.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

RefPtr<Line::Editor> editor;
Shell::Shell* s_shell;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::EventLoop loop;

    Core::EventLoop::register_signal(SIGINT, [](int) {
        s_shell->kill_job(s_shell->current_job(), SIGINT);
    });

    Core::EventLoop::register_signal(SIGWINCH, [](int) {
        s_shell->kill_job(s_shell->current_job(), SIGWINCH);
    });

    Core::EventLoop::register_signal(SIGTTIN, [](int) {});
    Core::EventLoop::register_signal(SIGTTOU, [](int) {});

    Core::EventLoop::register_signal(SIGHUP, [](int) {
        for (auto& it : s_shell->jobs)
            s_shell->kill_job(it.value.ptr(), SIGHUP);

        s_shell->editor()->save_history(s_shell->get_history_path());
    });

    TRY(Core::System::pledge("stdio rpath wpath cpath proc exec tty sigaction unix fattr"));

    RefPtr<::Shell::Shell> shell;
    bool attempt_interactive = false;

    auto initialize = [&](bool posix_mode) {
        auto configuration = Line::Configuration::from_config();
        if (!attempt_interactive) {
            configuration.set(Line::Configuration::Flags::None);
            configuration.set(Line::Configuration::SignalHandler::NoSignalHandlers);
            configuration.set(Line::Configuration::OperationMode::NonInteractive);
            configuration.set(Line::Configuration::RefreshBehavior::Eager);
        }

        editor = Line::Editor::construct(move(configuration));
        editor->initialize();

        shell = Shell::Shell::construct(*editor, attempt_interactive, posix_mode);
        s_shell = shell.ptr();

        s_shell->setup_signals();

        sigset_t blocked;
        sigemptyset(&blocked);
        sigaddset(&blocked, SIGTTOU);
        sigaddset(&blocked, SIGTTIN);
        pthread_sigmask(SIG_BLOCK, &blocked, nullptr);

        shell->termios = editor->termios();
        shell->default_termios = editor->default_termios();

        editor->on_display_refresh = [&](auto& editor) {
            editor.strip_styles();
            if (shell->should_format_live()) {
                auto line = editor.line();
                ssize_t cursor = editor.cursor();
                editor.clear_line();
                editor.insert(shell->format(line, cursor));
                if (cursor >= 0)
                    editor.set_cursor(cursor);
            }
            (void)shell->highlight(editor);
        };
        editor->on_tab_complete = [&](Line::Editor const&) {
            return shell->complete();
        };
        editor->on_paste = [&](Utf32View data, Line::Editor& editor) {
            auto line = editor.line(editor.cursor());
            Shell::Parser parser(line, false);
            auto ast = parser.parse();
            if (!ast) {
                editor.insert(data);
                return;
            }

            auto hit_test_result = ast->hit_test_position(editor.cursor());
            // If the argument isn't meant to be an entire command, escape it.
            // This allows copy-pasting entire commands where commands are expected, and otherwise escapes everything.
            auto should_escape = false;
            if (!hit_test_result.matching_node && hit_test_result.closest_command_node) {
                // There's *some* command, but our cursor is immediate after it
                should_escape = editor.cursor() >= hit_test_result.closest_command_node->position().end_offset;
                hit_test_result.matching_node = hit_test_result.closest_command_node;
            } else if (hit_test_result.matching_node && hit_test_result.closest_command_node) {
                // There's a command, and we're at the end of or in the middle of some node.
                auto leftmost_literal = hit_test_result.closest_command_node->leftmost_trivial_literal();
                if (leftmost_literal)
                    should_escape = !hit_test_result.matching_node->position().contains(leftmost_literal->position().start_offset);
            }

            if (should_escape) {
                ByteString escaped_string;
                Optional<char> trivia {};
                bool starting_trivia_already_provided = false;
                auto escape_mode = Shell::Shell::EscapeMode::Bareword;
                if (hit_test_result.matching_node->kind() == Shell::AST::Node::Kind::StringLiteral) {
                    // If we're pasting in a string literal, make sure to only consider that specific escape mode
                    auto* node = static_cast<Shell::AST::StringLiteral const*>(hit_test_result.matching_node.ptr());
                    switch (node->enclosure_type()) {
                    case Shell::AST::StringLiteral::EnclosureType::None:
                        break;
                    case Shell::AST::StringLiteral::EnclosureType::SingleQuotes:
                        escape_mode = Shell::Shell::EscapeMode::SingleQuotedString;
                        trivia = '\'';
                        starting_trivia_already_provided = true;
                        break;
                    case Shell::AST::StringLiteral::EnclosureType::DoubleQuotes:
                        escape_mode = Shell::Shell::EscapeMode::DoubleQuotedString;
                        trivia = '"';
                        starting_trivia_already_provided = true;
                        break;
                    }
                }

                if (starting_trivia_already_provided) {
                    escaped_string = shell->escape_token(data, escape_mode);
                } else {
                    escaped_string = shell->escape_token(data, Shell::Shell::EscapeMode::Bareword);
                    if (auto string = shell->escape_token(data, Shell::Shell::EscapeMode::SingleQuotedString); string.length() + 2 < escaped_string.length()) {
                        escaped_string = move(string);
                        trivia = '\'';
                    }
                    if (auto string = shell->escape_token(data, Shell::Shell::EscapeMode::DoubleQuotedString); string.length() + 2 < escaped_string.length()) {
                        escaped_string = move(string);
                        trivia = '"';
                    }
                }

                if (trivia.has_value() && !starting_trivia_already_provided)
                    editor.insert(*trivia);

                editor.insert(escaped_string);

                if (trivia.has_value())
                    editor.insert(*trivia);
            } else {
                editor.insert(data);
            }
        };
    };

    StringView command_to_run = {};
    StringView file_to_read_from = {};
    Vector<StringView> script_args;
    bool skip_rc_files = false;
    StringView format;
    bool should_format_live = false;
    bool keep_open = false;
    bool posix_mode = (LexicalPath::basename(arguments.strings[0]) == "sh"sv);

    Core::ArgsParser parser;
    parser.add_option(command_to_run, "String to read commands from", "command-string", 'c', "command-string");
    parser.add_option(skip_rc_files, "Skip running shellrc files", "skip-shellrc");
    parser.add_option(format, "Format the given file into stdout and exit", "format", 0, "file");
    parser.add_option(should_format_live, "Enable live formatting", "live-formatting", 'f');
    parser.add_option(keep_open, "Keep the shell open after running the specified command or file", "keep-open");
    parser.add_option(posix_mode, "Behave like a POSIX-compatible shell", "posix");
    parser.add_positional_argument(file_to_read_from, "File to read commands from", "file", Core::ArgsParser::Required::No);
    parser.add_positional_argument(script_args, "Extra arguments to pass to the script (via $* and co)", "argument", Core::ArgsParser::Required::No);

    parser.set_stop_on_first_non_option(true);
    parser.parse(arguments);

    if (!file_to_read_from.is_null())
        skip_rc_files = true;

    if (!format.is_empty()) {
        auto file = TRY(Core::File::open(format, Core::File::OpenMode::Read));

        initialize(posix_mode);

        ssize_t cursor = -1;
        puts(shell->format(TRY(file->read_until_eof()), cursor).characters());
        return 0;
    }

    auto pid = getpid();
    if (auto sid = getsid(pid); sid == 0) {
        if (auto res = Core::System::setsid(); res.is_error())
            dbgln("{}", res.release_error());
    } else if (sid != pid) {
        if (getpgid(pid) != pid) {
            if (auto res = Core::System::setpgid(pid, sid); res.is_error())
                dbgln("{}", res.release_error());

            if (auto res = Core::System::setsid(); res.is_error())
                dbgln("{}", res.release_error());
        }
    }

    auto execute_file = !file_to_read_from.is_empty() && "-"sv != file_to_read_from;
    attempt_interactive = !execute_file && (command_to_run.is_empty() || keep_open);

    if (keep_open && command_to_run.is_empty() && !execute_file) {
        warnln("Option --keep-open can only be used in combination with -c or when specifying a file to execute.");
        return 1;
    }

    initialize(posix_mode);

    shell->set_live_formatting(should_format_live);
    shell->current_script = arguments.strings[0];

    if (!skip_rc_files) {
        auto run_rc_file = [&](auto& name) {
            ByteString file_path = name;
            if (file_path.starts_with('~'))
                file_path = shell->expand_tilde(file_path);
            if (FileSystem::exists(file_path)) {
                shell->run_file(file_path, false);
            }
        };
        if (posix_mode) {
            run_rc_file(Shell::Shell::global_posix_init_file_path);
            run_rc_file(Shell::Shell::local_posix_init_file_path);
        } else {
            run_rc_file(Shell::Shell::global_init_file_path);
            run_rc_file(Shell::Shell::local_init_file_path);
        }
        shell->cache_path();
    }

    Vector<String> args_to_pass;
    TRY(args_to_pass.try_ensure_capacity(script_args.size()));
    for (auto& arg : script_args)
        TRY(args_to_pass.try_append(TRY(String::from_utf8(arg))));

    shell->set_local_variable("ARGV", adopt_ref(*new Shell::AST::ListValue(move(args_to_pass))));

    if (!command_to_run.is_empty()) {
        auto result = shell->run_command(command_to_run);
        if (!keep_open)
            return result;
    }

    if (execute_file) {
        auto result = shell->run_file(file_to_read_from);
        if (!keep_open) {
            if (result)
                return 0;
            return 1;
        }
    }

    shell->add_child(*editor);

    Core::EventLoop::current().post_event(*shell, make<Core::CustomEvent>(Shell::Shell::ShellEventType::ReadLine));

    return loop.exec();
}
