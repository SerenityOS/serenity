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

#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <LibCore/File.h>
#include <getopt.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

bool g_there_was_an_error = false;

[[noreturn]] static void fatal_error(const char* format, ...)
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
    virtual ~Condition() { }
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
            ASSERT_NOT_REACHED();
        }
    }

    String m_path;
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
            ASSERT_NOT_REACHED();
        }
    }

    String m_path;
    Permission m_kind { Read };
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

    NumericCompare(String lhs, String rhs, Mode mode)
        : m_mode(mode)
    {
        auto lhs_option = lhs.trim_whitespace().to_int();
        auto rhs_option = rhs.trim_whitespace().to_int();

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
            ASSERT_NOT_REACHED();
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

    FileCompare(String lhs, String rhs, Mode mode)
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
            ASSERT_NOT_REACHED();
        }
    }

    String m_lhs;
    String m_rhs;
    Mode m_mode { Same };
};

static OwnPtr<Condition> parse_complex_expression(char* argv[]);

static bool should_treat_expression_as_single_string(const StringView& arg_after)
{
    return arg_after.is_null() || arg_after == "-a" || arg_after == "-o";
}

static OwnPtr<Condition> parse_simple_expression(char* argv[])
{
    StringView arg = argv[optind];
    if (arg.is_null()) {
        return nullptr;
    }

    if (arg == "(") {
        optind++;
        auto command = parse_complex_expression(argv);
        if (command && argv[optind] && StringView(argv[++optind]) == ")")
            return command;
        fatal_error("Unmatched \033[1m(");
    }

    if (arg == "!") {
        if (should_treat_expression_as_single_string(argv[optind]))
            return make<StringCompare>(move(arg), "", StringCompare::NotEqual);
        auto command = parse_complex_expression(argv);
        if (!command)
            fatal_error("Expected an expression after \033[1m!");
        return make<Not>(command.release_nonnull());
    }

    // Try to read a unary op.
    if (arg.starts_with('-') && arg.length() == 2) {
        optind++;
        if (should_treat_expression_as_single_string(argv[optind])) {
            --optind;
            return make<StringCompare>(move(arg), "", StringCompare::NotEqual);
        }

        StringView value = argv[optind];
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
        case 'o':
        case 'a':
            // '-a' and '-o' are boolean ops, which are part of a complex expression
            // so we have nothing to parse, simply return to caller.
            --optind;
            return nullptr;
        case 'n':
            return make<StringCompare>("", value, StringCompare::NotEqual);
        case 'z':
            return make<StringCompare>("", value, StringCompare::Equal);
        case 'g':
        case 'G':
        case 'k':
        case 'N':
        case 'O':
        case 's':
            fatal_error("Unsupported operator \033[1m%s", argv[optind]);
        default:
            break;
        }
    }

    // Try to read a binary op, this is either a <string> op <string>, <integer> op <integer>, or <file> op <file>.
    auto lhs = arg;
    arg = argv[++optind];

    if (arg == "=") {
        StringView rhs = argv[++optind];
        return make<StringCompare>(lhs, rhs, StringCompare::Equal);
    } else if (arg == "!=") {
        StringView rhs = argv[++optind];
        return make<StringCompare>(lhs, rhs, StringCompare::NotEqual);
    } else if (arg == "-eq") {
        StringView rhs = argv[++optind];
        return make<NumericCompare>(lhs, rhs, NumericCompare::Equal);
    } else if (arg == "-ge") {
        StringView rhs = argv[++optind];
        return make<NumericCompare>(lhs, rhs, NumericCompare::GreaterOrEqual);
    } else if (arg == "-gt") {
        StringView rhs = argv[++optind];
        return make<NumericCompare>(lhs, rhs, NumericCompare::Greater);
    } else if (arg == "-le") {
        StringView rhs = argv[++optind];
        return make<NumericCompare>(lhs, rhs, NumericCompare::LessOrEqual);
    } else if (arg == "-lt") {
        StringView rhs = argv[++optind];
        return make<NumericCompare>(lhs, rhs, NumericCompare::Less);
    } else if (arg == "-ne") {
        StringView rhs = argv[++optind];
        return make<NumericCompare>(lhs, rhs, NumericCompare::NotEqual);
    } else if (arg == "-ef") {
        StringView rhs = argv[++optind];
        return make<FileCompare>(lhs, rhs, FileCompare::Same);
    } else if (arg == "-nt") {
        StringView rhs = argv[++optind];
        return make<FileCompare>(lhs, rhs, FileCompare::ModificationTimestampGreater);
    } else if (arg == "-ot") {
        StringView rhs = argv[++optind];
        return make<FileCompare>(lhs, rhs, FileCompare::ModificationTimestampLess);
    } else if (arg == "-o" || arg == "-a") {
        // '-a' and '-o' are boolean ops, which are part of a complex expression
        // put them back and return with lhs as string compare.
        --optind;
        return make<StringCompare>("", lhs, StringCompare::NotEqual);
    } else {
        --optind;
        return make<StringCompare>("", lhs, StringCompare::NotEqual);
    }
}

static OwnPtr<Condition> parse_complex_expression(char* argv[])
{
    auto command = parse_simple_expression(argv);

    while (argv[optind] && argv[optind + 1]) {
        if (!command && argv[optind])
            fatal_error("expected an expression");

        StringView arg = argv[++optind];

        enum {
            AndOp,
            OrOp,
        } binary_operation { AndOp };

        if (arg == "-a") {
            optind++;
            binary_operation = AndOp;
        } else if (arg == "-o") {
            optind++;
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

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 126;
    }

    if (LexicalPath { argv[0] }.basename() == "[") {
        --argc;
        if (StringView { argv[argc] } != "]")
            fatal_error("test invoked as '[' requires a closing bracket ']'");
        argv[argc] = nullptr;
    }

    // Exit false when no arguments are given.
    if (argc == 1)
        return 1;

    auto condition = parse_complex_expression(argv);
    if (optind != argc - 1)
        fatal_error("Too many arguments");
    auto result = condition ? condition->check() : false;

    if (g_there_was_an_error)
        return 126;
    return result ? 0 : 1;
}
