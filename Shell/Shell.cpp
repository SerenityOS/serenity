/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include "Shell.h"
#include "Execution.h"
#include "Formatter.h"
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibLine/Editor.h>
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <pwd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

static bool s_disable_hyperlinks = false;
extern char** environ;

//#define SH_DEBUG

namespace Shell {

// FIXME: This should eventually be removed once we've established that
//        waitpid() is not passed the same job twice.
#ifdef __serenity__
#    define ENSURE_WAITID_ONCE
#endif

void Shell::setup_signals()
{
    if (m_should_reinstall_signal_handlers) {
        Core::EventLoop::register_signal(SIGCHLD, [this](int) {
#ifdef SH_DEBUG
            dbgln("SIGCHLD!");
#endif
            notify_child_event();
        });

        Core::EventLoop::register_signal(SIGTSTP, [this](auto) {
            auto job = current_job();
            kill_job(job, SIGTSTP);
            if (job) {
                job->set_is_suspended(true);
                job->unblock();
            }
        });
    }
}

void Shell::print_path(const String& path)
{
    if (s_disable_hyperlinks || !m_is_interactive) {
        printf("%s", path.characters());
        return;
    }
    printf("\033]8;;file://%s%s\033\\%s\033]8;;\033\\", hostname, path.characters(), path.characters());
}

String Shell::prompt() const
{
    auto build_prompt = [&]() -> String {
        auto* ps1 = getenv("PROMPT");
        if (!ps1) {
            if (uid == 0)
                return "# ";

            StringBuilder builder;
            builder.appendf("\033]0;%s@%s:%s\007", username.characters(), hostname, cwd.characters());
            builder.appendf("\033[31;1m%s\033[0m@\033[37;1m%s\033[0m:\033[32;1m%s\033[0m$> ", username.characters(), hostname, cwd.characters());
            return builder.to_string();
        }

        StringBuilder builder;
        for (char* ptr = ps1; *ptr; ++ptr) {
            if (*ptr == '\\') {
                ++ptr;
                if (!*ptr)
                    break;
                switch (*ptr) {
                case 'X':
                    builder.append("\033]0;");
                    break;
                case 'a':
                    builder.append(0x07);
                    break;
                case 'e':
                    builder.append(0x1b);
                    break;
                case 'u':
                    builder.append(username);
                    break;
                case 'h':
                    builder.append(hostname);
                    break;
                case 'w': {
                    String home_path = getenv("HOME");
                    if (cwd.starts_with(home_path)) {
                        builder.append('~');
                        builder.append(cwd.substring_view(home_path.length(), cwd.length() - home_path.length()));
                    } else {
                        builder.append(cwd);
                    }
                    break;
                }
                case 'p':
                    builder.append(uid == 0 ? '#' : '$');
                    break;
                }
                continue;
            }
            builder.append(*ptr);
        }
        return builder.to_string();
    };

    return build_prompt();
}

String Shell::expand_tilde(const String& expression)
{
    ASSERT(expression.starts_with('~'));

    StringBuilder login_name;
    size_t first_slash_index = expression.length();
    for (size_t i = 1; i < expression.length(); ++i) {
        if (expression[i] == '/') {
            first_slash_index = i;
            break;
        }
        login_name.append(expression[i]);
    }

    StringBuilder path;
    for (size_t i = first_slash_index; i < expression.length(); ++i)
        path.append(expression[i]);

    if (login_name.is_empty()) {
        const char* home = getenv("HOME");
        if (!home) {
            auto passwd = getpwuid(getuid());
            ASSERT(passwd && passwd->pw_dir);
            return String::format("%s/%s", passwd->pw_dir, path.to_string().characters());
        }
        return String::format("%s/%s", home, path.to_string().characters());
    }

    auto passwd = getpwnam(login_name.to_string().characters());
    if (!passwd)
        return expression;
    ASSERT(passwd->pw_dir);

    return String::format("%s/%s", passwd->pw_dir, path.to_string().characters());
}

bool Shell::is_glob(const StringView& s)
{
    for (size_t i = 0; i < s.length(); i++) {
        char c = s.characters_without_null_termination()[i];
        if (c == '*' || c == '?')
            return true;
    }
    return false;
}

Vector<StringView> Shell::split_path(const StringView& path)
{
    Vector<StringView> parts;

    size_t substart = 0;
    for (size_t i = 0; i < path.length(); i++) {
        char ch = path[i];
        if (ch != '/')
            continue;
        size_t sublen = i - substart;
        if (sublen != 0)
            parts.append(path.substring_view(substart, sublen));
        substart = i + 1;
    }

    size_t taillen = path.length() - substart;
    if (taillen != 0)
        parts.append(path.substring_view(substart, taillen));

    return parts;
}

Vector<String> Shell::expand_globs(const StringView& path, StringView base)
{
    auto explicitly_set_base = false;
    if (path.starts_with('/')) {
        base = "/";
        explicitly_set_base = true;
    }
    auto parts = split_path(path);
    String base_string = base;
    struct stat statbuf;
    if (lstat(base_string.characters(), &statbuf) < 0) {
        perror("lstat");
        return {};
    }

    StringBuilder resolved_base_path_builder;
    resolved_base_path_builder.append(Core::File::real_path_for(base));
    if (S_ISDIR(statbuf.st_mode))
        resolved_base_path_builder.append('/');

    auto resolved_base = resolved_base_path_builder.string_view();

    auto results = expand_globs(move(parts), resolved_base);

    if (explicitly_set_base && base == "/")
        resolved_base = resolved_base.substring_view(1, resolved_base.length() - 1);
    for (auto& entry : results) {
        entry = entry.substring(resolved_base.length(), entry.length() - resolved_base.length());
        if (entry.is_empty())
            entry = ".";
    }

    // Make the output predictable and nice.
    quick_sort(results);

    return results;
}

Vector<String> Shell::expand_globs(Vector<StringView> path_segments, const StringView& base)
{
    if (path_segments.is_empty()) {
        String base_str = base;
        if (access(base_str.characters(), F_OK) == 0)
            return { move(base_str) };
        return {};
    }

    auto first_segment = path_segments.take_first();
    if (is_glob(first_segment)) {
        Vector<String> result;

        Core::DirIterator di(base, Core::DirIterator::SkipParentAndBaseDir);
        if (di.has_error())
            return {};

        while (di.has_next()) {
            String path = di.next_path();

            // Dotfiles have to be explicitly requested
            if (path[0] == '.' && first_segment[0] != '.')
                continue;

            if (path.matches(first_segment, CaseSensitivity::CaseSensitive)) {
                StringBuilder builder;
                builder.append(base);
                if (!base.ends_with('/'))
                    builder.append('/');
                builder.append(path);
                result.append(expand_globs(path_segments, builder.string_view()));
            }
        }

        return result;
    } else {
        StringBuilder builder;
        builder.append(base);
        if (!base.ends_with('/'))
            builder.append('/');
        builder.append(first_segment);

        return expand_globs(move(path_segments), builder.string_view());
    }
}

Vector<AST::Command> Shell::expand_aliases(Vector<AST::Command> initial_commands)
{
    Vector<AST::Command> commands;

    Function<void(AST::Command&)> resolve_aliases_and_append = [&](auto& command) {
        if (!command.argv.is_empty()) {
            auto alias = resolve_alias(command.argv[0]);
            if (!alias.is_null()) {
                auto argv0 = command.argv.take_first();
                auto subcommand_ast = Parser { alias }.parse();
                if (subcommand_ast) {
                    while (subcommand_ast->is_execute()) {
                        auto* ast = static_cast<AST::Execute*>(subcommand_ast.ptr());
                        subcommand_ast = ast->command();
                    }
                    auto subcommand_nonnull = subcommand_ast.release_nonnull();
                    NonnullRefPtr<AST::Node> substitute = adopt(*new AST::Join(subcommand_nonnull->position(),
                        subcommand_nonnull,
                        adopt(*new AST::CommandLiteral(subcommand_nonnull->position(), command))));
                    auto res = substitute->run(*this);
                    for (auto& subst_command : res->resolve_as_commands(*this)) {
                        if (!subst_command.argv.is_empty() && subst_command.argv.first() == argv0) // Disallow an alias resolving to itself.
                            commands.append(subst_command);
                        else
                            resolve_aliases_and_append(subst_command);
                    }
                } else {
                    commands.append(command);
                }
            } else {
                commands.append(command);
            }
        } else {
            commands.append(command);
        }
    };

    for (auto& command : initial_commands)
        resolve_aliases_and_append(command);

    return commands;
}

String Shell::resolve_path(String path) const
{
    if (!path.starts_with('/'))
        path = String::format("%s/%s", cwd.characters(), path.characters());

    return Core::File::real_path_for(path);
}

Shell::LocalFrame* Shell::find_frame_containing_local_variable(const String& name)
{
    for (size_t i = m_local_frames.size(); i > 0; --i) {
        auto& frame = m_local_frames[i - 1];
        if (frame.local_variables.contains(name))
            return &frame;
    }
    return nullptr;
}

RefPtr<AST::Value> Shell::lookup_local_variable(const String& name)
{
    if (auto* frame = find_frame_containing_local_variable(name))
        return frame->local_variables.get(name).value();

    if (auto index = name.to_uint(); index.has_value())
        return get_argument(index.value());

    return nullptr;
}

RefPtr<AST::Value> Shell::get_argument(size_t index)
{
    if (index == 0)
        return adopt(*new AST::StringValue(current_script));

    --index;
    if (auto argv = lookup_local_variable("ARGV")) {
        if (argv->is_list_without_resolution()) {
            AST::ListValue* list = static_cast<AST::ListValue*>(argv.ptr());
            if (list->values().size() <= index)
                return nullptr;

            return list->values().at(index);
        }

        if (index != 0)
            return nullptr;

        return argv;
    }

    return nullptr;
}

String Shell::local_variable_or(const String& name, const String& replacement)
{
    auto value = lookup_local_variable(name);
    if (value) {
        StringBuilder builder;
        builder.join(" ", value->resolve_as_list(*this));
        return builder.to_string();
    }
    return replacement;
}

void Shell::set_local_variable(const String& name, RefPtr<AST::Value> value, bool only_in_current_frame)
{
    if (!only_in_current_frame) {
        if (auto* frame = find_frame_containing_local_variable(name)) {
            frame->local_variables.set(name, move(value));
            return;
        }
    }

    m_local_frames.last().local_variables.set(name, move(value));
}

void Shell::unset_local_variable(const String& name, bool only_in_current_frame)
{
    if (!only_in_current_frame) {
        if (auto* frame = find_frame_containing_local_variable(name))
            frame->local_variables.remove(name);
        return;
    }

    m_local_frames.last().local_variables.remove(name);
}

void Shell::define_function(String name, Vector<String> argnames, RefPtr<AST::Node> body)
{
    add_entry_to_cache(name);
    m_functions.set(name, { name, move(argnames), move(body) });
}

bool Shell::has_function(const String& name)
{
    return m_functions.contains(name);
}

bool Shell::invoke_function(const AST::Command& command, int& retval)
{
    if (command.argv.is_empty())
        return false;

    StringView name = command.argv.first();

    TemporaryChange<String> script_change { current_script, name };

    auto function_option = m_functions.get(name);
    if (!function_option.has_value())
        return false;

    auto& function = function_option.value();

    if (!function.body) {
        retval = 0;
        return true;
    }

    if (command.argv.size() - 1 < function.arguments.size()) {
        raise_error(ShellError::EvaluatedSyntaxError, String::formatted("Expected at least {} arguments to {}, but got {}", function.arguments.size(), function.name, command.argv.size() - 1), command.position);
        retval = 1;
        return true;
    }

    auto frame = push_frame(String::formatted("function {}", function.name));
    size_t index = 0;
    for (auto& arg : function.arguments) {
        ++index;
        set_local_variable(arg, adopt(*new AST::StringValue(command.argv[index])), true);
    }

    auto argv = command.argv;
    argv.take_first();
    set_local_variable("ARGV", adopt(*new AST::ListValue(move(argv))), true);

    Core::EventLoop loop;
    setup_signals();

    function.body->run(*this);

    retval = last_return_code;
    return true;
}

String Shell::format(const StringView& source, ssize_t& cursor) const
{
    Formatter formatter(source, cursor);
    auto result = formatter.format();
    cursor = formatter.cursor();

    return result;
}

Shell::Frame Shell::push_frame(String name)
{
    m_local_frames.append(make<LocalFrame>(name, decltype(LocalFrame::local_variables) {}));
#ifdef SH_DEBUG
    dbgln("New frame '{}' at {:p}", name, &m_local_frames.last());
#endif
    return { m_local_frames, m_local_frames.last() };
}

void Shell::pop_frame()
{
    ASSERT(m_local_frames.size() > 1);
    m_local_frames.take_last();
}

Shell::Frame::~Frame()
{
    if (!should_destroy_frame)
        return;
    if (&frames.last() != &frame) {
        dbgln("Frame destruction order violation near {:p} (container = {:p}) in '{}'", &frame, this, frame.name);
        dbgln("Current frames:");
        for (auto& frame : frames)
            dbgln("- {:p}: {}", &frame, frame.name);
        ASSERT_NOT_REACHED();
    }
    frames.take_last();
}

String Shell::resolve_alias(const String& name) const
{
    return m_aliases.get(name).value_or({});
}

bool Shell::is_runnable(const StringView& name)
{
    if (access(name.to_string().characters(), X_OK) == 0)
        return true;

    return binary_search(
        cached_path.span(),
        name.to_string(),
        nullptr,
        [](auto& name, auto& program) { return strcmp(name.characters(), program.characters()); });
}

int Shell::run_command(const StringView& cmd, Optional<SourcePosition> source_position_override)
{
    // The default-constructed mode of the shell
    // should not be used for execution!
    ASSERT(!m_default_constructed);

    take_error();

    ScopedValueRollback source_position_rollback { m_source_position };
    if (source_position_override.has_value())
        m_source_position = move(source_position_override);

    if (!m_source_position.has_value())
        m_source_position = SourcePosition { .source_file = {}, .literal_source_text = cmd, .position = {} };

    if (cmd.is_empty())
        return 0;

    auto command = Parser(cmd).parse();

    if (!command)
        return 0;

#ifdef SH_DEBUG
    dbgln("Command follows");
    command->dump(0);
#endif

    if (command->is_syntax_error()) {
        auto& error_node = command->syntax_error_node();
        auto& position = error_node.position();
        raise_error(ShellError::EvaluatedSyntaxError, error_node.error_text(), position);
    }

    if (!has_error(ShellError::None)) {
        possibly_print_error();
        take_error();
        return 1;
    }

    tcgetattr(0, &termios);

    command->run(*this);

    if (!has_error(ShellError::None)) {
        possibly_print_error();
        take_error();
        return 1;
    }

    return last_return_code;
}

RefPtr<Job> Shell::run_command(const AST::Command& command)
{
    FileDescriptionCollector fds;

    if (options.verbose)
        warnln("+ {}", command);

    // If the command is empty, store the redirections and apply them to all later commands.
    if (command.argv.is_empty() && !command.should_immediately_execute_next) {
        m_global_redirections.append(command.redirections);
        return nullptr;
    }

    // Resolve redirections.
    NonnullRefPtrVector<AST::Rewiring> rewirings;
    auto resolve_redirection = [&](auto& redirection) -> IterationDecision {
        auto rewiring_result = redirection.apply();
        if (rewiring_result.is_error()) {
            if (!rewiring_result.error().is_empty())
                fprintf(stderr, "error: %s\n", rewiring_result.error().characters());
            return IterationDecision::Break;
        }
        auto& rewiring = rewiring_result.value();

        if (rewiring->fd_action != AST::Rewiring::Close::ImmediatelyCloseNew)
            rewirings.append(*rewiring);

        if (rewiring->fd_action == AST::Rewiring::Close::Old) {
            fds.add(rewiring->old_fd);
        } else if (rewiring->fd_action == AST::Rewiring::Close::New) {
            if (rewiring->new_fd != -1)
                fds.add(rewiring->new_fd);
        } else if (rewiring->fd_action == AST::Rewiring::Close::ImmediatelyCloseNew) {
            fds.add(rewiring->new_fd);
        } else if (rewiring->fd_action == AST::Rewiring::Close::RefreshNew) {
            ASSERT(rewiring->other_pipe_end);

            int pipe_fd[2];
            int rc = pipe(pipe_fd);
            if (rc < 0) {
                perror("pipe(RedirRefresh)");
                return IterationDecision::Break;
            }
            rewiring->new_fd = pipe_fd[1];
            rewiring->other_pipe_end->new_fd = pipe_fd[0]; // This fd will be added to the collection on one of the next iterations.
            fds.add(pipe_fd[1]);
        } else if (rewiring->fd_action == AST::Rewiring::Close::RefreshOld) {
            ASSERT(rewiring->other_pipe_end);

            int pipe_fd[2];
            int rc = pipe(pipe_fd);
            if (rc < 0) {
                perror("pipe(RedirRefresh)");
                return IterationDecision::Break;
            }
            rewiring->old_fd = pipe_fd[1];
            rewiring->other_pipe_end->old_fd = pipe_fd[0]; // This fd will be added to the collection on one of the next iterations.
            fds.add(pipe_fd[1]);
        }
        return IterationDecision::Continue;
    };

    auto apply_rewirings = [&] {
        for (auto& rewiring : rewirings) {

#ifdef SH_DEBUG
            dbgln("in {}<{}>, dup2({}, {})", command.argv.is_empty() ? "(<Empty>)" : command.argv[0].characters(), getpid(), rewiring.old_fd, rewiring.new_fd);
#endif
            int rc = dup2(rewiring.old_fd, rewiring.new_fd);
            if (rc < 0) {
                perror("dup2(run)");
                return IterationDecision::Break;
            }
            // {new,old}_fd is closed via the `fds` collector, but rewiring.other_pipe_end->{new,old}_fd
            // isn't yet in that collector when the first child spawns.
            if (rewiring.other_pipe_end) {
                if (rewiring.fd_action == AST::Rewiring::Close::RefreshNew) {
                    if (rewiring.other_pipe_end && close(rewiring.other_pipe_end->new_fd) < 0)
                        perror("close other pipe end");
                } else if (rewiring.fd_action == AST::Rewiring::Close::RefreshOld) {
                    if (rewiring.other_pipe_end && close(rewiring.other_pipe_end->old_fd) < 0)
                        perror("close other pipe end");
                }
            }
        }

        return IterationDecision::Continue;
    };

    TemporaryChange signal_handler_install { m_should_reinstall_signal_handlers, false };

    for (auto& redirection : m_global_redirections) {
        if (resolve_redirection(redirection) == IterationDecision::Break)
            return nullptr;
    }

    for (auto& redirection : command.redirections) {
        if (resolve_redirection(redirection) == IterationDecision::Break)
            return nullptr;
    }

    if (command.should_wait && run_builtin(command, rewirings, last_return_code)) {
        for (auto& next_in_chain : command.next_chain)
            run_tail(command, next_in_chain, last_return_code);
        return nullptr;
    }

    auto can_be_run_in_current_process = command.should_wait && !command.pipeline && !command.argv.is_empty();
    if (can_be_run_in_current_process && has_function(command.argv.first())) {
        SavedFileDescriptors fds { rewirings };

        for (auto& rewiring : rewirings) {
            int rc = dup2(rewiring.old_fd, rewiring.new_fd);
            if (rc < 0) {
                perror("dup2(run)");
                return nullptr;
            }
        }

        if (invoke_function(command, last_return_code)) {
            for (auto& next_in_chain : command.next_chain)
                run_tail(command, next_in_chain, last_return_code);
            return nullptr;
        }
    }

    if (command.argv.is_empty() && !command.next_chain.is_empty() && command.should_immediately_execute_next && command.next_chain.first().node->should_override_execution_in_current_process()) {
        for (auto& next_in_chain : command.next_chain)
            run_tail(command, next_in_chain, last_return_code);
        return nullptr;
    }

    Vector<const char*> argv;
    Vector<String> copy_argv = command.argv;
    argv.ensure_capacity(command.argv.size() + 1);

    for (auto& arg : copy_argv)
        argv.append(arg.characters());

    argv.append(nullptr);

    int sync_pipe[2];
    if (pipe(sync_pipe) < 0) {
        perror("pipe");
        return nullptr;
    }

    pid_t child = fork();
    if (child < 0) {
        perror("fork");
        return nullptr;
    }

    if (child == 0) {
        close(sync_pipe[1]);

        m_is_subshell = true;
        m_pid = getpid();
        Core::EventLoop::notify_forked(Core::EventLoop::ForkEvent::Child);
        TemporaryChange signal_handler_install { m_should_reinstall_signal_handlers, true };

        if (apply_rewirings() == IterationDecision::Break)
            _exit(126);

        fds.collect();

        u8 c;
        while (read(sync_pipe[0], &c, 1) < 0) {
            if (errno != EINTR) {
                perror("read");
                // There's nothing interesting we can do here.
                break;
            }
        }

#ifdef SH_DEBUG
        dbgln("Synced up with parent, we're good to exec()");
#endif

        close(sync_pipe[0]);

        if (!m_is_subshell && command.should_wait)
            tcsetattr(0, TCSANOW, &default_termios);

        if (command.should_immediately_execute_next) {
            ASSERT(command.argv.is_empty());

            Core::EventLoop mainloop;
            setup_signals();

            for (auto& next_in_chain : command.next_chain)
                run_tail(command, next_in_chain, 0);

            _exit(last_return_code);
        }

        if (run_builtin(command, {}, last_return_code))
            _exit(last_return_code);

        if (invoke_function(command, last_return_code))
            _exit(last_return_code);

        // We no longer need the jobs here.
        jobs.clear();

        execute_process(move(argv));
        ASSERT_NOT_REACHED();
    }

    close(sync_pipe[0]);

    bool is_first = !command.pipeline || (command.pipeline && command.pipeline->pgid == -1);

    if (command.pipeline) {
        if (is_first) {
            command.pipeline->pgid = child;
        }
    }

    pid_t pgid = is_first ? child : (command.pipeline ? command.pipeline->pgid : child);
    if (!m_is_subshell || command.pipeline) {
        if (setpgid(child, pgid) < 0 && m_is_interactive)
            perror("setpgid");

        if (!m_is_subshell) {
            if (tcsetpgrp(STDOUT_FILENO, pgid) != 0 && m_is_interactive)
                perror("tcsetpgrp(OUT)");
            if (tcsetpgrp(STDIN_FILENO, pgid) != 0 && m_is_interactive)
                perror("tcsetpgrp(IN)");
        }
    }

    while (write(sync_pipe[1], "x", 1) < 0) {
        if (errno != EINTR) {
            perror("write");
            // There's nothing interesting we can do here.
            break;
        }
    }

    close(sync_pipe[1]);

    StringBuilder cmd;
    cmd.join(" ", command.argv);

    auto command_copy = AST::Command(command);
    // Clear the next chain if it's to be immediately executed
    // as the child will run this chain.
    if (command.should_immediately_execute_next)
        command_copy.next_chain.clear();
    auto job = Job::create(child, pgid, cmd.build(), find_last_job_id() + 1, move(command_copy));
    jobs.set((u64)child, job);

    job->on_exit = [this](auto job) {
        if (!job->exited())
            return;

        restore_ios();
        if (job->is_running_in_background() && job->should_announce_exit())
            warnln("Shell: Job {} ({}) exited\n", job->job_id(), job->cmd().characters());
        else if (job->signaled() && job->should_announce_signal())
            warnln("Shell: Job {} ({}) {}\n", job->job_id(), job->cmd().characters(), strsignal(job->termination_signal()));

        last_return_code = job->exit_code();
        job->disown();

        run_tail(job);
    };

    fds.collect();

    return *job;
}

void Shell::execute_process(Vector<const char*>&& argv)
{
    int rc = execvp(argv[0], const_cast<char* const*>(argv.data()));
    if (rc < 0) {
        int saved_errno = errno;
        struct stat st;
        if (stat(argv[0], &st)) {
            fprintf(stderr, "stat(%s): %s\n", argv[0], strerror(errno));
            _exit(126);
        }
        if (!(st.st_mode & S_IXUSR)) {
            fprintf(stderr, "%s: Not executable\n", argv[0]);
            _exit(126);
        }
        if (saved_errno == ENOENT) {
            int shebang_fd = open(argv[0], O_RDONLY);
            auto close_argv = ScopeGuard([shebang_fd]() { if (shebang_fd >= 0)  close(shebang_fd); });
            char shebang[256] {};
            ssize_t num_read = -1;
            if ((shebang_fd >= 0) && ((num_read = read(shebang_fd, shebang, sizeof(shebang))) >= 2) && (StringView(shebang).starts_with("#!"))) {
                StringView shebang_path_view(&shebang[2], num_read - 2);
                Optional<size_t> newline_pos = shebang_path_view.find_first_of("\n\r");
                shebang[newline_pos.has_value() ? (newline_pos.value() + 2) : num_read] = '\0';
                argv[0] = shebang;
                int rc = execvp(argv[0], const_cast<char* const*>(argv.data()));
                if (rc < 0)
                    fprintf(stderr, "%s: Invalid interpreter \"%s\": %s\n", argv[0], &shebang[2], strerror(errno));
            } else
                fprintf(stderr, "%s: Command not found.\n", argv[0]);
        } else {
            if (S_ISDIR(st.st_mode)) {
                fprintf(stderr, "Shell: %s: Is a directory\n", argv[0]);
                _exit(126);
            }
            fprintf(stderr, "execvp(%s): %s\n", argv[0], strerror(saved_errno));
        }
        _exit(126);
    }
    ASSERT_NOT_REACHED();
}

void Shell::run_tail(const AST::Command& invoking_command, const AST::NodeWithAction& next_in_chain, int head_exit_code)
{
    if (m_error != ShellError::None) {
        possibly_print_error();
        if (!is_control_flow(m_error))
            take_error();
        return;
    }
    auto evaluate = [&] {
        if (next_in_chain.node->would_execute()) {
            next_in_chain.node->run(*this);
            return;
        }
        auto node = next_in_chain.node;
        if (!invoking_command.should_wait)
            node = adopt(static_cast<AST::Node&>(*new AST::Background(next_in_chain.node->position(), move(node))));
        adopt(static_cast<AST::Node&>(*new AST::Execute(next_in_chain.node->position(), move(node))))->run(*this);
    };
    switch (next_in_chain.action) {
    case AST::NodeWithAction::And:
        if (head_exit_code == 0)
            evaluate();
        break;
    case AST::NodeWithAction::Or:
        if (head_exit_code != 0)
            evaluate();
        break;
    case AST::NodeWithAction::Sequence:
        evaluate();
        break;
    }
}

void Shell::run_tail(RefPtr<Job> job)
{
    if (auto cmd = job->command_ptr()) {
        deferred_invoke([=, this](auto&) {
            for (auto& next_in_chain : cmd->next_chain) {
                run_tail(*cmd, next_in_chain, job->exit_code());
            }
        });
    }
}

NonnullRefPtrVector<Job> Shell::run_commands(Vector<AST::Command>& commands)
{
    if (m_error != ShellError::None) {
        possibly_print_error();
        if (!is_control_flow(m_error))
            take_error();
        return {};
    }

    NonnullRefPtrVector<Job> spawned_jobs;

    for (auto& command : commands) {
#ifdef SH_DEBUG
        dbgln("Command");
        for (auto& arg : command.argv)
            dbgln("argv: {}", arg);
        for (auto& redir : command.redirections) {
            if (redir.is_path_redirection()) {
                auto path_redir = (const AST::PathRedirection*)&redir;
                dbgln("redir path '{}' <-({})-> {}", path_redir->path, (int)path_redir->direction, path_redir->fd);
            } else if (redir.is_fd_redirection()) {
                auto* fdredir = (const AST::FdRedirection*)&redir;
                dbgln("redir fd {} -> {}", fdredir->old_fd, fdredir->new_fd);
            } else if (redir.is_close_redirection()) {
                auto close_redir = (const AST::CloseRedirection*)&redir;
                dbgln("close fd {}", close_redir->fd);
            } else {
                ASSERT_NOT_REACHED();
            }
        }
#endif
        auto job = run_command(command);
        if (!job)
            continue;

        spawned_jobs.append(*job);
        if (command.should_wait) {
            block_on_job(job);
        } else {
            job->set_running_in_background(true);
            if (!command.is_pipe_source && command.should_notify_if_in_background)
                job->set_should_announce_exit(true);
        }
    }

    if (m_error != ShellError::None) {
        possibly_print_error();
        if (!is_control_flow(m_error))
            take_error();
    }

    return spawned_jobs;
}

bool Shell::run_file(const String& filename, bool explicitly_invoked)
{
    TemporaryChange script_change { current_script, filename };
    TemporaryChange interactive_change { m_is_interactive, false };
    TemporaryChange<Optional<SourcePosition>> source_change { m_source_position, SourcePosition { .source_file = filename, .literal_source_text = {}, .position = {} } };

    auto file_result = Core::File::open(filename, Core::File::ReadOnly);
    if (file_result.is_error()) {
        auto error = String::formatted("'{}': {}", escape_token_for_single_quotes(filename), file_result.error());
        if (explicitly_invoked)
            raise_error(ShellError::OpenFailure, error);
        else
            dbgln("open() failed for {}", error);
        return false;
    }
    auto file = file_result.value();
    auto data = file->read_all();
    run_command(data);
    return true;
}
void Shell::restore_ios()
{
    if (m_is_subshell)
        return;
    tcsetattr(0, TCSANOW, &termios);
    tcsetpgrp(STDOUT_FILENO, m_pid);
    tcsetpgrp(STDIN_FILENO, m_pid);
}

void Shell::block_on_pipeline(RefPtr<AST::Pipeline> pipeline)
{
    if (!pipeline)
        return;

    for (auto& it : jobs) {
        if (auto cmd = it.value->command_ptr(); cmd->pipeline == pipeline && cmd->is_pipe_source)
            block_on_job(it.value);
    }
}

void Shell::block_on_job(RefPtr<Job> job)
{
    TemporaryChange<const Job*> current_job { m_current_job, job.ptr() };

    if (!job)
        return;

    if (job->is_suspended())
        return; // We cannot wait for a suspended job.

    ScopeGuard io_restorer { [&]() {
        if (job->exited() && !job->is_running_in_background()) {
            restore_ios();
        }
    } };

    Core::EventLoop loop;
    job->on_exit = [&, old_exit = move(job->on_exit)](auto job) {
        if (old_exit)
            old_exit(job);
        loop.quit(0);
    };

    if (job->exited())
        return;

    loop.exec();

    // If the job is part of a pipeline, wait for the rest of the members too.
    if (auto command = job->command_ptr())
        block_on_pipeline(command->pipeline);
}

String Shell::get_history_path()
{
    if (auto histfile = getenv("HISTFILE"))
        return { histfile };
    return String::formatted("{}/.history", home);
}

String Shell::escape_token_for_single_quotes(const String& token)
{
    StringBuilder builder;

    for (auto c : token) {
        switch (c) {
        case '\'':
            builder.append("'\\'");
            break;
        default:
            break;
        }
        builder.append(c);
    }

    return builder.build();
}

bool Shell::is_special(char c)
{
    switch (c) {
    case '\'':
    case '"':
    case '$':
    case '|':
    case '>':
    case '<':
    case '(':
    case ')':
    case '{':
    case '}':
    case '&':
    case '\\':
    case ' ':
        return true;
    default:
        return false;
    }
}

String Shell::escape_token(const String& token)
{
    StringBuilder builder;

    for (auto c : token) {
        if (is_special(c))
            builder.append('\\');
        builder.append(c);
    }

    return builder.build();
}

String Shell::unescape_token(const String& token)
{
    StringBuilder builder;

    enum {
        Free,
        Escaped
    } state { Free };

    for (auto c : token) {
        switch (state) {
        case Escaped:
            builder.append(c);
            state = Free;
            break;
        case Free:
            if (c == '\\')
                state = Escaped;
            else
                builder.append(c);
            break;
        }
    }

    if (state == Escaped)
        builder.append('\\');

    return builder.build();
}

void Shell::cache_path()
{
    if (!cached_path.is_empty())
        cached_path.clear_with_capacity();

    // Add shell builtins to the cache.
    for (const auto& builtin_name : builtin_names)
        cached_path.append(escape_token(builtin_name));

    // Add aliases to the cache.
    for (const auto& alias : m_aliases) {
        auto name = escape_token(alias.key);
        if (cached_path.contains_slow(name))
            continue;
        cached_path.append(name);
    }

    String path = getenv("PATH");
    if (!path.is_empty()) {
        auto directories = path.split(':');
        for (const auto& directory : directories) {
            Core::DirIterator programs(directory.characters(), Core::DirIterator::SkipDots);
            while (programs.has_next()) {
                auto program = programs.next_path();
                String program_path = String::format("%s/%s", directory.characters(), program.characters());
                auto escaped_name = escape_token(program);
                if (cached_path.contains_slow(escaped_name))
                    continue;
                if (access(program_path.characters(), X_OK) == 0)
                    cached_path.append(escaped_name);
            }
        }
    }

    quick_sort(cached_path);
}

void Shell::add_entry_to_cache(const String& entry)
{
    size_t index = 0;
    auto match = binary_search(
        cached_path.span(),
        entry,
        &index,
        [](auto& name, auto& program) { return strcmp(name.characters(), program.characters()); });

    if (match)
        return;

    while (index < cached_path.size() && strcmp(cached_path[index].characters(), entry.characters()) < 0) {
        index++;
    }
    cached_path.insert(index, entry);
}

void Shell::highlight(Line::Editor& editor) const
{
    auto line = editor.line();
    Parser parser(line);
    auto ast = parser.parse();
    if (!ast)
        return;
    ast->highlight_in_editor(editor, const_cast<Shell&>(*this));
}

Vector<Line::CompletionSuggestion> Shell::complete()
{
    auto line = m_editor->line(m_editor->cursor());

    Parser parser(line);

    auto ast = parser.parse();

    if (!ast)
        return {};

    return ast->complete_for_editor(*this, line.length());
}

Vector<Line::CompletionSuggestion> Shell::complete_path(const String& base, const String& part, size_t offset)
{
    auto token = offset ? part.substring_view(0, offset) : "";
    String path;

    ssize_t last_slash = token.length() - 1;
    while (last_slash >= 0 && token[last_slash] != '/')
        --last_slash;

    StringBuilder path_builder;
    auto init_slash_part = token.substring_view(0, last_slash + 1);
    auto last_slash_part = token.substring_view(last_slash + 1, token.length() - last_slash - 1);

    // Depending on the base, we will have to prepend cwd.
    if (base.is_empty()) {
        // '' /foo -> absolute
        // '' foo -> relative
        if (!token.starts_with('/'))
            path_builder.append(cwd);
        path_builder.append('/');
        path_builder.append(init_slash_part);
    } else {
        // /foo * -> absolute
        // foo * -> relative
        if (!base.starts_with('/'))
            path_builder.append(cwd);
        path_builder.append('/');
        path_builder.append(base);
        path_builder.append('/');
        path_builder.append(init_slash_part);
    }
    path = path_builder.build();
    token = last_slash_part;

    // the invariant part of the token is actually just the last segment
    // e. in `cd /foo/bar', 'bar' is the invariant
    //      since we are not suggesting anything starting with
    //      `/foo/', but rather just `bar...'
    auto token_length = escape_token(token).length();
    if (m_editor)
        m_editor->suggest(token_length, last_slash + 1);

    // only suggest dot-files if path starts with a dot
    Core::DirIterator files(path,
        token.starts_with('.') ? Core::DirIterator::SkipParentAndBaseDir : Core::DirIterator::SkipDots);

    Vector<Line::CompletionSuggestion> suggestions;

    while (files.has_next()) {
        auto file = files.next_path();
        if (file.starts_with(token)) {
            struct stat program_status;
            String file_path = String::format("%s/%s", path.characters(), file.characters());
            int stat_error = stat(file_path.characters(), &program_status);
            if (!stat_error) {
                if (S_ISDIR(program_status.st_mode)) {
                    suggestions.append({ escape_token(file), "/" });
                } else {
                    suggestions.append({ escape_token(file), " " });
                }
                suggestions.last().input_offset = token_length;
            }
        }
    }

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_program_name(const String& name, size_t offset)
{
    auto match = binary_search(
        cached_path.span(),
        name,
        nullptr,
        [](auto& name, auto& program) { return strncmp(name.characters(), program.characters(), name.length()); });

    if (!match)
        return complete_path("", name, offset);

    String completion = *match;
    auto token_length = escape_token(name).length();
    if (m_editor)
        m_editor->suggest(token_length, 0);

    // Now that we have a program name starting with our token, we look at
    // other program names starting with our token and cut off any mismatching
    // characters.

    Vector<Line::CompletionSuggestion> suggestions;

    int index = match - cached_path.data();
    for (int i = index - 1; i >= 0 && cached_path[i].starts_with(name); --i) {
        suggestions.append({ cached_path[i], " " });
        suggestions.last().input_offset = token_length;
    }
    for (size_t i = index + 1; i < cached_path.size() && cached_path[i].starts_with(name); ++i) {
        suggestions.append({ cached_path[i], " " });
        suggestions.last().input_offset = token_length;
    }
    suggestions.append({ cached_path[index], " " });
    suggestions.last().input_offset = token_length;

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_variable(const String& name, size_t offset)
{
    Vector<Line::CompletionSuggestion> suggestions;
    auto pattern = offset ? name.substring_view(0, offset) : "";

    if (m_editor)
        m_editor->suggest(offset);

    // Look at local variables.
    for (auto& frame : m_local_frames) {
        for (auto& variable : frame.local_variables) {
            if (variable.key.starts_with(pattern) && !suggestions.contains_slow(variable.key))
                suggestions.append(variable.key);
        }
    }

    // Look at the environment.
    for (auto i = 0; environ[i]; ++i) {
        auto entry = StringView { environ[i] };
        if (entry.starts_with(pattern)) {
            auto parts = entry.split_view('=');
            if (parts.is_empty() || parts.first().is_empty())
                continue;
            String name = parts.first();
            if (suggestions.contains_slow(name))
                continue;
            suggestions.append(move(name));
            suggestions.last().input_offset = offset;
        }
    }

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_user(const String& name, size_t offset)
{
    Vector<Line::CompletionSuggestion> suggestions;
    auto pattern = offset ? name.substring_view(0, offset) : "";

    if (m_editor)
        m_editor->suggest(offset);

    Core::DirIterator di("/home", Core::DirIterator::SkipParentAndBaseDir);

    if (di.has_error())
        return suggestions;

    while (di.has_next()) {
        String name = di.next_path();
        if (name.starts_with(pattern)) {
            suggestions.append(name);
            suggestions.last().input_offset = offset;
        }
    }

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_option(const String& program_name, const String& option, size_t offset)
{
    size_t start = 0;
    while (start < option.length() && option[start] == '-' && start < 2)
        ++start;
    auto option_pattern = offset > start ? option.substring_view(start, offset - start) : "";
    if (m_editor)
        m_editor->suggest(offset);

    Vector<Line::CompletionSuggestion> suggestions;

    dbgln("Shell::complete_option({}, {})", program_name, option_pattern);

    // FIXME: Figure out how to do this stuff.
    if (has_builtin(program_name)) {
        // Complete builtins.
        if (program_name == "setopt") {
            bool negate = false;
            if (option_pattern.starts_with("no_")) {
                negate = true;
                option_pattern = option_pattern.substring_view(3, option_pattern.length() - 3);
            }
            auto maybe_negate = [&](const StringView& view) {
                static StringBuilder builder;
                builder.clear();
                builder.append("--");
                if (negate)
                    builder.append("no_");
                builder.append(view);
                return builder.to_string();
            };
#define __ENUMERATE_SHELL_OPTION(name, d_, descr_)          \
    if (StringView { #name }.starts_with(option_pattern)) { \
        suggestions.append(maybe_negate(#name));            \
        suggestions.last().input_offset = offset;           \
    }

            ENUMERATE_SHELL_OPTIONS();
#undef __ENUMERATE_SHELL_OPTION
            return suggestions;
        }
    }
    return suggestions;
}

void Shell::bring_cursor_to_beginning_of_a_line() const
{
    struct winsize ws;
    if (m_editor) {
        ws = m_editor->terminal_size();
    } else {
        if (ioctl(STDERR_FILENO, TIOCGWINSZ, &ws) < 0) {
            // Very annoying assumptions.
            ws.ws_col = 80;
            ws.ws_row = 25;
        }
    }

    // Black with Cyan background.
    constexpr auto default_mark = "\e[30;46m%\e[0m";
    String eol_mark = getenv("PROMPT_EOL_MARK");
    if (eol_mark.is_null())
        eol_mark = default_mark;
    size_t eol_mark_length = Line::Editor::actual_rendered_string_metrics(eol_mark).line_metrics.last().total_length();
    if (eol_mark_length >= ws.ws_col) {
        eol_mark = default_mark;
        eol_mark_length = 1;
    }

    fputs(eol_mark.characters(), stderr);

    for (auto i = eol_mark_length; i < ws.ws_col; ++i)
        putc(' ', stderr);

    putc('\r', stderr);
}

bool Shell::read_single_line()
{
    restore_ios();
    bring_cursor_to_beginning_of_a_line();
    auto line_result = m_editor->get_line(prompt());

    if (line_result.is_error()) {
        if (line_result.error() == Line::Editor::Error::Eof || line_result.error() == Line::Editor::Error::Empty) {
            // Pretend the user tried to execute builtin_exit()
            run_command("exit");
            return read_single_line();
        } else {
            Core::EventLoop::current().quit(1);
            return false;
        }
    }

    auto& line = line_result.value();

    if (line.is_empty())
        return true;

    run_command(line);

    m_editor->add_to_history(line);
    return true;
}

void Shell::custom_event(Core::CustomEvent& event)
{
    if (event.custom_type() == ReadLine) {
        if (read_single_line())
            Core::EventLoop::current().post_event(*this, make<Core::CustomEvent>(ShellEventType::ReadLine));
        return;
    }
}

void Shell::notify_child_event()
{
#ifdef ENSURE_WAITID_ONCE
    static HashTable<pid_t> s_waited_for_pids;
#endif
    Vector<u64> disowned_jobs;
    // Workaround the fact that we can't receive *who* exactly changed state.
    // The child might still be alive (and even running) when this signal is dispatched to us
    // so just...repeat until we find a suitable child.
    // This, of course, will mean that someone can send us a SIGCHILD and we'd be spinning here
    // until the next child event we can actually handle.
    bool found_child = false;
    do {
        // Ignore stray SIGCHLD when there are no jobs.
        if (jobs.is_empty())
            return;

        for (auto& it : jobs) {
            auto job_id = it.key;
            auto& job = *it.value;
#ifdef ENSURE_WAITID_ONCE
            // Theoretically, this should never trip, as jobs are removed from
            // the job table when waitpid() succeeds *and* the child is dead.
            ASSERT(!s_waited_for_pids.contains(job.pid()));
#endif

            int wstatus = 0;
#ifdef SH_DEBUG
            dbgln("waitpid({}) = ...", job.pid());
#endif
            auto child_pid = waitpid(job.pid(), &wstatus, WNOHANG | WUNTRACED);
#ifdef SH_DEBUG
            dbgln("... = {} - {}", child_pid, wstatus);
#endif

            if (child_pid < 0) {
                if (errno == ECHILD) {
                    // The child process went away before we could process its death, just assume it exited all ok.
                    // FIXME: This should never happen, the child should stay around until we do the waitpid above.
                    child_pid = job.pid();
                } else {
                    ASSERT_NOT_REACHED();
                }
            }
            if (child_pid == 0) {
                // If the child existed, but wasn't dead.
                continue;
            }
            if (child_pid == job.pid()) {
                if (WIFSIGNALED(wstatus) && !WIFSTOPPED(wstatus)) {
                    job.set_signalled(WTERMSIG(wstatus));
                } else if (WIFEXITED(wstatus)) {
                    job.set_has_exit(WEXITSTATUS(wstatus));
                } else if (WIFSTOPPED(wstatus)) {
                    job.unblock();
                    job.set_is_suspended(true);
                }
                found_child = true;
#ifdef ENSURE_WAITID_ONCE
                // NOTE: This check is here to find bugs about our assumptions about waitpid(),
                //       it does not hold in general, and it definitely does not hold in the long run.
                // Reasons that we would call waitpid() more than once:
                // - PID reuse/wraparound: This will simply fail the assertion, ignored here.
                // - Non-terminating unblocks:
                //   - Suspension: (e.g. via ^Z)
                //   - ?
                // - ?
                if (job.exited())
                    s_waited_for_pids.set(child_pid);
#endif
            }
            if (job.should_be_disowned())
                disowned_jobs.append(job_id);
        }

        for (auto job_id : disowned_jobs) {
            jobs.remove(job_id);
        }
    } while (!found_child);
}

Shell::Shell()
    : m_default_constructed(true)
{
    push_frame("main").leak_frame();

    int rc = gethostname(hostname, Shell::HostNameSize);
    if (rc < 0)
        perror("gethostname");

    {
        auto* pw = getpwuid(getuid());
        if (pw) {
            username = pw->pw_name;
            home = pw->pw_dir;
            setenv("HOME", pw->pw_dir, 1);
        }
        endpwent();
    }

    // For simplicity, start at the user's home directory.
    this->cwd = home;
    setenv("PWD", home.characters(), 1);

    // Add the default PATH vars.
    {
        StringBuilder path;
        path.append(getenv("PATH"));
        if (path.length())
            path.append(":");
        path.append("/bin:/usr/bin:/usr/local/bin");
        setenv("PATH", path.to_string().characters(), true);
    }

    cache_path();
}

Shell::Shell(Line::Editor& editor)
    : m_editor(editor)
{
    uid = getuid();
    tcsetpgrp(0, getpgrp());
    m_pid = getpid();

    push_frame("main").leak_frame();

    int rc = gethostname(hostname, Shell::HostNameSize);
    if (rc < 0)
        perror("gethostname");

    auto istty = isatty(STDIN_FILENO);
    m_is_interactive = istty;

    if (istty) {
        rc = ttyname_r(0, ttyname, Shell::TTYNameSize);
        if (rc < 0)
            perror("ttyname_r");
    } else {
        ttyname[0] = 0;
    }

    {
        auto* cwd = getcwd(nullptr, 0);
        this->cwd = cwd;
        setenv("PWD", cwd, 1);
        free(cwd);
    }

    {
        auto* pw = getpwuid(getuid());
        if (pw) {
            username = pw->pw_name;
            home = pw->pw_dir;
            setenv("HOME", pw->pw_dir, 1);
        }
        endpwent();
    }

    directory_stack.append(cwd);
    m_editor->load_history(get_history_path());
    cache_path();

    m_editor->register_key_input_callback('\n', [](Line::Editor& editor) {
        auto ast = Parser(editor.line()).parse();
        if (ast && ast->is_syntax_error() && ast->syntax_error_node().is_continuable())
            return true;

        return EDITOR_INTERNAL_FUNCTION(finish)(editor);
    });
}

Shell::~Shell()
{
    if (m_default_constructed)
        return;

    stop_all_jobs();
    m_editor->save_history(get_history_path());
}

void Shell::stop_all_jobs()
{
    if (!jobs.is_empty()) {
        if (m_is_interactive && !m_is_subshell)
            printf("Killing active jobs\n");
        for (auto& entry : jobs) {
            if (entry.value->is_suspended()) {
#ifdef SH_DEBUG
                dbgln("Job {} is suspended", entry.value->pid());
#endif
                kill_job(entry.value, SIGCONT);
            }

            kill_job(entry.value, SIGHUP);
        }

        usleep(10000); // Wait for a bit before killing the job

        for (auto& entry : jobs) {
#ifdef SH_DEBUG
            dbgln("Actively killing {} ({})", entry.value->pid(), entry.value->cmd());
#endif
            kill_job(entry.value, SIGKILL);
        }

        jobs.clear();
    }
}

u64 Shell::find_last_job_id() const
{
    u64 job_id = 0;
    for (auto& entry : jobs) {
        if (entry.value->job_id() > job_id)
            job_id = entry.value->job_id();
    }
    return job_id;
}

const Job* Shell::find_job(u64 id)
{
    for (auto& entry : jobs) {
        if (entry.value->job_id() == id)
            return entry.value;
    }
    return nullptr;
}

void Shell::kill_job(const Job* job, int sig)
{
    if (!job)
        return;

    if (killpg(job->pgid(), sig) < 0) {
        if (kill(job->pid(), sig) < 0) {
            perror("kill");
        }
    }
}

void Shell::save_to(JsonObject& object)
{
    Core::Object::save_to(object);
    object.set("working_directory", cwd);
    object.set("username", username);
    object.set("user_home_path", home);
    object.set("user_id", uid);
    object.set("directory_stack_size", directory_stack.size());
    object.set("cd_history_size", cd_history.size());

    // Jobs.
    JsonArray job_objects;
    for (auto& job_entry : jobs) {
        JsonObject job_object;
        job_object.set("pid", job_entry.value->pid());
        job_object.set("pgid", job_entry.value->pgid());
        job_object.set("running_time", job_entry.value->timer().elapsed());
        job_object.set("command", job_entry.value->cmd());
        job_object.set("is_running_in_background", job_entry.value->is_running_in_background());
        job_objects.append(move(job_object));
    }
    object.set("jobs", move(job_objects));
}

void Shell::possibly_print_error() const
{
    switch (m_error) {
    case ShellError::EvaluatedSyntaxError:
        warnln("Shell Syntax Error: {}", m_error_description);
        break;
    case ShellError::InvalidGlobError:
    case ShellError::NonExhaustiveMatchRules:
        warnln("Shell: {}", m_error_description);
        break;
    case ShellError::OpenFailure:
        warnln("Shell: Open failed for {}", m_error_description);
        break;
    case ShellError::InternalControlFlowBreak:
    case ShellError::InternalControlFlowContinue:
        return;
    case ShellError::None:
        return;
    }

    if (m_source_position.has_value() && m_source_position->position.has_value()) {
        auto& source_position = m_source_position.value();
        auto do_line = [&](auto line, auto& current_line) {
            auto is_in_range = line >= (i64)source_position.position->start_line.line_number && line <= (i64)source_position.position->end_line.line_number;
            warnln("{:>3}| {}", line, current_line);
            if (is_in_range) {
                warn("\x1b[31m");
                size_t length_written_so_far = 0;
                if (line == (i64)source_position.position->start_line.line_number) {
                    warn("{:~>{}}", "", 5 + source_position.position->start_line.line_column);
                    length_written_so_far += source_position.position->start_line.line_column;
                } else {
                    warn("{:~>{}}", "", 5);
                }
                if (line == (i64)source_position.position->end_line.line_number) {
                    warn("{:^>{}}", "", source_position.position->end_line.line_column - length_written_so_far);
                    length_written_so_far += source_position.position->start_line.line_column;
                } else {
                    warn("{:^>{}}", "", current_line.length() - length_written_so_far);
                }
                warnln("\x1b[0m");
            }
        };
        int line = -1;
        String current_line;
        i64 line_to_skip_to = max(source_position.position->start_line.line_number, 2ul) - 2;

        if (!source_position.source_file.is_null()) {
            auto file = Core::File::open(source_position.source_file, Core::IODevice::OpenMode::ReadOnly);
            if (file.is_error()) {
                warnln("Shell: Internal error while trying to display source information: {} (while reading '{}')", file.error(), source_position.source_file);
                return;
            }
            while (line < line_to_skip_to) {
                if (file.value()->eof())
                    return;
                current_line = file.value()->read_line();
                ++line;
            }

            for (; line < (i64)source_position.position->end_line.line_number + 2; ++line) {
                do_line(line, current_line);
                if (file.value()->eof())
                    current_line = "";
                else
                    current_line = file.value()->read_line();
            }
        } else if (!source_position.literal_source_text.is_empty()) {
            GenericLexer lexer { source_position.literal_source_text };
            while (line < line_to_skip_to) {
                if (lexer.is_eof())
                    return;
                current_line = lexer.consume_line();
                ++line;
            }

            for (; line < (i64)source_position.position->end_line.line_number + 2; ++line) {
                do_line(line, current_line);
                if (lexer.is_eof())
                    current_line = "";
                else
                    current_line = lexer.consume_line();
            }
        }
    }
    warnln();
}

void FileDescriptionCollector::collect()
{
    for (auto fd : m_fds)
        close(fd);
    m_fds.clear();
}

FileDescriptionCollector::~FileDescriptionCollector()
{
    collect();
}

void FileDescriptionCollector::add(int fd)
{
    m_fds.append(fd);
}

SavedFileDescriptors::SavedFileDescriptors(const NonnullRefPtrVector<AST::Rewiring>& intended_rewirings)
{
    for (auto& rewiring : intended_rewirings) {
        int new_fd = dup(rewiring.new_fd);
        if (new_fd < 0) {
            if (errno != EBADF)
                perror("dup");
            // The fd that will be overwritten isn't open right now,
            // it will be cleaned up by the exec()-side collector
            // and we have nothing to do here, so just ignore this error.
            continue;
        }

        auto flags = fcntl(new_fd, F_GETFL);
        auto rc = fcntl(new_fd, F_SETFL, flags | FD_CLOEXEC);
        ASSERT(rc == 0);

        m_saves.append({ rewiring.new_fd, new_fd });
        m_collector.add(new_fd);
    }
}

SavedFileDescriptors::~SavedFileDescriptors()
{
    for (auto& save : m_saves) {
        if (dup2(save.saved, save.original) < 0) {
            perror("dup2(~SavedFileDescriptors)");
            continue;
        }
    }
}

}
