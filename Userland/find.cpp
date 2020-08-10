/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <getopt.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>

bool g_follow_symlinks = false;
bool g_there_was_an_error = false;
bool g_have_seen_action_command = false;

[[noreturn]] static void fatal_error(const char* format, ...)
{
    fputs("\033[31m", stderr);

    va_list ap;
    va_start(ap, format);
    vfprintf(stderr, format, ap);
    va_end(ap);

    fputs("\033[0m\n", stderr);
    exit(1);
}

class Command {
public:
    virtual ~Command() { }
    virtual bool evaluate(const char* file_path) const = 0;
};

class StatCommand : public Command {
public:
    virtual bool evaluate(const struct stat&) const = 0;

private:
    virtual bool evaluate(const char* file_path) const override
    {
        struct stat stat;
        auto stat_func = g_follow_symlinks ? ::stat : ::lstat;
        int rc = stat_func(file_path, &stat);
        if (rc < 0) {
            perror(file_path);
            g_there_was_an_error = true;
            return false;
        }
        return evaluate(stat);
    }
};

class TypeCommand final : public StatCommand {
public:
    TypeCommand(const char* arg)
    {
        StringView type = arg;
        if (type.length() != 1 || !StringView("bcdlpfs").contains(type[0]))
            fatal_error("Invalid mode: \033[1m%s", arg);
        m_type = type[0];
    }

private:
    virtual bool evaluate(const struct stat& stat) const override
    {
        auto type = stat.st_mode;
        switch (m_type) {
        case 'b':
            return S_ISBLK(type);
        case 'c':
            return S_ISCHR(type);
        case 'd':
            return S_ISDIR(type);
        case 'l':
            return S_ISLNK(type);
        case 'p':
            return S_ISFIFO(type);
        case 'f':
            return S_ISREG(type);
        case 's':
            return S_ISSOCK(type);
        default:
            // We've verified this is a correct character before.
            ASSERT_NOT_REACHED();
        }
    }

    char m_type { 0 };
};

class LinksCommand final : public StatCommand {
public:
    LinksCommand(const char* arg)
    {
        auto number = StringView(arg).to_uint();
        if (!number.has_value())
            fatal_error("Invalid number: \033[1m%s", arg);
        m_links = number.value();
    }

private:
    virtual bool evaluate(const struct stat& stat) const override
    {
        return stat.st_nlink == m_links;
    }

    nlink_t m_links { 0 };
};

class UserCommand final : public StatCommand {
public:
    UserCommand(const char* arg)
    {
        if (struct passwd* passwd = getpwnam(arg)) {
            m_uid = passwd->pw_uid;
        } else {
            // Attempt to parse it as decimal UID.
            auto number = StringView(arg).to_uint();
            if (!number.has_value())
                fatal_error("Invalid user: \033[1m%s", arg);
            m_uid = number.value();
        }
    }

private:
    virtual bool evaluate(const struct stat& stat) const override
    {
        return stat.st_uid == m_uid;
    }

    uid_t m_uid { 0 };
};

class GroupCommand final : public StatCommand {
public:
    GroupCommand(const char* arg)
    {
        if (struct group* gr = getgrnam(arg)) {
            m_gid = gr->gr_gid;
        } else {
            // Attempt to parse it as decimal GID.
            auto number = StringView(arg).to_int();
            if (!number.has_value())
                fatal_error("Invalid group: \033[1m%s", arg);
            m_gid = number.value();
        }
    }

private:
    virtual bool evaluate(const struct stat& stat) const override
    {
        return stat.st_gid == m_gid;
    }

    gid_t m_gid { 0 };
};

class SizeCommand final : public StatCommand {
public:
    SizeCommand(const char* arg)
    {
        StringView view = arg;
        if (view.ends_with('c')) {
            m_is_bytes = true;
            view = view.substring_view(0, view.length() - 1);
        }
        auto number = view.to_uint();
        if (!number.has_value())
            fatal_error("Invalid size: \033[1m%s", arg);
        m_size = number.value();
    }

private:
    virtual bool evaluate(const struct stat& stat) const override
    {
        if (m_is_bytes)
            return stat.st_size == m_size;

        auto size_divided_by_512_rounded_up = (stat.st_size + 511) / 512;
        return size_divided_by_512_rounded_up == m_size;
    }

    off_t m_size { 0 };
    bool m_is_bytes { false };
};

class PrintCommand final : public Command {
public:
    PrintCommand(char terminator = '\n')
        : m_terminator(terminator)
    {
    }

private:
    virtual bool evaluate(const char* file_path) const override
    {
        printf("%s%c", file_path, m_terminator);
        return true;
    }

    char m_terminator { '\n' };
};

class ExecCommand final : public Command {
public:
    ExecCommand(Vector<char*>&& argv)
        : m_argv(move(argv))
    {
    }

private:
    virtual bool evaluate(const char* file_path) const override
    {
        pid_t pid = fork();

        if (pid < 0) {
            perror("fork");
            g_there_was_an_error = true;
            return false;
        } else if (pid == 0) {
            // Replace any occurrences of "{}" with the path. Since we're in the
            // child and going to exec real soon, let's just const_cast away the
            // constness.
            auto argv = const_cast<Vector<char*>&>(m_argv);
            for (auto& arg : argv) {
                if (StringView(arg) == "{}")
                    arg = const_cast<char*>(file_path);
            }
            argv.append(nullptr);
            execvp(m_argv[0], argv.data());
            perror("execvp");
            exit(1);
        } else {
            int status;
            int rc = waitpid(pid, &status, 0);
            if (rc < 0) {
                perror("waitpid");
                g_there_was_an_error = true;
                return false;
            }
            return WIFEXITED(status) && WEXITSTATUS(status) == 0;
        }
    }

