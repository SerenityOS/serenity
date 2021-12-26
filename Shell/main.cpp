/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "GlobalState.h"
#include "Parser.h"
#include <AK/FileSystemPath.h>
#include <AK/Function.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibLine/Editor.h>
#include <errno.h>
#include <fcntl.h>
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

//#define SH_DEBUG

GlobalState g;
static Line::Editor editor { Line::Configuration { Line::Configuration::UnescapedSpaces } };

static int run_command(const String&);
void cache_path();

static String prompt()
{
    auto* ps1 = getenv("PROMPT");
    if (!ps1) {
        if (g.uid == 0)
            return "# ";

        StringBuilder builder;
        builder.appendf("\033]0;%s@%s:%s\007", g.username.characters(), g.hostname, g.cwd.characters());
        builder.appendf("\033[31;1m%s\033[0m@\033[37;1m%s\033[0m:\033[32;1m%s\033[0m$> ", g.username.characters(), g.hostname, g.cwd.characters());
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
                builder.append(g.username);
                break;
            case 'h':
                builder.append(g.hostname);
                break;
            case 'w': {
                String home_path = getenv("HOME");
                if (g.cwd.starts_with(home_path)) {
                    builder.append('~');
                    builder.append(g.cwd.substring_view(home_path.length(), g.cwd.length() - home_path.length()));
                } else {
                    builder.append(g.cwd);
                }
                break;
            }
            case 'p':
                builder.append(g.uid == 0 ? '#' : '$');
                break;
            }
            continue;
        }
        builder.append(*ptr);
    }
    return builder.to_string();
}

static int sh_pwd(int, const char**)
{
    printf("%s\n", g.cwd.characters());
    return 0;
}

static int sh_exit(int, const char**)
{
    printf("Good-bye!\n");
    exit(0);
    return 0;
}

static int sh_export(int argc, const char** argv)
{
    if (argc == 1) {
        for (int i = 0; environ[i]; ++i)
            puts(environ[i]);
        return 0;
    }
    auto parts = String(argv[1]).split('=');
    if (parts.size() != 2) {
        fprintf(stderr, "usage: export variable=value\n");
        return 1;
    }

    int setenv_return = setenv(parts[0].characters(), parts[1].characters(), 1);

    if (setenv_return == 0 && parts[0] == "PATH")
        cache_path();

    return setenv_return;
}

static int sh_unset(int argc, const char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: unset variable\n");
        return 1;
    }

    unsetenv(argv[1]);
    return 0;
}

static String expand_tilde(const String& expression)
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

static int sh_cd(int argc, const char** argv)
{
    if (argc > 2) {
        fprintf(stderr, "cd: too many arguments\n");
        return 1;
    }

    String new_path;

    if (argc == 1) {
        new_path = g.home;
        if (g.cd_history.is_empty() || g.cd_history.last() != g.home)
            g.cd_history.enqueue(g.home);
    } else {
        if (g.cd_history.is_empty() || g.cd_history.last() != argv[1])
            g.cd_history.enqueue(argv[1]);
        if (strcmp(argv[1], "-") == 0) {
            char* oldpwd = getenv("OLDPWD");
            if (oldpwd == nullptr)
                return 1;
            new_path = oldpwd;
        } else if (argv[1][0] == '/') {
            new_path = argv[1];
        } else {
            StringBuilder builder;
            builder.append(g.cwd);
            builder.append('/');
            builder.append(argv[1]);
            new_path = builder.to_string();
        }
    }

    FileSystemPath canonical_path(new_path);
    if (!canonical_path.is_valid()) {
        printf("FileSystemPath failed to canonicalize '%s'\n", new_path.characters());
        return 1;
    }
    const char* path = canonical_path.string().characters();

    struct stat st;
    int rc = stat(path, &st);
    if (rc < 0) {
        printf("stat(%s) failed: %s\n", path, strerror(errno));
        return 1;
    }
    if (!S_ISDIR(st.st_mode)) {
        printf("Not a directory: %s\n", path);
        return 1;
    }
    rc = chdir(path);
    if (rc < 0) {
        printf("chdir(%s) failed: %s\n", path, strerror(errno));
        return 1;
    }
    setenv("OLDPWD", g.cwd.characters(), 1);
    g.cwd = canonical_path.string();
    setenv("PWD", g.cwd.characters(), 1);
    return 0;
}

