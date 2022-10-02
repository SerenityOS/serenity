/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <Shell/AST.h>

namespace Shell::Posix {
enum class Reduction {
    None,
    End,
    Operator,
    Comment,
    SingleQuotedString,
    DoubleQuotedString,
    Expansion,
    CommandExpansion,
    Start,
    ArithmeticExpansion,
    SpecialParameterExpansion,
    ParameterExpansion,
    CommandOrArithmeticSubstitutionExpansion,
    ExtendedParameterExpansion,
};

struct ParameterExpansion {
    StringBuilder parameter;
};
struct CommandExpansion {
    StringBuilder command;
    AST::Position location;
};
struct ArithmeticExpansion {
    String expression;
    StringBuilder value;
    AST::Position location;
};
using Expansion = Variant<ParameterExpansion, CommandExpansion, ArithmeticExpansion>;

struct State {
    StringBuilder buffer {};
    Reduction previous_reduction { Reduction::Start };
    bool escaping { false };
    AST::Position position {
        .start_offset = 0,
        .end_offset = 0,
        .start_line = {
            .line_number = 0,
            .line_column = 0,
        },
        .end_line = {
            .line_number = 0,
            .line_column = 0,
        },
    };
    Vector<Expansion> expansions {};
};

struct Token {
    enum class Type {
        Eof,
        Newline,
        Continuation,
        Token,
        And,
        Pipe,
        OpenParen,
        CloseParen,
        Great,
        Less,
        AndIf,
        OrIf,
        DoubleSemicolon,
        DoubleLess,
        DoubleGreat,
        LessAnd,
        GreatAnd,
        LessGreat,
        DoubleLessDash,
        Clobber,
        Semicolon,
    };

    Type type;
    String value;
    Optional<AST::Position> position;
    Vector<Expansion> expansions;
    StringView original_text;

    static Vector<Token> maybe_from_state(State const& state)
    {
        if (state.buffer.is_empty() || state.buffer.string_view().trim_whitespace().is_empty())
            return {};

        auto token = Token {
            .type = Type::Token,
            .value = state.buffer.to_string(),
            .position = state.position,
            .expansions = state.expansions,
            .original_text = {},
        };
        return { move(token) };
    }

    static Optional<Token::Type> operator_from_name(StringView name)
    {
        if (name == "&&"sv)
            return Token::Type::AndIf;
        if (name == "||"sv)
            return Token::Type::OrIf;
        if (name == ";;"sv)
            return Token::Type::DoubleSemicolon;
        if (name == "<<"sv)
            return Token::Type::DoubleLess;
        if (name == ">>"sv)
            return Token::Type::DoubleGreat;
        if (name == "<&"sv)
            return Token::Type::LessAnd;
        if (name == ">&"sv)
            return Token::Type::GreatAnd;
        if (name == "<>"sv)
            return Token::Type::LessGreat;
        if (name == "<<-"sv)
            return Token::Type::DoubleLessDash;
        if (name == ">|"sv)
            return Token::Type::Clobber;
        if (name == ";"sv)
            return Token::Type::Semicolon;
        if (name == "&"sv)
            return Token::Type::And;
        if (name == "|"sv)
            return Token::Type::Pipe;
        if (name == "("sv)
            return Token::Type::OpenParen;
        if (name == ")"sv)
            return Token::Type::CloseParen;
        if (name == ">"sv)
            return Token::Type::Great;
        if (name == "<"sv)
            return Token::Type::Less;

        return {};
    }

    static Vector<Token> operators_from(State const& state)
    {
        auto name = state.buffer.string_view();
        auto type = operator_from_name(name);
        if (!type.has_value())
            return {};

        return {
            Token {
                .type = *type,
                .value = name,
                .position = state.position,
                .expansions = {},
                .original_text = {},
            }
        };
    }

    static Token eof()
    {
        return {
            .type = Type::Eof,
            .value = {},
            .position = {},
            .expansions = {},
            .original_text = {},
        };
    }

    static Token newline()
    {
        return {
            .type = Type::Newline,
            .value = "\n",
            .position = {},
            .expansions = {},
            .original_text = {},
        };
    }

    static Token continuation(char expected)
    {
        return {
            .type = Type::Continuation,
            .value = String::formatted("{:c}", expected),
            .position = {},
            .expansions = {},
            .original_text = {},
        };
    }

    static Token continuation(String expected)
    {
        return {
            .type = Type::Continuation,
            .value = expected,
            .position = {},
            .expansions = {},
            .original_text = {},
        };
    }
};

class Lexer {
public:
    explicit Lexer(StringView input)
        : m_lexer(input)
    {
    }

    Vector<Token> batch_next();

private:
    struct ReductionResult {
        Vector<Token> tokens;
        Reduction next_reduction { Reduction::None };
    };

    ReductionResult reduce(Reduction);
    ReductionResult reduce_end();
    ReductionResult reduce_operator();
    ReductionResult reduce_comment();
    ReductionResult reduce_single_quoted_string();
    ReductionResult reduce_double_quoted_string();
    ReductionResult reduce_expansion();
    ReductionResult reduce_command_expansion();
    ReductionResult reduce_start();
    ReductionResult reduce_arithmetic_expansion();
    ReductionResult reduce_special_parameter_expansion();
    ReductionResult reduce_parameter_expansion();
    ReductionResult reduce_command_or_arithmetic_substitution_expansion();
    ReductionResult reduce_extended_parameter_expansion();

    char consume();
    bool consume_specific(char);

    GenericLexer m_lexer;
    State m_state;
    Reduction m_next_reduction { Reduction::Start };
};
}
