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

int Shell::builtin_noop(int, const char**)
{
    return 0;
}

int Shell::builtin_dump(int argc, const char** argv)
{
    if (argc != 2)
        return 1;

    Parser { argv[1] }.parse()->dump(0);
    return 0;
}

int Shell::builtin_alias(int argc, const char** argv)
{
    Vector<const char*> arguments;

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
        auto parts = String { argument }.split_limit('=', 2, true);
        if (parts.size() == 1) {
            auto alias = m_aliases.get(parts[0]);
            if (alias.has_value()) {
                printf("%s=%s\n", escape_token(parts[0]).characters(), escape_token(alias.value()).characters());
            } else {
                fail = true;
            }
        } else {
            m_aliases.set(parts[0], parts[1]);
            add_entry_to_cache(parts[0]);
        }
    }

    return fail ? 1 : 0;
}

int Shell::builtin_unalias(int argc, const char** argv)
{
    bool remove_all { false };
    Vector<const char*> arguments;

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

int Shell::builtin_bg(int argc, const char** argv)
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

int Shell::builtin_type(int argc, const char** argv)
{
    Vector<const char*> commands;
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
            printf("%s is a function\n", command);
            if (!dont_show_function_source) {
                StringBuilder builder;
                builder.append(fn.name);
                builder.append("(");
                for (size_t i = 0; i < fn.arguments.size(); i++) {
                    builder.append(fn.arguments[i]);
                    if (!(i == fn.arguments.size() - 1))
                        builder.append(" ");
                }
                builder.append(") {\n");
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
            printf("%s is a shell builtin\n", command);
            continue;
        }

        // check if its an executable in PATH
        auto fullpath = Core::find_executable_in_path(command);
        if (!fullpath.is_empty()) {
            printf("%s is %s\n", command, escape_token(fullpath).characters());
            continue;
        }
        something_not_found = true;
        printf("type: %s not found\n", command);
    }

    if (something_not_found)
        return 1;
    else
        return 0;
}

int Shell::builtin_cd(int argc, const char** argv)
{
    const char* arg_path = nullptr;

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
    const char* path = path_relative_to_current_directory.characters();

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

int Shell::builtin_cdh(int argc, const char** argv)
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

    const char* path = cd_history.at(cd_history.size() - index).characters();
    const char* cd_args[] = { "cd", path, nullptr };
    return Shell::builtin_cd(2, cd_args);
}

