/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/GenericLexer.h>
#include <AK/LogStream.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Queue.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibRegex/Regex.h>
#include <stdio.h>
#include <unistd.h>

static void print_help_and_exit()
{
    outln(R"(
Usage: expr EXPRESSION
       expr [--help]

Print the value of EXPRESSION to standard output.)");
    exit(0);
}

template<typename... Args>
[[noreturn]] void fail(Args&&... args)
{
    warn("ERROR: \e[31m");
    warnln(args...);
    warn("\e[0m");
    exit(1);
}

class Expression {
public:
    enum Precedence {
        Or,
        And,
        Comp,
        ArithS,
        ArithM,
        StringO,
        Paren,
    };
    static NonnullOwnPtr<Expression> parse(Queue<StringView>& args, Precedence prec = Or);

    enum class Type {
        Integer,
        String,
    };

    virtual bool truth() const = 0;
    virtual int integer() const = 0;
    virtual String string() const = 0;
    virtual Type type() const = 0;
    virtual ~Expression() { }
};

class ValueExpression : public Expression {
public:
    ValueExpression(int v)
        : as_integer(v)
        , m_type(Type::Integer)
    {
    }

    ValueExpression(String&& v)
        : as_string(move(v))
        , m_type(Type::String)
    {
    }

    virtual ~ValueExpression() { }

private:
    virtual bool truth() const override
    {
        if (m_type == Type::String)
            return !as_string.is_empty();
        return integer() != 0;
    }
    virtual int integer() const override
    {
        switch (m_type) {
        case Type::Integer:
            return as_integer;
        case Type::String:
            if (auto converted = as_string.to_int(); converted.has_value())
                return converted.value();
            fail("Not an integer: '{}'", as_string);
        }
        ASSERT_NOT_REACHED();
    }
    virtual String string() const override
    {
        switch (m_type) {
        case Type::Integer:
            return String::formatted("{}", as_integer);
        case Type::String:
            return as_string;
        }
        ASSERT_NOT_REACHED();
    }
    virtual Type type() const override { return m_type; }

    union {
        int as_integer;
        String as_string;
    };
    Type m_type { Type::String };
};

class BooleanExpression : public Expression {
public:
    enum class BooleanOperator {
        And,
        Or,
    };
    static BooleanOperator op_from(const StringView& sv)
    {
        if (sv == "&")
            return BooleanOperator::And;
        return BooleanOperator::Or;
    }
    BooleanExpression(BooleanOperator op, NonnullOwnPtr<Expression>&& left, NonnullOwnPtr<Expression>&& right)
        : m_op(op)
        , m_left(move(left))
        , m_right(move(right))
    {
        if (m_op == BooleanOperator::Or)
            m_left_truth = m_left->truth();
        else
            m_right_truth = m_right->truth();
    }

private:
    virtual bool truth() const override
    {
        if (m_op == BooleanOperator::Or)
            return m_left_truth ? true : m_right->truth();
        return m_right_truth ? m_left->truth() : false;
    }

    virtual int integer() const override
    {
        switch (m_op) {
        case BooleanOperator::And:
            if (m_right_truth)
                return m_left->integer();
            return 0;
        case BooleanOperator::Or:
            if (m_left_truth)
                return m_left->integer();
            return m_right->integer();
        }
        ASSERT_NOT_REACHED();
    }

    virtual String string() const override
    {
        switch (m_op) {
        case BooleanOperator::And:
            if (m_right_truth)
                return m_left->string();
            return "0";
        case BooleanOperator::Or:
            if (m_left_truth)
                return m_left->string();
            return m_right->string();
        }
        ASSERT_NOT_REACHED();
    }
    virtual Type type() const override
    {
        switch (m_op) {
        case BooleanOperator::And:
            if (m_right_truth)
                return m_left->type();
            return m_right->type();
        case BooleanOperator::Or:
            if (m_left_truth)
                return m_left->type();
            return m_right->type();
        }
        ASSERT_NOT_REACHED();
    }

