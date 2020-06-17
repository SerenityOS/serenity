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
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <signal.h>
#include <sys/wait.h>

extern RefPtr<Line::Editor> editor;

int Shell::builtin_bg(int argc, const char** argv)
{
    int job_id = -1;

    Core::ArgsParser parser;
    parser.add_positional_argument(job_id, "Job ID to run in background", "job-id", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    if (job_id == -1 && !jobs.is_empty())
        job_id = find_last_job_id();

    auto* job = const_cast<Job*>(find_job(job_id));

    if (!job) {
        if (job_id == -1) {
            fprintf(stderr, "bg: no current job\n");
        } else {
            fprintf(stderr, "bg: job with id %d not found\n", job_id);
        }
        return 1;
    }

    job->set_running_in_background(true);

    dbg() << "Resuming " << job->pid() << " (" << job->cmd() << ")";
    fprintf(stderr, "Resuming job %llu - %s\n", job->job_id(), job->cmd().characters());

    if (killpg(job->pgid(), SIGCONT) < 0) {
        perror("killpg");
        return 1;
    }

    return 0;
}

int Shell::builtin_cd(int argc, const char** argv)
{
    const char* arg_path = nullptr;

    Core::ArgsParser parser;
    parser.add_positional_argument(arg_path, "Path to change to", "path", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    String new_path;

    if (!arg_path) {
        new_path = home;
        if (cd_history.is_empty() || cd_history.last() != home)
            cd_history.enqueue(home);
    } else {
        if (cd_history.is_empty() || cd_history.last() != arg_path)
            cd_history.enqueue(arg_path);
        if (strcmp(arg_path, "-") == 0) {
            char* oldpwd = getenv("OLDPWD");
            if (oldpwd == nullptr)
                return 1;
            new_path = oldpwd;
        } else if (arg_path[0] == '/') {
            new_path = argv[1];
        } else {
            StringBuilder builder;
            builder.append(cwd);
            builder.append('/');
            builder.append(arg_path);
            new_path = builder.to_string();
        }
    }

    auto real_path = Core::File::real_path_for(new_path);
    if (real_path.is_empty()) {
        fprintf(stderr, "Invalid path '%s'\n", new_path.characters());
        return 1;
    }
    const char* path = real_path.characters();

    int rc = chdir(path);
    if (rc < 0) {
        if (errno == ENOTDIR) {
            fprintf(stderr, "Not a directory: %s\n", path);
        } else {
            fprintf(stderr, "chdir(%s) failed: %s\n", path, strerror(errno));
        }
        return 1;
    }
    setenv("OLDPWD", cwd.characters(), 1);
    cwd = real_path;
    setenv("PWD", cwd.characters(), 1);
    return 0;
}

int Shell::builtin_cdh(int argc, const char** argv)
{
    int index = -1;

    Core::ArgsParser parser;
    parser.add_positional_argument(index, "Index of the cd history entry (leave out for a list)", "index", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    if (index == -1) {
        if (cd_history.is_empty()) {
            fprintf(stderr, "cdh: no history available\n");
            return 0;
        }

        for (ssize_t i = cd_history.size() - 1; i >= 0; --i)
            printf("%lu: %s\n", cd_history.size() - i, cd_history.at(i).characters());
        return 0;
    }

    if (index < 1 || (size_t)index > cd_history.size()) {
        fprintf(stderr, "cdh: history index out of bounds: %d not in (0, %zu)\n", index, cd_history.size());
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

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    // -v implies -p
    print = print || number_when_printing;

    if (print) {
        if (!paths.is_empty()) {
            fprintf(stderr, "dirs: 'print' and 'number' are not allowed when any path is specified");
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

int Shell::builtin_exit(int argc, const char** argv)
{
    int exit_code = 0;
    Core::ArgsParser parser;
    parser.add_positional_argument(exit_code, "Exit code", "code", Core::ArgsParser::Required::No);
    if (!parser.parse(argc, const_cast<char**>(argv)))
        return 1;

    if (!jobs.is_empty()) {
        if (!m_should_ignore_jobs_on_next_exit) {
            fprintf(stderr, "Shell: You have %zu active job%s, run 'exit' again to really exit.\n", jobs.size(), jobs.size() > 1 ? "s" : "");
            m_should_ignore_jobs_on_next_exit = true;
            return 1;
        }
    }
    stop_all_jobs();
    save_history();
    printf("Good-bye!\n");
    exit(exit_code);
    return 0;
}

int Shell::builtin_export(int argc, const char** argv)
{
    Vector<const char*> vars;

    Core::ArgsParser parser;
    parser.add_positional_argument(vars, "List of variable[=value]'s", "values", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
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

int Shell::builtin_fg(int argc, const char** argv)
{
    int job_id = -1;

    Core::ArgsParser parser;
    parser.add_positional_argument(job_id, "Job ID to bring to foreground", "job-id", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    if (job_id == -1 && !jobs.is_empty())
        job_id = find_last_job_id();

    auto* job = const_cast<Job*>(find_job(job_id));

    if (!job) {
        if (job_id == -1) {
            fprintf(stderr, "fg: no current job\n");
        } else {
            fprintf(stderr, "fg: job with id %d not found\n", job_id);
        }
        return 1;
    }

    job->set_running_in_background(false);

    dbg() << "Resuming " << job->pid() << " (" << job->cmd() << ")";
    fprintf(stderr, "Resuming job %llu - %s\n", job->job_id(), job->cmd().characters());

    if (killpg(job->pgid(), SIGCONT) < 0) {
        perror("killpg");
        return 1;
    }

    block_on_job(job);

    return job->exit_code();
}

int Shell::builtin_disown(int argc, const char** argv)
{
    Vector<const char*> str_job_ids;

    Core::ArgsParser parser;
    parser.add_positional_argument(str_job_ids, "Id of the jobs to disown (omit for current job)", "job_ids", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    Vector<size_t> job_ids;
    for (auto& job_id : str_job_ids) {
        auto id = StringView(job_id).to_uint();
        if (id.has_value())
            job_ids.append(id.value());
        else
            fprintf(stderr, "disown: Invalid job id %s\n", job_id);
    }

    if (job_ids.is_empty()) {
        u64 id = 0;
        for (auto& job : jobs)
            id = max(id, job.value->job_id());
        job_ids.append(id);
    }

    Vector<size_t> keys_of_jobs_to_disown;

    for (auto id : job_ids) {
        bool found = false;
        for (auto& entry : jobs) {
            if (entry.value->job_id() == id) {
                keys_of_jobs_to_disown.append(entry.key);
                found = true;
                break;
            }
        }
        if (!found) {
            fprintf(stderr, "disown: job with id %zu not found\n", id);
        }
    }
    if (keys_of_jobs_to_disown.is_empty()) {
        if (str_job_ids.is_empty()) {
            fprintf(stderr, "disown: no current job\n");
        }
        // An error message has already been printed about the nonexistence of each listed job.
        return 1;
    }
    for (auto job_index : keys_of_jobs_to_disown) {
        auto job = jobs.get(job_index).value();

        job->deactivate();

        if (!job->is_running_in_background())
            fprintf(stderr, "disown warning: job %llu is currently not running, 'kill -%d %d' to make it continue\n", job->job_id(), SIGCONT, job->pid());

        jobs.remove(job_index);
    }

    return 0;
}

int Shell::builtin_history(int, const char**)
{
    for (size_t i = 0; i < editor->history().size(); ++i) {
        printf("%6zu  %s\n", i, editor->history()[i].characters());
    }
    return 0;
}

int Shell::builtin_jobs(int argc, const char** argv)
{
    bool list = false, show_pid = false;

    Core::ArgsParser parser;
    parser.add_option(list, "List all information about jobs", "list", 'l');
    parser.add_option(show_pid, "Display the PID of the jobs", "pid", 'p');

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    enum {
        Basic,
        OnlyPID,
        ListAll,
    } mode { Basic };

    if (show_pid)
        mode = OnlyPID;

    if (list)
        mode = ListAll;

    for (auto& job : jobs) {
        auto pid = job.value->pid();
        int wstatus;
        auto rc = waitpid(pid, &wstatus, WNOHANG);
        if (rc == -1) {
            perror("waitpid");
            return 1;
        }
        auto status = "running";

        if (rc != 0) {
            if (WIFEXITED(wstatus))
                status = "exited";

            if (WIFSTOPPED(wstatus))
                status = "stopped";

            if (WIFSIGNALED(wstatus))
                status = "signaled";
        }

        char background_indicator = '-';

        if (job.value->is_running_in_background())
            background_indicator = '+';

        switch (mode) {
        case Basic:
            printf("[%llu] %c %s %s\n", job.value->job_id(), background_indicator, status, job.value->cmd().characters());
            break;
        case OnlyPID:
            printf("[%llu] %c %d %s %s\n", job.value->job_id(), background_indicator, pid, status, job.value->cmd().characters());
            break;
        case ListAll:
            printf("[%llu] %c %d %d %s %s\n", job.value->job_id(), background_indicator, pid, job.value->pgid(), status, job.value->cmd().characters());
            break;
        }
    }

    return 0;
}

int Shell::builtin_popd(int argc, const char** argv)
{
    if (directory_stack.size() <= 1) {
        fprintf(stderr, "Shell: popd: directory stack empty\n");
        return 1;
    }

    bool should_not_switch = false;
    String path = directory_stack.take_last();

    Core::ArgsParser parser;
    parser.add_option(should_not_switch, "Do not switch dirs", "no-switch", 'n');

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    bool should_switch = !should_not_switch;

    // When no arguments are given, popd removes the top directory from the stack and performs a cd to the new top directory.
    if (argc == 1) {
        int rc = chdir(path.characters());
        if (rc < 0) {
            fprintf(stderr, "chdir(%s) failed: %s\n", path.characters(), strerror(errno));
            return 1;
        }

        cwd = path;
        return 0;
    }

    LexicalPath lexical_path(path.characters());
    if (!lexical_path.is_valid()) {
        fprintf(stderr, "LexicalPath failed to canonicalize '%s'\n", path.characters());
        return 1;
    }

    const char* real_path = lexical_path.string().characters();

    struct stat st;
    int rc = stat(real_path, &st);
    if (rc < 0) {
        fprintf(stderr, "stat(%s) failed: %s\n", real_path, strerror(errno));
        return 1;
    }

    if (!S_ISDIR(st.st_mode)) {
        fprintf(stderr, "Not a directory: %s\n", real_path);
        return 1;
    }

    if (should_switch) {
        int rc = chdir(real_path);
        if (rc < 0) {
            fprintf(stderr, "chdir(%s) failed: %s\n", real_path, strerror(errno));
            return 1;
        }

        cwd = lexical_path.string();
    }

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
            fprintf(stderr, "pushd: no other directory\n");
            return 1;
        }

        String dir1 = directory_stack.take_first();
        String dir2 = directory_stack.take_first();
        directory_stack.insert(0, dir2);
        directory_stack.insert(1, dir1);

        int rc = chdir(dir2.characters());
        if (rc < 0) {
            fprintf(stderr, "chdir(%s) failed: %s\n", dir2.characters(), strerror(errno));
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
            path_builder.appendf("%s/%s", cwd.characters(), argv[1]);
        }
    } else if (argc == 3) {
        directory_stack.append(cwd.characters());
        for (int i = 1; i < argc; i++) {
            const char* arg = argv[i];

            if (arg[0] != '-') {
                if (arg[0] == '/') {
                    path_builder.append(arg);
                } else
                    path_builder.appendf("%s/%s", cwd.characters(), arg);
            }

            if (!strcmp(arg, "-n"))
                should_switch = false;
        }
    }

    LexicalPath lexical_path(path_builder.to_string());
    if (!lexical_path.is_valid()) {
        fprintf(stderr, "LexicalPath failed to canonicalize '%s'\n", path_builder.to_string().characters());
        return 1;
    }

    const char* real_path = lexical_path.string().characters();

    struct stat st;
    int rc = stat(real_path, &st);
    if (rc < 0) {
        fprintf(stderr, "stat(%s) failed: %s\n", real_path, strerror(errno));
        return 1;
    }

    if (!S_ISDIR(st.st_mode)) {
        fprintf(stderr, "Not a directory: %s\n", real_path);
        return 1;
    }

    if (should_switch) {
        int rc = chdir(real_path);
        if (rc < 0) {
            fprintf(stderr, "chdir(%s) failed: %s\n", real_path, strerror(errno));
            return 1;
        }

        cwd = lexical_path.string();
    }

    return 0;
}

int Shell::builtin_pwd(int, const char**)
{
    print_path(cwd);
    fputc('\n', stdout);
    return 0;
}

int Shell::builtin_time(int argc, const char** argv)
{
    Vector<const char*> args;

    Core::ArgsParser parser;
    parser.add_positional_argument(args, "Command to execute with arguments", "command", Core::ArgsParser::Required::Yes);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    StringBuilder builder;
    builder.join(' ', args);

    Core::ElapsedTimer timer;
    timer.start();
    // TODO: Exit code
    run_command(builder.string_view());
    fprintf(stderr, "Time: %d ms\n", timer.elapsed());
    return 0;
}

int Shell::builtin_umask(int argc, const char** argv)
{
    const char* mask_text = nullptr;

    Core::ArgsParser parser;
    parser.add_positional_argument(mask_text, "New mask (omit to get current mask)", "octal-mask", Core::ArgsParser::Required::No);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
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

    fprintf(stderr, "umask: Invalid mask '%s'\n", mask_text);
    return 1;
}

int Shell::builtin_unset(int argc, const char** argv)
{
    Vector<const char*> vars;

    Core::ArgsParser parser;
    parser.add_positional_argument(vars, "List of variables", "variables", Core::ArgsParser::Required::Yes);

    if (!parser.parse(argc, const_cast<char**>(argv), false))
        return 1;

    for (auto& value : vars) {
        if (lookup_local_variable(value)) {
            unset_local_variable(value);
        } else {
            unsetenv(value);
        }
    }

    return 0;
}

bool Shell::run_builtin(int argc, const char** argv, int& retval)
{
    if (argc == 0)
        return false;

    StringView name { argv[0] };

#define __ENUMERATE_SHELL_BUILTIN(builtin)      \
    if (name == #builtin) {                     \
        retval = builtin_##builtin(argc, argv); \
        return true;                            \
    }

    ENUMERATE_SHELL_BUILTINS();

#undef __ENUMERATE_SHELL_BUILTIN

    return false;
}