static int sh_cdh(int argc, const char** argv)
{
    if (argc > 2) {
        fprintf(stderr, "usage: cdh [index]\n");
        return 1;
    }

    if (argc == 1) {
        if (g.cd_history.size() == 0) {
            printf("cdh: no history available\n");
            return 0;
        }

        for (int i = g.cd_history.size() - 1; i >= 0; --i)
            printf("%lu: %s\n", g.cd_history.size() - i, g.cd_history.at(i).characters());
        return 0;
    }

    bool ok;
    size_t cd_history_index = String(argv[1]).to_uint(ok);

    if (!ok || cd_history_index < 1 || cd_history_index > g.cd_history.size()) {
        fprintf(stderr, "usage: cdh [index]\n");
        return 1;
    }

    const char* path = g.cd_history.at(g.cd_history.size() - cd_history_index).characters();
    const char* cd_args[] = { "cd", path };
    return sh_cd(2, cd_args);
}

static int sh_history(int, const char**)
{
    for (size_t i = 0; i < editor.history().size(); ++i) {
        printf("%6zu  %s\n", i, editor.history()[i].characters());
    }
    return 0;
}

static int sh_time(int argc, const char** argv)
{
    if (argc == 1) {
        printf("usage: time <command>\n");
        return 0;
    }
    StringBuilder builder;
    for (int i = 1; i < argc; ++i) {
        builder.append(argv[i]);
        if (i != argc - 1)
            builder.append(' ');
    }
    Core::ElapsedTimer timer;
    timer.start();
    int exit_code = run_command(builder.to_string());
    printf("Time: %d ms\n", timer.elapsed());
    return exit_code;
}

static int sh_umask(int argc, const char** argv)
{
    if (argc == 1) {
        mode_t old_mask = umask(0);
        printf("%#o\n", old_mask);
        umask(old_mask);
        return 0;
    }
    if (argc == 2) {
        unsigned mask;
        int matches = sscanf(argv[1], "%o", &mask);
        if (matches == 1) {
            umask(mask);
            return 0;
        }
    }
    printf("usage: umask <octal-mask>\n");
    return 0;
}

static int sh_popd(int argc, const char** argv)
{
    if (g.directory_stack.size() <= 1) {
        fprintf(stderr, "Shell: popd: directory stack empty\n");
        return 1;
    }

    bool should_switch = true;
    String path = g.directory_stack.take_last();

    // When no arguments are given, popd removes the top directory from the stack and performs a cd to the new top directory.
    if (argc == 1) {
        int rc = chdir(path.characters());
        if (rc < 0) {
            fprintf(stderr, "chdir(%s) failed: %s", path.characters(), strerror(errno));
            return 1;
        }

        g.cwd = path;
        return 0;
    }

    for (int i = 1; i < argc; i++) {
        const char* arg = argv[i];
        if (!strcmp(arg, "-n")) {
            should_switch = false;
        }
    }

    FileSystemPath canonical_path(path.characters());
    if (!canonical_path.is_valid()) {
        fprintf(stderr, "FileSystemPath failed to canonicalize '%s'\n", path.characters());
        return 1;
    }

    const char* real_path = canonical_path.string().characters();

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

        g.cwd = canonical_path.string();
    }

    return 0;
}

