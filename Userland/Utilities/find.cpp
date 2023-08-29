/*
 * Copyright (c) 2020-2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/CheckedFormatString.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibRegex/Regex.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

bool g_follow_symlinks = false;
bool g_there_was_an_error = false;
bool g_have_seen_action_command = false;

template<typename... Parameters>
[[noreturn]] static void fatal_error(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    warn("\033[31m");
    warn(move(fmtstr), parameters...);
    warn("\033[0m");
    warnln();
    exit(1);
}

struct FileData {
    // Full path to the file; either absolute or relative to cwd.
    LexicalPath full_path;
    // The parent directory of the file.
    int dirfd { -1 };
    // The file's basename, relative to the directory.
    char const* basename { nullptr };
    // Optionally, cached information as returned by stat/lstat/fstatat.
    struct stat stat {
    };
    bool stat_is_valid : 1 { false };
    // File type as returned from readdir(), or DT_UNKNOWN.
    unsigned char d_type { DT_UNKNOWN };

    const struct stat* ensure_stat()
    {
        if (stat_is_valid)
            return &stat;

        int flags = g_follow_symlinks ? 0 : AT_SYMLINK_NOFOLLOW;
        int rc = fstatat(dirfd, basename, &stat, flags);
        if (rc < 0) {
            perror(full_path.string().characters());
            g_there_was_an_error = true;
            return nullptr;
        }

        stat_is_valid = true;

        if (S_ISREG(stat.st_mode))
            d_type = DT_REG;
        else if (S_ISDIR(stat.st_mode))
            d_type = DT_DIR;
        else if (S_ISCHR(stat.st_mode))
            d_type = DT_CHR;
        else if (S_ISBLK(stat.st_mode))
            d_type = DT_BLK;
        else if (S_ISFIFO(stat.st_mode))
            d_type = DT_FIFO;
        else if (S_ISLNK(stat.st_mode))
            d_type = DT_LNK;
        else if (S_ISSOCK(stat.st_mode))
            d_type = DT_SOCK;
        else
            VERIFY_NOT_REACHED();

        return &stat;
    }
};

class Command {
public:
    virtual ~Command() = default;
    virtual bool evaluate(FileData& file_data) const = 0;
};

class StatCommand : public Command {
public:
    virtual bool evaluate(const struct stat&) const = 0;

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        const struct stat* stat = file_data.ensure_stat();
        if (!stat)
            return false;
        return evaluate(*stat);
    }
};

class TypeCommand final : public Command {
public:
    TypeCommand(char const* arg)
    {
        StringView type { arg, strlen(arg) };
        if (type.length() != 1 || !"bcdlpfs"sv.contains(type[0]))
            fatal_error("Invalid mode: \033[1m{}", arg);
        m_type = type[0];
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        // First, make sure we have a type, but avoid calling
        // sys$stat() unless we need to.
        if (file_data.d_type == DT_UNKNOWN) {
            if (file_data.ensure_stat() == nullptr)
                return false;
        }

        auto type = file_data.d_type;
        switch (m_type) {
        case 'b':
            return type == DT_BLK;
        case 'c':
            return type == DT_CHR;
        case 'd':
            return type == DT_DIR;
        case 'l':
            return type == DT_LNK;
        case 'p':
            return type == DT_FIFO;
        case 'f':
            return type == DT_REG;
        case 's':
            return type == DT_SOCK;
        default:
            // We've verified this is a correct character before.
            VERIFY_NOT_REACHED();
        }
    }

    char m_type { 0 };
};

class LinksCommand final : public StatCommand {
public:
    LinksCommand(char const* arg)
    {
        auto number = StringView { arg, strlen(arg) }.to_uint();
        if (!number.has_value())
            fatal_error("Invalid number: \033[1m{}", arg);
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
    UserCommand(char const* arg)
    {
        if (struct passwd* passwd = getpwnam(arg)) {
            m_uid = passwd->pw_uid;
        } else {
            // Attempt to parse it as decimal UID.
            auto number = StringView { arg, strlen(arg) }.to_uint();
            if (!number.has_value())
                fatal_error("Invalid user: \033[1m{}", arg);
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
    GroupCommand(char const* arg)
    {
        if (struct group* gr = getgrnam(arg)) {
            m_gid = gr->gr_gid;
        } else {
            // Attempt to parse it as decimal GID.
            auto number = StringView { arg, strlen(arg) }.to_uint<gid_t>();
            if (!number.has_value())
                fatal_error("Invalid group: \033[1m{}", arg);
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
    SizeCommand(char const* arg)
    {
        StringView view { arg, strlen(arg) };
        auto suffix = view[view.length() - 1];
        if (!is_ascii_digit(suffix)) {
            switch (suffix) {
            case 'c':
                m_unit_size = 1;
                break;
            case 'w':
                m_unit_size = 2;
                break;
            case 'k':
                m_unit_size = KiB;
                break;
            case 'M':
                m_unit_size = MiB;
                break;
            case 'G':
                m_unit_size = GiB;
                break;
            case 'b':
                // The behavior of this suffix is the same as no suffix.
                break;
            default:
                fatal_error("Invalid -size type '{}'", suffix);
            }

            view = view.substring_view(0, view.length() - 1);
        }
        auto number = view.to_uint<u64>();
        if (!number.has_value() || number.value() > AK::NumericLimits<off_t>::max())
            fatal_error("Invalid size: \033[1m{}", arg);
        m_number_of_units = number.value();
    }

private:
    virtual bool evaluate(const struct stat& stat) const override
    {
        if (m_unit_size == 1)
            return stat.st_size == m_number_of_units;

        auto size_divided_by_unit_rounded_up = (stat.st_size + m_unit_size - 1) / m_unit_size;
        return size_divided_by_unit_rounded_up == m_number_of_units;
    }

    off_t m_number_of_units { 0 };
    off_t m_unit_size { 512 };
};

class NameCommand : public Command {
public:
    NameCommand(char const* pattern, CaseSensitivity case_sensitivity)
        : m_pattern(pattern, strlen(pattern))
        , m_case_sensitivity(case_sensitivity)
    {
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        return file_data.full_path.basename().matches(m_pattern, m_case_sensitivity);
    }

    StringView m_pattern;
    CaseSensitivity m_case_sensitivity { CaseSensitivity::CaseSensitive };
};

class RegexCommand : public Command {
public:
    RegexCommand(Regex<PosixExtended>&& regex)
        : m_regex(move(regex))
    {
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        auto haystack = file_data.full_path.string();
        auto match_result = m_regex.match(haystack);
        return match_result.success;
    }

    Regex<PosixExtended> m_regex;
};

class AccessCommand final : public Command {
public:
    AccessCommand(mode_t mode)
        : m_mode(mode)
    {
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        auto maybe_error = Core::System::access(file_data.full_path.string(), m_mode);
        return !maybe_error.is_error();
    }

    mode_t m_mode { 0 };
};

class PrintCommand final : public Command {
public:
    PrintCommand(char terminator = '\n')
        : m_terminator(terminator)
    {
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        out("{}{}", file_data.full_path, m_terminator);
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
    virtual bool evaluate(FileData& file_data) const override
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
                if (StringView { arg, strlen(arg) } == "{}")
                    arg = const_cast<char*>(file_data.full_path.string().characters());
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
    virtual bool evaluate(FileData& file_data) const override
    {
        return m_lhs->evaluate(file_data) && m_rhs->evaluate(file_data);
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
    virtual bool evaluate(FileData& file_data) const override
    {
        return m_lhs->evaluate(file_data) || m_rhs->evaluate(file_data);
    }

    NonnullOwnPtr<Command> m_lhs;
    NonnullOwnPtr<Command> m_rhs;
};

static OwnPtr<Command> parse_complex_command(Vector<char*>& args);

// Parse a simple command starting at optind; leave optind at its the last
// argument. Return nullptr if we reach the end of arguments.
static OwnPtr<Command> parse_simple_command(Vector<char*>& args)
{
    if (args.is_empty())
        return {};

    char* raw_arg = args.take_first();
    StringView arg { raw_arg, strlen(raw_arg) };

    if (arg == "(") {
        auto command = parse_complex_command(args);
        if (command && !args.is_empty() && StringView { args.first(), strlen(args.first()) } == ")")
            return command;
        fatal_error("Unmatched \033[1m(");
    } else if (arg == "-type") {
        if (args.is_empty())
            fatal_error("-type: requires additional arguments");
        return make<TypeCommand>(args.take_first());
    } else if (arg == "-links") {
        if (args.is_empty())
            fatal_error("-links: requires additional arguments");
        return make<LinksCommand>(args.take_first());
    } else if (arg == "-user") {
        if (args.is_empty())
            fatal_error("-user: requires additional arguments");
        return make<UserCommand>(args.take_first());
    } else if (arg == "-group") {
        if (args.is_empty())
            fatal_error("-group: requires additional arguments");
        return make<GroupCommand>(args.take_first());
    } else if (arg == "-size") {
        if (args.is_empty())
            fatal_error("-size: requires additional arguments");
        return make<SizeCommand>(args.take_first());
    } else if (arg == "-name") {
        if (args.is_empty())
            fatal_error("-name: requires additional arguments");
        return make<NameCommand>(args.take_first(), CaseSensitivity::CaseSensitive);
    } else if (arg == "-iname") {
        if (args.is_empty())
            fatal_error("-iname: requires additional arguments");
        return make<NameCommand>(args.take_first(), CaseSensitivity::CaseInsensitive);
    } else if (arg == "-regex") {
        if (args.is_empty())
            fatal_error("-regex: requires additional arguments");
        Regex<PosixExtended> regex { args.take_first(), PosixFlags::Default };
        if (regex.parser_result.error != regex::Error::NoError)
            fatal_error("{}", regex.error_string());
        return make<RegexCommand>(move(regex));
    } else if (arg == "-iregex") {
        if (args.is_empty())
            fatal_error("-iregex: requires additional arguments");
        Regex<PosixExtended> regex { args.take_first(), PosixFlags::Insensitive };
        if (regex.parser_result.error != regex::Error::NoError)
            fatal_error("{}", regex.error_string());
        return make<RegexCommand>(move(regex));
    } else if (arg == "-readable") {
        return make<AccessCommand>(R_OK);
    } else if (arg == "-writable") {
        return make<AccessCommand>(W_OK);
    } else if (arg == "-executable") {
        return make<AccessCommand>(X_OK);
    } else if (arg == "-print") {
        g_have_seen_action_command = true;
        return make<PrintCommand>();
    } else if (arg == "-print0") {
        g_have_seen_action_command = true;
        return make<PrintCommand>(0);
    } else if (arg == "-exec") {
        if (args.is_empty())
            fatal_error("-exec: requires additional arguments");
        g_have_seen_action_command = true;
        Vector<char*> command_argv;
        while (!args.is_empty()) {
            char* next = args.take_first();
            if (next[0] == ';')
                break;
            command_argv.append(next);
        }
        return make<ExecCommand>(move(command_argv));
    } else {
        fatal_error("Unsupported command \033[1m{}", arg);
    }
}

static OwnPtr<Command> parse_complex_command(Vector<char*>& args)
{
    auto command = parse_simple_command(args);

    while (command && !args.is_empty()) {
        char* raw_arg = args.take_first();
        StringView arg { raw_arg, strlen(raw_arg) };

        enum {
            And,
            Or,
        } binary_operation { And };

        if (arg == "-a") {
            binary_operation = And;
        } else if (arg == "-o") {
            binary_operation = Or;
        } else if (arg == ")") {
            // Ooops, looked too far.
            args.prepend(raw_arg);
            return command;
        } else {
            // Juxtaposition is an And too, and there's nothing to skip.
            args.prepend(raw_arg);
            binary_operation = And;
        }

        auto rhs = parse_complex_command(args);
        if (!rhs)
            fatal_error("Missing right-hand side");

        if (binary_operation == And)
            command = make<AndCommand>(command.release_nonnull(), rhs.release_nonnull());
        else
            command = make<OrCommand>(command.release_nonnull(), rhs.release_nonnull());
    }

    return command;
}

static NonnullOwnPtr<Command> parse_all_commands(Vector<char*>& args)
{
    auto command = parse_complex_command(args);

    if (g_have_seen_action_command) {
        VERIFY(command);
        return command.release_nonnull();
    }

    if (!command) {
        return make<PrintCommand>();
    }

    return make<AndCommand>(command.release_nonnull(), make<PrintCommand>());
}

static void walk_tree(FileData& root_data, Command& command)
{
    command.evaluate(root_data);

    // We should try to read directory entries if either:
    // * This is a directory.
    // * This is a symlink (that could point to a directory),
    //   and we're following symlinks.
    // * The type is unknown, so it could be a directory.
    switch (root_data.d_type) {
    case DT_DIR:
    case DT_UNKNOWN:
        break;
    case DT_LNK:
        if (g_follow_symlinks)
            break;
        return;
    default:
        return;
    }

    int dirfd = openat(root_data.dirfd, root_data.basename, O_RDONLY | O_DIRECTORY | O_CLOEXEC);
    if (dirfd < 0) {
        if (errno == ENOTDIR) {
            // Above we decided to try to open this file because it could
            // be a directory, but turns out it's not. This is fine though.
            return;
        }
        perror(root_data.full_path.string().characters());
        g_there_was_an_error = true;
        return;
    }

    DIR* dir = fdopendir(dirfd);

    while (true) {
        errno = 0;
        auto* dirent = readdir(dir);
        if (!dirent)
            break;

        if (strcmp(dirent->d_name, ".") == 0 || strcmp(dirent->d_name, "..") == 0)
            continue;

        FileData file_data {
            root_data.full_path.append({ dirent->d_name, strlen(dirent->d_name) }),
            dirfd,
            dirent->d_name,
            (struct stat) {},
            false,
            dirent->d_type,
        };
        walk_tree(file_data, command);
    }

    if (errno != 0) {
        perror(root_data.full_path.string().characters());
        g_there_was_an_error = true;
    }

    closedir(dir);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<char*> args;
    args.append(arguments.argv + 1, arguments.argc - 1);

    OwnPtr<Command> command;
    Vector<LexicalPath> paths;

    while (!args.is_empty()) {
        char* raw_arg = args.take_first();
        StringView arg { raw_arg, strlen(raw_arg) };
        if (arg == "-L") {
            g_follow_symlinks = true;
        } else if (!arg.starts_with('-')) {
            paths.append(LexicalPath(arg));
        } else {
            // No special case, so add back the argument and try to parse a command.
            args.prepend(raw_arg);
            command = parse_all_commands(args);
        }
    }

    if (!command)
        command = make<PrintCommand>();

    if (paths.is_empty())
        paths.append(LexicalPath("."));

    for (auto& path : paths) {
        DeprecatedString dirname = path.dirname();
        DeprecatedString basename = path.basename();

        int dirfd = TRY(Core::System::open(dirname, O_RDONLY | O_DIRECTORY | O_CLOEXEC));

        FileData file_data {
            path,
            dirfd,
            basename.characters(),
            (struct stat) {},
            false,
            DT_UNKNOWN,
        };
        walk_tree(file_data, *command);
        close(dirfd);
    }

    return g_there_was_an_error ? 1 : 0;
}
