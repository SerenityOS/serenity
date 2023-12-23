/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

bool g_there_was_an_error = false;

[[noreturn, gnu::format(printf, 1, 2)]] static void fatal_error(char const* format, ...)
{
    fputs("\033[31m", stderr);

    va_list ap;
    va_start(ap, format);
    vfprintf(stderr, format, ap);
    va_end(ap);

    fputs("\033[0m\n", stderr);
    exit(126);
}

class Condition {
public:
    virtual ~Condition() = default;
    virtual bool check() const = 0;
};

class And : public Condition {
public:
    And(NonnullOwnPtr<Condition> lhs, NonnullOwnPtr<Condition> rhs)
        : m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

private:
    virtual bool check() const override
    {
        return m_lhs->check() && m_rhs->check();
    }

    NonnullOwnPtr<Condition> m_lhs;
    NonnullOwnPtr<Condition> m_rhs;
};

class Or : public Condition {
public:
    Or(NonnullOwnPtr<Condition> lhs, NonnullOwnPtr<Condition> rhs)
        : m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

private:
    virtual bool check() const override
    {
        return m_lhs->check() || m_rhs->check();
    }

    NonnullOwnPtr<Condition> m_lhs;
    NonnullOwnPtr<Condition> m_rhs;
};

class Not : public Condition {
public:
    Not(NonnullOwnPtr<Condition> cond)
        : m_cond(move(cond))
    {
    }

private:
    virtual bool check() const override
    {
        return !m_cond->check();
    }

    NonnullOwnPtr<Condition> m_cond;
};