static int sh_pushd(int argc, const char** argv)
{
    StringBuilder path_builder;
    bool should_switch = true;

    // From the BASH reference manual: https://www.gnu.org/software/bash/manual/html_node/Directory-Stack-Builtins.html
    // With no arguments, pushd exchanges the top two directories and makes the new top the current directory.
    if (argc == 1) {
        if (g.directory_stack.size() < 2) {
            fprintf(stderr, "pushd: no other directory\n");
            return 1;
        }

        String dir1 = g.directory_stack.take_first();
        String dir2 = g.directory_stack.take_first();
        g.directory_stack.insert(0, dir2);
        g.directory_stack.insert(1, dir1);

        int rc = chdir(dir2.characters());
        if (rc < 0) {
            fprintf(stderr, "chdir(%s) failed: %s", dir2.characters(), strerror(errno));
            return 1;
        }

        g.cwd = dir2;

        return 0;
    }

    // Let's assume the user's typed in 'pushd <dir>'
    if (argc == 2) {
        g.directory_stack.append(g.cwd.characters());
        if (argv[1][0] == '/') {
            path_builder.append(argv[1]);
        } else {
            path_builder.appendf("%s/%s", g.cwd.characters(), argv[1]);
        }
    } else if (argc == 3) {
        g.directory_stack.append(g.cwd.characters());
        for (int i = 1; i < argc; i++) {
            const char* arg = argv[i];

            if (arg[0] != '-') {
                if (arg[0] == '/') {
                    path_builder.append(arg);
                } else
                    path_builder.appendf("%s/%s", g.cwd.characters(), arg);
            }

            if (!strcmp(arg, "-n"))
                should_switch = false;
        }
    }

    FileSystemPath canonical_path(path_builder.to_string());
    if (!canonical_path.is_valid()) {
        fprintf(stderr, "FileSystemPath failed to canonicalize '%s'\n", path_builder.to_string().characters());
        return 1;
    }

    const char* real_path = canonical_path.string().characters();

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

        g.cwd = canonical_path.string();
    }

    return 0;
}

static int sh_dirs(int argc, const char** argv)
{
    // The first directory in the stack is ALWAYS the current directory
    g.directory_stack.at(0) = g.cwd.characters();

    if (argc == 1) {
        for (String dir : g.directory_stack)
            printf("%s ", dir.characters());

        printf("\n");
        return 0;
    }

    bool printed = false;
    for (int i = 0; i < argc; i++) {
        const char* arg = argv[i];
        if (!strcmp(arg, "-c")) {
            for (size_t i = 1; i < g.directory_stack.size(); i++)
                g.directory_stack.remove(i);

            printed = true;
            continue;
        }
        if (!strcmp(arg, "-p") && !printed) {
            for (auto& directory : g.directory_stack)
                printf("%s\n", directory.characters());

            printed = true;
            continue;
        }
        if (!strcmp(arg, "-v") && !printed) {
            int idx = 0;
            for (auto& directory : g.directory_stack) {
                printf("%d %s\n", idx++, directory.characters());
            }

            printed = true;
            continue;
        }
    }

    return 0;
}

static bool handle_builtin(int argc, const char** argv, int& retval)
{
    if (argc == 0)
        return false;
    if (!strcmp(argv[0], "cd")) {
        retval = sh_cd(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "cdh")) {
        retval = sh_cdh(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "pwd")) {
        retval = sh_pwd(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "exit")) {
        retval = sh_exit(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "export")) {
        retval = sh_export(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "unset")) {
        retval = sh_unset(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "history")) {
        retval = sh_history(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "umask")) {
        retval = sh_umask(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "dirs")) {
        retval = sh_dirs(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "pushd")) {
        retval = sh_pushd(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "popd")) {
        retval = sh_popd(argc, argv);
        return true;
    }
    if (!strcmp(argv[0], "time")) {
        retval = sh_time(argc, argv);
        return true;
    }
    return false;
}

class FileDescriptionCollector {
public:
    FileDescriptionCollector() {}
    ~FileDescriptionCollector() { collect(); }

    void collect()
    {
        for (auto fd : m_fds)
            close(fd);
        m_fds.clear();
    }
    void add(int fd) { m_fds.append(fd); }

private:
    Vector<int, 32> m_fds;
};

class CommandTimer {
public:
    explicit CommandTimer(const String& command)
        : m_command(command)
    {
        m_timer.start();
    }
    ~CommandTimer()
    {
        dbg() << "Command \"" << m_command << "\" finished in " << m_timer.elapsed() << " ms";
    }

private:
    Core::ElapsedTimer m_timer;
    String m_command;
};

static bool is_glob(const StringView& s)
{
    for (size_t i = 0; i < s.length(); i++) {
        char c = s.characters_without_null_termination()[i];
        if (c == '*' || c == '?')
            return true;
    }
    return false;
}

