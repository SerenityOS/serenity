/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST.h"
#include "Shell.h"
#include "Shell/Formatter.h"
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/Statistics.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <errno.h>
#include <inttypes.h>
#include <limits.h>
#include <signal.h>
#include <sys/wait.h>
#include <unistd.h>

extern char** environ;

namespace Shell {

int Shell::builtin_noop(int, char const**)
{
    return 0;
}

int Shell::builtin_dump(int argc, char const** argv)
{
    if (argc != 2)
        return 1;

    Parser { StringView { argv[1], strlen(argv[1]) } }.parse()->dump(0);
    return 0;
}

int Shell::builtin_alias(int argc, char const** argv)
{
    Vector<String> arguments;

    Core::ArgsParser parser;
    parser.add_positional_argument(arguments, "List of name[=values]'s", "name[=value]", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (arguments.is_empty()) {
        for (auto& alias : m_aliases)
            printf("%s=%s\n", escape_token(alias.key).characters(), escape_token(alias.value).characters());
        return 0;
    }

    bool fail = false;
    for (auto& argument : arguments) {
        auto parts = argument.split_limit('=', 2, true);
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

int Shell::builtin_unalias(int argc, char const** argv)
{
    bool remove_all { false };
    Vector<String> arguments;

    Core::ArgsParser parser;
    parser.set_general_help("Remove alias from the list of aliases");
    parser.add_option(remove_all, "Remove all aliases", nullptr, 'a');
    parser.add_positional_argument(arguments, "List of aliases to remove", "alias", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (remove_all) {
        m_aliases.clear();
        cache_path();
        return 0;
    }

    if (arguments.is_empty()) {
        warnln("unalias: not enough arguments");
        parser.print_usage(stderr, argv[0]);
        return 1;
    }

    bool failed { false };
    for (auto& argument : arguments) {
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

int Shell::builtin_bg(int argc, char const** argv)
{
    int job_id = -1;
    bool is_pid = false;

    Core::ArgsParser parser;
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Job ID or Jobspec to run in background",
        .name = "job-id",
        .min_values = 0,
        .max_values = 1,
        .accept_value = [&](auto value_ptr) -> bool {
            StringView value { value_ptr, strlen(value_ptr) };
            // Check if it's a pid (i.e. literal integer)
            if (auto number = value.to_uint(); number.has_value()) {
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

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_type(int argc, char const** argv)
{
    Vector<String> commands;
    bool dont_show_function_source = false;

    Core::ArgsParser parser;
    parser.set_general_help("Display information about commands.");
    parser.add_positional_argument(commands, "Command(s) to list info about", "command");
    parser.add_option(dont_show_function_source, "Do not show functions source.", "no-fn-source", 'f');

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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
                StringBuilder builder;
                builder.append(fn.name);
                builder.append('(');
                for (size_t i = 0; i < fn.arguments.size(); i++) {
                    builder.append(fn.arguments[i]);
                    if (!(i == fn.arguments.size() - 1))
                        builder.append(' ');
                }
                builder.append(") {\n"sv);
                if (fn.body) {
                    auto formatter = Formatter(*fn.body);
                    builder.append(formatter.format());
                    printf("%s\n}\n", builder.build().characters());
                } else {
                    printf("%s\n}\n", builder.build().characters());
                }
            }
            continue;
        }

        // check if its a builtin
        if (has_builtin(command)) {
            printf("%s is a shell builtin\n", command.characters());
            continue;
        }

        // check if its an executable in PATH
        auto fullpath = Core::File::resolve_executable_from_environment(command);
        if (fullpath.has_value()) {
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

int Shell::builtin_cd(int argc, char const** argv)
{
    char const* arg_path = nullptr;

    Core::ArgsParser parser;
    parser.add_positional_argument(arg_path, "Path to change to", "path", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    String new_path;

    if (!arg_path) {
        new_path = home;
    } else {
        if (strcmp(arg_path, "-") == 0) {
            char* oldpwd = getenv("OLDPWD");
            if (oldpwd == nullptr)
                return 1;
            new_path = oldpwd;
        } else {
            new_path = arg_path;
        }
    }

    auto real_path = Core::File::real_path_for(new_path);
    if (real_path.is_empty()) {
        warnln("Invalid path '{}'", new_path);
        return 1;
    }

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

int Shell::builtin_cdh(int argc, char const** argv)
{
    int index = -1;

    Core::ArgsParser parser;
    parser.add_positional_argument(index, "Index of the cd history entry (leave out for a list)", "index", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

    char const* path = cd_history.at(cd_history.size() - index).characters();
    char const* cd_args[] = { "cd", path, nullptr };
    return Shell::builtin_cd(2, cd_args);
}

int Shell::builtin_dirs(int argc, char const** argv)
{
    // The first directory in the stack is ALWAYS the current directory
    directory_stack.at(0) = cwd.characters();

    bool clear = false;
    bool print = false;
    bool number_when_printing = false;
    char separator = ' ';

    Vector<String> paths;

    Core::ArgsParser parser;
    parser.add_option(clear, "Clear the directory stack", "clear", 'c');
    parser.add_option(print, "Print directory entries one per line", "print", 'p');
    parser.add_option(number_when_printing, "Number the directories in the stack when printing", "number", 'v');
    parser.add_positional_argument(paths, "Extra paths to put on the stack", "path", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_exec(int argc, char const** argv)
{
    if (argc < 2) {
        warnln("Shell: No command given to exec");
        return 1;
    }

    Vector<char const*> argv_vector;
    argv_vector.append(argv + 1, argc - 1);
    argv_vector.append(nullptr);

    execute_process(move(argv_vector));
}

int Shell::builtin_exit(int argc, char const** argv)
{
    int exit_code = 0;
    Core::ArgsParser parser;
    parser.add_positional_argument(exit_code, "Exit code", "code", Core::ArgsParser::Required::No);
    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_export(int argc, char const** argv)
{
    Vector<String> vars;

    Core::ArgsParser parser;
    parser.add_positional_argument(vars, "List of variable[=value]'s", "values", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (vars.is_empty()) {
        for (size_t i = 0; environ[i]; ++i)
            puts(environ[i]);
        return 0;
    }

    for (auto& value : vars) {
        auto parts = value.split_limit('=', 2);

        if (parts.size() == 1) {
            auto value = lookup_local_variable(parts[0]);
            if (value) {
                auto values = value->resolve_as_list(*this);
                StringBuilder builder;
                builder.join(' ', values);
                parts.append(builder.to_string());
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

int Shell::builtin_glob(int argc, char const** argv)
{
    Vector<String> globs;
    Core::ArgsParser parser;
    parser.add_positional_argument(globs, "Globs to resolve", "glob");

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    for (auto& glob : globs) {
        for (auto& expanded : expand_globs(glob, cwd))
            outln("{}", expanded);
    }

    return 0;
}

int Shell::builtin_fg(int argc, char const** argv)
{
    int job_id = -1;
    bool is_pid = false;

    Core::ArgsParser parser;
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Job ID or Jobspec to bring to foreground",
        .name = "job-id",
        .min_values = 0,
        .max_values = 1,
        .accept_value = [&](auto const* value_ptr) -> bool {
            StringView value { value_ptr, strlen(value_ptr) };
            // Check if it's a pid (i.e. literal integer)
            if (auto number = value.to_uint(); number.has_value()) {
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

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_disown(int argc, char const** argv)
{
    Vector<int> job_ids;
    Vector<bool> id_is_pid;

    Core::ArgsParser parser;
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Job IDs or Jobspecs to disown",
        .name = "job-id",
        .min_values = 0,
        .max_values = INT_MAX,
        .accept_value = [&](auto const* value_ptr) -> bool {
            StringView value { value_ptr, strlen(value_ptr) };
            // Check if it's a pid (i.e. literal integer)
            if (auto number = value.to_uint(); number.has_value()) {
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

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_history(int, char const**)
{
    for (size_t i = 0; i < m_editor->history().size(); ++i) {
        printf("%6zu  %s\n", i + 1, m_editor->history()[i].entry.characters());
    }
    return 0;
}

int Shell::builtin_jobs(int argc, char const** argv)
{
    bool list = false, show_pid = false;

    Core::ArgsParser parser;
    parser.add_option(list, "List all information about jobs", "list", 'l');
    parser.add_option(show_pid, "Display the PID of the jobs", "pid", 'p');

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_popd(int argc, char const** argv)
{
    if (directory_stack.size() <= 1) {
        warnln("Shell: popd: directory stack empty");
        return 1;
    }

    bool should_not_switch = false;
    Core::ArgsParser parser;
    parser.add_option(should_not_switch, "Do not switch dirs", "no-switch", 'n');

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_pushd(int argc, char const** argv)
{
    StringBuilder path_builder;
    bool should_switch = true;

    // From the BASH reference manual: https://www.gnu.org/software/bash/manual/html_node/Directory-Stack-Builtins.html
    // With no arguments, pushd exchanges the top two directories and makes the new top the current directory.
    if (argc == 1) {
        if (directory_stack.size() < 2) {
            warnln("pushd: no other directory");
            return 1;
        }

        String dir1 = directory_stack.take_first();
        String dir2 = directory_stack.take_first();
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
    if (argc == 2) {
        directory_stack.append(cwd.characters());
        if (argv[1][0] == '/') {
            path_builder.append({ argv[1], strlen(argv[1]) });
        } else {
            path_builder.appendff("{}/{}", cwd, argv[1]);
        }
    } else if (argc == 3) {
        directory_stack.append(cwd.characters());
        for (int i = 1; i < argc; i++) {
            char const* arg = argv[i];

            if (arg[0] != '-') {
                if (arg[0] == '/') {
                    path_builder.append({ arg, strlen(arg) });
                } else
                    path_builder.appendff("{}/{}", cwd, arg);
            }

            if (!strcmp(arg, "-n"))
                should_switch = false;
        }
    }

    auto real_path = LexicalPath::canonicalized_path(path_builder.to_string());

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

int Shell::builtin_pwd(int, char const**)
{
    print_path(cwd);
    fputc('\n', stdout);
    return 0;
}

int Shell::builtin_setopt(int argc, char const** argv)
{
    if (argc == 1) {
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

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_shift(int argc, char const** argv)
{
    int count = 1;

    Core::ArgsParser parser;
    parser.add_positional_argument(count, "Shift count", "count", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (count < 1)
        return 0;

    auto argv_ = lookup_local_variable("ARGV"sv);
    if (!argv_) {
        warnln("shift: ARGV is unset");
        return 1;
    }

    if (!argv_->is_list())
        argv_ = adopt_ref(*new AST::ListValue({ argv_.release_nonnull() }));

    auto& values = static_cast<AST::ListValue*>(argv_.ptr())->values();
    if ((size_t)count > values.size()) {
        warnln("shift: shift count must not be greater than {}", values.size());
        return 1;
    }

    for (auto i = 0; i < count; ++i)
        (void)values.take_first();

    return 0;
}

int Shell::builtin_source(int argc, char const** argv)
{
    char const* file_to_source = nullptr;
    Vector<String> args;

    Core::ArgsParser parser;
    parser.add_positional_argument(file_to_source, "File to read commands from", "path");
    parser.add_positional_argument(args, "ARGV for the sourced file", "args", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv)))
        return 1;

    auto previous_argv = lookup_local_variable("ARGV"sv);
    ScopeGuard guard { [&] {
        if (!args.is_empty())
            set_local_variable("ARGV", move(previous_argv));
    } };

    if (!args.is_empty())
        set_local_variable("ARGV", AST::make_ref_counted<AST::ListValue>(move(args)));

    if (!run_file(file_to_source, true))
        return 126;

    return 0;
}

int Shell::builtin_time(int argc, char const** argv)
{
    AST::Command command;

    int number_of_iterations = 1;

    Core::ArgsParser parser;
    parser.add_option(number_of_iterations, "Number of iterations", "iterations", 'n', "iterations");
    parser.set_stop_on_first_non_option(true);
    parser.add_positional_argument(command.argv, "Command to execute with arguments", "command", Core::ArgsParser::Required::Yes);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (number_of_iterations < 1)
        return 1;

    auto commands = expand_aliases({ move(command) });

    AK::Statistics iteration_times;

    int exit_code = 1;
    for (int i = 0; i < number_of_iterations; ++i) {
        auto timer = Core::ElapsedTimer::start_new();
        for (auto& job : run_commands(commands)) {
            block_on_job(job);
            exit_code = job.exit_code();
        }
        iteration_times.add(timer.elapsed());
    }

    if (number_of_iterations == 1) {
        warnln("Time: {} ms", iteration_times.values().first());
    } else {
        AK::Statistics iteration_times_excluding_first;
        for (size_t i = 1; i < iteration_times.size(); i++)
            iteration_times_excluding_first.add(iteration_times.values()[i]);

        warnln("Timing report: {} ms", iteration_times.sum());
        warnln("==============");
        warnln("Command:         {}", String::join(' ', Span<char const*>(argv, argc)));
        warnln("Average time:    {:.2} ms (median: {}, stddev: {:.2}, min: {}, max:{})",
            iteration_times.average(), iteration_times.median(),
            iteration_times.standard_deviation(),
            iteration_times.min(), iteration_times.max());
        warnln("Excluding first: {:.2} ms (median: {}, stddev: {:.2}, min: {}, max:{})",
            iteration_times_excluding_first.average(), iteration_times_excluding_first.median(),
            iteration_times_excluding_first.standard_deviation(),
            iteration_times_excluding_first.min(), iteration_times_excluding_first.max());
    }

    return exit_code;
}

int Shell::builtin_umask(int argc, char const** argv)
{
    char const* mask_text = nullptr;

    Core::ArgsParser parser;
    parser.add_positional_argument(mask_text, "New mask (omit to get current mask)", "octal-mask", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (!mask_text) {
        mode_t old_mask = umask(0);
        printf("%#o\n", old_mask);
        umask(old_mask);
        return 0;
    }

    unsigned mask;
    int matches = sscanf(mask_text, "%o", &mask);
    if (matches == 1) {
        umask(mask);
        return 0;
    }

    warnln("umask: Invalid mask '{}'", mask_text);
    return 1;
}

int Shell::builtin_wait(int argc, char const** argv)
{
    Vector<int> job_ids;
    Vector<bool> id_is_pid;

    Core::ArgsParser parser;
    parser.add_positional_argument(Core::ArgsParser::Arg {
        .help_string = "Job IDs or Jobspecs to wait for",
        .name = "job-id",
        .min_values = 0,
        .max_values = INT_MAX,
        .accept_value = [&](auto const* value_ptr) -> bool {
            StringView value { value_ptr, strlen(value_ptr) };
            // Check if it's a pid (i.e. literal integer)
            if (auto number = value.to_uint(); number.has_value()) {
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

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
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

int Shell::builtin_unset(int argc, char const** argv)
{
    Vector<String> vars;

    Core::ArgsParser parser;
    parser.add_positional_argument(vars, "List of variables", "variables", Core::ArgsParser::Required::Yes);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    bool did_touch_path = false;
    for (auto& value : vars) {
        if (!did_touch_path && value == "PATH"sv)
            did_touch_path = true;

        if (lookup_local_variable(value)) {
            unset_local_variable(value);
        } else {
            unsetenv(value.characters());
        }
    }

    if (did_touch_path)
        cache_path();

    return 0;
}

int Shell::builtin_not(int argc, char const** argv)
{
    // FIXME: Use ArgsParser when it can collect unrelated -arguments too.
    if (argc == 1)
        return 1;

    AST::Command command;
    for (size_t i = 1; i < (size_t)argc; ++i)
        command.argv.append(argv[i]);

    auto commands = expand_aliases({ move(command) });
    int exit_code = 1;
    auto found_a_job = false;
    for (auto& job : run_commands(commands)) {
        found_a_job = true;
        block_on_job(job);
        exit_code = job.exit_code();
    }
    // In case it was a function.
    if (!found_a_job)
        exit_code = last_return_code.value_or(0);
    return exit_code == 0 ? 1 : 0;
}

int Shell::builtin_kill(int argc, char const** argv)
{
    // Simply translate the arguments and pass them to `kill'
    Vector<String> replaced_values;
    auto kill_path = Core::File::resolve_executable_from_environment("kill"sv);
    if (!kill_path.has_value()) {
        warnln("kill: `kill' not found in PATH");
        return 126;
    }
    replaced_values.append(kill_path.release_value());
    for (auto i = 1; i < argc; ++i) {
        if (auto job_id = resolve_job_spec({ argv[i], strlen(argv[1]) }); job_id.has_value()) {
            auto job = find_job(job_id.value());
            if (job) {
                replaced_values.append(String::number(job->pid()));
            } else {
                warnln("kill: Job with pid {} not found", job_id.value());
                return 1;
            }
        } else {
            replaced_values.append(argv[i]);
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

bool Shell::run_builtin(const AST::Command& command, NonnullRefPtrVector<AST::Rewiring> const& rewirings, int& retval)
{
    if (command.argv.is_empty())
        return false;

    if (!has_builtin(command.argv.first()))
        return false;

    Vector<char const*> argv;
    for (auto& arg : command.argv)
        argv.append(arg.characters());

    argv.append(nullptr);

    StringView name = command.argv.first();

    SavedFileDescriptors fds { rewirings };

    for (auto& rewiring : rewirings) {
        int rc = dup2(rewiring.old_fd, rewiring.new_fd);
        if (rc < 0) {
            perror("dup2(run)");
            return false;
        }
    }

    Core::EventLoop loop;
    setup_signals();

    if (name == ":"sv)
        name = "noop"sv;

#define __ENUMERATE_SHELL_BUILTIN(builtin)                               \
    if (name == #builtin) {                                              \
        retval = builtin_##builtin(argv.size() - 1, argv.data());        \
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

int Shell::builtin_argsparser_parse(int argc, char const** argv)
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

    Vector<char const*> arguments;
    Variant<Core::ArgsParser::Option, Core::ArgsParser::Arg, Empty> current;
    String current_variable;
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

    auto try_convert = [](StringView value, Type type) -> Optional<RefPtr<AST::Value>> {
        switch (type) {
        case Type::Bool:
            return AST::make_ref_counted<AST::StringValue>("true");
        case Type::String:
            return AST::make_ref_counted<AST::StringValue>(value);
        case Type::I32:
            if (auto number = value.to_int(); number.has_value())
                return AST::make_ref_counted<AST::StringValue>(String::number(*number));

            warnln("Invalid value for type i32: {}", value);
            return {};
        case Type::U32:
        case Type::Size:
            if (auto number = value.to_uint(); number.has_value())
                return AST::make_ref_counted<AST::StringValue>(String::number(*number));

            warnln("Invalid value for type u32|size: {}", value);
            return {};
        case Type::Double: {
            String string = value;
            char* endptr = nullptr;
            auto number = strtod(string.characters(), &endptr);
            if (endptr != string.characters() + string.length()) {
                warnln("Invalid value for type double: {}", value);
                return {};
            }

            return AST::make_ref_counted<AST::StringValue>(String::number(number));
        }
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto enlist = [&](auto name, auto value) -> NonnullRefPtr<AST::Value> {
        auto variable = lookup_local_variable(name);
        if (variable) {
            auto list = variable->resolve_as_list(*this);
            auto new_value = value->resolve_as_string(*this);
            list.append(move(new_value));
            return make_ref_counted<AST::ListValue>(move(list));
        }
        return *value;
    };
    auto commit = [&] {
        return current.visit(
            [&](Core::ArgsParser::Option& option) {
                if (!option.long_name && !option.short_name) {
                    warnln("Defined option must have at least one of --long-name or --short-name");
                    return false;
                }
                option.accept_value = [&, current_variable, treat_arg_as_list, type](auto value) {
                    auto result = try_convert({ value, strlen(value) }, type);
                    if (result.has_value()) {
                        auto value = result.release_value();
                        if (treat_arg_as_list)
                            value = enlist(current_variable, move(value));
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
                arg.accept_value = [&, current_variable, treat_arg_as_list, type](auto value) {
                    auto result = try_convert({ value, strlen(value) }, type);
                    if (result.has_value()) {
                        auto value = result.release_value();
                        if (treat_arg_as_list)
                            value = enlist(current_variable, move(value));
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
        .help_string = "Stop processing arguments after a non-argument parameter is seen",
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
        .accept_value = [&](auto value) {
            user_parser.set_general_help(value);
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
        .accept_value = [&](auto name) {
            if (current.has<Empty>()) {
                warnln("Must be defining an argument or option to use --type");
                return false;
            }

            StringView ty { name, strlen(name) };
            if (ty == "bool") {
                if (auto option = current.get_pointer<Core::ArgsParser::Option>()) {
                    if (option->value_name != nullptr) {
                        warnln("Type 'bool' does not apply to options with a value (value name is set to {})", option->value_name);
                        return false;
                    }
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

            if (type == Type::Bool)
                set_local_variable(current_variable, make_ref_counted<AST::StringValue>("false"), true);
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the help string of the option or argument being defined",
        .long_name = "help-string",
        .value_name = "string",
        .accept_value = [&](auto value) {
            return current.visit(
                [](Empty) {
                    warnln("Must be defining an option or argument to use --help-string");
                    return false;
                },
                [&](auto& option) {
                    option.help_string = value;
                    return true;
                });
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the long name of the option being defined",
        .long_name = "long-name",
        .value_name = "name",
        .accept_value = [&](auto value) {
            auto option = current.get_pointer<Core::ArgsParser::Option>();
            if (!option) {
                warnln("Must be defining an option to use --long-name");
                return false;
            }
            if (option->long_name) {
                warnln("Repeated application of --long-name is not allowed, current option has long name set to \"{}\"", option->long_name);
                return false;
            }
            option->long_name = value;
            return true;
        },
    });
    parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Set the short name of the option being defined",
        .long_name = "short-name",
        .value_name = "char",
        .accept_value = [&](auto value) {
            auto option = current.get_pointer<Core::ArgsParser::Option>();
            if (!option) {
                warnln("Must be defining an option to use --short-name");
                return false;
            }
            if (strlen(value) != 1) {
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
        .accept_value = [&](auto value) {
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

                    option.value_name = value;
                    return true;
                },
                [&](Core::ArgsParser::Arg& arg) {
                    if (arg.name) {
                        warnln("Repeated application of --value-name is not allowed, current argument has value name set to \"{}\"", arg.name);
                        return false;
                    }

                    arg.name = value;
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
        .help_string = "Set the minimum required number of positional arguments for the argument being described",
        .long_name = "min",
        .value_name = "n",
        .accept_value = [&](auto value) {
            auto arg = current.get_pointer<Core::ArgsParser::Arg>();
            if (!arg) {
                warnln("Must be describing a positional argument to use --min");
                return false;
            }

            auto number = StringView { value, strlen(value) }.to_uint();
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
        .help_string = "Set the maximum required number of positional arguments for the argument being described",
        .long_name = "max",
        .value_name = "n",
        .accept_value = [&](auto value) {
            auto arg = current.get_pointer<Core::ArgsParser::Arg>();
            if (!arg) {
                warnln("Must be describing a positional argument to use --max");
                return false;
            }

            auto number = StringView { value, strlen(value) }.to_uint();
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
    parser.add_positional_argument(arguments, "Arguments to parse via the described ArgsParser configuration", "arg", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char* const*>(argv), Core::ArgsParser::FailureBehavior::Ignore))
        return 2;

    if (!commit())
        return 2;

    if (!user_parser.parse(static_cast<int>(arguments.size()), const_cast<char* const*>(arguments.data()), Core::ArgsParser::FailureBehavior::Ignore))
        return 1;

    return 0;
}

bool Shell::has_builtin(StringView name) const
{
    if (name == ":"sv)
        return true;

#define __ENUMERATE_SHELL_BUILTIN(builtin) \
    if (name == #builtin) {                \
        return true;                       \
    }

    ENUMERATE_SHELL_BUILTINS();

#undef __ENUMERATE_SHELL_BUILTIN
    return false;
}
}
