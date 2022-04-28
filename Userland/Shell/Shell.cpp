/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Shell.h"
#include "Execution.h"
#include "Formatter.h"
#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/GenericLexer.h>
#include <AK/JsonParser.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopedValueRollback.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <AK/URL.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibCore/Timer.h>
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

namespace Shell {

void Shell::setup_signals()
{
    if (m_should_reinstall_signal_handlers) {
        Core::EventLoop::register_signal(SIGCHLD, [this](int) {
            dbgln_if(SH_DEBUG, "SIGCHLD!");
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

void Shell::print_path(StringView path)
{
    if (s_disable_hyperlinks || !m_is_interactive) {
        out("{}", path);
        return;
    }
    auto url = URL::create_with_file_scheme(path, {}, hostname);
    out("\033]8;;{}\033\\{}\033]8;;\033\\", url.serialize(), path);
}

String Shell::prompt() const
{
    auto build_prompt = [&]() -> String {
        auto* ps1 = getenv("PROMPT");
        if (!ps1) {
            if (uid == 0)
                return "# ";

            StringBuilder builder;
            builder.appendff("\033]0;{}@{}:{}\007", username, hostname, cwd);
            builder.appendff("\033[31;1m{}\033[0m@\033[37;1m{}\033[0m:\033[32;1m{}\033[0m$> ", username, hostname, cwd);
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

String Shell::expand_tilde(StringView expression)
{
    VERIFY(expression.starts_with('~'));

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
        char const* home = getenv("HOME");
        if (!home) {
            auto passwd = getpwuid(getuid());
            VERIFY(passwd && passwd->pw_dir);
            return String::formatted("{}/{}", passwd->pw_dir, path.to_string());
        }
        return String::formatted("{}/{}", home, path.to_string());
    }

    auto passwd = getpwnam(login_name.to_string().characters());
    if (!passwd)
        return expression;
    VERIFY(passwd->pw_dir);

    return String::formatted("{}/{}", passwd->pw_dir, path.to_string());
}

bool Shell::is_glob(StringView s)
{
    for (size_t i = 0; i < s.length(); i++) {
        char c = s.characters_without_null_termination()[i];
        if (c == '*' || c == '?')
            return true;
    }
    return false;
}

Vector<StringView> Shell::split_path(StringView path)
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

Vector<String> Shell::expand_globs(StringView path, StringView base)
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

Vector<String> Shell::expand_globs(Vector<StringView> path_segments, StringView base)
{
    if (path_segments.is_empty()) {
        String base_str = base;
        struct stat statbuf;
        if (lstat(base_str.characters(), &statbuf) < 0)
            return {};
        return { move(base_str) };
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
                result.extend(expand_globs(path_segments, builder.string_view()));
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
                    NonnullRefPtr<AST::Node> substitute = adopt_ref(*new AST::Join(subcommand_nonnull->position(),
                        subcommand_nonnull,
                        adopt_ref(*new AST::CommandLiteral(subcommand_nonnull->position(), command))));
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
        path = String::formatted("{}/{}", cwd, path);

    return Core::File::real_path_for(path);
}

Shell::LocalFrame* Shell::find_frame_containing_local_variable(StringView name)
{
    for (size_t i = m_local_frames.size(); i > 0; --i) {
        auto& frame = m_local_frames[i - 1];
        if (frame.local_variables.contains(name))
            return &frame;
    }
    return nullptr;
}

RefPtr<AST::Value> Shell::lookup_local_variable(StringView name) const
{
    if (auto* frame = find_frame_containing_local_variable(name))
        return frame->local_variables.get(name).value();

    if (auto index = name.to_uint(); index.has_value())
        return get_argument(index.value());

    return nullptr;
}

RefPtr<AST::Value> Shell::get_argument(size_t index) const
{
    if (index == 0)
        return adopt_ref(*new AST::StringValue(current_script));

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

String Shell::local_variable_or(StringView name, String const& replacement) const
{
    auto value = lookup_local_variable(name);
    if (value) {
        StringBuilder builder;
        builder.join(" ", value->resolve_as_list(*this));
        return builder.to_string();
    }
    return replacement;
}

void Shell::set_local_variable(String const& name, RefPtr<AST::Value> value, bool only_in_current_frame)
{
    if (!only_in_current_frame) {
        if (auto* frame = find_frame_containing_local_variable(name)) {
            frame->local_variables.set(name, move(value));
            return;
        }
    }

    m_local_frames.last().local_variables.set(name, move(value));
}

void Shell::unset_local_variable(StringView name, bool only_in_current_frame)
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
    add_entry_to_cache({ RunnablePath::Kind::Function, name });
    m_functions.set(name, { name, move(argnames), move(body) });
}

bool Shell::has_function(StringView name)
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
        set_local_variable(arg, adopt_ref(*new AST::StringValue(command.argv[index])), true);
    }

    auto argv = command.argv;
    argv.take_first();
    set_local_variable("ARGV", adopt_ref(*new AST::ListValue(move(argv))), true);

    Core::EventLoop loop;
    setup_signals();

    (void)function.body->run(*this);

    retval = last_return_code.value_or(0);
    return true;
}

String Shell::format(StringView source, ssize_t& cursor) const
{
    Formatter formatter(source, cursor);
    auto result = formatter.format();
    cursor = formatter.cursor();

    return result;
}

Shell::Frame Shell::push_frame(String name)
{
    m_local_frames.append(make<LocalFrame>(name, decltype(LocalFrame::local_variables) {}));
    dbgln_if(SH_DEBUG, "New frame '{}' at {:p}", name, &m_local_frames.last());
    return { m_local_frames, m_local_frames.last() };
}

void Shell::pop_frame()
{
    VERIFY(m_local_frames.size() > 1);
    (void)m_local_frames.take_last();
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
        VERIFY_NOT_REACHED();
    }
    (void)frames.take_last();
}

String Shell::resolve_alias(StringView name) const
{
    return m_aliases.get(name).value_or({});
}

Optional<Shell::RunnablePath> Shell::runnable_path_for(StringView name)
{
    auto parts = name.split_view('/');
    auto path = name.to_string();
    if (parts.size() > 1) {
        auto file = Core::File::open(path.characters(), Core::OpenMode::ReadOnly);
        if (!file.is_error() && !file.value()->is_directory() && access(path.characters(), X_OK) == 0)
            return RunnablePath { RunnablePath::Kind::Executable, name };
    }

    auto* found = binary_search(cached_path.span(), path, nullptr, RunnablePathComparator {});
    if (!found)
        return {};

    return *found;
}

Optional<String> Shell::help_path_for(Vector<RunnablePath> visited, Shell::RunnablePath const& runnable_path)
{
    switch (runnable_path.kind) {
    case RunnablePath::Kind::Executable: {
        LexicalPath lexical_path(runnable_path.path);
        return lexical_path.basename();
    }

    case RunnablePath::Kind::Alias: {
        if (visited.contains_slow(runnable_path))
            return {}; // Break out of an alias loop

        auto resolved = resolve_alias(runnable_path.path);
        auto* runnable = binary_search(cached_path.span(), resolved, nullptr, RunnablePathComparator {});
        if (!runnable)
            return {};

        visited.append(runnable_path);
        return help_path_for(visited, *runnable);
    }

    default:
        return {};
    }
}

int Shell::run_command(StringView cmd, Optional<SourcePosition> source_position_override)
{
    // The default-constructed mode of the shell
    // should not be used for execution!
    VERIFY(!m_default_constructed);

    take_error();

    if (!last_return_code.has_value())
        last_return_code = 0;

    ScopedValueRollback source_position_rollback { m_source_position };
    if (source_position_override.has_value())
        m_source_position = move(source_position_override);

    if (!m_source_position.has_value())
        m_source_position = SourcePosition { .source_file = {}, .literal_source_text = cmd, .position = {} };

    if (cmd.is_empty())
        return 0;

    auto command = Parser(cmd, m_is_interactive).parse();

    if (!command)
        return 0;

    if constexpr (SH_DEBUG) {
        dbgln("Command follows");
        command->dump(0);
    }

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

    (void)command->run(*this);

    if (!has_error(ShellError::None)) {
        possibly_print_error();
        take_error();
        return 1;
    }

    return last_return_code.value_or(0);
}

ErrorOr<RefPtr<Job>> Shell::run_command(const AST::Command& command)
{
    FileDescriptionCollector fds;

    if (options.verbose)
        warnln("+ {}", command);

    // If the command is empty, store the redirections and apply them to all later commands.
    if (command.argv.is_empty() && !command.should_immediately_execute_next) {
        m_global_redirections.extend(command.redirections);
        for (auto& next_in_chain : command.next_chain)
            run_tail(command, next_in_chain, last_return_code.value_or(0));
        return nullptr;
    }

    // Resolve redirections.
    NonnullRefPtrVector<AST::Rewiring> rewirings;
    auto resolve_redirection = [&](auto& redirection) -> ErrorOr<void> {
        auto rewiring = TRY(redirection.apply());

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
            VERIFY(rewiring->other_pipe_end);

            int pipe_fd[2];
            int rc = pipe(pipe_fd);
            if (rc < 0)
                return Error::from_syscall("pipe"sv, rc);
            rewiring->new_fd = pipe_fd[1];
            rewiring->other_pipe_end->new_fd = pipe_fd[0]; // This fd will be added to the collection on one of the next iterations.
            fds.add(pipe_fd[1]);
        } else if (rewiring->fd_action == AST::Rewiring::Close::RefreshOld) {
            VERIFY(rewiring->other_pipe_end);

            int pipe_fd[2];
            int rc = pipe(pipe_fd);
            if (rc < 0)
                return Error::from_syscall("pipe"sv, rc);
            rewiring->old_fd = pipe_fd[1];
            rewiring->other_pipe_end->old_fd = pipe_fd[0]; // This fd will be added to the collection on one of the next iterations.
            fds.add(pipe_fd[1]);
        }
        return {};
    };

    auto apply_rewirings = [&]() -> ErrorOr<void> {
        for (auto& rewiring : rewirings) {

            dbgln_if(SH_DEBUG, "in {}<{}>, dup2({}, {})", command.argv.is_empty() ? "(<Empty>)" : command.argv[0].characters(), getpid(), rewiring.old_fd, rewiring.new_fd);
            int rc = dup2(rewiring.old_fd, rewiring.new_fd);
            if (rc < 0)
                return Error::from_syscall("dup2"sv, rc);
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
        return {};
    };

    TemporaryChange signal_handler_install { m_should_reinstall_signal_handlers, false };

    for (auto& redirection : m_global_redirections)
        TRY(resolve_redirection(redirection));

    for (auto& redirection : command.redirections)
        TRY(resolve_redirection(redirection));

    if (int local_return_code = 0; command.should_wait && run_builtin(command, rewirings, local_return_code)) {
        last_return_code = local_return_code;
        for (auto& next_in_chain : command.next_chain)
            run_tail(command, next_in_chain, *last_return_code);
        return nullptr;
    }

    auto can_be_run_in_current_process = command.should_wait && !command.pipeline && !command.argv.is_empty();
    if (can_be_run_in_current_process && has_function(command.argv.first())) {
        SavedFileDescriptors fds { rewirings };

        for (auto& rewiring : rewirings)
            TRY(Core::System::dup2(rewiring.old_fd, rewiring.new_fd));

        if (int local_return_code = 0; invoke_function(command, local_return_code)) {
            last_return_code = local_return_code;
            for (auto& next_in_chain : command.next_chain)
                run_tail(command, next_in_chain, *last_return_code);
            return nullptr;
        }
    }

    if (command.argv.is_empty()
        && !command.next_chain.is_empty()
        && command.should_immediately_execute_next
        && command.redirections.is_empty()
        && command.next_chain.first().node->should_override_execution_in_current_process()) {

        for (auto& next_in_chain : command.next_chain)
            run_tail(command, next_in_chain, last_return_code.value_or(0));
        return nullptr;
    }

    Vector<char const*> argv;
    Vector<String> copy_argv = command.argv;
    argv.ensure_capacity(command.argv.size() + 1);

    for (auto& arg : copy_argv)
        argv.append(arg.characters());

    argv.append(nullptr);

    auto sync_pipe = TRY(Core::System::pipe2(0));
    auto child = TRY(Core::System::fork());

    if (child == 0) {
        close(sync_pipe[1]);

        m_pid = getpid();
        Core::EventLoop::notify_forked(Core::EventLoop::ForkEvent::Child);
        TemporaryChange signal_handler_install { m_should_reinstall_signal_handlers, true };

        if (auto result = apply_rewirings(); result.is_error()) {
            warnln("Shell: Failed to apply rewirings in {}: {}", copy_argv[0], result.error());
            _exit(126);
        }

        fds.collect();

        u8 c;
        while (read(sync_pipe[0], &c, 1) < 0) {
            if (errno != EINTR) {
                warnln("Shell: Failed to sync in {}: {}", copy_argv[0], Error::from_syscall("read"sv, -errno));
                // There's nothing interesting we can do here.
                break;
            }
        }

        dbgln_if(SH_DEBUG, "Synced up with parent, we're good to exec()");

        close(sync_pipe[0]);

        if (!m_is_subshell && command.should_wait)
            tcsetattr(0, TCSANOW, &default_termios);

        m_is_subshell = true;

        if (command.should_immediately_execute_next) {
            VERIFY(command.argv.is_empty());

            Core::EventLoop mainloop;
            setup_signals();

            for (auto& next_in_chain : command.next_chain)
                run_tail(command, next_in_chain, 0);

            _exit(last_return_code.value_or(0));
        }

        if (int local_return_code = 0; run_builtin(command, {}, local_return_code))
            _exit(local_return_code);

        if (int local_return_code = 0; invoke_function(command, local_return_code))
            _exit(local_return_code);

        // We no longer need the jobs here.
        jobs.clear();

        execute_process(move(argv));
        VERIFY_NOT_REACHED();
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
        auto result = Core::System::setpgid(child, pgid);
        if (result.is_error() && m_is_interactive)
            warnln("Shell: {}", result.error());

        if (!m_is_subshell) {
            // There's no reason to care about the errors here
            // either we're in a tty, we're interactive, and this works
            // or we're not, and it fails - in which case, we don't need
            // stdin/stdout handoff to child processes anyway.
            tcsetpgrp(STDOUT_FILENO, pgid);
            tcsetpgrp(STDIN_FILENO, pgid);
        }
    }

    while (write(sync_pipe[1], "x", 1) < 0) {
        if (errno != EINTR) {
            warnln("Shell: Failed to sync with {}: {}", copy_argv[0], Error::from_syscall("write"sv, -errno));
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

        if (job->is_running_in_background() && job->should_announce_exit())
            warnln("Shell: Job {} ({}) exited\n", job->job_id(), job->cmd());
        else if (job->signaled() && job->should_announce_signal())
            warnln("Shell: Job {} ({}) {}\n", job->job_id(), job->cmd(), strsignal(job->termination_signal()));

        last_return_code = job->exit_code();
        job->disown();

        if (m_editor && job->exit_code() == 0 && is_allowed_to_modify_termios(job->command())) {
            m_editor->refetch_default_termios();
            default_termios = m_editor->default_termios();
            termios = m_editor->termios();
        }

        run_tail(job);
    };

    fds.collect();

    return *job;
}

void Shell::execute_process(Vector<char const*>&& argv)
{
#ifdef __serenity__
    for (auto& promise : m_active_promises) {
        pledge("stdio rpath exec", promise.data.exec_promises.characters());
        for (auto& item : promise.data.unveils)
            unveil(item.path.characters(), item.access.characters());
    }
#endif

    int rc = execvp(argv[0], const_cast<char* const*>(argv.data()));
    if (rc < 0) {
        auto parts = StringView { argv[0] }.split_view('/');
        if (parts.size() == 1) {
            // If this is a path in the current directory and it caused execvp() to fail,
            // simply don't attempt to execute it, see #6774.
            warnln("{}: Command not found.", argv[0]);
            _exit(127);
        }
        int saved_errno = errno;
        struct stat st;
        if (stat(argv[0], &st)) {
            warnln("stat({}): {}", argv[0], strerror(errno));
            // Return code 127 on command not found.
            _exit(127);
        }
        if (!(st.st_mode & S_IXUSR)) {
            warnln("{}: Not executable", argv[0]);
            // Return code 126 when file is not executable.
            _exit(126);
        }
        if (saved_errno == ENOENT) {
            do {
                auto file_result = Core::File::open(argv[0], Core::OpenMode::ReadOnly);
                if (file_result.is_error())
                    break;
                auto& file = file_result.value();
                auto line = file->read_line();
                if (!line.starts_with("#!"))
                    break;
                GenericLexer shebang_lexer { line.substring_view(2) };
                auto shebang = shebang_lexer.consume_until(is_any_of("\n\r")).to_string();
                argv.prepend(shebang.characters());
                int rc = execvp(argv[0], const_cast<char* const*>(argv.data()));
                if (rc < 0) {
                    warnln("{}: Invalid interpreter \"{}\": {}", argv[0], shebang.characters(), strerror(errno));
                    _exit(126);
                }
            } while (false);
            warnln("{}: Command not found.", argv[0]);
        } else {
            if (S_ISDIR(st.st_mode)) {
                warnln("Shell: {}: Is a directory", argv[0]);
                _exit(126);
            }
            warnln("execvp({}): {}", argv[0], strerror(saved_errno));
        }
        _exit(126);
    }
    VERIFY_NOT_REACHED();
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
            (void)next_in_chain.node->run(*this);
            return;
        }
        auto node = next_in_chain.node;
        if (!invoking_command.should_wait)
            node = adopt_ref(static_cast<AST::Node&>(*new AST::Background(next_in_chain.node->position(), move(node))));
        (void)adopt_ref(static_cast<AST::Node&>(*new AST::Execute(next_in_chain.node->position(), move(node))))->run(*this);
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
        deferred_invoke([=, this] {
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
        if constexpr (SH_DEBUG) {
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
                    VERIFY_NOT_REACHED();
                }
            }
        }
        auto job_result = run_command(command);
        if (job_result.is_error()) {
            raise_error(ShellError::LaunchError, String::formatted("{} while running '{}'", job_result.error(), command.argv.first()), command.position);
            break;
        }

        auto job = job_result.release_value();
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

bool Shell::run_file(String const& filename, bool explicitly_invoked)
{
    TemporaryChange script_change { current_script, filename };
    TemporaryChange interactive_change { m_is_interactive, false };
    TemporaryChange<Optional<SourcePosition>> source_change { m_source_position, SourcePosition { .source_file = filename, .literal_source_text = {}, .position = {} } };

    auto file_result = Core::File::open(filename, Core::OpenMode::ReadOnly);
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
    return run_command(data) == 0;
}

bool Shell::is_allowed_to_modify_termios(const AST::Command& command) const
{
    if (command.argv.is_empty())
        return false;

    auto value = lookup_local_variable("PROGRAMS_ALLOWED_TO_MODIFY_DEFAULT_TERMIOS"sv);
    if (!value)
        return false;

    return value->resolve_as_list(*this).contains_slow(command.argv[0]);
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
    TemporaryChange<Job const*> current_job { m_current_job, job.ptr() };

    if (!job)
        return;

    if (job->is_suspended() && !job->shell_did_continue())
        return; // We cannot wait for a suspended job.

    ScopeGuard io_restorer { [&]() {
        if (job->exited() && !job->is_running_in_background()) {
            restore_ios();
        }
    } };

    bool job_exited { false };
    job->on_exit = [&, old_exit = move(job->on_exit)](auto job) {
        if (old_exit)
            old_exit(job);
        job_exited = true;
    };

    if (job->exited())
        return;

    while (!job_exited)
        Core::EventLoop::current().pump();

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

String Shell::escape_token_for_single_quotes(StringView token)
{
    // `foo bar \n '` -> `'foo bar \n '"'"`

    StringBuilder builder;
    builder.append("'");
    auto started_single_quote = true;

    for (auto c : token) {
        switch (c) {
        case '\'':
            builder.append("\"'\"");
            started_single_quote = false;
            continue;
        default:
            builder.append(c);
            if (!started_single_quote) {
                started_single_quote = true;
                builder.append("'");
            }
            break;
        }
    }

    if (started_single_quote)
        builder.append("'");

    return builder.build();
}

String Shell::escape_token_for_double_quotes(StringView token)
{
    // `foo bar \n $x 'blah "hello` -> `"foo bar \\n $x 'blah \"hello"`

    StringBuilder builder;
    builder.append('"');

    for (auto c : token) {
        switch (c) {
        case '\"':
            builder.append("\\\"");
            continue;
        case '\\':
            builder.append("\\\\");
            continue;
        default:
            builder.append(c);
            break;
        }
    }

    builder.append('"');

    return builder.build();
}

Shell::SpecialCharacterEscapeMode Shell::special_character_escape_mode(u32 code_point, EscapeMode mode)
{
    switch (code_point) {
    case '\'':
        if (mode == EscapeMode::DoubleQuotedString)
            return SpecialCharacterEscapeMode::Untouched;
        return SpecialCharacterEscapeMode::Escaped;
    case '"':
    case '$':
    case '\\':
        if (mode == EscapeMode::SingleQuotedString)
            return SpecialCharacterEscapeMode::Untouched;
        return SpecialCharacterEscapeMode::Escaped;
    case '|':
    case '>':
    case '<':
    case '(':
    case ')':
    case '{':
    case '}':
    case '&':
    case ';':
    case '?':
    case '*':
    case ' ':
        if (mode == EscapeMode::SingleQuotedString || mode == EscapeMode::DoubleQuotedString)
            return SpecialCharacterEscapeMode::Untouched;
        return SpecialCharacterEscapeMode::Escaped;
    case '\n':
    case '\t':
    case '\r':
        return SpecialCharacterEscapeMode::QuotedAsEscape;
    default:
        // FIXME: Should instead use unicode's "graphic" property (categories L, M, N, P, S, Zs)
        if (is_ascii(code_point))
            return is_ascii_printable(code_point) ? SpecialCharacterEscapeMode::Untouched : SpecialCharacterEscapeMode::QuotedAsHex;
        return SpecialCharacterEscapeMode::Untouched;
    }
}

static String do_escape(Shell::EscapeMode escape_mode, auto& token)
{
    StringBuilder builder;
    for (auto c : token) {
        static_assert(sizeof(c) == sizeof(u32) || sizeof(c) == sizeof(u8));
        switch (Shell::special_character_escape_mode(c, escape_mode)) {
        case Shell::SpecialCharacterEscapeMode::Untouched:
            if constexpr (sizeof(c) == sizeof(u8))
                builder.append(c);
            else
                builder.append(Utf32View { &c, 1 });
            break;
        case Shell::SpecialCharacterEscapeMode::Escaped:
            if (escape_mode == Shell::EscapeMode::SingleQuotedString)
                builder.append("'");
            builder.append('\\');
            builder.append(c);
            if (escape_mode == Shell::EscapeMode::SingleQuotedString)
                builder.append("'");
            break;
        case Shell::SpecialCharacterEscapeMode::QuotedAsEscape:
            if (escape_mode == Shell::EscapeMode::SingleQuotedString)
                builder.append("'");
            if (escape_mode != Shell::EscapeMode::DoubleQuotedString)
                builder.append("\"");
            switch (c) {
            case '\n':
                builder.append(R"(\n)");
                break;
            case '\t':
                builder.append(R"(\t)");
                break;
            case '\r':
                builder.append(R"(\r)");
                break;
            default:
                VERIFY_NOT_REACHED();
            }
            if (escape_mode != Shell::EscapeMode::DoubleQuotedString)
                builder.append("\"");
            if (escape_mode == Shell::EscapeMode::SingleQuotedString)
                builder.append("'");
            break;
        case Shell::SpecialCharacterEscapeMode::QuotedAsHex:
            if (escape_mode == Shell::EscapeMode::SingleQuotedString)
                builder.append("'");
            if (escape_mode != Shell::EscapeMode::DoubleQuotedString)
                builder.append("\"");

            if (c <= NumericLimits<u8>::max())
                builder.appendff(R"(\x{:0>2x})", static_cast<u8>(c));
            else
                builder.appendff(R"(\u{:0>8x})", static_cast<u32>(c));

            if (escape_mode != Shell::EscapeMode::DoubleQuotedString)
                builder.append("\"");
            if (escape_mode == Shell::EscapeMode::SingleQuotedString)
                builder.append("'");
            break;
        }
    }

    return builder.build();
}

String Shell::escape_token(Utf32View token, EscapeMode escape_mode)
{
    return do_escape(escape_mode, token);
}

String Shell::escape_token(StringView token, EscapeMode escape_mode)
{
    Utf8View view { token };
    if (view.validate())
        return do_escape(escape_mode, view);
    return do_escape(escape_mode, token);
}

String Shell::unescape_token(StringView token)
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

String Shell::find_in_path(StringView program_name)
{
    String path = getenv("PATH");
    if (!path.is_empty()) {
        auto directories = path.split(':');
        for (auto const& directory : directories) {
            Core::DirIterator programs(directory.characters(), Core::DirIterator::SkipDots);
            while (programs.has_next()) {
                auto program = programs.next_path();
                auto program_path = String::formatted("{}/{}", directory, program);
                if (access(program_path.characters(), X_OK) != 0)
                    continue;
                if (program == program_name)
                    return program_path;
            }
        }
    }

    return {};
}

void Shell::cache_path()
{
    if (!m_is_interactive)
        return;

    if (!cached_path.is_empty())
        cached_path.clear_with_capacity();

    // Add shell builtins to the cache.
    for (auto const& builtin_name : builtin_names)
        cached_path.append({ RunnablePath::Kind::Builtin, escape_token(builtin_name) });

    // Add functions to the cache.
    for (auto& function : m_functions) {
        auto name = escape_token(function.key);
        if (cached_path.contains_slow(name))
            continue;
        cached_path.append({ RunnablePath::Kind::Function, name });
    }

    // Add aliases to the cache.
    for (auto const& alias : m_aliases) {
        auto name = escape_token(alias.key);
        if (cached_path.contains_slow(name))
            continue;
        cached_path.append({ RunnablePath::Kind::Alias, name });
    }

    String path = getenv("PATH");
    if (!path.is_empty()) {
        auto directories = path.split(':');
        for (auto const& directory : directories) {
            Core::DirIterator programs(directory.characters(), Core::DirIterator::SkipDots);
            while (programs.has_next()) {
                auto program = programs.next_path();
                auto program_path = String::formatted("{}/{}", directory, program);
                auto escaped_name = escape_token(program);
                if (cached_path.contains_slow(escaped_name))
                    continue;
                if (access(program_path.characters(), X_OK) == 0)
                    cached_path.append({ RunnablePath::Kind::Executable, escaped_name });
            }
        }
    }

    quick_sort(cached_path);
}

void Shell::add_entry_to_cache(RunnablePath const& entry)
{
    size_t index = 0;
    auto match = binary_search(cached_path.span(), entry, &index, RunnablePathComparator {});

    if (match)
        return;

    while (index < cached_path.size() && strcmp(cached_path[index].path.characters(), entry.path.characters()) < 0) {
        index++;
    }
    cached_path.insert(index, entry);
}

void Shell::remove_entry_from_cache(StringView entry)
{
    size_t index { 0 };
    auto match = binary_search(cached_path.span(), entry, &index, RunnablePathComparator {});

    if (match)
        cached_path.remove(index);
}

void Shell::highlight(Line::Editor& editor) const
{
    auto line = editor.line();
    Parser parser(line, m_is_interactive);
    auto ast = parser.parse();
    if (!ast)
        return;
    ast->highlight_in_editor(editor, const_cast<Shell&>(*this));
}

Vector<Line::CompletionSuggestion> Shell::complete()
{
    m_completion_stack_info = {};
    return complete(m_editor->line(m_editor->cursor()));
}

Vector<Line::CompletionSuggestion> Shell::complete(StringView line)
{
    Parser parser(line, m_is_interactive);

    auto ast = parser.parse();

    if (!ast)
        return {};

    return ast->complete_for_editor(*this, line.length());
}

Vector<Line::CompletionSuggestion> Shell::complete_path(StringView base, StringView part, size_t offset, ExecutableOnly executable_only, AST::Node const* command_node, AST::Node const* node, EscapeMode escape_mode)
{
    auto token = offset ? part.substring_view(0, offset) : "";
    String path;

    ssize_t last_slash = token.length() - 1;
    while (last_slash >= 0 && token[last_slash] != '/')
        --last_slash;

    if (command_node) {
        auto program_results = complete_via_program_itself(offset, command_node, node, escape_mode, {});
        if (!program_results.is_error())
            return program_results.release_value();
    }

    StringBuilder path_builder;
    auto init_slash_part = token.substring_view(0, last_slash + 1);
    auto last_slash_part = token.substring_view(last_slash + 1, token.length() - last_slash - 1);

    bool allow_direct_children = true;

    // Depending on the base, we will have to prepend cwd.
    if (base.is_empty()) {
        // '' /foo -> absolute
        // '' foo -> relative
        if (!token.starts_with('/'))
            path_builder.append(cwd);
        path_builder.append('/');
        path_builder.append(init_slash_part);
        if (executable_only == ExecutableOnly::Yes && init_slash_part.is_empty())
            allow_direct_children = false;
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
    auto token_length = escape_token(token, escape_mode).length();
    size_t static_offset = 0;
    auto invariant_offset = token_length;
    if (m_editor)
        m_editor->transform_suggestion_offsets(invariant_offset, static_offset);

    // only suggest dot-files if path starts with a dot
    Core::DirIterator files(path,
        token.starts_with('.') ? Core::DirIterator::SkipParentAndBaseDir : Core::DirIterator::SkipDots);

    Vector<Line::CompletionSuggestion> suggestions;

    while (files.has_next()) {
        auto file = files.next_path();
        if (file.starts_with(token)) {
            struct stat program_status;
            auto file_path = String::formatted("{}/{}", path, file);
            int stat_error = stat(file_path.characters(), &program_status);
            if (!stat_error && (executable_only == ExecutableOnly::No || access(file_path.characters(), X_OK) == 0)) {
                if (S_ISDIR(program_status.st_mode)) {
                    suggestions.append({ escape_token(file, escape_mode), "/" });
                } else {
                    if (!allow_direct_children && !file.contains("/"))
                        continue;
                    suggestions.append({ escape_token(file, escape_mode), " " });
                }
                suggestions.last().input_offset = token_length;
                suggestions.last().invariant_offset = invariant_offset;
                suggestions.last().static_offset = static_offset;
            }
        }
    }

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_program_name(StringView name, size_t offset, EscapeMode escape_mode)
{
    auto match = binary_search(
        cached_path.span(),
        name,
        nullptr,
        [](auto& name, auto& program) {
            return strncmp(
                name.characters_without_null_termination(),
                program.path.characters(),
                name.length());
        });

    if (!match)
        return complete_path("", name, offset, ExecutableOnly::Yes, nullptr, nullptr, escape_mode);

    String completion = match->path;
    auto token_length = escape_token(name, escape_mode).length();
    auto invariant_offset = token_length;
    size_t static_offset = 0;
    if (m_editor)
        m_editor->transform_suggestion_offsets(invariant_offset, static_offset);

    // Now that we have a program name starting with our token, we look at
    // other program names starting with our token and cut off any mismatching
    // characters.

    Vector<Line::CompletionSuggestion> suggestions;

    int index = match - cached_path.data();
    for (int i = index - 1; i >= 0 && cached_path[i].path.starts_with(name); --i)
        suggestions.append({ cached_path[i].path, " " });
    for (size_t i = index + 1; i < cached_path.size() && cached_path[i].path.starts_with(name); ++i)
        suggestions.append({ cached_path[i].path, " " });
    suggestions.append({ cached_path[index].path, " " });

    for (auto& entry : suggestions) {
        entry.input_offset = token_length;
        entry.invariant_offset = invariant_offset;
        entry.static_offset = static_offset;
    }

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_variable(StringView name, size_t offset)
{
    Vector<Line::CompletionSuggestion> suggestions;
    auto pattern = offset ? name.substring_view(0, offset) : "";

    auto invariant_offset = offset;
    size_t static_offset = 0;
    if (m_editor)
        m_editor->transform_suggestion_offsets(invariant_offset, static_offset);

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
        }
    }

    for (auto& entry : suggestions) {
        entry.input_offset = offset;
        entry.invariant_offset = invariant_offset;
        entry.static_offset = static_offset;
    }

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_user(StringView name, size_t offset)
{
    Vector<Line::CompletionSuggestion> suggestions;
    auto pattern = offset ? name.substring_view(0, offset) : "";

    auto invariant_offset = offset;
    size_t static_offset = 0;
    if (m_editor)
        m_editor->transform_suggestion_offsets(invariant_offset, static_offset);

    Core::DirIterator di("/home", Core::DirIterator::SkipParentAndBaseDir);

    if (di.has_error())
        return suggestions;

    while (di.has_next()) {
        String name = di.next_path();
        if (name.starts_with(pattern)) {
            suggestions.append(name);
            auto& suggestion = suggestions.last();
            suggestion.input_offset = offset;
            suggestion.invariant_offset = invariant_offset;
            suggestion.static_offset = static_offset;
        }
    }

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_option(StringView program_name, StringView option, size_t offset, AST::Node const* command_node, AST::Node const* node)
{
    if (command_node) {
        auto program_results = complete_via_program_itself(offset, command_node, node, EscapeMode::Bareword, program_name);
        if (!program_results.is_error())
            return program_results.release_value();
    }

    size_t start = 0;
    while (start < option.length() && option[start] == '-' && start < 2)
        ++start;
    auto option_pattern = offset > start ? option.substring_view(start, offset - start) : "";
    auto invariant_offset = offset;
    size_t static_offset = 0;
    if (m_editor)
        m_editor->transform_suggestion_offsets(invariant_offset, static_offset);

    dbgln("Shell::complete_option({}, {})", program_name, option_pattern);
    return {};
}

ErrorOr<Vector<Line::CompletionSuggestion>> Shell::complete_via_program_itself(size_t, AST::Node const* command_node, AST::Node const* node, EscapeMode, StringView known_program_name)
{
    if (!command_node)
        return Error::from_string_literal("Cannot complete null command");

    if (command_node->would_execute())
        return Error::from_string_literal("Refusing to complete nodes that would execute");

    String program_name_storage;
    if (known_program_name.is_null()) {
        auto node = command_node->leftmost_trivial_literal();
        if (!node)
            return Error::from_string_literal("Cannot complete");

        program_name_storage = node->run(*this)->resolve_as_string(*this);
        known_program_name = program_name_storage;
    }

    auto program_name = known_program_name;

    AST::Command completion_command;
    completion_command.argv.append(program_name);
    completion_command = expand_aliases({ completion_command }).last();

    auto completion_utility_name = String::formatted("_complete_{}", completion_command.argv[0]);
    if (binary_search(cached_path.span(), completion_utility_name, nullptr, RunnablePathComparator {}) != nullptr)
        completion_command.argv[0] = completion_utility_name;
    else if (!options.invoke_program_for_autocomplete)
        return Error::from_string_literal("Refusing to use the program itself as completion source");

    completion_command.argv.extend({ "--complete", "--" });

    struct Visitor : public AST::NodeVisitor {
        Visitor(Shell& shell, AST::Position position)
            : shell(shell)
            , completion_position(position)
        {
            lists.empend();
        }

        Shell& shell;
        AST::Position completion_position;
        Vector<Vector<String>> lists;
        bool fail { false };

        void push_list() { lists.empend(); }
        Vector<String> pop_list() { return lists.take_last(); }
        Vector<String>& list() { return lists.last(); }

        bool should_include(AST::Node const* node) const { return node->position().end_offset <= completion_position.end_offset; }

        virtual void visit(AST::BarewordLiteral const* node) override
        {
            if (should_include(node))
                list().append(node->text());
        }

        virtual void visit(AST::BraceExpansion const* node) override
        {
            if (should_include(node))
                list().extend(static_cast<AST::Node*>(const_cast<AST::BraceExpansion*>(node))->run(shell)->resolve_as_list(shell));
        }

        virtual void visit(AST::CommandLiteral const* node) override
        {
            if (should_include(node))
                list().extend(node->command().argv);
        }

        virtual void visit(AST::DynamicEvaluate const* node) override
        {
            if (should_include(node))
                fail = true;
        }

        virtual void visit(AST::DoubleQuotedString const* node) override
        {
            if (!should_include(node))
                return;

            push_list();
            AST::NodeVisitor::visit(node);
            auto list = pop_list();
            StringBuilder builder;
            builder.join("", list);
            this->list().append(builder.build());
        }

        virtual void visit(AST::Glob const* node) override
        {
            if (should_include(node))
                list().append(node->text());
        }

        virtual void visit(AST::Heredoc const* node) override
        {
            if (!should_include(node))
                return;

            push_list();
            AST::NodeVisitor::visit(node);
            auto list = pop_list();
            StringBuilder builder;
            builder.join("", list);
            this->list().append(builder.build());
        }

        virtual void visit(AST::ImmediateExpression const* node) override
        {
            if (should_include(node))
                fail = true;
        }

        virtual void visit(AST::Range const* node) override
        {
            if (!should_include(node))
                return;

            push_list();
            node->start()->visit(*this);
            list().append(pop_list().first());
        }

        virtual void visit(AST::SimpleVariable const* node) override
        {
            if (should_include(node))
                list().extend(static_cast<AST::Node*>(const_cast<AST::SimpleVariable*>(node))->run(shell)->resolve_as_list(shell));
        }

        virtual void visit(AST::SpecialVariable const* node) override
        {
            if (should_include(node))
                list().extend(static_cast<AST::Node*>(const_cast<AST::SpecialVariable*>(node))->run(shell)->resolve_as_list(shell));
        }

        virtual void visit(AST::Juxtaposition const* node) override
        {
            if (!should_include(node))
                return;

            push_list();
            node->left()->visit(*this);
            auto left = pop_list();

            push_list();
            node->right()->visit(*this);
            auto right = pop_list();

            StringBuilder builder;
            for (auto& left_entry : left) {
                for (auto& right_entry : right) {
                    builder.append(left_entry);
                    builder.append(right_entry);
                    list().append(builder.build());
                    builder.clear();
                }
            }
        }

        virtual void visit(AST::StringLiteral const* node) override
        {
            if (should_include(node))
                list().append(node->text());
        }

        virtual void visit(AST::Tilde const* node) override
        {
            if (should_include(node))
                list().extend(static_cast<AST::Node*>(const_cast<AST::Tilde*>(node))->run(shell)->resolve_as_list(shell));
        }

        virtual void visit(AST::PathRedirectionNode const*) override { }
        virtual void visit(AST::CloseFdRedirection const*) override { }
        virtual void visit(AST::Fd2FdRedirection const*) override { }
        virtual void visit(AST::Execute const*) override { }
        virtual void visit(AST::ReadRedirection const*) override { }
        virtual void visit(AST::ReadWriteRedirection const*) override { }
        virtual void visit(AST::WriteAppendRedirection const*) override { }
        virtual void visit(AST::WriteRedirection const*) override { }
    } visitor { *this, node ? node->position() : AST::Position() };

    command_node->visit(visitor);
    if (visitor.fail)
        return Error::from_string_literal("Cannot complete");

    completion_command.argv.extend(visitor.list());

    completion_command.should_wait = true;
    completion_command.redirections.append(AST::PathRedirection::create("/dev/null", STDERR_FILENO, AST::PathRedirection::Write));
    completion_command.redirections.append(AST::PathRedirection::create("/dev/null", STDIN_FILENO, AST::PathRedirection::Read));

    auto execute_node = make_ref_counted<AST::Execute>(
        AST::Position {},
        make_ref_counted<AST::CommandLiteral>(AST::Position {}, move(completion_command)),
        true);

    Vector<Line::CompletionSuggestion> suggestions;
    auto timer = Core::Timer::create_single_shot(300, [&] {
        Core::EventLoop::current().quit(1);
    });
    timer->start();

    // Restrict the process to effectively readonly access to the FS.
    auto scoped_promise = promise({
        .exec_promises = "stdio rpath prot_exec no_error",
        .unveils = {
            { "/", "rx" },
        },
    });
    {
        TemporaryChange change(m_is_interactive, false);
        execute_node->for_each_entry(*this, [&](NonnullRefPtr<AST::Value> entry) -> IterationDecision {
            auto result = entry->resolve_as_string(*this);
            JsonParser parser(result);
            auto parsed_result = parser.parse();
            if (parsed_result.is_error())
                return IterationDecision::Continue;
            auto parsed = parsed_result.release_value();
            if (parsed.is_object()) {
                auto& object = parsed.as_object();
                auto kind = object.get("kind").as_string_or("plain");
                if (kind == "path") {
                    auto base = object.get("base").as_string_or("");
                    auto part = object.get("part").as_string_or("");
                    auto executable_only = object.get("executable_only").to_bool(false) ? ExecutableOnly::Yes : ExecutableOnly::No;
                    suggestions.extend(complete_path(base, part, part.length(), executable_only, nullptr, nullptr));
                } else if (kind == "program") {
                    auto name = object.get("name").as_string_or("");
                    suggestions.extend(complete_program_name(name, name.length()));
                } else if (kind == "proxy") {
                    if (m_completion_stack_info.size_free() < 4 * KiB) {
                        dbgln("Not enough stack space, recursion?");
                        return IterationDecision::Continue;
                    }
                    auto argv = object.get("argv").as_string_or("");
                    dbgln("Proxy completion for {}", argv);
                    suggestions.extend(complete(argv));
                } else if (kind == "plain") {
                    Line::CompletionSuggestion suggestion {
                        object.get("completion").as_string_or(""),
                        object.get("trailing_trivia").as_string_or(""),
                        object.get("display_trivia").as_string_or(""),
                    };
                    suggestion.static_offset = object.get("static_offset").to_u64(0);
                    suggestion.invariant_offset = object.get("invariant_offset").to_u64(0);
                    suggestion.allow_commit_without_listing = object.get("allow_commit_without_listing").to_bool(true);
                    suggestions.append(move(suggestion));
                } else {
                    dbgln("LibLine: Unhandled completion kind: {}", kind);
                }
            } else {
                suggestions.append(parsed.to_string());
            }

            return IterationDecision::Continue;
        });
    }

    auto pgid = getpgrp();
    tcsetpgrp(STDOUT_FILENO, pgid);
    tcsetpgrp(STDIN_FILENO, pgid);

    if (suggestions.is_empty())
        return Error::from_string_literal("No results");

    return suggestions;
}

Vector<Line::CompletionSuggestion> Shell::complete_immediate_function_name(StringView name, size_t offset)
{
    Vector<Line::CompletionSuggestion> suggestions;

    auto invariant_offset = offset;
    size_t static_offset = 0;
    if (m_editor)
        m_editor->transform_suggestion_offsets(invariant_offset, static_offset);

#define __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(fn_name)               \
    if (auto name_view = #fn_name##sv; name_view.starts_with(name)) \
        suggestions.append({ name_view, " " });

    ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS();

#undef __ENUMERATE_SHELL_IMMEDIATE_FUNCTION

    for (auto& entry : suggestions) {
        entry.input_offset = offset;
        entry.invariant_offset = invariant_offset;
        entry.static_offset = static_offset;
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

    // We write a line's worth of whitespace to the terminal. This way, we ensure that
    // the prompt ends up on a new line even if there is dangling output on the current line.
    size_t fill_count = ws.ws_col - eol_mark_length;
    auto fill_buffer = String::repeated(' ', fill_count);
    fwrite(fill_buffer.characters(), 1, fill_count, stderr);

    putc('\r', stderr);
}

bool Shell::has_history_event(StringView source)
{
    struct : public AST::NodeVisitor {
        virtual void visit(const AST::HistoryEvent* node) override
        {
            has_history_event = true;
            AST::NodeVisitor::visit(node);
        }

        bool has_history_event { false };
    } visitor;

    auto ast = Parser { source, true }.parse();
    if (!ast)
        return false;

    ast->visit(visitor);
    return visitor.has_history_event;
}

bool Shell::read_single_line()
{
    while (true) {
        restore_ios();
        bring_cursor_to_beginning_of_a_line();
        auto line_result = m_editor->get_line(prompt());

        if (line_result.is_error()) {
            auto is_eof = line_result.error() == Line::Editor::Error::Eof;
            auto is_empty = line_result.error() == Line::Editor::Error::Empty;

            if (is_eof || is_empty) {
                // Pretend the user tried to execute builtin_exit()
                auto exit_code = run_command("exit");
                if (exit_code != 0) {
                    // If we didn't end up actually calling exit(), and the command didn't succeed, just pretend it's all okay
                    // unless we can't, then just quit anyway.
                    if (!is_empty)
                        continue;
                }
            }
            Core::EventLoop::current().quit(1);
            return false;
        }

        auto& line = line_result.value();

        if (line.is_empty())
            return true;

        run_command(line);

        if (!has_history_event(line))
            m_editor->add_to_history(line);

        return true;
    }
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

            int wstatus = 0;
            dbgln_if(SH_DEBUG, "waitpid({} = {}) = ...", job.pid(), job.cmd());
            auto child_pid = waitpid(job.pid(), &wstatus, WNOHANG | WUNTRACED);
            dbgln_if(SH_DEBUG, "... = {} - exited: {}, suspended: {}", child_pid, WIFEXITED(wstatus), WIFSTOPPED(wstatus));

            if (child_pid < 0) {
                if (errno == ECHILD) {
                    // The child process went away before we could process its death, just assume it exited all ok.
                    // FIXME: This should never happen, the child should stay around until we do the waitpid above.
                    child_pid = job.pid();
                } else {
                    VERIFY_NOT_REACHED();
                }
            }
            if (child_pid == 0) {
                // If the child existed, but wasn't dead.
                if (job.is_suspended() || job.shell_did_continue()) {
                    // The job was suspended, and someone sent it a SIGCONT.
                    job.set_is_suspended(false);
                    if (job.shell_did_continue())
                        job.set_shell_did_continue(false);
                    found_child = true;
                }
                continue;
            }
            if (child_pid == job.pid()) {
                if (WIFSIGNALED(wstatus) && !WIFSTOPPED(wstatus)) {
                    auto signal = WTERMSIG(wstatus);
                    job.set_signalled(signal);
                    if (signal == SIGINT)
                        raise_error(ShellError::InternalControlFlowInterrupted, "Interrupted"sv, job.command().position);
                    else if (signal == SIGKILL)
                        raise_error(ShellError::InternalControlFlowKilled, "Interrupted"sv, job.command().position);
                } else if (WIFEXITED(wstatus)) {
                    job.set_has_exit(WEXITSTATUS(wstatus));
                } else if (WIFSTOPPED(wstatus)) {
                    job.unblock();
                    job.set_is_suspended(true);
                }
                found_child = true;
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
        path.append("/usr/local/bin:/usr/bin:/bin");
        setenv("PATH", path.to_string().characters(), true);
    }

    cache_path();
}

Shell::Shell(Line::Editor& editor, bool attempt_interactive)
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
    m_is_interactive = attempt_interactive && istty;

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
    if (m_is_interactive) {
        m_editor->load_history(get_history_path());
        cache_path();
    }

    m_editor->register_key_input_callback('\n', [](Line::Editor& editor) {
        auto ast = Parser(editor.line()).parse();
        if (ast && ast->is_syntax_error() && ast->syntax_error_node().is_continuable())
            return true;

        return EDITOR_INTERNAL_FUNCTION(finish)(editor);
    });

    start_timer(3000);
}

Shell::~Shell()
{
    if (m_default_constructed)
        return;

    stop_all_jobs();
    if (!m_is_interactive)
        return;

    m_editor->save_history(get_history_path());
}

void Shell::stop_all_jobs()
{
    if (!jobs.is_empty()) {
        if (m_is_interactive && !m_is_subshell)
            printf("Killing active jobs\n");
        for (auto& entry : jobs) {
            if (entry.value->is_suspended()) {
                dbgln_if(SH_DEBUG, "Job {} is suspended", entry.value->pid());
                kill_job(entry.value, SIGCONT);
            }

            kill_job(entry.value, SIGHUP);
        }

        usleep(10000); // Wait for a bit before killing the job

        for (auto& entry : jobs) {
            dbgln_if(SH_DEBUG, "Actively killing {} ({})", entry.value->pid(), entry.value->cmd());
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

Job const* Shell::find_job(u64 id, bool is_pid)
{
    for (auto& entry : jobs) {
        if (is_pid) {
            if (entry.value->pid() == static_cast<int>(id))
                return entry.value;
        } else {
            if (entry.value->job_id() == id)
                return entry.value;
        }
    }
    return nullptr;
}

void Shell::kill_job(Job const* job, int sig)
{
    if (!job)
        return;

    if (killpg(job->pgid(), sig) < 0) {
        if (kill(job->pid(), sig) < 0) {
            if (errno != ESRCH)
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
    case ShellError::InvalidSliceContentsError:
    case ShellError::InvalidGlobError:
    case ShellError::NonExhaustiveMatchRules:
        warnln("Shell: {}", m_error_description);
        break;
    case ShellError::OpenFailure:
        warnln("Shell: Open failed for {}", m_error_description);
        break;
    case ShellError::OutOfMemory:
        warnln("Shell: Hit an OOM situation");
        break;
    case ShellError::LaunchError:
        warnln("Shell: {}", m_error_description);
        break;
    case ShellError::InternalControlFlowBreak:
    case ShellError::InternalControlFlowContinue:
    case ShellError::InternalControlFlowInterrupted:
    case ShellError::InternalControlFlowKilled:
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
            auto file = Core::File::open(source_position.source_file, Core::OpenMode::ReadOnly);
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

Optional<int> Shell::resolve_job_spec(StringView str)
{
    if (!str.starts_with('%'))
        return {};

    // %number -> job id <number>
    if (auto number = str.substring_view(1).to_uint(); number.has_value())
        return number.value();

    // '%?str' -> iterate jobs and pick one with `str' in its command
    // Note: must be quoted, since '?' will turn it into a glob - pretty ugly...
    GenericLexer lexer { str.substring_view(1) };
    if (!lexer.consume_specific('?'))
        return {};
    auto search_term = lexer.remaining();
    for (auto& it : jobs) {
        if (it.value->cmd().contains(search_term))
            return it.key;
    }

    return {};
}

void Shell::timer_event(Core::TimerEvent& event)
{
    event.accept();

    if (m_is_subshell)
        return;

    StringView option = getenv("HISTORY_AUTOSAVE_TIME_MS");

    auto time = option.to_uint();
    if (!time.has_value() || time.value() == 0) {
        m_history_autosave_time.clear();
        stop_timer();
        start_timer(3000);
        return;
    }

    if (m_history_autosave_time != time) {
        m_history_autosave_time = time.value();
        stop_timer();
        start_timer(m_history_autosave_time.value());
    }

    if (!m_history_autosave_time.has_value())
        return;

    if (m_editor && m_editor->is_history_dirty())
        m_editor->save_history(get_history_path());
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

SavedFileDescriptors::SavedFileDescriptors(NonnullRefPtrVector<AST::Rewiring> const& intended_rewirings)
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

        auto flags = fcntl(new_fd, F_GETFD);
        auto rc = fcntl(new_fd, F_SETFD, flags | FD_CLOEXEC);
        VERIFY(rc == 0);

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