class FileIsOfKind : public Condition {
public:
    enum Kind {
        BlockDevice,
        CharacterDevice,
        Directory,
        FIFO,
        Regular,
        Socket,
        SymbolicLink,
    };
    FileIsOfKind(StringView path, Kind kind)
        : m_path(path)
        , m_kind(kind)
    {
    }

private:
    virtual bool check() const override
    {
        struct stat statbuf;
        int rc;

        if (m_kind == SymbolicLink)
            rc = stat(m_path.characters(), &statbuf);
        else
            rc = lstat(m_path.characters(), &statbuf);

        if (rc < 0) {
            if (errno != ENOENT) {
                perror(m_path.characters());
                g_there_was_an_error = true;
            }
            return false;
        }

        switch (m_kind) {
        case BlockDevice:
            return S_ISBLK(statbuf.st_mode);
        case CharacterDevice:
            return S_ISCHR(statbuf.st_mode);
        case Directory:
            return S_ISDIR(statbuf.st_mode);
        case FIFO:
            return S_ISFIFO(statbuf.st_mode);
        case Regular:
            return S_ISREG(statbuf.st_mode);
        case Socket:
            return S_ISSOCK(statbuf.st_mode);
        case SymbolicLink:
            return S_ISLNK(statbuf.st_mode);
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ByteString m_path;
    Kind m_kind { Regular };
};

class UserHasPermission : public Condition {
public:
    enum Permission {
        Any,
        Read,
        Write,
        Execute,
    };
    UserHasPermission(StringView path, Permission kind)
        : m_path(path)
        , m_kind(kind)
    {
    }

private:
    virtual bool check() const override
    {
        switch (m_kind) {
        case Read:
            return access(m_path.characters(), R_OK) == 0;
        case Write:
            return access(m_path.characters(), W_OK) == 0;
        case Execute:
            return access(m_path.characters(), X_OK) == 0;
        case Any:
            return access(m_path.characters(), F_OK) == 0;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ByteString m_path;
    Permission m_kind { Read };
};

class FileHasFlag : public Condition {
public:
    enum Flag {
        SGID,
        SUID,
        SVTX,
    };
    FileHasFlag(StringView path, Flag kind)
        : m_path(path)
        , m_kind(kind)
    {
    }

private:
    virtual bool check() const override
    {
        struct stat statbuf;
        int rc = stat(m_path.characters(), &statbuf);

        if (rc < 0) {
            if (errno != ENOENT) {
                perror(m_path.characters());
                g_there_was_an_error = true;
            }
            return false;
        }

        switch (m_kind) {
        case SGID:
            return statbuf.st_mode & S_ISGID;
        case SUID:
            return statbuf.st_mode & S_ISUID;
        case SVTX:
            return statbuf.st_mode & S_ISVTX;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ByteString m_path;
    Flag m_kind { SGID };
};

class FileIsOwnedBy : public Condition {
public:
    enum Owner {
        EffectiveGID,
        EffectiveUID,
    };
    FileIsOwnedBy(StringView path, Owner kind)
        : m_path(path)
        , m_kind(kind)
    {
    }

private:
    virtual bool check() const override
    {
        struct stat statbuf;
        int rc = stat(m_path.characters(), &statbuf);

        if (rc < 0) {
            if (errno != ENOENT) {
                perror(m_path.characters());
                g_there_was_an_error = true;
            }
            return false;
        }

        switch (m_kind) {
        case EffectiveGID:
            return statbuf.st_gid == getgid();
        case EffectiveUID:
            return statbuf.st_uid == getuid();
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ByteString m_path;
    Owner m_kind { EffectiveGID };
};

class StringCompare : public Condition {
public:
    enum Mode {
        Equal,
        NotEqual,
    };

    StringCompare(StringView lhs, StringView rhs, Mode mode)
        : m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_mode(mode)
    {
    }

private:
    virtual bool check() const override
    {
        if (m_mode == Equal)
            return m_lhs == m_rhs;
        return m_lhs != m_rhs;
    }

    StringView m_lhs;
    StringView m_rhs;
    Mode m_mode { Equal };
};

class NumericCompare : public Condition {
public:
    enum Mode {
        Equal,
        Greater,
        GreaterOrEqual,
        Less,
        LessOrEqual,
        NotEqual,
    };

    NumericCompare(ByteString lhs, ByteString rhs, Mode mode)
        : m_mode(mode)
    {
        auto lhs_option = lhs.trim_whitespace().to_number<int>();
        auto rhs_option = rhs.trim_whitespace().to_number<int>();

        if (!lhs_option.has_value())
            fatal_error("expected integer expression: '%s'", lhs.characters());

        if (!rhs_option.has_value())
            fatal_error("expected integer expression: '%s'", rhs.characters());

        m_lhs = lhs_option.value();
        m_rhs = rhs_option.value();
    }

private:
    virtual bool check() const override
    {
        switch (m_mode) {
        case Equal:
            return m_lhs == m_rhs;
        case Greater:
            return m_lhs > m_rhs;
        case GreaterOrEqual:
            return m_lhs >= m_rhs;
        case Less:
            return m_lhs < m_rhs;
        case LessOrEqual:
            return m_lhs <= m_rhs;
        case NotEqual:
            return m_lhs != m_rhs;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    int m_lhs { 0 };
    int m_rhs { 0 };
    Mode m_mode { Equal };
};

class FileCompare : public Condition {
public:
    enum Mode {
        Same,
        ModificationTimestampGreater,
        ModificationTimestampLess,
    };

    FileCompare(ByteString lhs, ByteString rhs, Mode mode)
        : m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_mode(mode)
    {
    }

private:
    virtual bool check() const override
    {
        struct stat statbuf_l;
        int rc = stat(m_lhs.characters(), &statbuf_l);

        if (rc < 0) {
            perror(m_lhs.characters());
            g_there_was_an_error = true;
            return false;
        }

        struct stat statbuf_r;
        rc = stat(m_rhs.characters(), &statbuf_r);

        if (rc < 0) {
            perror(m_rhs.characters());
            g_there_was_an_error = true;
            return false;
        }

        switch (m_mode) {
        case Same:
            return statbuf_l.st_dev == statbuf_r.st_dev && statbuf_l.st_ino == statbuf_r.st_ino;
        case ModificationTimestampLess:
            return statbuf_l.st_mtime < statbuf_r.st_mtime;
        case ModificationTimestampGreater:
            return statbuf_l.st_mtime > statbuf_r.st_mtime;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ByteString m_lhs;
    ByteString m_rhs;
    Mode m_mode { Same };
};

static OwnPtr<Condition> parse_complex_expression(char* argv[]);

static bool should_treat_expression_as_single_string(StringView arg_after)
{
    return arg_after.is_null() || arg_after == "-a" || arg_after == "-o";
}

static OwnPtr<Condition> parse_simple_expression(char* argv[])
{
    StringView arg { argv[optind], strlen(argv[optind]) };
    if (arg.is_null()) {
        return {};
    }

    if (arg == "(") {
        optind++;
        auto command = parse_complex_expression(argv);
        if (command && argv[optind]) {
            auto const* next_option = argv[++optind];
            if (StringView { next_option, strlen(next_option) } == ")")
                return command;
        }

        fatal_error("Unmatched \033[1m(");
    }

    // Try to read a unary op.
    if (arg.starts_with('-') && arg.length() == 2) {
        if (argv[++optind] == nullptr)
            fatal_error("expected an argument");
        if (should_treat_expression_as_single_string({ argv[optind], strlen(argv[optind]) })) {
            --optind;
            return make<StringCompare>(move(arg), ""sv, StringCompare::NotEqual);
        }

        StringView value { argv[optind], strlen(argv[optind]) };
        switch (arg[1]) {
        case 'b':
            return make<FileIsOfKind>(value, FileIsOfKind::BlockDevice);
        case 'c':
            return make<FileIsOfKind>(value, FileIsOfKind::CharacterDevice);
        case 'd':
            return make<FileIsOfKind>(value, FileIsOfKind::Directory);
        case 'f':
            return make<FileIsOfKind>(value, FileIsOfKind::Regular);
        case 'h':
        case 'L':
            return make<FileIsOfKind>(value, FileIsOfKind::SymbolicLink);
        case 'p':
            return make<FileIsOfKind>(value, FileIsOfKind::FIFO);
        case 'S':
            return make<FileIsOfKind>(value, FileIsOfKind::Socket);
        case 'r':
            return make<UserHasPermission>(value, UserHasPermission::Read);
        case 'w':
            return make<UserHasPermission>(value, UserHasPermission::Write);
        case 'x':
            return make<UserHasPermission>(value, UserHasPermission::Execute);
        case 'e':
            return make<UserHasPermission>(value, UserHasPermission::Any);
        case 'g':
            return make<FileHasFlag>(value, FileHasFlag::SGID);
        case 'k':
            return make<FileHasFlag>(value, FileHasFlag::SVTX);
        case 'u':
            return make<FileHasFlag>(value, FileHasFlag::SUID);
        case 'o':
        case 'a':
            // '-a' and '-o' are boolean ops, which are part of a complex expression
            // so we have nothing to parse, simply return to caller.
            --optind;
            return {};
        case 'n':
            return make<StringCompare>(""sv, value, StringCompare::NotEqual);
        case 'z':
            return make<StringCompare>(""sv, value, StringCompare::Equal);
        case 'G':
            return make<FileIsOwnedBy>(value, FileIsOwnedBy::EffectiveGID);
        case 'O':
            return make<FileIsOwnedBy>(value, FileIsOwnedBy::EffectiveUID);
        case 'N':
        case 's':
            // 'optind' has been incremented to refer to the argument after the
            // operator, while we want to print the operator itself.
            fatal_error("Unsupported operator \033[1m%s", argv[optind - 1]);
        default:
            --optind;
            break;
        }
    }

    auto get_next_arg = [&argv]() -> StringView {
        auto const* next_arg = argv[++optind];
        if (next_arg == NULL)
            return StringView {};
        return StringView { next_arg, strlen(next_arg) };
    };

    // Try to read a binary op, this is either a <string> op <string>, <integer> op <integer>, or <file> op <file>.
    auto lhs = arg;
    arg = get_next_arg();

    if (arg == "=") {
        StringView rhs = get_next_arg();
        return make<StringCompare>(lhs, rhs, StringCompare::Equal);
    } else if (arg == "!=") {
        StringView rhs = get_next_arg();
        return make<StringCompare>(lhs, rhs, StringCompare::NotEqual);
    } else if (arg == "-eq") {
        StringView rhs = get_next_arg();
        return make<NumericCompare>(lhs, rhs, NumericCompare::Equal);
    } else if (arg == "-ge") {
        StringView rhs = get_next_arg();
        return make<NumericCompare>(lhs, rhs, NumericCompare::GreaterOrEqual);
    } else if (arg == "-gt") {
        StringView rhs = get_next_arg();
        return make<NumericCompare>(lhs, rhs, NumericCompare::Greater);
    } else if (arg == "-le") {
        StringView rhs = get_next_arg();
        return make<NumericCompare>(lhs, rhs, NumericCompare::LessOrEqual);
    } else if (arg == "-lt") {
        StringView rhs = get_next_arg();
        return make<NumericCompare>(lhs, rhs, NumericCompare::Less);
    } else if (arg == "-ne") {
        StringView rhs = get_next_arg();
        return make<NumericCompare>(lhs, rhs, NumericCompare::NotEqual);
    } else if (arg == "-ef") {
        StringView rhs = get_next_arg();
        return make<FileCompare>(lhs, rhs, FileCompare::Same);
    } else if (arg == "-nt") {
        StringView rhs = get_next_arg();
        return make<FileCompare>(lhs, rhs, FileCompare::ModificationTimestampGreater);
    } else if (arg == "-ot") {
        StringView rhs = get_next_arg();
        return make<FileCompare>(lhs, rhs, FileCompare::ModificationTimestampLess);
    } else if (arg == "-o" || arg == "-a") {
        // '-a' and '-o' are boolean ops, which are part of a complex expression
        // put them back and return with lhs as string compare.
        --optind;
        return make<StringCompare>(""sv, lhs, StringCompare::NotEqual);
    } else {
        // Now that we know it's not a well-formed expression, see if it's actually a negation
        if (lhs == "!") {
            if (should_treat_expression_as_single_string(arg))
                return make<StringCompare>(move(lhs), ""sv, StringCompare::NotEqual);

            auto command = parse_complex_expression(argv);
            if (!command)
                fatal_error("Expected an expression after \x1b[1m!");

            return make<Not>(command.release_nonnull());
        }
        --optind;
        return make<StringCompare>(""sv, lhs, StringCompare::NotEqual);
    }
}

static OwnPtr<Condition> parse_complex_expression(char* argv[])
{
    auto command = parse_simple_expression(argv);

    while (argv[optind] && argv[optind + 1]) {
        if (!command && argv[optind])
            fatal_error("expected an expression");

        auto const* arg_ptr = argv[++optind];
        StringView arg { arg_ptr, strlen(arg_ptr) };

        enum {
            AndOp,
            OrOp,
        } binary_operation { AndOp };

        if (arg == "-a") {
            if (argv[++optind] == nullptr)
                fatal_error("expected an expression");
            binary_operation = AndOp;
        } else if (arg == "-o") {
            if (argv[++optind] == nullptr)
                fatal_error("expected an expression");
            binary_operation = OrOp;
        } else {
            // Ooops, looked too far.
            optind--;
            return command;
        }
        auto rhs = parse_complex_expression(argv);
        if (!rhs)
            fatal_error("Missing right-hand side");

        if (binary_operation == AndOp)
            command = make<And>(command.release_nonnull(), rhs.release_nonnull());
        else
            command = make<Or>(command.release_nonnull(), rhs.release_nonnull());
    }

    return command;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto maybe_error = Core::System::pledge("stdio rpath");
    if (maybe_error.is_error()) {
        warnln("{}", maybe_error.error());
        return 126;
    }

    int argc = arguments.argc;
    if (LexicalPath::basename(arguments.strings[0]) == "[") {
        --argc;
        if (StringView { arguments.strings[argc] } != "]")
            fatal_error("test invoked as '[' requires a closing bracket ']'");
        arguments.strings[argc] = {};
    }

    // Exit false when no arguments are given.
    if (argc == 1)
        return 1;

    auto condition = parse_complex_expression(arguments.argv);
    if (optind != argc - 1)
        fatal_error("Too many arguments");
    auto result = condition ? condition->check() : false;

    if (g_there_was_an_error)
        return 126;
    return result ? 0 : 1;
}