int Shell::builtin_dirs(int argc, const char** argv)
{
    // The first directory in the stack is ALWAYS the current directory
    directory_stack.at(0) = cwd.characters();

    bool clear = false;
    bool print = false;
    bool number_when_printing = false;
    char separator = ' ';

    Vector<const char*> paths;

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

int Shell::builtin_exec(int argc, const char** argv)
{
    if (argc < 2) {
        warnln("Shell: No command given to exec");
        return 1;
    }

    Vector<const char*> argv_vector;
    argv_vector.append(argv + 1, argc - 1);
    argv_vector.append(nullptr);

    execute_process(move(argv_vector));
}

int Shell::builtin_exit(int argc, const char** argv)
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

int Shell::builtin_export(int argc, const char** argv)
{
    Vector<const char*> vars;

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
        auto parts = String { value }.split_limit('=', 2);

        if (parts.size() == 1) {
            auto value = lookup_local_variable(parts[0]);
            if (value) {
                auto values = value->resolve_as_list(*this);
                StringBuilder builder;
                builder.join(" ", values);
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

int Shell::builtin_glob(int argc, const char** argv)
{
    Vector<const char*> globs;
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

int Shell::builtin_fg(int argc, const char** argv)
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

int Shell::builtin_disown(int argc, const char** argv)
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

    Vector<const Job*> jobs_to_disown;

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

int Shell::builtin_history(int, const char**)
{
    for (size_t i = 0; i < m_editor->history().size(); ++i) {
        printf("%6zu  %s\n", i, m_editor->history()[i].entry.characters());
    }
    return 0;
}

int Shell::builtin_jobs(int argc, const char** argv)
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

int Shell::builtin_popd(int argc, const char** argv)
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

int Shell::builtin_pushd(int argc, const char** argv)
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
            path_builder.append(argv[1]);
        } else {
            path_builder.appendff("{}/{}", cwd, argv[1]);
        }
    } else if (argc == 3) {
        directory_stack.append(cwd.characters());
        for (int i = 1; i < argc; i++) {
            const char* arg = argv[i];

            if (arg[0] != '-') {
                if (arg[0] == '/') {
                    path_builder.append(arg);
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

int Shell::builtin_pwd(int, const char**)
{
    print_path(cwd);
    fputc('\n', stdout);
    return 0;
}

int Shell::builtin_setopt(int argc, const char** argv)
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

int Shell::builtin_shift(int argc, const char** argv)
{
    int count = 1;

    Core::ArgsParser parser;
    parser.add_positional_argument(count, "Shift count", "count", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (count < 1)
        return 0;

    auto argv_ = lookup_local_variable("ARGV");
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

int Shell::builtin_source(int argc, const char** argv)
{
    const char* file_to_source = nullptr;
    Vector<const char*> args;

    Core::ArgsParser parser;
    parser.add_positional_argument(file_to_source, "File to read commands from", "path");
    parser.add_positional_argument(args, "ARGV for the sourced file", "args", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv)))
        return 1;

    Vector<String> string_argv;
    for (auto& arg : args)
        string_argv.append(arg);

    auto previous_argv = lookup_local_variable("ARGV");
    ScopeGuard guard { [&] {
        if (!args.is_empty())
            set_local_variable("ARGV", move(previous_argv));
    } };

    if (!args.is_empty())
        set_local_variable("ARGV", AST::make_ref_counted<AST::ListValue>(move(string_argv)));

    if (!run_file(file_to_source, true))
        return 126;

    return 0;
}

int Shell::builtin_time(int argc, const char** argv)
{
    Vector<const char*> args;

    int number_of_iterations = 1;

    Core::ArgsParser parser;
    parser.add_option(number_of_iterations, "Number of iterations", "iterations", 'n', "iterations");
    parser.set_stop_on_first_non_option(true);
    parser.add_positional_argument(args, "Command to execute with arguments", "command", Core::ArgsParser::Required::Yes);

    if (!parser.parse(argc, const_cast<char**>(argv), Core::ArgsParser::FailureBehavior::PrintUsage))
        return 1;

    if (number_of_iterations < 1)
        return 1;

    AST::Command command;
    for (auto& arg : args)
        command.argv.append(arg);

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
        warnln("Command:         {}", String::join(' ', args));
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

int Shell::builtin_umask(int argc, const char** argv)
{
    const char* mask_text = nullptr;

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

int Shell::builtin_wait(int argc, const char** argv)
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
        for (const auto& it : jobs)
            jobs_to_wait_for.append(it.value);
    }

    for (auto& job : jobs_to_wait_for) {
        job->set_running_in_background(false);
        block_on_job(job);
    }

    return 0;
}

int Shell::builtin_unset(int argc, const char** argv)
{
    Vector<const char*> vars;

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
            unsetenv(value);
        }
    }

    if (did_touch_path)
        cache_path();

    return 0;
}

int Shell::builtin_not(int argc, const char** argv)
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

int Shell::builtin_kill(int argc, const char** argv)
{
    // Simply translate the arguments and pass them to `kill'
    Vector<String> replaced_values;
    auto kill_path = find_in_path("kill");
    if (kill_path.is_empty()) {
        warnln("kill: `kill' not found in PATH");
        return 126;
    }
    replaced_values.append(kill_path);
    for (auto i = 1; i < argc; ++i) {
        if (auto job_id = resolve_job_spec(argv[i]); job_id.has_value()) {
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

bool Shell::run_builtin(const AST::Command& command, const NonnullRefPtrVector<AST::Rewiring>& rewirings, int& retval)
{
    if (command.argv.is_empty())
        return false;

    if (!has_builtin(command.argv.first()))
        return false;

    Vector<const char*> argv;
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
