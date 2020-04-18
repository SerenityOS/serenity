/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include "regex.h"
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <ctype.h>
#include <stdio.h>

namespace regex {

const char* StackValue::name(OpCode type)
{
    switch (type) {
#define __ENUMERATE_OPCODE(x) \
    case OpCode::x:           \
        return #x;
        ENUMERATE_OPCODES
#undef __ENUMERATE_OPCODE
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* StackValue::name() const
{
    return name(op_code);
}

const char* Token::name(TokenType type)
{
    switch (type) {
#define __ENUMERATE_REGEX_TOKEN(x) \
    case TokenType::x:             \
        return #x;
        ENUMERATE_REGEX_TOKENS
#undef __ENUMERATE_REGEX_TOKEN
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* Token::name() const
{
    return name(m_type);
}

Lexer::Lexer(const StringView source)
    : m_source(source)
    , m_current_token(TokenType::Eof, 0, StringView(nullptr))
{
}

char Lexer::peek(size_t offset) const
{
    if ((m_position + offset) >= m_source.length())
        return EOF;
    return m_source[m_position + offset];
}

void Lexer::back(size_t offset)
{
    m_position -= offset;
    m_previous_position = m_position - 1;
    m_current_char = m_source[m_position];
}

void Lexer::consume()
{
    m_previous_position = m_position;

    if (m_position >= m_source.length()) {
        m_position = m_source.length() + 1;
        m_current_char = EOF;
        return;
    }

    m_current_char = m_source[m_position++];
}

void Lexer::reset()
{
    m_position = 0;
    m_current_token = { TokenType::Eof, 0, StringView(nullptr) };
    m_current_char = 0;
    m_previous_position = 0;
}

Token Lexer::next()
{
    size_t token_start_position;

    auto begin_token = [&] {
        token_start_position = m_position;
    };

    auto commit_token = [&](auto type) {
        auto substring = m_source.substring_view(token_start_position, m_previous_position - token_start_position + 1);
        m_current_token = Token(type, token_start_position, substring);
    };

    auto emit_token = [&](auto type) {
        m_current_token = Token(type, m_position, m_source.substring_view(m_position, 1));
        consume();
    };

    auto match_escape_sequence = [&]() -> size_t {
        switch (peek(1)) {
        case '^':
        case '.':
        case '[':
        case '$':
        case '(':
        case ')':
        case '|':
        case '*':
        case '+':
        case '?':
        case '{':
        case '\\':
            return 2;
        default:
            return 0;
        }
    };

    while (m_position <= m_source.length()) {

        auto ch = peek();
        if (ch == '(') {
            emit_token(TokenType::LeftParen);
            return m_current_token;
        }
        if (ch == ')') {
            emit_token(TokenType::RightParen);
            return m_current_token;
        }
        if (ch == '{') {
            emit_token(TokenType::LeftCurly);
            return m_current_token;
        }
        if (ch == '}') {
            emit_token(TokenType::RightCurly);
            return m_current_token;
        }
        if (ch == '[') {
            emit_token(TokenType::LeftBracket);
            return m_current_token;
        }
        if (ch == ']') {
            emit_token(TokenType::RightBracket);
            return m_current_token;
        }
        if (ch == '.') {
            emit_token(TokenType::Period);
            return m_current_token;
        }
        if (ch == '*') {
            emit_token(TokenType::Asterisk);
            return m_current_token;
        }
        if (ch == '+') {
            emit_token(TokenType::Plus);
            return m_current_token;
        }
        if (ch == '$') {
            emit_token(TokenType::Dollar);
            return m_current_token;
        }
        if (ch == '^') {
            emit_token(TokenType::Circumflex);
            return m_current_token;
        }
        if (ch == '|') {
            emit_token(TokenType::Pipe);
            return m_current_token;
        }
        if (ch == '?') {
            emit_token(TokenType::Questionmark);
            return m_current_token;
        }
        if (ch == ',') {
            emit_token(TokenType::Comma);
            return m_current_token;
        }
        if (ch == '\\') {
            size_t escape = match_escape_sequence();
            if (escape > 0) {
                begin_token();
                for (size_t i = 0; i < escape; ++i)
                    consume();
                commit_token(TokenType::EscapeSequence);
                return m_current_token;
            }
        }

        if (ch == EOF)
            break;

        emit_token(TokenType::OrdinaryCharacter);
        return m_current_token;
    }

    return Token(TokenType::Eof, m_position, m_source.substring_view(0, 0));
}

Parser::ParserState::ParserState(Lexer lexer)
    : m_lexer(move(lexer))
    , m_current_token(m_lexer.next())
{
}

Parser::Parser(Lexer lexer)
    : m_parser_state(move(lexer))
{
}

bool Parser::match_ere_quoted_chars()
{
    auto type = m_parser_state.m_current_token.type();
    return (type == TokenType::Circumflex
        || type == TokenType::Period
        || type == TokenType::LeftBracket
        || type == TokenType::Dollar
        || type == TokenType::LeftParen
        || type == TokenType::RightParen
        || type == TokenType::Pipe
        || type == TokenType::Asterisk
        || type == TokenType::Plus
        || type == TokenType::Questionmark
        || type == TokenType::LeftCurly
        || type == TokenType::EscapeSequence);
}

bool Parser::match_ere_dupl_symbol()
{
    auto type = m_parser_state.m_current_token.type();
    return (type == TokenType::Asterisk
        || type == TokenType::Plus
        || type == TokenType::Questionmark
        || type == TokenType::LeftCurly);
}

bool Parser::parse_ere_dupl_symbol(Vector<StackValue>& operations, size_t& min_length)
{
    if (match(TokenType::LeftCurly)) {
        consume();

        bool is_minimum { false };
        StringBuilder number1_builder;
        while (match(TokenType::OrdinaryCharacter)) {
            number1_builder.append(consume().value());
        }
        bool ok;
        size_t number1 = number1_builder.build().to_uint(ok);
        if (!ok)
            return set_error(REG_BADBR);

        if (match(TokenType::Comma)) {
            consume();
            is_minimum = true;
        }

        size_t number2 { 0 };
        if (is_minimum) {
            StringBuilder number2_builder;
            while (match(TokenType::OrdinaryCharacter)) {
                number2_builder.append(consume().value());
            }
            if (!number2_builder.is_empty()) {
                bool ok2;
                number2 = number2_builder.build().to_uint(ok2);
                if (!ok || number1 > number2)
                    return set_error(REG_BADBR);
            }
        }
        min_length *= number1;

        // match number1-times (minmum or exactly)
        Vector<StackValue> new_operations;
        for (size_t i = 0; i < number1; ++i)
            new_operations.append(operations);

        if (number2 && number2 > number1) {
            auto maximum = number2 - number1;
            new_operations.empend(OpCode::ForkStay);
            new_operations.empend(maximum * (operations.size() + 2)); // Jump to the _END label

            for (size_t i = 0; i < maximum; ++i) {
                new_operations.append(operations);
                new_operations.empend(OpCode::ForkStay);
                new_operations.empend((maximum - i - 1) * (operations.size() + 2)); // Jump to the _END label
            }

        } else if (is_minimum) {
            new_operations.empend(OpCode::ForkJump);
            new_operations.empend(-operations.size() - 2); // Jump to the last iteration
        }

        operations = move(new_operations);

        consume(TokenType::RightCurly);

        // FIXME: Add OpCode for range based duplication

        return !has_error();

    } else if (match(TokenType::Plus)) {
        consume();

        // LABEL _START
        // REGEXP
        // FORKJUMP _START  (FORKSTAY -> Greedy)

        // LABEL _START = -operations.size()
        if (match(TokenType::Questionmark)) { // Greedy
            consume();
            operations.empend(OpCode::ForkStay);
        } else {
            operations.empend(OpCode::ForkJump);
        }
        operations.empend(-operations.size() - 1); // Jump to the _START label

        return !has_error();

    } else if (match(TokenType::Asterisk)) {
        consume();
        min_length = 0;

        // LABEL _START
        // FORKSTAY _END  (FORKJUMP -> Greedy)
        // REGEXP
        // JUMP  _START
        // LABEL _END

        Vector<StackValue> new_operations;

        // LABEL _START = 0
        if (match(TokenType::Questionmark)) { // Greedy
            consume();
            new_operations.empend(OpCode::ForkJump);
        } else {
            new_operations.empend(OpCode::ForkStay);
        }
        new_operations.empend(operations.size() + 2); // Jump to the _END label

        for (auto& op : operations)
            new_operations.append(move(op));

        new_operations.empend(OpCode::Jump);
        new_operations.empend(-new_operations.size() - 1); // Jump to the _START label
        // LABEL _END = new_operations.size()

        operations = move(new_operations);

        return !has_error();

    } else if (match(TokenType::Questionmark)) {
        consume();
        min_length = 0;

        // FORKSTAY _END (FORKJUMP -> Greedy)
        // REGEXP
        // LABEL _END

        Vector<StackValue> new_operations;

        if (match(TokenType::Questionmark)) { // Greedy
            consume();
            new_operations.empend(OpCode::ForkJump);
        } else {
            new_operations.empend(OpCode::ForkStay);
        }
        new_operations.empend(operations.size()); // Jump to the _END label

        for (auto& op : operations)
            new_operations.append(move(op));
        // LABEL _END = new_operations.size()

        operations = move(new_operations);

        return !has_error();
    }

    return false;
}

bool Parser::set_error(ReError error)
{
    if (m_parser_state.m_error == REG_NOERR) {
        m_parser_state.m_error = error;
        m_parser_state.m_error_token = m_parser_state.m_current_token;
    }
    return false; // always return false :^)
}

bool Parser::parse_bracket_expression(Vector<StackValue>& stack, size_t& min_length)
{
    Vector<CompareTypeAndValue> values;

    for (;;) {

        if (consume("-")) {

            if (values.is_empty() || (values.size() == 1 && values.last().type == CompareType::Inverse)) {
                // first in the bracket expression
                values.append({ CompareType::OrdinaryCharacter, { '-' } });

            } else if (match(TokenType::RightBracket)) {
                // Last in the bracket expression
                values.append({ CompareType::OrdinaryCharacter, { '-' } });

            } else if (values.last().type == CompareType::OrdinaryCharacter) {

                values.append({ CompareType::RangeExpressionDummy, 0 });

                if (consume("-")) {
                    // Valid range, add ordinary character
                    values.append({ CompareType::OrdinaryCharacter, { '-' } });
                }
            } else {
                return set_error(REG_ERANGE);
            }

        } else if (match(TokenType::OrdinaryCharacter) || match(TokenType::Period) || match(TokenType::Asterisk) || match(TokenType::EscapeSequence) || match(TokenType::Plus)) {
            values.append({ CompareType::OrdinaryCharacter, { *consume().value().characters_without_null_termination() } });

        } else if (match(TokenType::Circumflex)) {
            auto t = consume();

            if (values.is_empty())
                values.append({ CompareType::Inverse, 0 });
            else
                values.append({ CompareType::OrdinaryCharacter, { *t.value().characters_without_null_termination() } });

        } else if (match(TokenType::LeftBracket)) {
            consume();

            if (match(TokenType::Period)) {
                consume();

                // FIXME: Parse collating element, this is needed when we have locale support
                //        This could have impact on length parameter, I guess.
                ASSERT_NOT_REACHED();

                consume(TokenType::Period);
                consume(TokenType::RightBracket);

            } else if (match(TokenType::OrdinaryCharacter)) {
                if (match('=')) {
                    consume();
                    // FIXME: Parse collating element, this is needed when we have locale support
                    //        This could have impact on length parameter, I guess.
                    ASSERT_NOT_REACHED();

                    if (match('='))
                        consume();
                    else
                        return set_error(REG_ECOLLATE);

                    consume(TokenType::RightBracket);

                } else if (match(':')) {
                    consume();

                    CharacterClass ch_class;
                    // parse character class
                    if (match(TokenType::OrdinaryCharacter)) {
                        if (consume("alnum"))
                            ch_class = CharacterClass::Alnum;
                        else if (consume("alpha"))
                            ch_class = CharacterClass::Alpha;
                        else if (consume("blank"))
                            ch_class = CharacterClass::Blank;
                        else if (consume("cntrl"))
                            ch_class = CharacterClass::Cntrl;
                        else if (consume("digit"))
                            ch_class = CharacterClass::Digit;
                        else if (consume("graph"))
                            ch_class = CharacterClass::Graph;
                        else if (consume("lower"))
                            ch_class = CharacterClass::Lower;
                        else if (consume("print"))
                            ch_class = CharacterClass::Print;
                        else if (consume("punct"))
                            ch_class = CharacterClass::Punct;
                        else if (consume("space"))
                            ch_class = CharacterClass::Space;
                        else if (consume("upper"))
                            ch_class = CharacterClass::Upper;
                        else if (consume("xdigit"))
                            ch_class = CharacterClass::Xdigit;
                        else {
                            return set_error(REG_ECTYPE);
                        }

                        values.append({ CompareType::CharacterClass, ch_class });

                    } else
                        return set_error(REG_ECTYPE);

                    // FIXME: we do not support locale specific character classes until locales are implemented

                    if (match(':'))
                        consume();
                    else
                        return set_error(REG_ECTYPE);

                    consume(TokenType::RightBracket);
                } else
                    return set_error(REG_EBRACK);
            }

        } else if (match(TokenType::RightBracket)) {

            if (values.is_empty() || (values.size() == 1 && values.last().type == CompareType::Inverse)) {
                // handle bracket as ordinary character
                values.append({ CompareType::OrdinaryCharacter, { *consume().value().characters_without_null_termination() } });
            } else {
                // closing bracket expression
                break;
            }

        } else
            // nothing matched, this is a failure, as at least the closing bracket must match...
            return set_error(REG_EBRACK);

        // check if range expression has to be completed...
        if (values.size() >= 3 && values.at(values.size() - 2).type == CompareType::RangeExpressionDummy) {
            if (values.last().type != CompareType::OrdinaryCharacter)
                return set_error(REG_ERANGE);

            auto value2 = values.take_last();
            values.take_last(); // RangeExpressionDummy
            auto value1 = values.take_last();

            values.append({ CompareType::RangeExpression, StackValue { value1.value.ch, value2.value.ch } });
        }
    }

    if (values.size())
        min_length = 1;

    if (values.first().type == CompareType::Inverse)
        min_length = 0;

    Vector<StackValue> operations;

    operations.empend(OpCode::Compare);
    operations.empend(values.size()); // number of arguments

    for (auto& value : values) {
        ASSERT(value.type != CompareType::RangeExpressionDummy);
        ASSERT(value.type != CompareType::OrdinaryCharacters);
        ASSERT(value.type != CompareType::Undefined);

        operations.append(move(value.type));
        if (value.type != CompareType::Inverse)
            operations.append(move(value.value));
    }

    stack.append(move(operations));

    return !has_error();
}

bool Parser::parse_ere_expression(Vector<StackValue>& stack, size_t& min_length)
{
    Vector<StackValue> operations;
    size_t length = 0;
    bool can_match_dupl_symbol { false };

    for (;;) {
        if (match(TokenType::OrdinaryCharacter)) {
            Token start_token = m_parser_state.m_current_token;
            Token last_token = m_parser_state.m_current_token;
            for (;;) {
                if (!match(TokenType::OrdinaryCharacter))
                    break;
                ++length;
                last_token = consume();
            }

            if (length > 1) {
                stack.empend(OpCode::Compare);
                stack.empend(1ul); // number of arguments
                stack.empend(CompareType::OrdinaryCharacters);
                stack.empend(start_token.value().characters_without_null_termination());
                stack.empend(length - ((match_ere_dupl_symbol() && length > 1) ? 1 : 0)); // last character is inserted into 'operations' for duplication symbol handling
            }

            if ((match_ere_dupl_symbol() && length > 1) || length == 1) { // Create own compare opcode for last character before duplication symbol
                operations.empend(OpCode::Compare);
                operations.empend(1ul); // number of arguments
                operations.empend(CompareType::OrdinaryCharacter);
                operations.empend(last_token.value().characters_without_null_termination()[0]);
            }

            can_match_dupl_symbol = true;
            break;
        }

        else if (match(TokenType::Period)) {
            length = 1;
            consume();
            operations.empend(OpCode::Compare);
            operations.empend(1ul); // number of arguments
            operations.empend(CompareType::AnySingleCharacter);

            can_match_dupl_symbol = true;
            break;
        }

        else if (match(TokenType::EscapeSequence)) {
            length = 1;
            Token t = consume();
            operations.empend(OpCode::Compare);
            operations.empend(1ul); // number of arguments

#ifdef REGEX_DEBUG
            printf("[PARSER] EscapeSequence with substring %s\n", String(t.value()).characters());
#endif
            operations.empend(CompareType::OrdinaryCharacter);
            operations.empend((char)t.value().characters_without_null_termination()[1]);
            can_match_dupl_symbol = true;
            break;
        }

        if (match(TokenType::LeftBracket)) {
            consume();

            Vector<StackValue> sub_ops;
            if (!parse_bracket_expression(sub_ops, length) || !sub_ops.size())
                return set_error(REG_EBRACK);

            operations.append(move(sub_ops));

            consume(TokenType::RightBracket);
            can_match_dupl_symbol = true;
            break;
        }

        if (match(TokenType::Circumflex)) {
            consume();
            operations.empend(OpCode::CheckBegin);

            stack.append(move(operations));
            return true;
        }

        if (match(TokenType::Dollar)) {
            consume();
            operations.empend(OpCode::CheckEnd);

            stack.append(move(operations));
            return true;
        }

        if (match(TokenType::LeftParen)) {
            consume();

            if (!(m_parser_state.m_cflags & REG_NOSUB)) {
                operations.empend(OpCode::SaveLeftGroup);
                operations.empend(m_parser_state.m_match_groups);
            }

            Vector<StackValue> sub_ops;
            if (!parse_extended_reg_exp(sub_ops, length) || !sub_ops.size())
                return set_error(REG_EPAREN);

            operations.append(move(sub_ops));

            consume(TokenType::RightParen);

            if (!(m_parser_state.m_cflags & REG_NOSUB)) {
                operations.empend(OpCode::SaveRightGroup);
                operations.empend(m_parser_state.m_match_groups);
            }

            ++m_parser_state.m_match_groups;
            can_match_dupl_symbol = true;
            break;
        }

        return false;
    }

    if (match_ere_dupl_symbol()) {
        if (can_match_dupl_symbol)
            parse_ere_dupl_symbol(operations, length);
        else
            return set_error(REG_BADRPT);
    }

    stack.append(move(operations));
    min_length += length;

    return true;
}

bool Parser::parse_extended_reg_exp(Vector<StackValue>& stack, size_t& min_length)
{
    Vector<StackValue> operations;
    size_t length { 0 };

    for (;;) {
        if (!parse_ere_expression(operations, length))
            break;

        if (match(TokenType::Pipe)) {
            consume();

            Vector<StackValue> operations_alternative;
            size_t alt_length { 0 };

            if (!(parse_extended_reg_exp(operations_alternative, alt_length) && operations_alternative.size()))
                return set_error(REG_BADPAT);

            // FORKSTAY _ALT
            // REGEXP ALT1
            // JUMP  _END
            // LABEL _ALT
            // REGEXP ALT2
            // LABEL _END

            Vector<StackValue> new_operations;

            new_operations.empend(OpCode::ForkJump);
            new_operations.empend(operations.size() + 2); // Jump to the _ALT label

            for (auto& op : operations)
                new_operations.append(move(op));

            new_operations.empend(OpCode::Jump);
            new_operations.empend(operations_alternative.size()); // Jump to the _END label

            // LABEL _ALT = operations.size() + 2

            for (auto& op : operations_alternative)
                new_operations.append(move(op));

            // LABEL _END = operations_alternative.size

            operations = move(new_operations);
            length = min(alt_length, length);
        }
    }

    stack.append(move(operations));
    min_length = length;
    return !has_error();
}

bool Parser::done() const
{
    return match(TokenType::Eof);
}

bool Parser::match(TokenType type) const
{
    return m_parser_state.m_current_token.type() == type;
}

bool Parser::match(char ch) const
{
    return m_parser_state.m_current_token.type() == TokenType::OrdinaryCharacter
        && m_parser_state.m_current_token.value().length() == 1
        && m_parser_state.m_current_token.value().characters_without_null_termination()[0] == ch;
}

Token Parser::consume()
{
    auto old_token = m_parser_state.m_current_token;
    m_parser_state.m_current_token = m_parser_state.m_lexer.next();
    return old_token;
}

Token Parser::consume(TokenType type)
{
    if (m_parser_state.m_current_token.type() != type) {
        set_error(REG_BADPAT);
#ifdef REGEX_DEBUG
        fprintf(stderr, "[PARSER] Error: Unexpected token %s. Expected %s\n", m_parser_state.m_current_token.name(), Token::name(type));
#endif
    }
    return consume();
}

bool Parser::consume(StringView view)
{
    size_t length { 0 };

    for (auto ch : view) {
        if (match(TokenType::OrdinaryCharacter)) {
            if (*m_parser_state.m_current_token.value().characters_without_null_termination() != ch) {
                m_parser_state.m_lexer.back(length);
                return false;
            }
        } else {
            m_parser_state.m_lexer.back(length);
            return false;
        }
        consume(TokenType::OrdinaryCharacter);
        ++length;
    }

    return true;
}

Parser::ParserResult Parser::parse(int cflags)
{
    m_parser_state.m_cflags = cflags;
    if (parse_extended_reg_exp(m_parser_state.m_bytes, m_parser_state.m_min_match_length))
        consume(TokenType::Eof);
#ifdef REGEX_DEBUG
    else
        printf("[PARSER] Error during parsing!\n");
#endif

#ifdef REGEX_DEBUG
    printf("[PARSER] Produced stack with %lu entries\n", m_parser_state.m_bytes.size());
#endif
    return {
        move(m_parser_state.m_bytes),
        move(m_parser_state.m_match_groups),
        move(m_parser_state.m_min_match_length),
        move(m_parser_state.m_error),
        move(m_parser_state.m_error_token)
    };
}

void Parser::reset()
{
    m_parser_state.m_bytes.clear();
    m_parser_state.m_lexer.reset();
    m_parser_state.m_current_token = m_parser_state.m_lexer.next();
    m_parser_state.m_error = REG_NOERR;
    m_parser_state.m_error_token = { TokenType::Eof, 0, StringView(nullptr) };
    m_parser_state.m_cflags = 0;
}

VM::MatchState::MatchState(size_t instructionp, size_t stringp, StringView view)
    : m_view(view)
    , m_instructionp(instructionp)
    , m_stringp(stringp)
{
}

VM::MatchResult VM::match(StringView view, size_t max_matches_result, size_t match_groups, size_t min_length, int flags) const
{
    regoff_t match_start;
    size_t match_count { 0 };
    bool match;

    MatchState state { view };
    state.m_matches.ensure_capacity(max_matches_result);
    state.m_left.ensure_capacity(match_groups);

    for (size_t j = 0; j < max_matches_result; ++j)
        state.m_matches.append({ -1, -1, 0 });
    for (size_t j = 0; j < match_groups; ++j)
        state.m_left.append(-1);

    for (size_t i = 0; i < view.length(); ++i) {
        if (min_length && min_length > view.length() - i)
            break;
        for (size_t j = 0; j < match_groups; ++j)
            state.m_left.at(j) = -1;

        state.m_stringp = i;
        state.m_instructionp = 0;
        match_start = i;
        match = match_recurse(state);

        if (match) {
            ++match_count;

            if (flags & REG_MATCHALL) {
                if (state.m_matches_offset < state.m_matches.size())
                    state.m_matches.at(state.m_matches_offset) = { match_start, (regoff_t)state.m_stringp, 1 };

                i = state.m_stringp - 1;
                state.m_matches_offset += match_groups + 1;

                continue;

            } else if (!(flags & REG_SEARCH) && state.m_stringp < view.length())
                return { 0, {}, state.m_ops };

            if (state.m_matches.size()) {
                state.m_matches.at(0) = { match_start, (regoff_t)state.m_stringp, 0 };
            }

            break;
        }
        if (!((flags & REG_SEARCH) || (flags & REG_MATCHALL)))
            break;
    }

    return { match_count, move(state.m_matches), state.m_ops };
}

const StackValue VM::get(MatchState& state, size_t offset) const
{
    if (state.m_instructionp + offset < m_bytecode.size())
        return m_bytecode.at(state.m_instructionp + offset);
    else
        return StackValue(OpCode::Exit);
}

const StackValue VM::get_and_increment(MatchState& state, size_t value) const
{
    auto& current = get(state);
    state.m_instructionp += value;
    return current;
}

bool VM::match_recurse(MatchState& state, size_t recursion_level) const
{
    if (recursion_level > REG_MAX_RECURSE)
        return false;

    Vector<ForkStayTuple> fork_stay_tuples;

    auto run_forkstay = [&]() -> bool {
        auto tuples_size = fork_stay_tuples.size();
        for (size_t i = 0; i < tuples_size; ++i) {
            auto& fork_stay_tuple = fork_stay_tuples.at(tuples_size - i - 1);
            auto instructionp = state.m_instructionp;
            auto stringp = state.m_stringp;

            state.m_instructionp = fork_stay_tuple.m_instructionp;
            state.m_stringp = fork_stay_tuple.m_stringp;

#ifdef REGEX_DEBUG
            printf("[VM][r=%lu] Execute ForkStay - instructionp: %2lu, stringp: %2lu - ", recursion_level, state.m_instructionp, state.m_stringp);
            printf("[%20s]\n", String(&state.m_view[state.m_stringp], state.m_view.length() - state.m_stringp).characters());
#endif

            if (match_recurse(state, recursion_level + 1))
                return true;

            state.m_instructionp = instructionp;
            state.m_stringp = stringp;
        }
        return false;
    };

    auto run_forkstay_or_false = [&]() -> bool {
        if (run_forkstay())
            return true;

        state.m_stringp = 0;
        return false;
    };

    auto check_exit_conditions = [&]() -> bool {
#ifdef REGEX_DEBUG
        bool verbose = false;
        if (verbose) {
            printf("[VM][r=%lu] Checking exit conditions: ", recursion_level);
            printf("String: stringp: %lu, length: %lu; ", state.m_stringp, state.m_view.length());
            printf("Instruction: instructionp: %lu, size: %lu; ", state.m_instructionp, bytes().size());
            printf("Condition: %s\n", state.m_stringp > state.m_view.length() || state.m_instructionp >= bytes().size() ? "true" : "false");
        } else {
            if (state.m_instructionp >= m_bytecode.size())
                printf("[VM][r=%lu] Reached end of OpCodes with stringp = %lu!\n", recursion_level, state.m_stringp);

            if (state.m_stringp > state.m_view.length())
                printf("[VM][r=%lu] Reached end of string with instructionp = %lu!\n", recursion_level, state.m_instructionp);
        }
#endif

        if (state.m_stringp > state.m_view.length() || state.m_instructionp >= bytes().size())
            return true;

        return false;
    };

    for (;;) {
        ++state.m_ops;
        auto current_ip = state.m_instructionp;
        auto stack_item = get_and_increment(state);

#ifdef REGEX_DEBUG
        printf("[VM][r=%lu]  OpCode: 0x%i (%14s) - instructionp: %2lu, stringp: %2lu - ", recursion_level, stack_item.number, stack_item.name(), current_ip, state.m_stringp);
        printf("[%20s]\n", String(&state.m_view[state.m_stringp], state.m_view.length() - state.m_stringp).characters());
#endif

        if (stack_item.op_code == OpCode::Compare) {
            bool inverse { false };
            auto& arguments = get_and_increment(state).positive_number;
            size_t fetched_arguments = 0;
            size_t stringp = state.m_stringp;
            bool inverse_matched { false };

            for (; fetched_arguments < arguments; ++fetched_arguments) {
                if (state.m_stringp > stringp)
                    break;

                auto& compare_type = get_and_increment(state).compare_type;

                if (compare_type == CompareType::Inverse)
                    inverse = true;

                else if (compare_type == CompareType::OrdinaryCharacter) {
                    auto ch = get_and_increment(state).ch;

                    // We want to compare a string that is definitely longer than the available string
                    if (state.m_view.length() - state.m_stringp < 1)
                        return run_forkstay_or_false();

                    if (ch == state.m_view[state.m_stringp]) {
                        if (inverse)
                            inverse_matched = true;
                        else
                            ++state.m_stringp;
                    }

                } else if (compare_type == CompareType::AnySingleCharacter) {
                    // We want to compare a string that is definitely longer than the available string
                    if (state.m_view.length() - state.m_stringp < 1)
                        return run_forkstay_or_false();

                    ASSERT(!inverse);
                    ++state.m_stringp;

                } else if (compare_type == CompareType::OrdinaryCharacters) {
                    // We want to compare a string that is definitely longer than the available string
                    ASSERT(!inverse);

                    auto* str = get_and_increment(state).string;
                    auto& length = get_and_increment(state).positive_number;

                    // We want to compare a string that is definitely longer than the available string
                    if (state.m_view.length() - state.m_stringp < length)
                        return run_forkstay_or_false();

                    if (!strncmp(str, &state.m_view[state.m_stringp], length))
                        state.m_stringp += length;
                    else
                        return run_forkstay_or_false();
                } else if (compare_type == CompareType::CharacterClass) {

                    if (state.m_view.length() - state.m_stringp < 1)
                        return run_forkstay_or_false();

                    auto& character_class = get_and_increment(state).compare_value.character_class;
                    auto& ch = state.m_view[state.m_stringp];

                    switch (character_class) {
                    case CharacterClass::Alnum:
                        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Alpha:
                        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'))
                            ++state.m_stringp;
                        break;
                    case CharacterClass::Blank:
                        if (ch == ' ' || ch == '\t') {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Cntrl:
                        if ((ch >= '\0' && ch <= ' ') || ch == 127) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Digit:
                        if (ch >= '0' && ch <= '9') {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Graph:
                        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch >= '!' && ch <= '/') || (ch >= ':' && ch <= '@') || (ch >= '[' && ch <= '`') || (ch >= '{' && ch <= '~')) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Lower:
                        if (ch >= 'a' && ch <= 'z') {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Print:
                        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch >= '\t' && ch <= '\r') || ch == ' ' || (ch >= '!' && ch <= '/') || (ch >= ':' && ch <= '@') || (ch >= '[' && ch <= '`') || (ch >= '{' && ch <= '~')) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Punct:
                        if ((ch >= '!' && ch <= '/') || (ch >= ':' && ch <= '@') || (ch >= '[' && ch <= '`') || (ch >= '{' && ch <= '~')) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Space:
                        if ((ch >= '\t' && ch <= '\r') || ch == ' ') {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Upper:
                        if (ch >= 'A' && ch <= 'Z') {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    case CharacterClass::Xdigit:
                        if ((ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f') || (ch >= '0' && ch <= '9')) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.m_stringp;
                        }
                        break;
                    }

                } else if (compare_type == CompareType::RangeExpression) {
                    auto& value = get_and_increment(state).compare_value;
                    auto& from = value.range_values.from;
                    auto& to = value.range_values.to;
                    auto& ch = state.m_view[state.m_stringp];

                    if (ch >= from && ch <= to) {
                        if (inverse)
                            inverse_matched = true;
                        else
                            ++state.m_stringp;
                    }
                } else {
                    fprintf(stderr, "Undefined comparison: %i\n", (int)compare_type);
                    ASSERT_NOT_REACHED();
                    break;
                }
            }

            if (inverse && !inverse_matched)
                ++state.m_stringp;

            // fetch missing arguments from stack
            for (; fetched_arguments < arguments; ++fetched_arguments) {
                auto& compare_type = get_and_increment(state).compare_type;
                switch (compare_type) {
                case CompareType::OrdinaryCharacter:
                    get_and_increment(state);
                    break;
                case CompareType::OrdinaryCharacters:
                    get_and_increment(state);
                    get_and_increment(state);
                    break;
                case CompareType::CharacterClass:
                    get_and_increment(state);
                    break;
                case CompareType::RangeExpression:
                    get_and_increment(state);
                    break;
                default: {
                }
                }
            }

            if (stringp == state.m_stringp)
                return run_forkstay_or_false();

            if (state.m_stringp > state.m_view.length())
                return run_forkstay_or_false();

        } else if (stack_item.op_code == OpCode::ForkJump) {
            auto& offset = get_and_increment(state).number;
#ifdef REGEX_DEBUG
            printf(" > ForkJump to offset: %i, instructionp: %lu, stringp: %lu\n", offset, state.m_instructionp + offset, state.m_stringp);
#endif
            //save instructionp and stringp
            auto saved_instructionp = state.m_instructionp;
            auto saved_stringp = state.m_stringp;
            state.m_instructionp = state.m_instructionp + offset;

            if (!match_recurse(state, recursion_level + 1)) {
                // no valid solution via forkjump found... continue here at old string position and instruction
                state.m_stringp = saved_stringp;
                state.m_instructionp = saved_instructionp;
            }
        } else if (stack_item.op_code == OpCode::ForkStay) {
            auto& offset = get_and_increment(state).number;
            fork_stay_tuples.append({ state.m_instructionp + offset, state.m_stringp });

#ifdef REGEX_DEBUG
            printf(" > ForkStay to offset: %i, instructionp: %lu, stringp: %lu\n", offset, fork_stay_tuples.last().m_instructionp, fork_stay_tuples.last().m_stringp);
#endif
        } else if (stack_item.op_code == OpCode::Jump) {
            auto& offset = get_and_increment(state).number;
            state.m_instructionp += offset;
#ifdef REGEX_DEBUG
            printf(" > Jump to offset: %i: new instructionp: %lu\n", offset, state.m_instructionp);
#endif
            continue; // directly jump to next instruction!
        } else if (stack_item.op_code == OpCode::SaveLeftGroup) {
            auto& id = get_and_increment(state).positive_number;
#ifdef REGEX_DEBUG
            printf(" > Left parens for group match %lu at stringp = %lu\n", id, state.m_stringp);
#endif
            if ((size_t)id < state.m_left.size() && state.m_stringp < state.m_view.length())
                state.m_left.at(id) = state.m_stringp;
        } else if (stack_item.op_code == OpCode::SaveRightGroup) {
            auto& id = get_and_increment(state).positive_number;
            auto index = id + 1 + state.m_matches_offset;

#ifdef REGEX_DEBUG
            printf(" > Right parens for group match %lu at stringp = %lu\n", id, state.m_stringp);
#endif
            if ((size_t)id < state.m_left.size() && state.m_left.at(id) != -1 && index < state.m_matches.size()) {
                auto left = state.m_left.at(id);
                state.m_matches.at(index) = { left, (regoff_t)state.m_stringp, 1 };
#ifdef REGEX_DEBUG
                printf("Match result group id %lu: from %lu to %lu\n", id, left, state.m_stringp);
#endif
            }
        } else if (stack_item.op_code == OpCode::CheckBegin) {
#ifdef REGEX_DEBUG
            printf("\n");
#endif
            if (state.m_stringp != 0)
                return false;
        } else if (stack_item.op_code == OpCode::CheckEnd) {
#ifdef REGEX_DEBUG
            printf(" > Check end: %lu == %lu\n", state.m_stringp, state.m_view.length());
#endif
            if (state.m_stringp != state.m_view.length())
                return false;
        } else if (stack_item.op_code == OpCode::Exit) {
            bool cond = check_exit_conditions();
#ifdef REGEX_DEBUG
            printf(" > Condition %s\n", cond ? "true" : "false");
#endif
            if (cond)
                return true;
            return false;

        } else {
            printf("\n[VM][r=%lu] Invalid opcode: %lu, stackpointer: %lu\n", recursion_level, (size_t)stack_item.op_code, current_ip);
            exit(1);
        }

        if (check_exit_conditions())
            return true;
    }

    ASSERT_NOT_REACHED();
}
}

extern "C" {

int regcomp(regex_t* preg, const char* pattern, int cflags)
{
    *preg = { 0, 0, 0, 0, nullptr, 0, REG_NOERR, "" };

    if (!(cflags & REG_EXTENDED))
        return REG_ENOSYS;

    preg->cflags = cflags;

    String s(pattern);
    regex::Lexer lexer(pattern);

#ifdef REGEX_DEBUG
    printf("[LEXER] Tokens for pattern '%s':\n", pattern);
    while (true) {
        regex::Token token = lexer.next();
        if (token.type() == regex::TokenType::Eof)
            break;

        String a(token.value());
        printf("[LEXER] %s -> %s\n", token.name(), a.characters());
    }
    lexer.reset();
#endif

    regex::Parser parser(move(lexer));
    auto result = parser.parse(cflags);

#ifdef REGEX_DEBUG
    int i = 0;
    for (auto& item : result.m_bytes) {
        printf("[PARSER] [%i]: %i\n", i, item.number);
        i++;
    }
#endif

    if (result.m_error != REG_NOERR) {
        preg->re_pat_errpos = result.m_error_token.position();
        preg->re_pat_err = result.m_error;
        preg->re_pat = pattern;

        return result.m_error;
    }

    preg->re_nsub = result.m_match_groups;
    preg->re_minlength = result.m_min_match_length;

#ifdef REGEX_DEBUG
    printf("Minlength for pattern '%s' = %lu\n", pattern, preg->re_minlength);
#endif

    preg->vm = make<regex::VM>(result.m_bytes, move(s));
    return REG_NOERR;
}


int regexec(const regex_t* preg, const char* string, size_t nmatch, regmatch_t pmatch[], int eflags)
{
    if (!preg->vm || preg->re_pat_err) {
        if (preg->re_pat_err)
            return preg->re_pat_err;
        return REG_BADPAT;
    }

    auto& vm = preg->vm;

    regex::VM::MatchResult result;

    result = vm->match(string, nmatch, preg->re_nsub, preg->re_minlength, eflags);

    if (result.m_match_count) {
        auto size = result.m_matches.size();
        for (size_t i = 0; i < nmatch; ++i) {
            if (i < size)
                pmatch[i] = result.m_matches.at(i);
            else
                pmatch[i] = { -1, -1, 0 };
        }
    }

    if (nmatch && pmatch)
        pmatch[0].match_count = result.m_match_count;

    if (result.m_match_count) {
        if (eflags & REG_STATS) {
            if (eflags & REG_MATCHALL)
                printf("[regexec] match_all successful, found %lu occurences, took %lu operations.\n", result.m_match_count, result.m_ops);
            else
                printf("[regexec] match successful, took %lu operations.\n", result.m_ops);
        }

        return REG_NOERR;
    }

    for (size_t i = 0; i < nmatch; ++i)
        pmatch[i] = { -1, -1, 0 };

    if (eflags & REG_STATS) {
        if (eflags & REG_MATCHALL)
            printf("[regexec] match_all not successful, found %lu occurences, took %lu operations.\n", result.m_match_count, result.m_ops);
        else
            printf("[regexec] match not successful, took %lu operations.\n", result.m_ops);
    }

    return REG_NOMATCH;
}

inline static String get_error(ReError errcode)
{
    String error;
    switch ((ReError)errcode) {
    case REG_NOERR:
        error = "No error";
        break;
    case REG_NOMATCH:
        error = "regexec() failed to match.";
        break;
    case REG_BADPAT:
        error = "Invalid regular expression.";
        break;
    case REG_ECOLLATE:
        error = "Invalid collating element referenced.";
        break;
    case REG_ECTYPE:
        error = "Invalid character class type referenced.";
        break;
    case REG_EESCAPE:
        error = "Trailing \\ in pattern.";
        break;
    case REG_ESUBREG:
        error = "Number in \\digit invalid or in error.";
        break;
    case REG_EBRACK:
        error = "[ ] imbalance.";
        break;
    case REG_EPAREN:
        error = "\\( \\) or ( ) imbalance.";
        break;
    case REG_EBRACE:
        error = "\\{ \\} imbalance.";
        break;
    case REG_BADBR:
        error = "Content of \\{ \\} invalid: not a number, number too large, more than two numbers, first larger than second.";
        break;
    case REG_ERANGE:
        error = "Invalid endpoint in range expression.";
        break;
    case REG_ESPACE:
        error = "Out of memory.";
        break;
    case REG_BADRPT:
        error = "?, * or + not preceded by valid regular expression.";
        break;
    case REG_ENOSYS:
        error = "The implementation does not support the function.";
        break;
    }
    return error;
}

size_t regerror(int errcode, const regex_t* preg, char* errbuf, size_t errbuf_size)
{
    String error;

    if (preg && preg->re_pat != nullptr && preg->re_pat_err != REG_NOERR && preg->re_pat_errpos) {
        StringBuilder eb;
        eb.appendf("Error in Regular Expression:\n");
        eb.appendf("    %s\n    ", preg->re_pat.characters());
        for (size_t i = 0; i < preg->re_pat_errpos - 1; ++i)
            eb.append(" ");
        eb.appendf("^---- %s\n", get_error(preg->re_pat_err).characters());
        error = eb.build();
    } else
        error = get_error((ReError)errcode);

    if (!errbuf_size)
        return error.length();
    else if (error.length() > errbuf_size) {
        strncpy(errbuf, error.characters(), errbuf_size - 1);
        errbuf[errbuf_size - 1] = '\0';
    } else {
        strncpy(errbuf, error.characters(), error.length());
    }
    return error.length();
}

void regfree(regex_t* preg)
{
    preg->re_nsub = 0;
    preg->cflags = 0;
    preg->eflags = 0;
    preg->vm.clear();
}
}