    BooleanOperator m_op { BooleanOperator::And };
    NonnullOwnPtr<Expression> m_left, m_right;
    bool m_left_truth { false }, m_right_truth { false };
};

class ComparisonExpression : public Expression {
public:
    enum class ComparisonOperation {
        Less,
        LessEq,
        Eq,
        Neq,
        GreaterEq,
        Greater,
    };

    static ComparisonOperation op_from(const StringView& sv)
    {
        if (sv == "<")
            return ComparisonOperation::Less;
        if (sv == "<=")
            return ComparisonOperation::LessEq;
        if (sv == "=")
            return ComparisonOperation::Eq;
        if (sv == "!=")
            return ComparisonOperation::Neq;
        if (sv == ">=")
            return ComparisonOperation::GreaterEq;
        return ComparisonOperation::Greater;
    }

    ComparisonExpression(ComparisonOperation op, NonnullOwnPtr<Expression>&& left, NonnullOwnPtr<Expression>&& right)
        : m_op(op)
        , m_left(move(left))
        , m_right(move(right))
    {
    }

private:
    template<typename T>
    bool compare(const T& left, const T& right) const
    {
        switch (m_op) {
        case ComparisonOperation::Less:
            return left < right;
        case ComparisonOperation::LessEq:
            return left == right || left < right;
        case ComparisonOperation::Eq:
            return left == right;
        case ComparisonOperation::Neq:
            return left != right;
        case ComparisonOperation::GreaterEq:
            return !(left < right);
        case ComparisonOperation::Greater:
            return left != right && !(left < right);
        }
        ASSERT_NOT_REACHED();
    }

    virtual bool truth() const override
    {
        switch (m_left->type()) {
        case Type::Integer:
            return compare(m_left->integer(), m_right->integer());
        case Type::String:
            return compare(m_left->string(), m_right->string());
        }
        ASSERT_NOT_REACHED();
    }
    virtual int integer() const override { return truth(); }
    virtual String string() const override { return truth() ? "1" : "0"; }
    virtual Type type() const override { return Type::Integer; }

    ComparisonOperation m_op { ComparisonOperation::Less };
    NonnullOwnPtr<Expression> m_left, m_right;
};

class ArithmeticExpression : public Expression {
public:
    enum class ArithmeticOperation {
        Sum,
        Difference,
        Product,
        Quotient,
        Remainder,
    };
    static ArithmeticOperation op_from(const StringView& sv)
    {
        if (sv == "+")
            return ArithmeticOperation::Sum;
        if (sv == "-")
            return ArithmeticOperation::Difference;
        if (sv == "*")
            return ArithmeticOperation::Product;
        if (sv == "/")
            return ArithmeticOperation::Quotient;
        return ArithmeticOperation::Remainder;
    }
    ArithmeticExpression(ArithmeticOperation op, NonnullOwnPtr<Expression>&& left, NonnullOwnPtr<Expression>&& right)
        : m_op(op)
        , m_left(move(left))
        , m_right(move(right))
    {
    }

private:
    virtual bool truth() const override
    {
        switch (m_op) {
        case ArithmeticOperation::Sum:
            return m_left->truth() || m_right->truth();
        default:
            return integer() != 0;
        }
    }
    virtual int integer() const override
    {
        auto right = m_right->integer();
        if (right == 0) {
            if (m_op == ArithmeticOperation::Product)
                return 0;
            if (m_op == ArithmeticOperation::Quotient || m_op == ArithmeticOperation::Remainder)
                fail("Division by zero");
        }

        auto left = m_left->integer();
        switch (m_op) {
        case ArithmeticOperation::Product:
            return right * left;
        case ArithmeticOperation::Sum:
            return right + left;
        case ArithmeticOperation::Difference:
            return left - right;
        case ArithmeticOperation::Quotient:
            return left / right;
        case ArithmeticOperation::Remainder:
            return left % right;
        }
        ASSERT_NOT_REACHED();
    }
    virtual String string() const override
    {
        return String::formatted("{}", integer());
    }
    virtual Type type() const override
    {
        return Type::Integer;
    }

