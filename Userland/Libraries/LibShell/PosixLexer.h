/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/Queue.h>
#include <AK/String.h>
#include <AK/TemporaryChange.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibShell/AST.h>

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

    // Separate rule, not used by the main flow.
    HeredocContents,
};

struct ExpansionRange {
    size_t start;
    size_t length;
};

struct ParameterExpansion {
    StringBuilder parameter;
    ExpansionRange range;
};

struct CommandExpansion {
    StringBuilder command;
    ExpansionRange range;
};

struct ArithmeticExpansion {
    String expression;
    StringBuilder value;
    ExpansionRange range;
};

using Expansion = Variant<ParameterExpansion, CommandExpansion, ArithmeticExpansion>;

struct ResolvedParameterExpansion {
    String parameter;
    String argument;
    ExpansionRange range;
    enum class Op {
        UseDefaultValue,                    // ${parameter:-word}
        AssignDefaultValue,                 // ${parameter:=word}
        IndicateErrorIfEmpty,               // ${parameter:?word}
        UseAlternativeValue,                // ${parameter:+word}
        UseDefaultValueIfUnset,             // ${parameter-default}
        AssignDefaultValueIfUnset,          // ${parameter=default}
        IndicateErrorIfUnset,               // ${parameter?default}
        UseAlternativeValueIfUnset,         // ${parameter+default}
        RemoveLargestSuffixByPattern,       // ${parameter%%pattern}
        RemoveLargestPrefixByPattern,       // ${parameter##pattern}
        RemoveSmallestSuffixByPattern,      // ${parameter%pattern}
        RemoveSmallestPrefixByPattern,      // ${parameter#pattern}
        StringLength,                       // ${#parameter}
        GetPositionalParameter,             // ${parameter}
        GetVariable,                        // ${parameter}
        GetLastBackgroundPid,               // $!
        GetPositionalParameterList,         // $*
        GetCurrentOptionFlags,              // $-
        GetPositionalParameterCount,        // $#
        GetLastExitStatus,                  // $?
        GetPositionalParameterListAsString, // $@
        GetShellProcessId,                  // $$
    } op;

    enum class Expand {
        Nothing,
        Word,
    } expand { Expand::Nothing };

    ByteString to_byte_string() const
    {
        StringBuilder builder;
        builder.append("{"sv);
        switch (op) {
        case Op::UseDefaultValue:
            builder.append("UseDefaultValue"sv);
            break;
        case Op::AssignDefaultValue:
            builder.append("AssignDefaultValue"sv);
            break;
        case Op::IndicateErrorIfEmpty:
            builder.append("IndicateErrorIfEmpty"sv);
            break;
        case Op::UseAlternativeValue:
            builder.append("UseAlternativeValue"sv);
            break;
        case Op::UseDefaultValueIfUnset:
            builder.append("UseDefaultValueIfUnset"sv);
            break;
        case Op::AssignDefaultValueIfUnset:
            builder.append("AssignDefaultValueIfUnset"sv);
            break;
        case Op::IndicateErrorIfUnset:
            builder.append("IndicateErrorIfUnset"sv);
            break;
        case Op::UseAlternativeValueIfUnset:
            builder.append("UseAlternativeValueIfUnset"sv);
            break;
        case Op::RemoveLargestSuffixByPattern:
            builder.append("RemoveLargestSuffixByPattern"sv);
            break;
        case Op::RemoveLargestPrefixByPattern:
            builder.append("RemoveLargestPrefixByPattern"sv);
            break;
        case Op::RemoveSmallestSuffixByPattern:
            builder.append("RemoveSmallestSuffixByPattern"sv);
            break;
        case Op::RemoveSmallestPrefixByPattern:
            builder.append("RemoveSmallestPrefixByPattern"sv);
            break;
        case Op::StringLength:
            builder.append("StringLength"sv);
            break;
        case Op::GetPositionalParameter:
            builder.append("GetPositionalParameter"sv);
            break;
        case Op::GetLastBackgroundPid:
            builder.append("GetLastBackgroundPid"sv);
            break;
        case Op::GetPositionalParameterList:
            builder.append("GetPositionalParameterList"sv);
            break;
        case Op::GetCurrentOptionFlags:
            builder.append("GetCurrentOptionFlags"sv);
            break;
        case Op::GetPositionalParameterCount:
            builder.append("GetPositionalParameterCount"sv);
            break;
        case Op::GetLastExitStatus:
            builder.append("GetLastExitStatus"sv);
            break;
        case Op::GetPositionalParameterListAsString:
            builder.append("GetPositionalParameterListAsString"sv);
            break;
        case Op::GetShellProcessId:
            builder.append("GetShellProcessId"sv);
            break;
        case Op::GetVariable:
            builder.append("GetVariable"sv);
            break;
        }
        builder.append(" "sv);
        builder.append(parameter);
        builder.append(" ("sv);
        builder.append(argument);
        builder.append(")"sv);
        builder.append("}"sv);
        return builder.to_byte_string();
    }
};

struct ResolvedCommandExpansion {
    RefPtr<AST::Node> command;
    ExpansionRange range;
};

