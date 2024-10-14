/*
 * Copyright (c) 2020-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST.h"
#include "Formatter.h"
#include "PosixParser.h"
#include "Shell.h"
#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/Statistics.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Environment.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <errno.h>
#include <inttypes.h>
#include <limits.h>
#include <signal.h>
#include <sys/wait.h>
#include <unistd.h>

extern char** environ;

namespace Shell {

ErrorOr<int> Shell::builtin_noop(Main::Arguments)
{
    return 0;
}

ErrorOr<int> Shell::builtin_dump(Main::Arguments arguments)
{
    bool posix = false;
    StringView source;

    Core::ArgsParser parser;
    parser.add_positional_argument(source, "Shell code to parse and dump", "source");
    parser.add_option(posix, "Use the POSIX parser", "posix", 'p');

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    TRY((posix ? Posix::Parser { source }.parse() : Parser { source }.parse())->dump(0));
    return 0;
}

enum FollowSymlinks {
    Yes,
    No
};

static Vector<ByteString> find_matching_executables_in_path(StringView filename, FollowSymlinks follow_symlinks = FollowSymlinks::No, Optional<StringView> force_path = {})
{
    // Edge cases in which there are guaranteed no solutions
    if (filename.is_empty() || filename.contains('/'))
        return {};

    char const* path_str = getenv("PATH");
    auto path = force_path.value_or(DEFAULT_PATH_SV);
    if (path_str != nullptr && !force_path.has_value())
        path = { path_str, strlen(path_str) };

    Vector<ByteString> executables;
    auto directories = path.split_view(':');
    for (auto directory : directories) {
        auto file = ByteString::formatted("{}/{}", directory, filename);

        if (follow_symlinks == FollowSymlinks::Yes) {
            auto path_or_error = FileSystem::read_link(file);
            if (!path_or_error.is_error())
                file = path_or_error.release_value();
        }
        if (!Core::System::access(file, X_OK).is_error())
            executables.append(move(file));
    }

    return executables;
}

ErrorOr<int> Shell::builtin_where(Main::Arguments arguments)
{
    Vector<StringView> values_to_look_up;
    bool do_only_path_search { false };
    bool do_follow_symlinks { false };
    bool do_print_only_type { false };

    Core::ArgsParser parser;
    parser.add_positional_argument(values_to_look_up, "List of shell builtins, aliases or executables", "arguments");
    parser.add_option(do_only_path_search, "Search only for executables in the PATH environment variable", "path-only", 'p');
    parser.add_option(do_follow_symlinks, "Follow symlinks and print the symlink free path", "follow-symlink", 's');
    parser.add_option(do_print_only_type, "Print the argument type instead of a human readable description", "type", 'w');

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    auto const look_up_alias = [do_only_path_search, &m_aliases = this->m_aliases](StringView alias) -> Optional<ByteString> {
        if (do_only_path_search)
            return {};
        return m_aliases.get(alias).copy();
    };

    auto const look_up_builtin = [do_only_path_search](StringView builtin) -> Optional<ByteString> {
        if (do_only_path_search)
            return {};
        for (auto const& _builtin : builtin_names) {
            if (_builtin == builtin) {
                return builtin;
            }
        }
        return {};
    };

    bool at_least_one_succeded { false };
    for (auto const& argument : values_to_look_up) {
        auto const alias = look_up_alias(argument);
        if (alias.has_value()) {
            if (do_print_only_type)
                outln("{}: alias", argument);
            else
                outln("{}: aliased to {}", argument, alias.value());
            at_least_one_succeded = true;
        }

        auto const builtin = look_up_builtin(argument);
        if (builtin.has_value()) {
            if (do_print_only_type)
                outln("{}: builtin", builtin.value());
            else
                outln("{}: shell built-in command", builtin.value());
            at_least_one_succeded = true;
        }

        auto const executables = find_matching_executables_in_path(argument, do_follow_symlinks ? FollowSymlinks::Yes : FollowSymlinks::No);
        for (auto const& path : executables) {
            if (do_print_only_type)
                outln("{}: command", argument);
            else
                outln(path);
            at_least_one_succeded = true;
        }
        if (!at_least_one_succeded)
            warnln("{} not found", argument);
    }
    return at_least_one_succeded ? 0 : 1;
}

ErrorOr<int> Shell::builtin_reset(Main::Arguments)
{
    destroy();
    initialize(m_is_interactive);

    // NOTE: As the last step before returning, clear (flush) the shell text entirely.
    fprintf(stderr, "\033[3J\033[H\033[2J");
    fflush(stderr);
    return 0;
}

ErrorOr<int> Shell::builtin_alias(Main::Arguments arguments)
{
    Vector<ByteString> aliases;

    Core::ArgsParser parser;
    parser.add_positional_argument(aliases, "List of name[=values]'s", "name[=value]", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (aliases.is_empty()) {
        for (auto& alias : m_aliases)
            printf("%s=%s\n", escape_token(alias.key).characters(), escape_token(alias.value).characters());
        return 0;
    }

    bool fail = false;
    for (auto& argument : aliases) {
        auto parts = argument.split_limit('=', 2, SplitBehavior::KeepEmpty);
        if (parts.size() == 1) {
            auto alias = m_aliases.get(parts[0]);
            if (alias.has_value()) {
                printf("%s=%s\n", escape_token(parts[0]).characters(), escape_token(alias.value()).characters());
            } else {
                fail = true;
            }
        } else {
            m_aliases.set(parts[0], parts[1]);
            add_entry_to_cache({ RunnablePath::Kind::Alias, parts[0] });
        }
    }

    return fail ? 1 : 0;
}

ErrorOr<int> Shell::builtin_unalias(Main::Arguments arguments)
{
    bool remove_all { false };
    Vector<ByteString> aliases;

    Core::ArgsParser parser;
    parser.set_general_help("Remove alias from the list of aliases");
    parser.add_option(remove_all, "Remove all aliases", nullptr, 'a');
    parser.add_positional_argument(aliases, "List of aliases to remove", "alias", Core::ArgsParser::Required::Yes);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (remove_all) {
        m_aliases.clear();
        cache_path();
        return 0;
    }

    bool failed { false };
    for (auto& argument : aliases) {
        if (!m_aliases.contains(argument)) {
            warnln("unalias: {}: alias not found", argument);
            failed = true;
            continue;
        }
        m_aliases.remove(argument);
        remove_entry_from_cache(argument);
    }

    return failed ? 1 : 0;
}

ErrorOr<int> Shell::builtin_break(Main::Arguments arguments)
{
    unsigned count = 1;

    Core::ArgsParser parser;
    parser.add_positional_argument(count, "Number of loops to 'break' out of", "count", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (count != 1) {
        raise_error(ShellError::EvaluatedSyntaxError, "break: count must be equal to 1 (NYI)");
        return 1;
    }

    raise_error(ShellError::InternalControlFlowBreak, "POSIX break");

    return 0;
}

ErrorOr<int> Shell::builtin_continue(Main::Arguments arguments)
{
    unsigned count = 1;

    Core::ArgsParser parser;
    parser.add_positional_argument(count, "Number of loops to 'continue' out of", "count", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (count != 1) {
        raise_error(ShellError::EvaluatedSyntaxError, "continue: count must be equal to 1 (NYI)");
        return 1;
    }

    raise_error(ShellError::InternalControlFlowContinue, "POSIX continue");

    return 0;
}

ErrorOr<int> Shell::builtin_return(Main::Arguments arguments)
{
    int return_code = last_return_code.value_or(0);

    Core::ArgsParser parser;
    parser.add_positional_argument(return_code, "Return code to return to the parent shell", "return-code", Core::ArgsParser::Required::No);
    parser.set_general_help("Return from a function or source file");

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    last_return_code = return_code & 0xff;
    raise_error(ShellError::InternalControlFlowReturn, "POSIX return");

    return 0;
}

ErrorOr<int> Shell::builtin_bg(Main::Arguments arguments)
{
    int job_id = -1;
    bool is_pid = false;

    Core::ArgsParser parser;
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Job ID or Jobspec to run in background",
        .name = "job-id",
        .min_values = 0,
        .max_values = 1,
        .accept_value = [&](StringView value) -> bool {
            // Check if it's a pid (i.e. literal integer)
            if (auto number = value.to_number<unsigned>(); number.has_value()) {
                job_id = number.value();
                is_pid = true;
                return true;
            }

            // Check if it's a jobspec
            if (auto id = resolve_job_spec(value); id.has_value()) {
                job_id = id.value();
                is_pid = false;
                return true;
            }

            return false;
        } });

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (job_id == -1 && !jobs.is_empty())
        job_id = find_last_job_id();

    auto* job = const_cast<Job*>(find_job(job_id, is_pid));

    if (!job) {
        if (job_id == -1) {
            warnln("bg: No current job");
        } else {
            warnln("bg: Job with id/pid {} not found", job_id);
        }
        return 1;
    }

    job->set_running_in_background(true);
    job->set_should_announce_exit(true);
    job->set_shell_did_continue(true);

    dbgln("Resuming {} ({})", job->pid(), job->cmd());
    warnln("Resuming job {} - {}", job->job_id(), job->cmd());

    // Try using the PGID, but if that fails, just use the PID.
    if (killpg(job->pgid(), SIGCONT) < 0) {
        if (kill(job->pid(), SIGCONT) < 0) {
            perror("kill");
            return 1;
        }
    }

    return 0;
}

ErrorOr<String> Shell::serialize_function_definition(ShellFunction const& fn) const
{
    StringBuilder builder;
    builder.append(fn.name);
    builder.append('(');
    for (size_t i = 0; i < fn.arguments.size(); i++) {
        builder.append(fn.arguments[i]);
        if (i != fn.arguments.size() - 1)
            builder.append(' ');
    }
    builder.append(") {\n"sv);
    if (fn.body) {
        auto formatter = Formatter(*fn.body);
        builder.append(formatter.format());
    }
    builder.append("\n}"sv);

    return builder.to_string();
}

ErrorOr<int> Shell::builtin_type(Main::Arguments arguments)
{
    Vector<ByteString> commands;
    bool dont_show_function_source = false;

    Core::ArgsParser parser;
    parser.set_general_help("Display information about commands.");
    parser.add_positional_argument(commands, "Command(s) to list info about", "command");
    parser.add_option(dont_show_function_source, "Do not show functions source.", "no-fn-source", 'f');

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    bool something_not_found = false;

    for (auto& command : commands) {
        // check if it is an alias
        if (auto alias = m_aliases.get(command); alias.has_value()) {
            printf("%s is aliased to `%s`\n", escape_token(command).characters(), escape_token(alias.value()).characters());
            continue;
        }

        // check if it is a function
        if (auto function = m_functions.get(command); function.has_value()) {
            auto fn = function.value();
            printf("%s is a function\n", command.characters());
            if (!dont_show_function_source) {
                auto source = TRY(serialize_function_definition(fn));
                outln("{}", source);
            }
            continue;
        }

        // check if its a builtin
        if (has_builtin(command)) {
            printf("%s is a shell builtin\n", command.characters());
            continue;
        }

        // check if its an executable in PATH
        auto fullpath = Core::System::resolve_executable_from_environment(command);
        if (!fullpath.is_error()) {
            printf("%s is %s\n", command.characters(), escape_token(fullpath.release_value()).characters());
            continue;
        }
        something_not_found = true;
        printf("type: %s not found\n", command.characters());
    }

    if (something_not_found)
        return 1;
    else
        return 0;
}

ErrorOr<int> Shell::builtin_cd(Main::Arguments arguments)
{
    StringView arg_path;

    Core::ArgsParser parser;
    parser.add_positional_argument(arg_path, "Path to change to", "path", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    ByteString new_path;

    if (arg_path.is_empty()) {
        new_path = home;
    } else {
        if (arg_path == "-"sv) {
            char* oldpwd = getenv("OLDPWD");
            if (oldpwd == nullptr)
                return 1;
            new_path = oldpwd;
        } else {
            new_path = arg_path;
        }
    }

    auto real_path_or_error = FileSystem::real_path(new_path);
    if (real_path_or_error.is_error()) {
        warnln("Invalid path '{}'", new_path);
        return 1;
    }
    auto real_path = real_path_or_error.release_value();

    if (cd_history.is_empty() || cd_history.last() != real_path)
        cd_history.enqueue(real_path);

    auto path_relative_to_current_directory = LexicalPath::relative_path(real_path, cwd);
    if (path_relative_to_current_directory.is_empty())
        path_relative_to_current_directory = real_path;
    char const* path = path_relative_to_current_directory.characters();

    int rc = chdir(path);
    if (rc < 0) {
        if (errno == ENOTDIR) {
            warnln("Not a directory: {}", path);
        } else {
            warnln("chdir({}) failed: {}", path, strerror(errno));
        }
        return 1;
    }
    setenv("OLDPWD", cwd.characters(), 1);
    cwd = move(real_path);
    setenv("PWD", cwd.characters(), 1);
    return 0;
}

ErrorOr<int> Shell::builtin_cdh(Main::Arguments arguments)
{
    int index = -1;

    Core::ArgsParser parser;
    parser.add_positional_argument(index, "Index of the cd history entry (leave out for a list)", "index", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (index == -1) {
        if (cd_history.is_empty()) {
            warnln("cdh: no history available");
            return 0;
        }

        for (ssize_t i = cd_history.size() - 1; i >= 0; --i)
            printf("%zu: %s\n", cd_history.size() - i, cd_history.at(i).characters());
        return 0;
    }

    if (index < 1 || (size_t)index > cd_history.size()) {
        warnln("cdh: history index out of bounds: {} not in (0, {})", index, cd_history.size());
        return 1;
    }

    StringView path = cd_history.at(cd_history.size() - index);
    StringView cd_args[] = { "cd"sv, path };
    return Shell::builtin_cd({ .argc = 0, .argv = 0, .strings = cd_args });
}

ErrorOr<int> Shell::builtin_command(Main::Arguments arguments)
{
    bool describe = false;
    bool describe_verbosely = false;
    bool search_in_default_path = false;
    Vector<StringView> commands_or_args;

    Core::ArgsParser parser;
    parser.add_option(search_in_default_path, "default-path", "Use a default value for PATH", 'p');
    parser.add_option(describe, "describe", "Describe the file that would be executed", 'v');
    parser.add_option(describe_verbosely, "describe-verbosely", "Describe the file that would be executed more verbosely", 'V');
    parser.add_positional_argument(commands_or_args, "Arguments or command names to search for", "arg");
    parser.set_stop_on_first_non_option(true);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    auto const look_up_builtin = [](StringView builtin) -> Optional<ByteString> {
        for (auto const& _builtin : builtin_names) {
            if (_builtin == builtin) {
                return builtin;
            }
        }
        return {};
    };

    describe |= describe_verbosely;
    if (!describe) {
        AST::Command command;
        TRY(command.argv.try_ensure_capacity(commands_or_args.size()));
        for (auto& arg : commands_or_args)
            command.argv.append(TRY(String::from_utf8(arg)));

        auto commands = TRY(expand_aliases({ move(command) }));

        int exit_code = 1;
        for (auto& job : run_commands(commands)) {
            block_on_job(job);
            exit_code = job->exit_code();
        }

        return exit_code;
    }

    bool any_failed = false;
    for (auto const& argument : commands_or_args) {
        auto alias = m_aliases.get(argument);
        if (alias.has_value()) {
            if (describe_verbosely)
                outln("{}: aliased to {}", argument, alias.value());
            else
                outln("{}", alias.value());
            continue;
        }

        auto const builtin = look_up_builtin(argument);
        if (builtin.has_value()) {
            if (describe_verbosely)
                outln("{}: shell built-in command", builtin.value());
            else
                outln("{}", builtin.value());
            continue;
        }

        auto const executables = find_matching_executables_in_path(argument, FollowSymlinks::No, search_in_default_path ? DEFAULT_PATH_SV : Optional<StringView>());
        if (!executables.is_empty()) {
            outln("{}", executables.first());
            continue;
        }

        if (describe_verbosely)
            warnln("{} not found", argument);
        any_failed = true;
    }

    return any_failed ? 1 : 0;
}

ErrorOr<int> Shell::builtin_dirs(Main::Arguments arguments)
{
    // The first directory in the stack is ALWAYS the current directory
    directory_stack.at(0) = cwd.characters();

    bool clear = false;
    bool print = false;
    bool number_when_printing = false;
    char separator = ' ';

    Vector<ByteString> paths;

    Core::ArgsParser parser;
    parser.add_option(clear, "Clear the directory stack", "clear", 'c');
    parser.add_option(print, "Print directory entries one per line", "print", 'p');
    parser.add_option(number_when_printing, "Number the directories in the stack when printing", "number", 'v');
    parser.add_positional_argument(paths, "Extra paths to put on the stack", "path", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    // -v implies -p
    print = print || number_when_printing;

    if (print) {
        if (!paths.is_empty()) {
            warnln("dirs: 'print' and 'number' are not allowed when any path is specified");
            return 1;
        }
        separator = '\n';
    }

    if (clear) {
        for (size_t i = 1; i < directory_stack.size(); i++)
            directory_stack.remove(i);
    }

    for (auto& path : paths)
        directory_stack.append(path);

    if (print || (!clear && paths.is_empty())) {
        int index = 0;
        for (auto& directory : directory_stack) {
            if (number_when_printing)
                printf("%d ", index++);
            print_path(directory);
            fputc(separator, stdout);
        }
    }

    return 0;
}

ErrorOr<int> Shell::builtin_eval(Main::Arguments arguments)
{
    if (!m_in_posix_mode) {
        warnln("eval: This shell is not in POSIX mode");
        return 1;
    }

    StringBuilder joined_arguments;
    for (size_t i = 1; i < arguments.strings.size(); ++i) {
        if (i != 1)
            joined_arguments.append(' ');
        joined_arguments.append(arguments.strings[i]);
    }

    auto result = Posix::Parser { TRY(joined_arguments.to_string()) }.parse();
    if (!result)
        return 1;

    auto value = TRY(result->run(*this));
    if (value && value->is_job())
        block_on_job(static_cast<AST::JobValue*>(value.ptr())->job());

    return last_return_code.value_or(0);
}

ErrorOr<int> Shell::builtin_exec(Main::Arguments arguments)
{
    if (arguments.strings.size() < 2) {
        warnln("Shell: No command given to exec");
        return 1;
    }

    TRY(execute_process(arguments.strings.slice(1)));
    // NOTE: Won't get here.
    return 0;
}

ErrorOr<int> Shell::builtin_exit(Main::Arguments arguments)
{
    int exit_code = 0;
    Core::ArgsParser parser;
    parser.add_positional_argument(exit_code, "Exit code", "code", Core::ArgsParser::Required::No);
    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (m_is_interactive) {
        if (!jobs.is_empty()) {
            if (!m_should_ignore_jobs_on_next_exit) {
                warnln("Shell: You have {} active job{}, run 'exit' again to really exit.", jobs.size(), jobs.size() > 1 ? "s" : "");
                m_should_ignore_jobs_on_next_exit = true;
                return 1;
            }
        }
    }
    stop_all_jobs();
    if (m_is_interactive) {
        m_editor->save_history(get_history_path());
        printf("Good-bye!\n");
    }
    exit(exit_code);
}

ErrorOr<int> Shell::builtin_export(Main::Arguments arguments)
{
    Vector<ByteString> vars;

    Core::ArgsParser parser;
    parser.add_positional_argument(vars, "List of variable[=value]'s", "values", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (vars.is_empty()) {
        for (auto entry : Core::Environment::entries())
            outln("{}", entry.full_entry);
        return 0;
    }

    for (auto& value : vars) {
        auto parts = value.split_limit('=', 2);
        if (parts.is_empty()) {
            warnln("Shell: Invalid export spec '{}', expected `variable=value' or `variable'", value);
            return 1;
        }

        if (parts.size() == 1) {
            auto value = TRY(look_up_local_variable(parts[0]));
            if (value) {
                auto values = TRY(const_cast<AST::Value&>(*value).resolve_as_list(*this));
                StringBuilder builder;
                builder.join(' ', values);
                parts.append(builder.to_byte_string());
            } else {
                // Ignore the export.
                continue;
            }
        }

        int setenv_return = setenv(parts[0].characters(), parts[1].characters(), 1);

        if (setenv_return != 0) {
            perror("setenv");
            return 1;
        }

        if (parts[0] == "PATH")
            cache_path();
    }

    return 0;
}

ErrorOr<int> Shell::builtin_glob(Main::Arguments arguments)
{
    Vector<ByteString> globs;
    Core::ArgsParser parser;
    parser.add_positional_argument(globs, "Globs to resolve", "glob");

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    for (auto& glob : globs) {
        for (auto& expanded : TRY(expand_globs(glob, cwd)))
            outln("{}", expanded);
    }

    return 0;
}

ErrorOr<int> Shell::builtin_fg(Main::Arguments arguments)
{
    int job_id = -1;
    bool is_pid = false;

    Core::ArgsParser parser;
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Job ID or Jobspec to bring to foreground",
        .name = "job-id",
        .min_values = 0,
        .max_values = 1,
        .accept_value = [&](StringView value) -> bool {
            // Check if it's a pid (i.e. literal integer)
            if (auto number = value.to_number<unsigned>(); number.has_value()) {
                job_id = number.value();
                is_pid = true;
                return true;
            }

            // Check if it's a jobspec
            if (auto id = resolve_job_spec(value); id.has_value()) {
                job_id = id.value();
                is_pid = false;
                return true;
            }

            return false;
        } });

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (job_id == -1 && !jobs.is_empty())
        job_id = find_last_job_id();

    RefPtr<Job> job = find_job(job_id, is_pid);

    if (!job) {
        if (job_id == -1) {
            warnln("fg: No current job");
        } else {
            warnln("fg: Job with id/pid {} not found", job_id);
        }
        return 1;
    }

    job->set_running_in_background(false);
    job->set_shell_did_continue(true);

    dbgln("Resuming {} ({})", job->pid(), job->cmd());
    warnln("Resuming job {} - {}", job->job_id(), job->cmd());

    tcsetpgrp(STDOUT_FILENO, job->pgid());
    tcsetpgrp(STDIN_FILENO, job->pgid());

    // Try using the PGID, but if that fails, just use the PID.
    if (killpg(job->pgid(), SIGCONT) < 0) {
        if (kill(job->pid(), SIGCONT) < 0) {
            perror("kill");
            return 1;
        }
    }

    block_on_job(job);

    if (job->exited())
        return job->exit_code();
    else
        return 0;
}

ErrorOr<int> Shell::builtin_disown(Main::Arguments arguments)
{
    Vector<int> job_ids;
    Vector<bool> id_is_pid;

    Core::ArgsParser parser;
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Job IDs or Jobspecs to disown",
        .name = "job-id",
        .min_values = 0,
        .max_values = INT_MAX,
        .accept_value = [&](StringView value) -> bool {
            // Check if it's a pid (i.e. literal integer)
            if (auto number = value.to_number<unsigned>(); number.has_value()) {
                job_ids.append(number.value());
                id_is_pid.append(true);
                return true;
            }

            // Check if it's a jobspec
            if (auto id = resolve_job_spec(value); id.has_value()) {
                job_ids.append(id.value());
                id_is_pid.append(false);
                return true;
            }

            return false;
        } });

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (job_ids.is_empty()) {
        job_ids.append(find_last_job_id());
        id_is_pid.append(false);
    }

    Vector<Job const*> jobs_to_disown;

    for (size_t i = 0; i < job_ids.size(); ++i) {
        auto id = job_ids[i];
        auto is_pid = id_is_pid[i];
        auto job = find_job(id, is_pid);
        if (!job)
            warnln("disown: Job with id/pid {} not found", id);
        else
            jobs_to_disown.append(job);
    }

    if (jobs_to_disown.is_empty()) {
        if (job_ids.is_empty())
            warnln("disown: No current job");
        // An error message has already been printed about the nonexistence of each listed job.
        return 1;
    }

    for (auto job : jobs_to_disown) {
        job->deactivate();

        if (!job->is_running_in_background())
            warnln("disown warning: Job {} is currently not running, 'kill -{} {}' to make it continue", job->job_id(), SIGCONT, job->pid());

        jobs.remove(job->pid());
    }

    return 0;
}

ErrorOr<int> Shell::builtin_history(Main::Arguments)
{
    for (size_t i = 0; i < m_editor->history().size(); ++i) {
        printf("%6zu  %s\n", i + 1, m_editor->history()[i].entry.characters());
    }
    return 0;
}

ErrorOr<int> Shell::builtin_jobs(Main::Arguments arguments)
{
    bool list = false, show_pid = false;

    Core::ArgsParser parser;
    parser.add_option(list, "List all information about jobs", "list", 'l');
    parser.add_option(show_pid, "Display the PID of the jobs", "pid", 'p');

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    Job::PrintStatusMode mode = Job::PrintStatusMode::Basic;

    if (show_pid)
        mode = Job::PrintStatusMode::OnlyPID;

    if (list)
        mode = Job::PrintStatusMode::ListAll;

    for (auto& it : jobs) {
        if (!it.value->print_status(mode))
            return 1;
    }

    return 0;
}

ErrorOr<int> Shell::builtin_popd(Main::Arguments arguments)
{
    if (directory_stack.size() <= 1) {
        warnln("Shell: popd: directory stack empty");
        return 1;
    }

    bool should_not_switch = false;
    Core::ArgsParser parser;
    parser.add_option(should_not_switch, "Do not switch dirs", "no-switch", 'n');

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    auto popped_path = directory_stack.take_last();

    if (should_not_switch)
        return 0;

    auto new_path = LexicalPath::canonicalized_path(popped_path);
    if (chdir(new_path.characters()) < 0) {
        warnln("chdir({}) failed: {}", new_path, strerror(errno));
        return 1;
    }
    cwd = new_path;
    return 0;
}

ErrorOr<int> Shell::builtin_pushd(Main::Arguments arguments)
{
    StringBuilder path_builder;
    bool should_switch = true;

    // From the BASH reference manual: https://www.gnu.org/software/bash/manual/html_node/Directory-Stack-Builtins.html
    // With no arguments, pushd exchanges the top two directories and makes the new top the current directory.
    if (arguments.strings.size() == 1) {
        if (directory_stack.size() < 2) {
            warnln("pushd: no other directory");
            return 1;
        }

        ByteString dir1 = directory_stack.take_first();
        ByteString dir2 = directory_stack.take_first();
        directory_stack.insert(0, dir2);
        directory_stack.insert(1, dir1);

        int rc = chdir(dir2.characters());
        if (rc < 0) {
            warnln("chdir({}) failed: {}", dir2, strerror(errno));
            return 1;
        }

        cwd = dir2;

        return 0;
    }

    // Let's assume the user's typed in 'pushd <dir>'
    if (arguments.strings.size() == 2) {
        directory_stack.append(cwd.characters());
        if (arguments.strings[1].starts_with('/')) {
            path_builder.append(arguments.strings[1]);
        } else {
            path_builder.appendff("{}/{}", cwd, arguments.strings[1]);
        }
    } else if (arguments.strings.size() == 3) {
        directory_stack.append(cwd.characters());
        for (size_t i = 1; i < arguments.strings.size(); i++) {
            auto arg = arguments.strings[i];

            if (arg.starts_with('-')) {
                if (arg.starts_with('/')) {
                    path_builder.append(arg);
                } else {
                    path_builder.appendff("{}/{}", cwd, arg);
                }
            }

            if (arg == "-n"sv)
                should_switch = false;
        }
    }

    auto real_path = LexicalPath::canonicalized_path(path_builder.to_byte_string());

    struct stat st;
    int rc = stat(real_path.characters(), &st);
    if (rc < 0) {
        warnln("stat({}) failed: {}", real_path, strerror(errno));
        return 1;
    }

    if (!S_ISDIR(st.st_mode)) {
        warnln("Not a directory: {}", real_path);
        return 1;
    }

    if (should_switch) {
        int rc = chdir(real_path.characters());
        if (rc < 0) {
            warnln("chdir({}) failed: {}", real_path, strerror(errno));
            return 1;
        }

        cwd = real_path;
    }

    return 0;
}

ErrorOr<int> Shell::builtin_pwd(Main::Arguments)
{
    print_path(cwd);
    fputc('\n', stdout);
    return 0;
}

ErrorOr<int> Shell::builtin_setopt(Main::Arguments arguments)
{
    if (arguments.strings.size() == 1) {
#define __ENUMERATE_SHELL_OPTION(name, default_, description) \
    if (options.name)                                         \
        warnln("{}", #name);

        ENUMERATE_SHELL_OPTIONS();

#undef __ENUMERATE_SHELL_OPTION
    }

    Core::ArgsParser parser;
#define __ENUMERATE_SHELL_OPTION(name, default_, description)     \
    bool name = false;                                            \
    bool not_##name = false;                                      \
    parser.add_option(name, "Enable: " description, #name, '\0'); \
    parser.add_option(not_##name, "Disable: " description, "no_" #name, '\0');

    ENUMERATE_SHELL_OPTIONS();

#undef __ENUMERATE_SHELL_OPTION

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

#define __ENUMERATE_SHELL_OPTION(name, default_, description) \
    if (name)                                                 \
        options.name = true;                                  \
    if (not_##name)                                           \
        options.name = false;

    ENUMERATE_SHELL_OPTIONS();

#undef __ENUMERATE_SHELL_OPTION

    return 0;
}

ErrorOr<int> Shell::builtin_shift(Main::Arguments arguments)
{
    int count = 1;

    Core::ArgsParser parser;
    parser.add_positional_argument(count, "Shift count", "count", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (count < 1)
        return 0;

    auto argv_ = TRY(look_up_local_variable("ARGV"sv));
    if (!argv_) {
        warnln("shift: ARGV is unset");
        return 1;
    }

    if (!argv_->is_list())
        argv_ = adopt_ref(*new AST::ListValue({ const_cast<AST::Value&>(*argv_) }));

    auto& values = const_cast<AST::ListValue*>(static_cast<AST::ListValue const*>(argv_.ptr()))->values();
    if ((size_t)count > values.size()) {
        warnln("shift: shift count must not be greater than {}", values.size());
        return 1;
    }

    for (auto i = 0; i < count; ++i)
        (void)values.take_first();

    return 0;
}

ErrorOr<int> Shell::builtin_source(Main::Arguments arguments)
{
    StringView file_to_source;
    Vector<StringView> args;

    Core::ArgsParser parser;
    parser.add_positional_argument(file_to_source, "File to read commands from", "path");
    parser.add_positional_argument(args, "ARGV for the sourced file", "args", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments))
        return 1;

    auto previous_argv = TRY(look_up_local_variable("ARGV"sv));
    ScopeGuard guard { [&] {
        if (!args.is_empty())
            set_local_variable("ARGV", const_cast<AST::Value&>(*previous_argv));
    } };

    if (!args.is_empty()) {
        Vector<String> arguments;
        arguments.ensure_capacity(args.size());
        for (auto& arg : args)
            arguments.append(TRY(String::from_utf8(arg)));

        set_local_variable("ARGV", AST::make_ref_counted<AST::ListValue>(move(arguments)));
    }

    if (!run_file(file_to_source, true))
        return 126;

    return 0;
}

ErrorOr<int> Shell::builtin_time(Main::Arguments arguments)
{
    Vector<StringView> args;

    int number_of_iterations = 1;

    Core::ArgsParser parser;
    parser.add_option(number_of_iterations, "Number of iterations", "iterations", 'n', "iterations");
    parser.set_stop_on_first_non_option(true);
    parser.add_positional_argument(args, "Command to execute with arguments", "command", Core::ArgsParser::Required::Yes);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (number_of_iterations < 1)
        return 1;

    AST::Command command;
    TRY(command.argv.try_ensure_capacity(args.size()));
    for (auto& arg : args)
        command.argv.append(TRY(String::from_utf8(arg)));

    auto commands = TRY(expand_aliases({ move(command) }));

    AK::Statistics iteration_times;

    int exit_code = 1;
    for (int i = 0; i < number_of_iterations; ++i) {
        auto timer = Core::ElapsedTimer::start_new();
        for (auto& job : run_commands(commands)) {
            block_on_job(job);
            exit_code = job->exit_code();
        }
        iteration_times.add(static_cast<float>(timer.elapsed()));
    }

    warnln();

    if (number_of_iterations == 1) {
        warnln("Time: {} ms", iteration_times.values().first());
    } else {
        AK::Statistics iteration_times_excluding_first;
        for (size_t i = 1; i < iteration_times.size(); i++)
            iteration_times_excluding_first.add(iteration_times.values()[i]);

        warnln("Timing report: {} ms", iteration_times.sum());
        warnln("==============");
        warnln("Command:         {}", ByteString::join(' ', arguments.strings));
        warnln("Average time:    {:.2} ms (median: {}, stddev: {:.2}, min: {}, max: {})",
            iteration_times.average(), iteration_times.median(),
            iteration_times.standard_deviation(),
            iteration_times.min(), iteration_times.max());
        warnln("Excluding first: {:.2} ms (median: {}, stddev: {:.2}, min: {}, max: {})",
            iteration_times_excluding_first.average(), iteration_times_excluding_first.median(),
            iteration_times_excluding_first.standard_deviation(),
            iteration_times_excluding_first.min(), iteration_times_excluding_first.max());
    }

    return exit_code;
}

ErrorOr<int> Shell::builtin_umask(Main::Arguments arguments)
{
    StringView mask_text;
    bool symbolic_output = false;

    Core::ArgsParser parser;

    parser.add_option(symbolic_output, "Produce symbolic output", "symbolic", 'S');
    parser.add_positional_argument(mask_text, "New mask (omit to get current mask)", "octal-mask", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    auto parse_symbolic_digit = [](int digit) -> ErrorOr<String> {
        StringBuilder builder;

        if ((digit & 4) == 0)
            TRY(builder.try_append('r'));
        if ((digit & 2) == 0)
            TRY(builder.try_append('w'));
        if ((digit & 1) == 0)
            TRY(builder.try_append('x'));
        if (builder.is_empty())
            TRY(builder.try_append('-'));

        return builder.to_string();
    };

    if (mask_text.is_empty()) {
        mode_t old_mask = umask(0);

        if (symbolic_output) {
            StringBuilder builder;

            TRY(builder.try_append("u="sv));
            TRY(builder.try_append(TRY(parse_symbolic_digit(old_mask >> 6 & 7)).bytes()));

            TRY(builder.try_append(",g="sv));
            TRY(builder.try_append(TRY(parse_symbolic_digit(old_mask >> 3 & 7)).bytes()));

            TRY(builder.try_append(",o="sv));
            TRY(builder.try_append(TRY(parse_symbolic_digit(old_mask >> 0 & 7)).bytes()));

            outln("{}", builder.string_view());
        } else {
            outln("{:#o}", old_mask);
        }

        umask(old_mask);
        return 0;
    }

    unsigned mask = 0;
    auto matches = true;

    // FIXME: There's currently no way to parse an StringView as an octal integer,
    //        when that becomes available, replace this code.
    for (auto byte : mask_text.bytes()) {
        if (isspace(byte))
            continue;
        if (!is_ascii_octal_digit(byte)) {
            matches = false;
            break;
        }

        mask = (mask << 3) + (byte - '0');
    }
    if (matches) {
        umask(mask);
        return 0;
    }

    warnln("umask: Invalid mask '{}'", mask_text);
    return 1;
}

ErrorOr<int> Shell::builtin_wait(Main::Arguments arguments)
{
    Vector<int> job_ids;
    Vector<bool> id_is_pid;

    Core::ArgsParser parser;
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Job IDs or Jobspecs to wait for",
        .name = "job-id",
        .min_values = 0,
        .max_values = INT_MAX,
        .accept_value = [&](StringView value) -> bool {
            // Check if it's a pid (i.e. literal integer)
            if (auto number = value.to_number<unsigned>(); number.has_value()) {
                job_ids.append(number.value());
                id_is_pid.append(true);
                return true;
            }

            // Check if it's a jobspec
            if (auto id = resolve_job_spec(value); id.has_value()) {
                job_ids.append(id.value());
                id_is_pid.append(false);
                return true;
            }

            return false;
        } });

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    Vector<NonnullRefPtr<Job>> jobs_to_wait_for;

    for (size_t i = 0; i < job_ids.size(); ++i) {
        auto id = job_ids[i];
        auto is_pid = id_is_pid[i];
        auto job = find_job(id, is_pid);
        if (!job)
            warnln("wait: Job with id/pid {} not found", id);
        else
            jobs_to_wait_for.append(*job);
    }

    if (job_ids.is_empty()) {
        for (auto const& it : jobs)
            jobs_to_wait_for.append(it.value);
    }

    for (auto& job : jobs_to_wait_for) {
        job->set_running_in_background(false);
        block_on_job(job);
    }

    return 0;
}

ErrorOr<int> Shell::builtin_unset(Main::Arguments arguments)
{
    Vector<ByteString> vars;
    bool unset_only_variables = false; // POSIX only.

    Core::ArgsParser parser;
    parser.add_positional_argument(vars, "List of variables", "variables", Core::ArgsParser::Required::Yes);
    if (m_in_posix_mode)
        parser.add_option(unset_only_variables, "Unset only variables", "variables", 'v');

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    bool did_touch_path = false;
    for (auto& value : vars) {
        if (!did_touch_path && value == "PATH"sv)
            did_touch_path = true;

        if (TRY(look_up_local_variable(value)) != nullptr) {
            unset_local_variable(value);
        } else if (!unset_only_variables) {
            unsetenv(value.characters());
        }
    }

    if (did_touch_path)
        cache_path();

    return 0;
}

ErrorOr<int> Shell::builtin_set(Main::Arguments arguments)
{
    if (arguments.strings.size() == 1) {
        HashMap<String, String> vars;

        StringBuilder builder;
        for (auto& frame : m_local_frames) {
            for (auto& var : frame->local_variables) {
                builder.join(" "sv, TRY(var.value->resolve_as_list(*this)));
                vars.set(TRY(String::from_byte_string(var.key)), TRY(builder.to_string()));
                builder.clear();
            }
        }

        struct Variable {
            StringView name;
            String value;
        };

        Vector<Variable> variables;
        variables.ensure_capacity(vars.size());

        for (auto& var : vars)
            variables.unchecked_append({ var.key, var.value });

        Vector<String> functions;
        functions.ensure_capacity(m_functions.size());
        for (auto& function : m_functions)
            functions.unchecked_append(TRY(serialize_function_definition(function.value)));

        quick_sort(variables, [](auto& a, auto& b) { return a.name < b.name; });
        quick_sort(functions, [](auto& a, auto& b) { return a < b; });

        for (auto& var : variables)
            outln("{}={}", var.name, escape_token(var.value));

        for (auto& fn : functions)
            outln("{}", fn);

        return 0;
    }

    Vector<StringView> argv_to_set;

    Core::ArgsParser parser;
    parser.set_stop_on_first_non_option(true);
    parser.add_positional_argument(argv_to_set, "List of arguments", "arg", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (!argv_to_set.is_empty() || arguments.strings.last() == "--"sv) {
        Vector<String> argv;
        argv.ensure_capacity(argv_to_set.size());
        for (auto& arg : argv_to_set)
            argv.unchecked_append(TRY(String::from_utf8(arg)));
        set_local_variable("ARGV", AST::make_ref_counted<AST::ListValue>(move(argv)));
    }

    return 0;
}

ErrorOr<int> Shell::builtin_not(Main::Arguments arguments)
{
    Vector<StringView> args;

    Core::ArgsParser parser;
    parser.set_stop_on_first_non_option(true);
    parser.add_positional_argument(args, "Command to run followed by its arguments", "string");

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::Ignore))
        return 1;

    AST::Command command;
    TRY(command.argv.try_ensure_capacity(args.size()));
    for (auto& arg : args)
        command.argv.unchecked_append(TRY(String::from_utf8(arg)));

    auto commands = TRY(expand_aliases({ move(command) }));
    int exit_code = 1;
    auto found_a_job = false;
    for (auto& job : run_commands(commands)) {
        found_a_job = true;
        block_on_job(job);
        exit_code = job->exit_code();
    }
    // In case it was a function.
    if (!found_a_job)
        exit_code = last_return_code.value_or(0);
    return exit_code == 0 ? 1 : 0;
}

ErrorOr<int> Shell::builtin_kill(Main::Arguments arguments)
{
    // Simply translate the arguments and pass them to `kill'
    Vector<String> replaced_values;
    auto kill_path_or_error = Core::System::resolve_executable_from_environment("kill"sv);
    if (kill_path_or_error.is_error()) {
        warnln("kill: `kill' not found in PATH");
        return 126;
    }

    replaced_values.append(kill_path_or_error.release_value());
    for (size_t i = 1; i < arguments.strings.size(); ++i) {
        if (auto job_id = resolve_job_spec(arguments.strings[i]); job_id.has_value()) {
            auto job = find_job(job_id.value());
            if (job) {
                replaced_values.append(String::number(job->pid()));
            } else {
                warnln("kill: Job with pid {} not found", job_id.value());
                return 1;
            }
        } else {
            replaced_values.append(TRY(String::from_utf8(arguments.strings[i])));
        }
    }

    // Now just run `kill'
    AST::Command command;
    command.argv = move(replaced_values);
    command.position = m_source_position.has_value() ? m_source_position->position : Optional<AST::Position> {};

    auto exit_code = 1;
    auto job_result = run_command(command);
    if (job_result.is_error()) {
        warnln("kill: Failed to run {}: {}", command.argv.first(), job_result.error());
        return exit_code;
    }

    if (auto job = job_result.release_value()) {
        block_on_job(job);
        exit_code = job->exit_code();
    }
    return exit_code;
}

ErrorOr<bool> Shell::run_builtin(const AST::Command& command, Vector<NonnullRefPtr<AST::Rewiring>> const& rewirings, int& retval)
{
    if (command.argv.is_empty())
        return false;

    if (!has_builtin(command.argv.first()))
        return false;

    Vector<StringView> arguments;
    TRY(arguments.try_ensure_capacity(command.argv.size()));
    for (auto& arg : command.argv)
        arguments.unchecked_append(arg);

    Main::Arguments arguments_object {
        .argc = 0,
        .argv = nullptr,
        .strings = arguments
    };

    StringView name = command.argv.first();

    SavedFileDescriptors fds { rewirings };

    for (auto& rewiring : rewirings) {
        int rc = dup2(rewiring->old_fd, rewiring->new_fd);
        if (rc < 0) {
            perror("dup2(run)");
            return false;
        }
    }

    Core::EventLoop loop;
    setup_signals();

    if (name == ":"sv)
        name = "noop"sv;
    else if (m_in_posix_mode && name == "."sv)
        name = "source"sv;

#define __ENUMERATE_SHELL_BUILTIN(builtin, _mode)                        \
    if (name == #builtin) {                                              \
        retval = TRY(builtin_##builtin(arguments_object));               \
        if (!has_error(ShellError::None))                                \
            raise_error(m_error, m_error_description, command.position); \
        fflush(stdout);                                                  \
        fflush(stderr);                                                  \
        return true;                                                     \
    }

    ENUMERATE_SHELL_BUILTINS();

#undef __ENUMERATE_SHELL_BUILTIN
    return false;
}

ErrorOr<int> Shell::builtin_argsparser_parse(Main::Arguments arguments)
{
    // argsparser_parse
    //   --add-option variable [--type (bool | string | i32 | u32 | double | size)] --help-string "" --long-name "" --short-name "" [--value-name "" <if not --type bool>] --list
    //   --add-positional-argument variable [--type (bool | string | i32 | u32 | double | size)] ([--min n] [--max n] | [--required]) --help-string "" --value-name ""
    //   [--general-help ""]
    //   [--stop-on-first-non-option]
    //   --
    //   $args_to_parse
    Core::ArgsParser parser;

    Core::ArgsParser user_parser;

    Vector<StringView> descriptors;
    Variant<Core::ArgsParser::Option, Core::ArgsParser::Arg, Empty> current;
    Vector<ByteString> help_string_storage;
    Vector<ByteString> long_name_storage;
    Vector<ByteString> value_name_storage;
    Vector<ByteString> name_storage;
    ByteString current_variable;
    // if max > 1 or min < 1, or explicit `--list`.
    bool treat_arg_as_list = false;
    enum class Type {
        Bool,
        String,
        I32,
        U32,
        Double,
        Size,
    };

    auto type = Type::String;

    auto try_convert = [](StringView value, Type type) -> ErrorOr<Optional<RefPtr<AST::Value>>> {
        switch (type) {
        case Type::Bool:
            return AST::make_ref_counted<AST::StringValue>("true"_string);
        case Type::String:
            return AST::make_ref_counted<AST::StringValue>(TRY(String::from_utf8(value)));
        case Type::I32:
            if (auto number = value.to_number<int>(); number.has_value())
                return AST::make_ref_counted<AST::StringValue>(String::number(*number));

            warnln("Invalid value for type i32: {}", value);
            return OptionalNone {};
        case Type::U32:
        case Type::Size:
            if (auto number = value.to_number<unsigned>(); number.has_value())
                return AST::make_ref_counted<AST::StringValue>(String::number(*number));

            warnln("Invalid value for type u32|size: {}", value);
            return OptionalNone {};
        case Type::Double: {
            ByteString string = value;
            char* endptr = nullptr;
            auto number = strtod(string.characters(), &endptr);
            if (endptr != string.characters() + string.length()) {
                warnln("Invalid value for type double: {}", value);
                return OptionalNone {};
            }

            return AST::make_ref_counted<AST::StringValue>(String::number(number));
        }
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto enlist = [&](auto name, auto value) -> ErrorOr<NonnullRefPtr<AST::Value>> {
        auto variable = TRY(look_up_local_variable(name));
        if (variable) {
            auto list = TRY(const_cast<AST::Value&>(*variable).resolve_as_list(*this));
            auto new_value = TRY(value->resolve_as_string(*this));
            list.append(move(new_value));
            return try_make_ref_counted<AST::ListValue>(move(list));
        }
        return *value;
    };

    // FIXME: We cannot return ErrorOr<T> from Core::ArgsParser::Option::accept_value(), fix the MUST's here whenever that function is ErrorOr-aware.
    auto commit = [&] {
        return current.visit(
            [&](Core::ArgsParser::Option& option) {
                if (!option.long_name && !option.short_name) {
                    warnln("Defined option must have at least one of --long-name or --short-name");
                    return false;
                }
                option.accept_value = [&, current_variable, treat_arg_as_list, type](StringView value) {
                    auto result = MUST(try_convert(value, type));
                    if (result.has_value()) {
                        auto value = result.release_value();
                        if (treat_arg_as_list)
                            value = MUST(enlist(current_variable, move(value)));
                        this->set_local_variable(current_variable, move(value), true);
                        return true;
                    }

                    return false;
                };
                user_parser.add_option(move(option));
                type = Type::String;
                treat_arg_as_list = false;
                return true;
            },
            [&](Core::ArgsParser::Arg& arg) {
                if (!arg.name) {
                    warnln("Defined positional argument must have a name");
                    return false;
                }
                arg.accept_value = [&, current_variable, treat_arg_as_list, type](StringView value) {
                    auto result = MUST(try_convert(value, type));
                    if (result.has_value()) {
                        auto value = result.release_value();
                        if (treat_arg_as_list)
                            value = MUST(enlist(current_variable, move(value)));
                        this->set_local_variable(current_variable, move(value), true);
                        return true;
                    }

                    return false;
                };
                user_parser.add_positional_argument(move(arg));
                type = Type::String;
                treat_arg_as_list = false;
                return true;
            },
            [&](Empty) {
                return true;
            });
    };

    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::None,
        .help_string = "Stop processing descriptors after a non-argument parameter is seen",
        .long_name = "stop-on-first-non-option",
        .accept_value = [&](auto) {
            user_parser.set_stop_on_first_non_option(true);
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the general help string for the parser",
        .long_name = "general-help",
        .value_name = "string",
        .accept_value = [&](StringView value) {
            VERIFY(strlen(value.characters_without_null_termination()) == value.length());
            user_parser.set_general_help(value.characters_without_null_termination());
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Start describing an option",
        .long_name = "add-option",
        .value_name = "variable-name",
        .accept_value = [&](auto name) {
            if (!commit())
                return false;

            current = Core::ArgsParser::Option {};
            current_variable = name;
            if (current_variable.is_empty() || !all_of(current_variable, [](auto ch) { return ch == '_' || isalnum(ch); })) {
                warnln("Option variable name must be a valid identifier");
                return false;
            }

            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::None,
        .help_string = "Accept multiple of the current option being given",
        .long_name = "list",
        .accept_value = [&](auto) {
            if (!current.has<Core::ArgsParser::Option>()) {
                warnln("Must be defining an option to use --list");
                return false;
            }
            treat_arg_as_list = true;
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Define the type of the option or argument being described",
        .long_name = "type",
        .value_name = "type",
        .accept_value = [&](StringView ty) {
            if (current.has<Empty>()) {
                warnln("Must be defining an argument or option to use --type");
                return false;
            }

            if (ty == "bool") {
                if (auto option = current.get_pointer<Core::ArgsParser::Option>()) {
                    if (option->value_name != nullptr) {
                        warnln("Type 'bool' does not apply to options with a value (value name is set to {})", option->value_name);
                        return false;
                    }

                    option->argument_mode = Core::ArgsParser::OptionArgumentMode::None;
                }
                type = Type::Bool;
            } else if (ty == "string") {
                type = Type::String;
            } else if (ty == "i32") {
                type = Type::I32;
            } else if (ty == "u32") {
                type = Type::U32;
            } else if (ty == "double") {
                type = Type::Double;
            } else if (ty == "size") {
                type = Type::Size;
            } else {
                warnln("Invalid type '{}', expected one of bool | string | i32 | u32 | double | size", ty);
                return false;
            }

            if (type == Type::Bool) {
                set_local_variable(
                    current_variable,
                    make_ref_counted<AST::StringValue>("false"_string),
                    true);
            }
            return true;
        },
    });

    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the help string of the option or argument being defined",
        .long_name = "help-string",
        .value_name = "string",
        .accept_value = [&](StringView value) {
            return current.visit(
                [](Empty) {
                    warnln("Must be defining an option or argument to use --help-string");
                    return false;
                },
                [&](auto& option) {
                    help_string_storage.append(value);
                    option.help_string = help_string_storage.last().characters();
                    return true;
                });
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the long name of the option being defined",
        .long_name = "long-name",
        .value_name = "name",
        .accept_value = [&](StringView value) {
            auto option = current.get_pointer<Core::ArgsParser::Option>();
            if (!option) {
                warnln("Must be defining an option to use --long-name");
                return false;
            }
            if (option->long_name) {
                warnln("Repeated application of --long-name is not allowed, current option has long name set to \"{}\"", option->long_name);
                return false;
            }

            long_name_storage.append(value);
            option->long_name = long_name_storage.last().characters();
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the short name of the option being defined",
        .long_name = "short-name",
        .value_name = "char",
        .accept_value = [&](StringView value) {
            auto option = current.get_pointer<Core::ArgsParser::Option>();
            if (!option) {
                warnln("Must be defining an option to use --short-name");
                return false;
            }
            if (value.length() != 1) {
                warnln("Option short name ('{}') must be exactly one character long", value);
                return false;
            }
            if (option->short_name) {
                warnln("Repeated application of --short-name is not allowed, current option has short name set to '{}'", option->short_name);
                return false;
            }
            option->short_name = value[0];
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the value name of the option being defined",
        .long_name = "value-name",
        .value_name = "string",
        .accept_value = [&](StringView value) {
            return current.visit(
                [](Empty) {
                    warnln("Must be defining an option or a positional argument to use --value-name");
                    return false;
                },
                [&](Core::ArgsParser::Option& option) {
                    if (option.value_name) {
                        warnln("Repeated application of --value-name is not allowed, current option has value name set to \"{}\"", option.value_name);
                        return false;
                    }
                    if (type == Type::Bool) {
                        warnln("Options of type bool cannot have a value name");
                        return false;
                    }

                    value_name_storage.append(value);
                    option.value_name = value_name_storage.last().characters();
                    return true;
                },
                [&](Core::ArgsParser::Arg& arg) {
                    if (arg.name) {
                        warnln("Repeated application of --value-name is not allowed, current argument has value name set to \"{}\"", arg.name);
                        return false;
                    }

                    name_storage.append(value);
                    arg.name = name_storage.last().characters();
                    return true;
                });
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Start describing a positional argument",
        .long_name = "add-positional-argument",
        .value_name = "variable",
        .accept_value = [&](auto value) {
            if (!commit())
                return false;

            current = Core::ArgsParser::Arg {};
            current_variable = value;
            if (current_variable.is_empty() || !all_of(current_variable, [](auto ch) { return ch == '_' || isalnum(ch); })) {
                warnln("Argument variable name must be a valid identifier");
                return false;
            }

            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the minimum required number of positional descriptors for the argument being described",
        .long_name = "min",
        .value_name = "n",
        .accept_value = [&](StringView value) {
            auto arg = current.get_pointer<Core::ArgsParser::Arg>();
            if (!arg) {
                warnln("Must be describing a positional argument to use --min");
                return false;
            }

            auto number = value.to_number<unsigned>();
            if (!number.has_value()) {
                warnln("Invalid value for --min: '{}', expected a non-negative number", value);
                return false;
            }

            if (static_cast<unsigned>(arg->max_values) < *number) {
                warnln("Invalid value for --min: {}, min must not be larger than max ({})", *number, arg->max_values);
                return false;
            }

            arg->min_values = *number;
            treat_arg_as_list = arg->max_values > 1 || arg->min_values < 1;
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the maximum required number of positional descriptors for the argument being described",
        .long_name = "max",
        .value_name = "n",
        .accept_value = [&](StringView value) {
            auto arg = current.get_pointer<Core::ArgsParser::Arg>();
            if (!arg) {
                warnln("Must be describing a positional argument to use --max");
                return false;
            }

            auto number = value.to_number<unsigned>();
            if (!number.has_value()) {
                warnln("Invalid value for --max: '{}', expected a non-negative number", value);
                return false;
            }

            if (static_cast<unsigned>(arg->min_values) > *number) {
                warnln("Invalid value for --max: {}, max must not be smaller than min ({})", *number, arg->min_values);
                return false;
            }

            arg->max_values = *number;
            treat_arg_as_list = arg->max_values > 1 || arg->min_values < 1;
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::None,
        .help_string = "Mark the positional argument being described as required (shorthand for --min 1)",
        .long_name = "required",
        .accept_value = [&](auto) {
            auto arg = current.get_pointer<Core::ArgsParser::Arg>();
            if (!arg) {
                warnln("Must be describing a positional argument to use --required");
                return false;
            }
            arg->min_values = 1;
            if (arg->max_values < arg->min_values)
                arg->max_values = 1;
            treat_arg_as_list = arg->max_values > 1 || arg->min_values < 1;
            return true;
        },
    });
    parser.add_positional_argument(descriptors, "Arguments to parse via the described ArgsParser configuration", "arg", Core::ArgsParser::Required::No);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::Ignore))
        return 2;

    if (!commit())
        return 2;

    if (!user_parser.parse(descriptors, Core::ArgsParser::FailureBehavior::Ignore))
        return 1;

    return 0;
}

ErrorOr<int> Shell::builtin_read(Main::Arguments arguments)
{
    bool no_escape = false;
    Vector<ByteString> variables;

    Core::ArgsParser parser;
    parser.add_option(no_escape, "Do not interpret backslash escapes", "no-escape", 'r');
    parser.add_positional_argument(variables, "Variables to read into", "variable", Core::ArgsParser::Required::Yes);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::Ignore))
        return 1;

    auto split_by_any_of = " \t\n"_string;

    if (auto const* value_from_env = getenv("IFS"); value_from_env)
        split_by_any_of = TRY(String::from_utf8({ value_from_env, strlen(value_from_env) }));
    else if (auto split_by_variable = TRY(look_up_local_variable("IFS"sv)); split_by_variable)
        split_by_any_of = TRY(const_cast<AST::Value&>(*split_by_variable).resolve_as_string(*this));

    auto file = TRY(Core::File::standard_input());
    auto buffered_stream = TRY(Core::InputBufferedFile::create(move(file)));

    StringBuilder builder;
    ByteBuffer buffer;

    enum class LineState {
        Done,
        EscapedNewline,
    };
    auto read_line = [&]() -> ErrorOr<LineState> {
        if (m_is_interactive && isatty(STDIN_FILENO)) {
            // Show prompt
            warn("read: ");
        }
        size_t attempted_line_size = 32;

        for (;;) {
            auto result = buffered_stream->read_line(TRY(buffer.get_bytes_for_writing(attempted_line_size)));
            if (result.is_error() && result.error().is_errno() && result.error().code() == EMSGSIZE) {
                attempted_line_size *= 2;
                continue;
            }

            auto used_bytes = TRY(move(result));
            if (!no_escape && used_bytes.ends_with("\\\n"sv)) {
                builder.append(used_bytes.substring_view(0, used_bytes.length() - 2));
                return LineState::EscapedNewline;
            }

            if (used_bytes.ends_with("\n"sv))
                used_bytes = used_bytes.substring_view(0, used_bytes.length() - 1);

            builder.append(used_bytes);
            return LineState::Done;
        }
    };

    LineState state;
    do {
        state = TRY(read_line());
    } while (state == LineState::EscapedNewline);

    auto line = builder.string_view();
    if (variables.size() == 1) {
        set_local_variable(variables[0], make_ref_counted<AST::StringValue>(TRY(String::from_utf8(line))));
        return 0;
    }

    auto fields = line.split_view_if(is_any_of(split_by_any_of), SplitBehavior::KeepEmpty);

    for (size_t i = 0; i < variables.size(); ++i) {
        auto& variable = variables[i];
        StringView variable_value;
        if (i >= fields.size())
            variable_value = ""sv;
        else if (i == variables.size() - 1)
            variable_value = line.substring_view_starting_from_substring(fields[i]);
        else
            variable_value = fields[i];

        set_local_variable(variable, make_ref_counted<AST::StringValue>(TRY(String::from_utf8(variable_value))));
    }

    return 0;
}

ErrorOr<int> Shell::builtin_run_with_env(Main::Arguments arguments)
{
    Vector<ByteString> environment_variables;
    Vector<StringView> command_and_arguments;

    Core::ArgsParser parser;
    parser.add_option(environment_variables, "Environment variables to set", "env", 'e', "NAME=VALUE");
    parser.add_positional_argument(command_and_arguments, "Command and arguments to run", "command", Core::ArgsParser::Required::Yes);
    parser.set_stop_on_first_non_option(true);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::Ignore))
        return 1;

    if (command_and_arguments.is_empty()) {
        warnln("run_with_env: No command to run");
        return 1;
    }

    AST::Command command;
    TRY(command.argv.try_ensure_capacity(command_and_arguments.size()));
    for (auto& arg : command_and_arguments)
        command.argv.append(TRY(String::from_utf8(arg)));

    auto commands = TRY(expand_aliases({ move(command) }));

    HashMap<ByteString, Optional<ByteString>> old_environment_entries;
    for (auto& variable : environment_variables) {
        auto parts = variable.split_limit('=', 2, SplitBehavior::KeepEmpty);
        if (parts.size() != 2) {
            warnln("run_with_env: Invalid environment variable: '{}'", variable);
            return 1;
        }

        ByteString name = parts[0];
        old_environment_entries.set(name, getenv(name.characters()) ?: Optional<ByteString> {});

        ByteString value = parts[1];
        setenv(name.characters(), value.characters(), 1);
    }

    int exit_code = 0;
    for (auto& job : run_commands(commands)) {
        block_on_job(job);
        exit_code = job->exit_code();
    }

    for (auto& entry : old_environment_entries) {
        if (entry.value.has_value())
            setenv(entry.key.characters(), entry.value->characters(), 1);
        else
            unsetenv(entry.key.characters());
    }

    return exit_code;
}

ErrorOr<int> Shell::builtin_shell_set_active_prompt(Main::Arguments arguments)
{
    StringView new_prompt;

    Core::ArgsParser parser;
    parser.add_positional_argument(new_prompt, "New prompt text", "prompt", Core::ArgsParser::Required::Yes);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::Ignore))
        return 1;

    if (!m_editor) {
        warnln("shell_set_active_prompt: No active prompt");
        return 1;
    }

    if (m_editor->is_editing())
        m_editor->set_prompt(new_prompt);
    else
        m_next_scheduled_prompt_text = new_prompt;
    return 0;
}

ErrorOr<int> Shell::builtin_in_parallel(Main::Arguments arguments)
{
    unsigned max_jobs = 1;
    Vector<StringView> command_and_arguments;

#ifdef _SC_NPROCESSORS_ONLN
    max_jobs = sysconf(_SC_NPROCESSORS_ONLN);
#endif

    Core::ArgsParser parser;
    parser.set_general_help("Run the given command in the background, allowing at most <N> jobs running at once.");
    parser.add_option(max_jobs, "Maximum number of jobs to run in parallel", "max-jobs", 'j', "N");
    parser.add_positional_argument(command_and_arguments, "Command and arguments to run", "argument", Core::ArgsParser::Required::Yes);
    parser.set_stop_on_first_non_option(true);

    if (!parser.parse(arguments, Core::ArgsParser::FailureBehavior::Ignore))
        return 1;

    if (command_and_arguments.is_empty()) {
        warnln("in_parallel: No command to run");
        return 1;
    }

    AST::Command command;
    TRY(command.argv.try_ensure_capacity(command_and_arguments.size()));
    for (auto& arg : command_and_arguments)
        command.argv.append(TRY(String::from_utf8(arg)));

    auto commands = TRY(expand_aliases({ move(command) }));

    Vector<AST::Command> commands_to_run;
    for (auto& command : commands) {
        if (command.argv.is_empty())
            continue;
        command.should_notify_if_in_background = false;
        command.should_wait = false;
        commands_to_run.append(move(command));
    }

    if (commands_to_run.is_empty()) {
        warnln("in_parallel: No command to run");
        return 1;
    }

    Core::EventLoop loop;
    loop.spin_until([&] { return jobs.size() + commands_to_run.size() <= max_jobs; });
    run_commands(commands_to_run);
    return 0;
}

bool Shell::has_builtin(StringView name) const
{
    if (name == ":"sv || (m_in_posix_mode && name == "."sv))
        return true;

#define __ENUMERATE_SHELL_BUILTIN(builtin, mode)                            \
    if (name == #builtin) {                                                 \
        if (POSIXModeRequirement::mode == POSIXModeRequirement::InAllModes) \
            return true;                                                    \
        return m_in_posix_mode;                                             \
    }

    ENUMERATE_SHELL_BUILTINS();

#undef __ENUMERATE_SHELL_BUILTIN
    return false;
}
}