    ArithmeticOperation m_op { ArithmeticOperation::Sum };
    NonnullOwnPtr<Expression> m_left, m_right;
};

class StringExpression : public Expression {
public:
    enum class StringOperation {
        Substring,
        Index,
        Length,
        Match,
    };

    StringExpression(StringOperation op, NonnullOwnPtr<Expression> string, OwnPtr<Expression> pos_or_chars = {}, OwnPtr<Expression> length = {})
        : m_op(op)
        , m_str(move(string))
        , m_pos_or_chars(move(pos_or_chars))
        , m_length(move(length))
    {
    }

private:
    virtual bool truth() const override { return integer() != 0; }
    virtual int integer() const override
    {
        if (m_op == StringOperation::Substring || m_op == StringOperation::Match) {
            auto substr = string();
            if (auto integer = substr.to_int(); integer.has_value())
                return integer.value();
            else
                fail("Not an integer: '{}'", substr);
        }

        if (m_op == StringOperation::Index) {
            if (auto idx = m_str->string().index_of(m_pos_or_chars->string()); idx.has_value())
                return idx.value() + 1;
            return 0;
        }

        if (m_op == StringOperation::Length)
            return m_str->string().length();

        ASSERT_NOT_REACHED();
    }
    static auto safe_substring(const String& str, int start, int length)
    {
        if (start < 1 || (size_t)start > str.length())
            fail("Index out of range");
        --start;
        if (str.length() - start < (size_t)length)
            fail("Index out of range");
        return str.substring(start, length);
    }
    virtual String string() const override
    {
        if (m_op == StringOperation::Substring)
            return safe_substring(m_str->string(), m_pos_or_chars->integer(), m_length->integer());

        if (m_op == StringOperation::Match) {
            auto match = m_compiled_regex->match(m_str->string(), PosixFlags::Global);
            if (m_compiled_regex->parser_result.capture_groups_count == 0) {
                if (!match.success)
                    return "0";

                size_t count = 0;
                for (auto& m : match.matches)
                    count += m.view.length();

                return String::number(count);
            } else {
                if (!match.success)
                    return "";

                StringBuilder result;
                for (auto& m : match.capture_group_matches) {
                    for (auto& e : m)
                        result.append(e.view.to_string());
                }

                return result.build();
            }
        }

        return String::number(integer());
    }
    virtual Type type() const override
    {
        if (m_op == StringOperation::Substring)
            return Type::String;
        if (m_op == StringOperation::Match) {
            if (!m_pos_or_chars)
                fail("'match' expects a string pattern");

            ensure_regex();
            if (m_compiled_regex->parser_result.capture_groups_count == 0)
                return Type::Integer;

            return Type::String;
        }
        return Type::Integer;
    }

    void ensure_regex() const
    {
        if (!m_compiled_regex)
            m_compiled_regex = make<regex::Regex<PosixExtended>>(m_pos_or_chars->string());
    }

    StringOperation m_op { StringOperation::Substring };
    NonnullOwnPtr<Expression> m_str;
    OwnPtr<Expression> m_pos_or_chars, m_length;
    mutable OwnPtr<regex::Regex<PosixExtended>> m_compiled_regex;
};