static Vector<StringView> split_path(const StringView& path)
{
    Vector<StringView> parts;

    size_t substart = 0;
    for (size_t i = 0; i < path.length(); i++) {
        char ch = path.characters_without_null_termination()[i];
        if (ch != '/')
            continue;
        size_t sublen = i - substart;
        if (sublen != 0)
            parts.append(path.substring_view(substart, sublen));
        parts.append(path.substring_view(i, 1));
        substart = i + 1;
    }

    size_t taillen = path.length() - substart;
    if (taillen != 0)
        parts.append(path.substring_view(substart, taillen));

    return parts;
}

static Vector<String> expand_globs(const StringView& path, const StringView& base)
{
    auto parts = split_path(path);

    StringBuilder builder;
    builder.append(base);
    Vector<String> res;

    for (size_t i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        if (!is_glob(part)) {
            builder.append(part);
            continue;
        }

        // Found a glob.
        String new_base = builder.to_string();
        StringView new_base_v = new_base;
        if (new_base_v.is_empty())
            new_base_v = ".";
        Core::DirIterator di(new_base_v, Core::DirIterator::SkipParentAndBaseDir);

        if (di.has_error()) {
            return res;
        }

        while (di.has_next()) {
            String name = di.next_path();

            // Dotfiles have to be explicitly requested
            if (name[0] == '.' && part[0] != '.')
                continue;

            if (name.matches(part, CaseSensitivity::CaseSensitive)) {

                StringBuilder nested_base;
                nested_base.append(new_base);
                nested_base.append(name);

                StringView remaining_path = path.substring_view_starting_after_substring(part);
                Vector<String> nested_res = expand_globs(remaining_path, nested_base.to_string());
                for (auto& s : nested_res)
                    res.append(s);
            }
        }
        return res;
    }

    // Found no globs.
    String new_path = builder.to_string();
    if (access(new_path.characters(), F_OK) == 0)
        res.append(new_path);
    return res;
}

static Vector<String> expand_parameters(const StringView& param)
{
    if (!param.starts_with('$'))
        return { param };

    String variable_name = String(param.substring_view(1, param.length() - 1));
    if (variable_name == "?")
        return { String::number(g.last_return_code) };
    else if (variable_name == "$")
        return { String::number(getpid()) };

    char* env_value = getenv(variable_name.characters());
    if (env_value == nullptr)
        return { "" };

    Vector<String> res;
    String str_env_value = String(env_value);
    const auto& split_text = str_env_value.split_view(' ');
    for (auto& part : split_text)
        res.append(part);
    return res;
}

static Vector<String> process_arguments(const Vector<String>& args)
{
    Vector<String> argv_string;
    for (auto& arg : args) {
        // This will return the text passed in if it wasn't a variable
        // This lets us just loop over its values
        auto expanded_parameters = expand_parameters(arg);

        for (auto& exp_arg : expanded_parameters) {
            if (exp_arg.starts_with('~'))
                exp_arg = expand_tilde(exp_arg);

            auto expanded_globs = expand_globs(exp_arg, "");
            for (auto& path : expanded_globs)
                argv_string.append(path);

            if (expanded_globs.is_empty())
                argv_string.append(exp_arg);
        }
    }

    return argv_string;
}