struct ResolvedArithmeticExpansion {
    String source_expression;
    ExpansionRange range;
};

using ResolvedExpansion = Variant<ResolvedParameterExpansion, ResolvedCommandExpansion, ResolvedArithmeticExpansion>;

struct HeredocEntry {
    String key;
    bool allow_interpolation;
    bool dedent;
};

struct State {
    StringBuilder buffer {};
    Reduction previous_reduction { Reduction::Start };
    bool escaping { false };
    bool in_skip_mode { false };
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
    Vector<HeredocEntry> heredoc_entries {};
    bool on_new_line { true };
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
        HeredocContents,

        // Not produced by this lexer, but generated in later stages.
        AssignmentWord,
        ListAssignmentWord,
        Bang,
        Case,
        CloseBrace,
        Do,
        Done,
        Elif,
        Else,
        Esac,
        Fi,
        For,
        If,
        In,
        IoNumber,
        OpenBrace,
        Then,
        Until,
        VariableName,
        While,
        Word,
    };

    Type type;
    String value;
    Optional<AST::Position> position;
    Vector<Expansion> expansions;
    Vector<ResolvedExpansion> resolved_expansions {};
    StringView original_text;
    Optional<String> relevant_heredoc_key {};
    bool could_be_start_of_a_simple_command { false };

    static ErrorOr<Vector<Token>> maybe_from_state(State const& state)
    {
        if (state.buffer.is_empty() || state.buffer.string_view().trim_whitespace().is_empty())
            return Vector<Token> {};

        auto token = Token {
            .type = Type::Token,
            .value = TRY(state.buffer.to_string()),
            .position = state.position,
            .expansions = state.expansions,
            .original_text = {},
        };
        return Vector<Token> { move(token) };
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
        if (name == ">"sv)
            return Token::Type::Great;
        if (name == "<"sv)
            return Token::Type::Less;
        if (name == "\n"sv)
            return Token::Type::Newline;
        if (name == "("sv)
            return Token::Type::OpenParen;
        if (name == "{"sv)
            return Token::Type::OpenBrace;
        if (name == ")"sv)
            return Token::Type::CloseParen;
        if (name == "}"sv)
            return Token::Type::CloseBrace;

        return {};
    }

    static ErrorOr<Vector<Token>> operators_from(State const& state)
    {
        auto name = TRY(state.buffer.to_string());
        auto type = operator_from_name(name);
        if (!type.has_value())
            return Vector<Token> {};

        return Vector {
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
            .value = "\n"_string,
            .position = {},
            .expansions = {},
            .original_text = {},
        };
    }

    static Token continuation(char expected)
    {
        return {
            .type = Type::Continuation,
            .value = String::from_code_point(expected),
            .position = {},
            .expansions = {},
            .original_text = {},
        };
    }

    static Token continuation(String expected)
    {
        return {
            .type = Type::Continuation,
            .value = move(expected),
            .position = {},
            .expansions = {},
            .original_text = {},
        };
    }

    StringView type_name() const;
};

class Lexer {
public:
    explicit Lexer(StringView input)
        : m_lexer(input)
    {
    }

    ErrorOr<Vector<Token>> batch_next(Optional<Reduction> starting_reduction = {});

    struct HeredocKeyResult {
        String key;
        bool allow_interpolation;
    };

    static HeredocKeyResult process_heredoc_key(Token const&);

private:
    struct ReductionResult {
        Vector<Token> tokens;
        Reduction next_reduction { Reduction::None };
    };

    ErrorOr<ReductionResult> reduce(Reduction);
    ErrorOr<ReductionResult> reduce_end();
    ErrorOr<ReductionResult> reduce_operator();
    ErrorOr<ReductionResult> reduce_comment();
    ErrorOr<ReductionResult> reduce_single_quoted_string();
    ErrorOr<ReductionResult> reduce_double_quoted_string();
    ErrorOr<ReductionResult> reduce_expansion();
    ErrorOr<ReductionResult> reduce_command_expansion();
    ErrorOr<ReductionResult> reduce_start();
    ErrorOr<ReductionResult> reduce_arithmetic_expansion();
    ErrorOr<ReductionResult> reduce_special_parameter_expansion();
    ErrorOr<ReductionResult> reduce_parameter_expansion();
    ErrorOr<ReductionResult> reduce_command_or_arithmetic_substitution_expansion();
    ErrorOr<ReductionResult> reduce_extended_parameter_expansion();
    ErrorOr<ReductionResult> reduce_heredoc_contents();

    struct SkipTokens {
        explicit SkipTokens(Lexer& lexer)
            : m_state_change(lexer.m_state, lexer.m_state)
        {
            lexer.m_state.in_skip_mode = true;
        }

        TemporaryChange<State> m_state_change;
    };

    SkipTokens switch_to_skip_mode() { return SkipTokens { *this }; }

    char consume();
    bool consume_specific(char);
    void reconsume(StringView);
    ExpansionRange range(ssize_t offset = 0) const;

    GenericLexer m_lexer;
    State m_state;
    Reduction m_next_reduction { Reduction::Start };
};

}