NonnullOwnPtr<Expression> Expression::parse(Queue<StringView>& args, Precedence prec)
{
    switch (prec) {
    case Or: {
        auto left = parse(args, And);
        while (!args.is_empty() && args.head() == "|") {
            args.dequeue();
            auto right = parse(args, And);
            left = make<BooleanExpression>(BooleanExpression::BooleanOperator::Or, move(left), move(right));
        }
        return left;
    }
    case And: {
        auto left = parse(args, Comp);
        while (!args.is_empty() && args.head() == "&") {
            args.dequeue();
            auto right = parse(args, Comp);
            left = make<BooleanExpression>(BooleanExpression::BooleanOperator::And, move(left), move(right));
        }
        return left;
    }
    case Comp: {
        auto left = parse(args, ArithS);
        while (!args.is_empty() && args.head().is_one_of("<", "<=", "=", "!=", "=>", ">")) {
            auto op = args.dequeue();
            auto right = parse(args, ArithM);
            left = make<ComparisonExpression>(ComparisonExpression::op_from(op), move(left), move(right));
        }
        return left;
    }
    case ArithS: {
        auto left = parse(args, ArithM);
        while (!args.is_empty() && args.head().is_one_of("+", "-")) {
            auto op = args.dequeue();
            auto right = parse(args, ArithM);
            left = make<ArithmeticExpression>(ArithmeticExpression::op_from(op), move(left), move(right));
        }
        return left;
    }
    case ArithM: {
        auto left = parse(args, StringO);
        while (!args.is_empty() && args.head().is_one_of("*", "/", "%")) {
            auto op = args.dequeue();
            auto right = parse(args, StringO);
            left = make<ArithmeticExpression>(ArithmeticExpression::op_from(op), move(left), move(right));
        }
        return left;
    }
    case StringO: {
        if (args.is_empty())
            fail("Expected a term");

        OwnPtr<Expression> left;

        while (!args.is_empty()) {
            auto& op = args.head();
            if (op == "+") {
                args.dequeue();
                left = make<ValueExpression>(args.dequeue());
            } else if (op == "substr") {
                args.dequeue();
                auto str = parse(args, Paren);
                auto pos = parse(args, Paren);
                auto len = parse(args, Paren);
                left = make<StringExpression>(StringExpression::StringOperation::Substring, move(str), move(pos), move(len));
            } else if (op == "index") {
                args.dequeue();
                auto str = parse(args, Paren);
                auto chars = parse(args, Paren);
                left = make<StringExpression>(StringExpression::StringOperation::Index, move(str), move(chars));
            } else if (op == "match") {
                args.dequeue();
                auto str = parse(args, Paren);
                auto pattern = parse(args, Paren);
                left = make<StringExpression>(StringExpression::StringOperation::Match, move(str), move(pattern));
            } else if (op == "length") {
                args.dequeue();
                auto str = parse(args, Paren);
                left = make<StringExpression>(StringExpression::StringOperation::Length, move(str));
            } else if (!left) {
                left = parse(args, Paren);
            }

            if (!args.is_empty() && args.head() == ":") {
                args.dequeue();
                auto right = parse(args, Paren);
                left = make<StringExpression>(StringExpression::StringOperation::Match, left.release_nonnull(), move(right));
            } else {
                return left.release_nonnull();
            }
        }

        return left.release_nonnull();
    }
    case Paren: {
        if (args.is_empty())
            fail("Expected a term");

        if (args.head() == "(") {
            args.dequeue();
            auto expr = parse(args);
            if (args.head() != ")")
                fail("Expected a close paren");
            args.dequeue();
            return expr;
        }

        return make<ValueExpression>(args.dequeue());
    }
    }

    fail("Invalid expression");
}

int main(int argc, char** argv)
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    if ((argc == 2 && StringView { "--help" } == argv[1]) || argc == 1)
        print_help_and_exit();

    Queue<StringView> args;
    for (int i = 1; i < argc; ++i)
        args.enqueue(argv[i]);

    auto expression = Expression::parse(args);
    if (!args.is_empty())
        fail("Extra tokens at the end of the expression");

    switch (expression->type()) {
    case Expression::Type::Integer:
        outln("{}", expression->integer());
        break;
    case Expression::Type::String:
        outln("{}", expression->string());
        break;
    }
    return 0;
}