static int run_command(const String& cmd)
{
    if (cmd.is_empty())
        return 0;

    if (cmd.starts_with("#"))
        return 0;

    auto commands = Parser(cmd).parse();

#ifdef SH_DEBUG
    for (auto& command : commands) {
        for (size_t i = 0; i < command.subcommands.size(); ++i) {
            for (size_t j = 0; j < i; ++j)
                dbgprintf("    ");
            for (auto& arg : command.subcommands[i].args) {
                dbgprintf("<%s> ", arg.characters());
            }
            dbgprintf("\n");
            for (auto& redirecton : command.subcommands[i].redirections) {
                for (size_t j = 0; j < i; ++j)
                    dbgprintf("    ");
                dbgprintf("  ");
                switch (redirecton.type) {
                case Redirection::Pipe:
                    dbgprintf("Pipe\n");
                    break;
                case Redirection::FileRead:
                    dbgprintf("fd:%d = FileRead: %s\n", redirecton.fd, redirecton.path.characters());
                    break;
                case Redirection::FileWrite:
                    dbgprintf("fd:%d = FileWrite: %s\n", redirecton.fd, redirecton.path.characters());
                    break;
                case Redirection::FileWriteAppend:
                    dbgprintf("fd:%d = FileWriteAppend: %s\n", redirecton.fd, redirecton.path.characters());
                    break;
                default:
                    break;
                }
            }
        }
        dbgprintf("\n");
    }
#endif

    struct termios trm;
    tcgetattr(0, &trm);

    struct SpawnedProcess {
        String name;
        pid_t pid;
    };

    int return_value = 0;

    for (auto& command : commands) {
        if (command.subcommands.is_empty())
            continue;

        FileDescriptionCollector fds;

        for (size_t i = 0; i < command.subcommands.size(); ++i) {
            auto& subcommand = command.subcommands[i];
            for (auto& redirection : subcommand.redirections) {
                switch (redirection.type) {
                case Redirection::Pipe: {
                    int pipefd[2];
                    int rc = pipe(pipefd);
                    if (rc < 0) {
                        perror("pipe");
                        return 1;
                    }
                    subcommand.rewirings.append({ STDOUT_FILENO, pipefd[1] });
                    auto& next_command = command.subcommands[i + 1];
                    next_command.rewirings.append({ STDIN_FILENO, pipefd[0] });
                    fds.add(pipefd[0]);
                    fds.add(pipefd[1]);
                    break;
                }
                case Redirection::FileWriteAppend: {
                    int fd = open(redirection.path.characters(), O_WRONLY | O_CREAT | O_APPEND, 0666);
                    if (fd < 0) {
                        perror("open");
                        return 1;
                    }
                    subcommand.rewirings.append({ redirection.fd, fd });
                    fds.add(fd);
                    break;
                }
                case Redirection::FileWrite: {
                    int fd = open(redirection.path.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
                    if (fd < 0) {
                        perror("open");
                        return 1;
                    }
                    subcommand.rewirings.append({ redirection.fd, fd });
                    fds.add(fd);
                    break;
                }
                case Redirection::FileRead: {
                    int fd = open(redirection.path.characters(), O_RDONLY);
                    if (fd < 0) {
                        perror("open");
                        return 1;
                    }
                    subcommand.rewirings.append({ redirection.fd, fd });
                    fds.add(fd);
                    break;
                }
                }
            }
        }

        Vector<SpawnedProcess> children;

        CommandTimer timer(cmd);

        for (size_t i = 0; i < command.subcommands.size(); ++i) {
            auto& subcommand = command.subcommands[i];
            Vector<String> argv_string = process_arguments(subcommand.args);
            Vector<const char*> argv;
            argv.ensure_capacity(argv_string.size());
            for (const auto& s : argv_string) {
                argv.append(s.characters());
            }
            argv.append(nullptr);

#ifdef SH_DEBUG
            for (auto& arg : argv) {
                dbgprintf("<%s> ", arg);
            }
            dbgprintf("\n");
#endif

            int retval = 0;
            if (handle_builtin(argv.size() - 1, argv.data(), retval))
                return retval;

            pid_t child = fork();
            if (!child) {
                setpgid(0, 0);
                tcsetpgrp(0, getpid());
                tcsetattr(0, TCSANOW, &g.default_termios);
                for (auto& rewiring : subcommand.rewirings) {
#ifdef SH_DEBUG
                    dbgprintf("in %s<%d>, dup2(%d, %d)\n", argv[0], getpid(), rewiring.rewire_fd, rewiring.fd);
#endif
                    int rc = dup2(rewiring.rewire_fd, rewiring.fd);
                    if (rc < 0) {
                        perror("dup2");
                        return 1;
                    }
                }

                fds.collect();

                int rc = execvp(argv[0], const_cast<char* const*>(argv.data()));
                if (rc < 0) {
                    if (errno == ENOENT) {
                        int shebang_fd = open(argv[0], O_RDONLY);
                        auto close_argv = ScopeGuard([shebang_fd]() { if (shebang_fd >= 0)  close(shebang_fd); });
                        char shebang[256] {};
                        ssize_t num_read = -1;
                        if ((shebang_fd >= 0) && ((num_read = read(shebang_fd, shebang, sizeof(shebang))) >= 2) && (StringView(shebang).starts_with("#!"))) {
                            StringView shebang_path_view(&shebang[2], num_read - 2);
                            Optional<size_t> newline_pos = shebang_path_view.find_first_of("\n\r");
                            shebang[newline_pos.has_value() ? newline_pos.value() : num_read] = '\0';
                            fprintf(stderr, "%s: Invalid interpreter \"%s\": %s\n", argv[0], &shebang[2], strerror(ENOENT));
                        } else
                            fprintf(stderr, "%s: Command not found.\n", argv[0]);
                    } else {
                        struct stat st;
                        if (stat(argv[0], &st) == 0 && S_ISDIR(st.st_mode)) {
                            fprintf(stderr, "Shell: %s: Is a directory\n", argv[0]);
                            _exit(126);
                        }
                        fprintf(stderr, "execvp(%s): %s\n", argv[0], strerror(errno));
                    }
                    _exit(126);
                }
                ASSERT_NOT_REACHED();
            }
            children.append({ argv[0], child });
        }

#ifdef SH_DEBUG
        dbgprintf("Closing fds in shell process:\n");
#endif
        fds.collect();

#ifdef SH_DEBUG
        dbgprintf("Now we gotta wait on children:\n");
        for (auto& child : children)
            dbgprintf("  %d (%s)\n", child.pid, child.name.characters());
#endif

        int wstatus = 0;

        for (size_t i = 0; i < children.size(); ++i) {
            auto& child = children[i];
            do {
                int rc = waitpid(child.pid, &wstatus, 0);
                if (rc < 0 && errno != EINTR) {
                    if (errno != ECHILD)
                        perror("waitpid");
                    break;
                }
                if (WIFEXITED(wstatus)) {
                    if (WEXITSTATUS(wstatus) != 0)
                        dbg() << "Shell: " << child.name << ":" << child.pid << " exited with status " << WEXITSTATUS(wstatus);
                    if (i == 0)
                        return_value = WEXITSTATUS(wstatus);
                } else if (WIFSTOPPED(wstatus)) {
                    fprintf(stderr, "Shell: %s(%d) %s\n", child.name.characters(), child.pid, strsignal(WSTOPSIG(wstatus)));
                } else {
                    if (WIFSIGNALED(wstatus)) {
                        printf("Shell: %s(%d) exited due to signal '%s'\n", child.name.characters(), child.pid, strsignal(WTERMSIG(wstatus)));
                    } else {
                        printf("Shell: %s(%d) exited abnormally\n", child.name.characters(), child.pid);
                    }
                }
            } while (errno == EINTR);
        }
    }

    g.last_return_code = return_value;

    // FIXME: Should I really have to tcsetpgrp() after my child has exited?
    //        Is the terminal controlling pgrp really still the PGID of the dead process?
    tcsetpgrp(0, getpid());
    tcsetattr(0, TCSANOW, &trm);
    return return_value;
}

static String get_history_path()
{
    StringBuilder builder;
    builder.append(g.home);
    builder.append("/.history");
    return builder.to_string();
}

void load_history()
{
    auto history_file = Core::File::construct(get_history_path());
    if (!history_file->open(Core::IODevice::ReadOnly))
        return;
    while (history_file->can_read_line()) {
        auto b = history_file->read_line(1024);
        // skip the newline and terminating bytes
        editor.add_to_history(String(reinterpret_cast<const char*>(b.data()), b.size() - 2));
    }
}

void save_history()
{
    auto file_or_error = Core::File::open(get_history_path(), Core::IODevice::WriteOnly, 0600);
    if (file_or_error.is_error())
        return;
    auto& file = *file_or_error.value();
    for (const auto& line : editor.history()) {
        file.write(line);
        file.write("\n");
    }
}

String escape_token(const String& token)
{
    StringBuilder builder;

    for (auto c : token) {
        switch (c) {
        case '\'':
        case '"':
        case '$':
        case '|':
        case '>':
        case '<':
        case '&':
        case '\\':
        case ' ':
            builder.append('\\');
            break;
        default:
            break;
        }
        builder.append(c);
    }

    return builder.build();
}

String unescape_token(const String& token)
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

Vector<String, 256> cached_path;
void cache_path()
{
    if (!cached_path.is_empty())
        cached_path.clear_with_capacity();

    String path = getenv("PATH");
    if (path.is_empty())
        return;

    auto directories = path.split(':');
    for (const auto& directory : directories) {
        Core::DirIterator programs(directory.characters(), Core::DirIterator::SkipDots);
        while (programs.has_next()) {
            auto program = programs.next_path();
            String program_path = String::format("%s/%s", directory.characters(), program.characters());
            if (access(program_path.characters(), X_OK) == 0)
                cached_path.append(escape_token(program.characters()));
        }
    }

    quick_sort(cached_path);
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath proc exec tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    g.uid = getuid();
    tcsetpgrp(0, getpgrp());

    editor.initialize();
    g.termios = editor.termios();
    g.default_termios = editor.default_termios();

    editor.on_tab_complete_first_token = [&](const String& token_to_complete) -> Vector<Line::CompletionSuggestion> {
        auto token = unescape_token(token_to_complete);

        auto match = binary_search(cached_path.data(), cached_path.size(), token, [](const String& token, const String& program) -> int {
            return strncmp(token.characters(), program.characters(), token.length());
        });

        if (!match) {
            // There is no executable in the $PATH starting with $token
            // Suggest local executables and directories
            String path;
            Vector<Line::CompletionSuggestion> local_suggestions;
            bool suggest_executables = true;

            ssize_t last_slash = token.length() - 1;
            while (last_slash >= 0 && token[last_slash] != '/')
                --last_slash;

            if (last_slash >= 0) {
                // Split on the last slash. We'll use the first part as the directory
                // to search and the second part as the token to complete.
                path = token.substring(0, last_slash + 1);
                if (path[0] != '/')
                    path = String::format("%s/%s", g.cwd.characters(), path.characters());
                path = canonicalized_path(path);
                token = token.substring(last_slash + 1, token.length() - last_slash - 1);
            } else {
                // We have no slashes, so the directory to search is the current
                // directory and the token to complete is just the original token.
                // In this case, do not suggest executables but directories only.
                path = g.cwd;
                suggest_executables = false;
            }

            // the invariant part of the token is actually just the last segment
            // e.g. in `cd /foo/bar', 'bar' is the invariant
            //      since we are not suggesting anything starting with
            //      `/foo/', but rather just `bar...'
            editor.suggest(escape_token(token).length(), 0);

            // only suggest dot-files if path starts with a dot
            Core::DirIterator files(path,
                token.starts_with('.') ? Core::DirIterator::NoFlags : Core::DirIterator::SkipDots);

            while (files.has_next()) {
                auto file = files.next_path();
                // manually skip `.' and `..'
                if (file == "." || file == "..")
                    continue;
                auto trivia = " ";
                if (file.starts_with(token)) {
                    String file_path = String::format("%s/%s", path.characters(), file.characters());
                    struct stat program_status;
                    int stat_error = stat(file_path.characters(), &program_status);
                    if (stat_error)
                        continue;
                    if (access(file_path.characters(), X_OK) != 0)
                        continue;
                    if (S_ISDIR(program_status.st_mode)) {
                        if (!suggest_executables)
                            continue;
                        else
                            trivia = "/";
                    }

                    local_suggestions.append({ escape_token(file), trivia });
                }
            }

            return local_suggestions;
        }

        String completion = *match;
        Vector<Line::CompletionSuggestion> suggestions;

        // Now that we have a program name starting with our token, we look at
        // other program names starting with our token and cut off any mismatching
        // characters.

        int index = match - cached_path.data();
        for (int i = index - 1; i >= 0 && cached_path[i].starts_with(token); --i) {
            suggestions.append({ cached_path[i], " " });
        }
        for (size_t i = index + 1; i < cached_path.size() && cached_path[i].starts_with(token); ++i) {
            suggestions.append({ cached_path[i], " " });
        }
        suggestions.append({ cached_path[index], " " });

        editor.suggest(escape_token(token).length(), 0);

        return suggestions;
    };
    editor.on_tab_complete_other_token = [&](const String& token_to_complete) -> Vector<Line::CompletionSuggestion> {
        auto token = unescape_token(token_to_complete);
        String path;
        Vector<Line::CompletionSuggestion> suggestions;

        ssize_t last_slash = token.length() - 1;
        while (last_slash >= 0 && token[last_slash] != '/')
            --last_slash;

        if (last_slash >= 0) {
            // Split on the last slash. We'll use the first part as the directory
            // to search and the second part as the token to complete.
            path = token.substring(0, last_slash + 1);
            if (path[0] != '/')
                path = String::format("%s/%s", g.cwd.characters(), path.characters());
            path = canonicalized_path(path);
            token = token.substring(last_slash + 1, token.length() - last_slash - 1);
        } else {
            // We have no slashes, so the directory to search is the current
            // directory and the token to complete is just the original token.
            path = g.cwd;
        }

        // the invariant part of the token is actually just the last segment
        // e.g. in `cd /foo/bar', 'bar' is the invariant
        //      since we are not suggesting anything starting with
        //      `/foo/', but rather just `bar...'
        editor.suggest(escape_token(token).length(), 0);

        // only suggest dot-files if path starts with a dot
        Core::DirIterator files(path,
            token.starts_with('.') ? Core::DirIterator::NoFlags : Core::DirIterator::SkipDots);

        while (files.has_next()) {
            auto file = files.next_path();
            // manually skip `.' and `..'
            if (file == "." || file == "..")
                continue;
            if (file.starts_with(token)) {
                struct stat program_status;
                String file_path = String::format("%s/%s", path.characters(), file.characters());
                int stat_error = stat(file_path.characters(), &program_status);
                if (!stat_error) {
                    if (S_ISDIR(program_status.st_mode))
                        suggestions.append({ escape_token(file), "/" });
                    else
                        suggestions.append({ escape_token(file), " " });
                }
            }
        }

        return suggestions;
    };

    signal(SIGINT, [](int) {
        g.was_interrupted = true;
        editor.interrupted();
    });

    signal(SIGWINCH, [](int) {
        g.was_resized = true;
        editor.resized();
    });

    signal(SIGHUP, [](int) {
        save_history();
    });

    int rc = gethostname(g.hostname, sizeof(g.hostname));
    if (rc < 0)
        perror("gethostname");
    rc = ttyname_r(0, g.ttyname, sizeof(g.ttyname));
    if (rc < 0)
        perror("ttyname_r");

    {
        auto* cwd = getcwd(nullptr, 0);
        g.cwd = cwd;
        setenv("PWD", cwd, 1);
        free(cwd);
    }

    {
        auto* pw = getpwuid(getuid());
        if (pw) {
            g.username = pw->pw_name;
            g.home = pw->pw_dir;
            setenv("HOME", pw->pw_dir, 1);
        }
        endpwent();
    }

    if (argc > 2 && !strcmp(argv[1], "-c")) {
        dbgprintf("sh -c '%s'\n", argv[2]);
        run_command(argv[2]);
        return 0;
    }

    if (argc == 2 && argv[1][0] != '-') {
        auto file = Core::File::construct(argv[1]);
        if (!file->open(Core::IODevice::ReadOnly)) {
            fprintf(stderr, "Failed to open %s: %s\n", file->filename().characters(), file->error_string());
            return 1;
        }
        for (;;) {
            auto line = file->read_line(4096);
            if (line.is_null())
                break;
            run_command(String::copy(line, Chomp));
        }
        return 0;
    }

    g.directory_stack.append(g.cwd);

    load_history();
    atexit(save_history);

    cache_path();

    for (;;) {
        auto line = editor.get_line(prompt());
        if (line.is_empty())
            continue;
        run_command(line);
        editor.add_to_history(line);
    }

    return 0;
}
