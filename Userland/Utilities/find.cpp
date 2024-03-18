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
#include <AK/Time.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibRegex/Regex.h>
#include <LibURL/URL.h>
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
bool g_print_hyperlinks = false;
Optional<u32> g_max_depth = {};
Optional<u32> g_min_depth = {};

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
    // The current path specified on the command line
    StringView root_path;
    // Path to the file relative to the current root path
    LexicalPath relative_path;
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

    ByteString full_path() const
    {
        if (root_path.is_empty())
            return relative_path.string();

        if (relative_path.string() == ".")
            return root_path;

        // POSIX says that a single slash should be added between the root path and the relative portion if the root path doesn't end in one
        // any additional trailing slashes should be left unaltered.
        if (root_path.ends_with('/'))
            return ByteString::formatted("{}{}", root_path, relative_path.string());

        return ByteString::formatted("{}/{}", root_path, relative_path.string());
    }

    const struct stat* ensure_stat()
    {
        if (stat_is_valid)
            return &stat;

        int flags = g_follow_symlinks ? 0 : AT_SYMLINK_NOFOLLOW;
        int rc = fstatat(dirfd, basename, &stat, flags);
        if (rc < 0) {
            perror(full_path().characters());
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

template<Unsigned T>
class NumericRange {
public:
    NumericRange() = default;

    static ErrorOr<NumericRange<T>> parse(StringView arg)
    {
        if (arg.is_empty())
            return Error::from_errno(EINVAL);

        auto comparison_type = ComparisonType::Equal;
        if (arg[0] == '-') {
            comparison_type = ComparisonType::LessThan;
            arg = arg.substring_view(1);
        } else if (arg[0] == '+') {
            comparison_type = ComparisonType::GreaterThan;
            arg = arg.substring_view(1);
        }

        auto maybe_value = arg.to_number<T>();
        if (!maybe_value.has_value())
            return Error::from_errno(EINVAL);

        return NumericRange<T>(maybe_value.release_value(), comparison_type);
    }

    bool contains(T other) const
    {
        if (m_comparison_type == ComparisonType::LessThan)
            return other < m_value;
        if (m_comparison_type == ComparisonType::GreaterThan)
            return other > m_value;

        return other == m_value;
    }

private:
    enum class ComparisonType {
        Equal,
        LessThan,
        GreaterThan
    };

    NumericRange(T value, ComparisonType comparison_type)
        : m_value(value)
        , m_comparison_type(comparison_type)
    {
    }

    T m_value {};
    ComparisonType m_comparison_type { ComparisonType::Equal };
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

class MaxDepthCommand : public Command {
public:
    MaxDepthCommand(char const* arg)
    {
        auto max_depth_string = StringView { arg, strlen(arg) };
        auto maybe_max_depth = max_depth_string.to_number<u32>();
        if (!maybe_max_depth.has_value())
            fatal_error("-maxdepth: '{}' is not a valid non-negative integer", arg);

        g_max_depth = maybe_max_depth.value();
    }

    virtual bool evaluate(FileData&) const override
    {
        return true;
    }
};

class MinDepthCommand : public Command {
public:
    MinDepthCommand(char const* arg)
    {
        auto min_depth_string = StringView { arg, strlen(arg) };
        auto maybe_min_depth = min_depth_string.to_number<u32>();
        if (!maybe_min_depth.has_value())
            fatal_error("-mindepth: '{}' is not a valid non-negative integer", arg);

        g_min_depth = maybe_min_depth.value();
    }

    virtual bool evaluate(FileData&) const override
    {
        return true;
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
        StringView arg_string { arg, strlen(arg) };
        auto links_or_error = NumericRange<nlink_t>::parse(arg_string);
        if (links_or_error.is_error())
            fatal_error("Invalid number: \033[1m{}", arg);
        m_links = links_or_error.release_value();
    }

private:
    virtual bool evaluate(const struct stat& stat) const override
    {
        return m_links.contains(stat.st_nlink);
    }

    NumericRange<nlink_t> m_links {};
};

class UserCommand final : public StatCommand {
public:
    UserCommand(char const* arg)
    {
        if (struct passwd* passwd = getpwnam(arg)) {
            m_uid = passwd->pw_uid;
        } else {
            // Attempt to parse it as decimal UID.
            auto number = StringView { arg, strlen(arg) }.to_number<uid_t>();
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
            auto number = StringView { arg, strlen(arg) }.to_number<gid_t>();
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
        auto number_or_error = NumericRange<u64>::parse(view);
        if (number_or_error.is_error())
            fatal_error("Invalid size: \033[1m{}", arg);
        m_number_of_units = number_or_error.release_value();
    }

private:
    virtual bool evaluate(const struct stat& stat) const override
    {
        if (m_unit_size == 1)
            return m_number_of_units.contains(stat.st_size);

        auto size_divided_by_unit_rounded_up = (stat.st_size + m_unit_size - 1) / m_unit_size;
        return m_number_of_units.contains(size_divided_by_unit_rounded_up);
    }

    NumericRange<u64> m_number_of_units {};
    u64 m_unit_size { 512 };
};

class EmptyCommand final : public Command {
public:
    EmptyCommand()
    {
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        struct stat const* stat = file_data.ensure_stat();
        if (!stat)
            return false;

        if (S_ISREG(stat->st_mode))
            return stat->st_size == 0;

        if (S_ISDIR(stat->st_mode)) {
            auto dir_iterator = Core::DirIterator(file_data.full_path(), Core::DirIterator::SkipDots);
            return !dir_iterator.has_next();
        }

        return false;
    }
};

class PathCommand : public Command {
public:
    enum class PathPart {
        FullPath,
        Basename
    };

    PathCommand(char const* pattern, CaseSensitivity case_sensitivity, PathPart path_part)
        : m_pattern(pattern, strlen(pattern))
        , m_case_sensitivity(case_sensitivity)
        , m_path_part(path_part)
    {
        if (path_part == PathPart::FullPath && m_pattern.ends_with('/'))
            warnln("find: warning: path command will not match anything because it ends with '/'.");
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        if (m_path_part == PathPart::Basename)
            return file_data.relative_path.basename().matches(m_pattern, m_case_sensitivity);

        return file_data.full_path().matches(m_pattern, m_case_sensitivity);
    }

    StringView m_pattern;
    CaseSensitivity m_case_sensitivity { CaseSensitivity::CaseSensitive };
    PathPart m_path_part { PathPart::FullPath };
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
        auto haystack = file_data.full_path();
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
        auto maybe_error = Core::System::access(file_data.full_path(), m_mode);
        return !maybe_error.is_error();
    }

    mode_t m_mode { 0 };
};

class NewerCommand final : public StatCommand {
public:
    enum class TimestampType {
        LastAccess,
        Creation,
        LastModification
    };

    NewerCommand(char const* arg, TimestampType timestamp_type)
    {
        auto stat_function = g_follow_symlinks ? Core::System::stat : Core::System::lstat;
        auto stat_or_error = stat_function({ arg, strlen(arg) });
        if (stat_or_error.is_error())
            fatal_error("find: '{}': {}", arg, strerror(stat_or_error.error().code()));

        m_reference_file_stat = stat_or_error.release_value();
        m_timestamp_type = timestamp_type;
    }

private:
    virtual bool evaluate(struct stat const& stat) const override
    {
        struct timespec current_file_timestamp;
        struct timespec reference_file_timestamp;
        switch (m_timestamp_type) {
        case TimestampType::LastAccess:
            current_file_timestamp = stat.st_atim;
            reference_file_timestamp = m_reference_file_stat.st_atim;
            break;
        case TimestampType::Creation:
            current_file_timestamp = stat.st_ctim;
            reference_file_timestamp = m_reference_file_stat.st_ctim;
            break;
        case TimestampType::LastModification:
            current_file_timestamp = stat.st_mtim;
            reference_file_timestamp = m_reference_file_stat.st_mtim;
            break;
        }

        return Duration::from_timespec(current_file_timestamp) > Duration::from_timespec(reference_file_timestamp);
    }

    struct stat m_reference_file_stat;
    TimestampType m_timestamp_type { TimestampType::LastModification };
};

class GidCommand final : public StatCommand {
public:
    GidCommand(char const* arg)
    {
        auto gid_or_error = NumericRange<gid_t>::parse({ arg, strlen(arg) });
        if (gid_or_error.is_error())
            fatal_error("find: Invalid argument '{}' to '-gid'", arg);

        m_gid_range = gid_or_error.release_value();
    }

private:
    virtual bool evaluate(struct stat const& stat) const override
    {
        return m_gid_range.contains(stat.st_gid);
    }

    NumericRange<gid_t> m_gid_range {};
};

class UidCommand final : public StatCommand {
public:
    UidCommand(char const* arg)
    {
        auto uid_or_error = NumericRange<uid_t>::parse({ arg, strlen(arg) });
        if (uid_or_error.is_error())
            fatal_error("find: Invalid argument '{}' to '-uid'", arg);

        m_uid_range = uid_or_error.release_value();
    }

private:
    virtual bool evaluate(struct stat const& stat) const override
    {
        return m_uid_range.contains(stat.st_uid);
    }

    NumericRange<uid_t> m_uid_range {};
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
        auto printed = false;
        if (g_print_hyperlinks) {
            auto full_path_or_error = FileSystem::real_path(file_data.full_path());
            if (!full_path_or_error.is_error()) {
                auto fullpath = full_path_or_error.release_value();
                auto url = URL::create_with_file_scheme(fullpath);
                out("\033]8;;{}\033\\{}{}\033]8;;\033\\", url.serialize(), file_data.full_path(), m_terminator);
                printed = true;
            }
        }

        if (!printed)
            out("{}{}", file_data.full_path(), m_terminator);

        return true;
    }

    char m_terminator { '\n' };
};

class ExecCommand final : public Command {
public:
    enum class AwaitConfirmation {
        Yes,
        No
    };

    ExecCommand(Vector<char*>&& argv, AwaitConfirmation await_confirmation = AwaitConfirmation::No)
        : m_argv(move(argv))
        , m_await_confirmation(await_confirmation)
    {
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        // Replace any occurrences of "{}" with the path.
        Vector<ByteString> full_paths;
        Vector<char*> argv = m_argv;
        for (auto& arg : argv) {
            if (StringView { arg, strlen(arg) } == "{}") {
                auto full_path = file_data.full_path();
                full_paths.append(full_path);
                arg = const_cast<char*>(full_path.characters());
            }
        }

        if (m_await_confirmation == AwaitConfirmation::Yes) {
            out(stderr, "\"");
            bool first = true;
            for (auto const& arg : argv) {
                if (!first)
                    out(stderr, " ");

                out(stderr, "{}", arg);
                first = false;
            }
            out(stderr, "\"? ");
            fflush(stderr);

            Array<u8, BUFSIZ> buffer;
            auto nread_or_error = Core::System::read(STDIN_FILENO, buffer);
            if (nread_or_error.is_error()) {
                warn("Failed to read from stdin: {}", strerror(nread_or_error.error().code()));
                return false;
            }

            auto nread = nread_or_error.release_value();
            if (nread <= 0 || (buffer[0] != 'y' && buffer[0] != 'Y'))
                return false;
        }

        pid_t pid = fork();

        if (pid < 0) {
            perror("fork");
            g_there_was_an_error = true;
            return false;
        } else if (pid == 0) {
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
    AwaitConfirmation m_await_confirmation { AwaitConfirmation::No };
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

class NotCommand final : public Command {
public:
    NotCommand(NonnullOwnPtr<Command>&& operand)
        : m_operand(move(operand))
    {
    }

private:
    virtual bool evaluate(FileData& file_data) const override
    {
        return !m_operand->evaluate(file_data);
    }

    NonnullOwnPtr<Command> m_operand;
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
    } else if (arg == "!") {
        if (args.is_empty())
            fatal_error("Expected an expression after '!'");

        auto command = parse_simple_command(args).release_nonnull();
        return make<NotCommand>(move(command));
    } else if (arg == "-maxdepth"sv) {
        if (args.is_empty())
            fatal_error("-maxdepth: requires additional arguments");
        return make<MaxDepthCommand>(args.take_first());
    } else if (arg == "-mindepth"sv) {
        if (args.is_empty())
            fatal_error("-mindepth: requires additional arguments");
        return make<MinDepthCommand>(args.take_first());
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
    } else if (arg == "-empty") {
        return make<EmptyCommand>();
    } else if (arg == "-path") {
        if (args.is_empty())
            fatal_error("-path: requires additional arguments");
        return make<PathCommand>(args.take_first(), CaseSensitivity::CaseSensitive, PathCommand::PathPart::FullPath);
    } else if (arg == "-ipath") {
        if (args.is_empty())
            fatal_error("-ipath: requires additional arguments");
        return make<PathCommand>(args.take_first(), CaseSensitivity::CaseInsensitive, PathCommand::PathPart::FullPath);
    } else if (arg == "-name") {
        if (args.is_empty())
            fatal_error("-name: requires additional arguments");
        return make<PathCommand>(args.take_first(), CaseSensitivity::CaseSensitive, PathCommand::PathPart::Basename);
    } else if (arg == "-iname") {
        if (args.is_empty())
            fatal_error("-iname: requires additional arguments");
        return make<PathCommand>(args.take_first(), CaseSensitivity::CaseInsensitive, PathCommand::PathPart::Basename);
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
    } else if (arg == "-newer") {
        if (args.is_empty())
            fatal_error("-newer: requires additional arguments");
        return make<NewerCommand>(args.take_first(), NewerCommand::TimestampType::LastModification);
    } else if (arg == "-anewer") {
        if (args.is_empty())
            fatal_error("-anewer: requires additional arguments");
        return make<NewerCommand>(args.take_first(), NewerCommand::TimestampType::LastAccess);
    } else if (arg == "-cnewer") {
        if (args.is_empty())
            fatal_error("-cnewer: requires additional arguments");
        return make<NewerCommand>(args.take_first(), NewerCommand::TimestampType::Creation);
    } else if (arg == "-gid") {
        if (args.is_empty())
            fatal_error("-gid: requires additional arguments");
        return make<GidCommand>(args.take_first());
    } else if (arg == "-uid") {
        if (args.is_empty())
            fatal_error("-uid: requires additional arguments");
        return make<UidCommand>(args.take_first());
    } else if (arg == "-print") {
        g_have_seen_action_command = true;
        return make<PrintCommand>();
    } else if (arg == "-print0") {
        g_have_seen_action_command = true;
        return make<PrintCommand>(0);
    } else if (arg == "-exec" || arg == "-ok") {
        if (args.is_empty())
            fatal_error("{}: requires additional arguments", arg);
        g_have_seen_action_command = true;
        Vector<char*> command_argv;
        bool terminator_found = false;
        while (!args.is_empty()) {
            char* next = args.take_first();
            if (next[0] == ';' && next[1] == '\0') {
                terminator_found = true;
                break;
            }
            command_argv.append(next);
        }

        if (!terminator_found)
            fatal_error("{}: Terminating ';' not found", arg);

        auto await_confirmation = (arg == "-ok") ? ExecCommand::AwaitConfirmation::Yes : ExecCommand::AwaitConfirmation::No;
        return make<ExecCommand>(move(command_argv), await_confirmation);
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

static void walk_tree(FileData& root_data, Command& command, u32 depth = 0)
{
    if (!g_min_depth.has_value() || g_min_depth.value() <= depth)
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
        perror(root_data.full_path().characters());
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
            root_data.root_path,
            root_data.relative_path.append({ dirent->d_name, strlen(dirent->d_name) }),
            dirfd,
            dirent->d_name,
            (struct stat) {},
            false,
            dirent->d_type,
        };

        bool should_increase_depth = false;
        if (g_max_depth.has_value() || g_min_depth.has_value()) {
            if (g_max_depth.has_value() && depth >= g_max_depth.value())
                return;

            if (file_data.d_type == DT_UNKNOWN)
                file_data.ensure_stat();

            if (file_data.d_type == DT_DIR)
                should_increase_depth = true;
        }

        walk_tree(file_data, command, should_increase_depth ? depth + 1 : depth);
    }

    if (errno != 0) {
        perror(root_data.full_path().characters());
        g_there_was_an_error = true;
    }

    closedir(dir);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<char*> args;
    args.append(arguments.argv + 1, arguments.argc - 1);

    OwnPtr<Command> command;
    Vector<StringView> paths;

    while (!args.is_empty()) {
        char* raw_arg = args.take_first();
        StringView arg { raw_arg, strlen(raw_arg) };
        if (arg == "-L") {
            g_follow_symlinks = true;
        } else if (arg.starts_with('-') || arg == "!"sv) {
            args.prepend(raw_arg);
            command = parse_all_commands(args);
        } else {
            paths.append(arg);
        }
    }

    g_print_hyperlinks = TRY(Core::System::isatty(STDOUT_FILENO));

    if (!command)
        command = make<PrintCommand>();

    if (paths.is_empty())
        paths.append("."sv);

    for (auto& path : paths) {
        LexicalPath lexical_path { path };
        ByteString dirname = lexical_path.dirname();
        ByteString basename = lexical_path.basename();

        int dirfd = TRY(Core::System::open(dirname, O_RDONLY | O_DIRECTORY | O_CLOEXEC));

        FileData file_data {
            path,
            LexicalPath { "" },
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