    Vector<char*> m_argv;
};

class AndCommand final : public Command {
public:
    AndCommand(NonnullOwnPtr<Command>&& lhs, NonnullOwnPtr<Command>&& rhs)
        : m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

private:
    virtual bool evaluate(const char* file_path) const override
    {
        return m_lhs->evaluate(file_path) && m_rhs->evaluate(file_path);
    }

    NonnullOwnPtr<Command> m_lhs;
    NonnullOwnPtr<Command> m_rhs;
};

class OrCommand final : public Command {
public:
    OrCommand(NonnullOwnPtr<Command>&& lhs, NonnullOwnPtr<Command>&& rhs)
        : m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

private:
    virtual bool evaluate(const char* file_path) const override
    {
        return m_lhs->evaluate(file_path) || m_rhs->evaluate(file_path);
    }

    NonnullOwnPtr<Command> m_lhs;
    NonnullOwnPtr<Command> m_rhs;
};

static OwnPtr<Command> parse_complex_command(char* argv[]);

// Parse a simple command starting at optind; leave optind at its the last
// argument. Return nullptr if we reach the end of arguments.
static OwnPtr<Command> parse_simple_command(char* argv[])
{
    StringView arg = argv[optind];

    if (arg.is_null()) {
        return nullptr;
    } else if (arg == "(") {
        optind++;
        auto command = parse_complex_command(argv);
        if (command && argv[optind] && StringView(argv[++optind]) == ")")
            return command;
        fatal_error("Unmatched \033[1m(");
    } else if (arg == "-type") {
        return make<TypeCommand>(argv[++optind]);
    } else if (arg == "-links") {
        return make<LinksCommand>(argv[++optind]);
    } else if (arg == "-user") {
        return make<UserCommand>(argv[++optind]);
    } else if (arg == "-group") {
        return make<GroupCommand>(argv[++optind]);
    } else if (arg == "-size") {
        return make<SizeCommand>(argv[++optind]);
    } else if (arg == "-print") {
        g_have_seen_action_command = true;
        return make<PrintCommand>();
    } else if (arg == "-print0") {
        g_have_seen_action_command = true;
        return make<PrintCommand>(0);
    } else if (arg == "-exec") {
        g_have_seen_action_command = true;
        Vector<char*> command_argv;
        while (argv[++optind] && StringView(argv[optind]) != ";")
            command_argv.append(argv[optind]);
        return make<ExecCommand>(move(command_argv));
    } else {
        fatal_error("Unsupported command \033[1m%s", argv[optind]);
    }
}

static OwnPtr<Command> parse_complex_command(char* argv[])
{
    auto command = parse_simple_command(argv);

    while (command && argv[optind] && argv[optind + 1]) {
        StringView arg = argv[++optind];

        enum { And,
            Or } binary_operation
            = And;

        if (arg == "-a") {
            optind++;
            binary_operation = And;
        } else if (arg == "-o") {
            optind++;
            binary_operation = Or;
        } else if (arg == ")") {
            // Ooops, looked too far.
            optind--;
            return command;
        } else {
            // Juxtaposition is an And too, and there's nothing to skip.
            binary_operation = And;
        }

        auto rhs = parse_complex_command(argv);
        if (!rhs)
            fatal_error("Missing right-hand side");

        if (binary_operation == And)
            command = make<AndCommand>(command.release_nonnull(), rhs.release_nonnull());
        else
            command = make<OrCommand>(command.release_nonnull(), rhs.release_nonnull());
    }

    return command;
}

static NonnullOwnPtr<Command> parse_all_commands(char* argv[])
{
    auto command = parse_complex_command(argv);

    if (g_have_seen_action_command) {
        ASSERT(command);
        return command.release_nonnull();
    }

    if (!command) {
        return make<PrintCommand>();
    }

    return make<AndCommand>(command.release_nonnull(), make<PrintCommand>());
}

static const char* parse_options(int argc, char* argv[])
{
    // Sadly, we can't use Core::ArgsParser, because find accepts arguments in
    // an extremely unusual format. We're going to try to use getopt(), though.
    opterr = 0;
    while (true) {
        int opt = getopt(argc, argv, "+L");
        switch (opt) {
        case -1: {
            // No more options.
            StringView arg = argv[optind];
            if (!arg.is_null() && !arg.starts_with('-')) {
                // It's our root path!
                return argv[optind++];
            } else {
                // It's a part of the script, and our root path is the current
                // directory by default.
                return ".";
            }
        }
        case '?':
            // Some error. Most likely, it's getopt() getting confused about
            // what it thought was an option, but is actually a command. Return
            // the default path, and hope the command parsing logic deals with
            // this.
            return ".";
        case 'L':
            g_follow_symlinks = true;
            break;
        default:
            ASSERT_NOT_REACHED();
        }
    }
}

static void walk_tree(const char* root_path, Command& command)
{
    command.evaluate(root_path);

    Core::DirIterator dir_iterator(root_path, Core::DirIterator::SkipParentAndBaseDir);
    if (dir_iterator.has_error() && dir_iterator.error() == ENOTDIR)
        return;

    while (dir_iterator.has_next())
        walk_tree(dir_iterator.next_full_path().characters(), command);

    if (dir_iterator.has_error()) {
        fprintf(stderr, "%s: %s\n", root_path, dir_iterator.error_string());
        g_there_was_an_error = true;
    }
}

int main(int argc, char* argv[])
{
    auto root_path = parse_options(argc, argv);
    auto command = parse_all_commands(argv);
    walk_tree(root_path, *command);
    return g_there_was_an_error ? 1 : 0;
}
