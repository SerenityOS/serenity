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

#include "RegexParser.h"
#include "RegexDebug.h"
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <cstdio>

namespace regex {

ALWAYS_INLINE bool Parser::set_error(Error error)
{
    if (m_parser_state.error == Error::NoError) {
        m_parser_state.error = error;
        m_parser_state.error_token = m_parser_state.current_token;
    }
    return false; // always return false, that eases the API usage (return set_error(...)) :^)
}

ALWAYS_INLINE bool Parser::done() const
{
    return match(TokenType::Eof);
}

ALWAYS_INLINE bool Parser::match(TokenType type) const
{
    return m_parser_state.current_token.type() == type;
}

ALWAYS_INLINE Token Parser::consume()
{
    auto old_token = m_parser_state.current_token;
    m_parser_state.current_token = m_parser_state.lexer.next();
    return old_token;
}

ALWAYS_INLINE Token Parser::consume(TokenType type, Error error)
{
    if (m_parser_state.current_token.type() != type) {
        set_error(error);
        dbg() << "[PARSER] Error: Unexpected token " << m_parser_state.current_token.name() << ". Expected: " << Token::name(type);
    }
    return consume();
}

ALWAYS_INLINE bool Parser::consume(const String& str)
{
    size_t potentially_go_back { 1 };
    for (auto ch : str) {
        if (match(TokenType::Char)) {
            if (m_parser_state.current_token.value()[0] != ch) {
                m_parser_state.lexer.back(potentially_go_back);
                m_parser_state.current_token = m_parser_state.lexer.next();
                return false;
            }
        } else {
            m_parser_state.lexer.back(potentially_go_back);
            m_parser_state.current_token = m_parser_state.lexer.next();
            return false;
        }
        consume(TokenType::Char, Error::NoError);
        ++potentially_go_back;
    }
    return true;
}

ALWAYS_INLINE void Parser::reset()
{
    m_parser_state.bytecode.clear();
    m_parser_state.lexer.reset();
    m_parser_state.current_token = m_parser_state.lexer.next();
    m_parser_state.error = Error::NoError;
    m_parser_state.error_token = { TokenType::Eof, 0, StringView(nullptr) };
    m_parser_state.regex_options = {};
}

Parser::Result Parser::parse(Optional<AllOptions> regex_options)
{
    reset();
    if (regex_options.has_value())
        m_parser_state.regex_options = regex_options.value();
    if (parse_internal(m_parser_state.bytecode, m_parser_state.match_length_minimum))
        consume(TokenType::Eof, Error::InvalidPattern);
    else
        set_error(Error::InvalidPattern);

#ifdef REGEX_DEBUG
    fprintf(stderr, "[PARSER] Produced bytecode with %lu entries (opcodes + arguments)\n", m_parser_state.bytecode.size());
#endif
    return {
        move(m_parser_state.bytecode),
        move(m_parser_state.capture_groups_count),
        move(m_parser_state.named_capture_groups_count),
        move(m_parser_state.match_length_minimum),
        move(m_parser_state.error),
        move(m_parser_state.error_token)
    };
}

// =============================
// PosixExtended Parser
// =============================

bool PosixExtendedParser::parse_internal(ByteCode& stack, size_t& match_length_minimum)
{
    return parse_root(stack, match_length_minimum);
}

ALWAYS_INLINE bool PosixExtendedParser::match_repetition_symbol()
{
    auto type = m_parser_state.current_token.type();
    return (type == TokenType::Asterisk
        || type == TokenType::Plus
        || type == TokenType::Questionmark
        || type == TokenType::LeftCurly);
}

ALWAYS_INLINE bool PosixExtendedParser::match_ordinary_characters()
{
    // NOTE: This method must not be called during bracket and repetition parsing!
    // FIXME: Add assertion for that?
    auto type = m_parser_state.current_token.type();
    return (type == TokenType::Char
        || type == TokenType::Comma
        || type == TokenType::Slash
        || type == TokenType::EqualSign
        || type == TokenType::HyphenMinus
        || type == TokenType::Colon);
}

ALWAYS_INLINE bool PosixExtendedParser::parse_repetition_symbol(ByteCode& bytecode_to_repeat, size_t& match_length_minimum)
{
    if (match(TokenType::LeftCurly)) {
        consume();

        StringBuilder number_builder;

        while (match(TokenType::Char)) {
            number_builder.append(consume().value());
        }

        auto maybe_minimum = number_builder.build().to_uint();
        if (!maybe_minimum.has_value())
            return set_error(Error::InvalidBraceContent);

        auto minimum = maybe_minimum.value();
        match_length_minimum *= minimum;

        if (match(TokenType::Comma)) {
            consume();
        } else {
            ByteCode bytecode;
            bytecode.insert_bytecode_repetition_n(bytecode_to_repeat, minimum);
            bytecode_to_repeat = move(bytecode);

            consume(TokenType::RightCurly, Error::MismatchingBrace);
            return !has_error();
        }

        Optional<size_t> maybe_maximum {};
        number_builder.clear();
        while (match(TokenType::Char)) {
            number_builder.append(consume().value());
        }
        if (!number_builder.is_empty()) {
            auto value = number_builder.build().to_uint();
            if (!value.has_value() || minimum > value.value())
                return set_error(Error::InvalidBraceContent);

            maybe_maximum = value.value();
        }

        bytecode_to_repeat.insert_bytecode_repetition_min_max(bytecode_to_repeat, minimum, maybe_maximum);

        consume(TokenType::RightCurly, Error::MismatchingBrace);
        return !has_error();

    } else if (match(TokenType::Plus)) {
        consume();

        bool nongreedy = match(TokenType::Questionmark);
        if (nongreedy)
            consume();

        // Note: dont touch match_length_minimum, it's already correct
        bytecode_to_repeat.insert_bytecode_repetition_min_one(bytecode_to_repeat, !nongreedy);
        return !has_error();

    } else if (match(TokenType::Asterisk)) {
        consume();
        match_length_minimum = 0;

        bool nongreedy = match(TokenType::Questionmark);
        if (nongreedy)
            consume();

        bytecode_to_repeat.insert_bytecode_repetition_any(bytecode_to_repeat, !nongreedy);

        return !has_error();

    } else if (match(TokenType::Questionmark)) {
        consume();
        match_length_minimum = 0;

        bool nongreedy = match(TokenType::Questionmark);
        if (nongreedy)
            consume();

        bytecode_to_repeat.insert_bytecode_repetition_zero_or_one(bytecode_to_repeat, !nongreedy);
        return !has_error();
    }

    return false;
}

ALWAYS_INLINE bool PosixExtendedParser::parse_bracket_expression(ByteCode& stack, size_t& match_length_minimum)
{
    Vector<CompareTypeAndValuePair> values;

    for (;;) {

        if (match(TokenType::HyphenMinus)) {
            consume();

            if (values.is_empty() || (values.size() == 1 && values.last().type == CharacterCompareType::Inverse)) {
                // first in the bracket expression
                values.append({ CharacterCompareType::Char, (ByteCodeValueType)'-' });
            } else if (match(TokenType::RightBracket)) {
                // Last in the bracket expression
                values.append({ CharacterCompareType::Char, (ByteCodeValueType)'-' });
            } else if (values.last().type == CharacterCompareType::Char) {
                values.append({ CharacterCompareType::RangeExpressionDummy, 0 });

                if (match(TokenType::HyphenMinus)) {
                    consume();
                    // Valid range, add ordinary character
                    values.append({ CharacterCompareType::Char, (ByteCodeValueType)'-' });
                }
            } else {
                return set_error(Error::InvalidRange);
            }

        } else if (match(TokenType::Char) || match(TokenType::Period) || match(TokenType::Asterisk) || match(TokenType::EscapeSequence) || match(TokenType::Plus)) {
            values.append({ CharacterCompareType::Char, (ByteCodeValueType)*consume().value().characters_without_null_termination() });

        } else if (match(TokenType::Circumflex)) {
            auto t = consume();

            if (values.is_empty())
                values.append({ CharacterCompareType::Inverse, 0 });
            else
                values.append({ CharacterCompareType::Char, (ByteCodeValueType)*t.value().characters_without_null_termination() });

        } else if (match(TokenType::LeftBracket)) {
            consume();

            if (match(TokenType::Period)) {
                consume();

                // FIXME: Parse collating element, this is needed when we have locale support
                //        This could have impact on length parameter, I guess.
                ASSERT_NOT_REACHED();

                consume(TokenType::Period, Error::InvalidCollationElement);
                consume(TokenType::RightBracket, Error::MismatchingBracket);

            } else if (match(TokenType::EqualSign)) {
                consume();
                // FIXME: Parse collating element, this is needed when we have locale support
                //        This could have impact on length parameter, I guess.
                ASSERT_NOT_REACHED();

                consume(TokenType::EqualSign, Error::InvalidCollationElement);
                consume(TokenType::RightBracket, Error::MismatchingBracket);

            } else if (match(TokenType::Colon)) {
                consume();

                CharClass ch_class;
                // parse character class
                if (match(TokenType::Char)) {
                    if (consume("alnum"))
                        ch_class = CharClass::Alnum;
                    else if (consume("alpha"))
                        ch_class = CharClass::Alpha;
                    else if (consume("blank"))
                        ch_class = CharClass::Blank;
                    else if (consume("cntrl"))
                        ch_class = CharClass::Cntrl;
                    else if (consume("digit"))
                        ch_class = CharClass::Digit;
                    else if (consume("graph"))
                        ch_class = CharClass::Graph;
                    else if (consume("lower"))
                        ch_class = CharClass::Lower;
                    else if (consume("print"))
                        ch_class = CharClass::Print;
                    else if (consume("punct"))
                        ch_class = CharClass::Punct;
                    else if (consume("space"))
                        ch_class = CharClass::Space;
                    else if (consume("upper"))
                        ch_class = CharClass::Upper;
                    else if (consume("xdigit"))
                        ch_class = CharClass::Xdigit;
                    else
                        return set_error(Error::InvalidCharacterClass);

                    values.append({ CharacterCompareType::CharClass, (ByteCodeValueType)ch_class });

                } else
                    return set_error(Error::InvalidCharacterClass);

                // FIXME: we do not support locale specific character classes until locales are implemented

                consume(TokenType::Colon, Error::InvalidCharacterClass);
                consume(TokenType::RightBracket, Error::MismatchingBracket);
            } else {
                return set_error(Error::MismatchingBracket);
            }

        } else if (match(TokenType::RightBracket)) {

            if (values.is_empty() || (values.size() == 1 && values.last().type == CharacterCompareType::Inverse)) {
                // handle bracket as ordinary character
                values.append({ CharacterCompareType::Char, (ByteCodeValueType)*consume().value().characters_without_null_termination() });
            } else {
                // closing bracket expression
                break;
            }
        } else
            // nothing matched, this is a failure, as at least the closing bracket must match...
            return set_error(Error::MismatchingBracket);

        // check if range expression has to be completed...
        if (values.size() >= 3 && values.at(values.size() - 2).type == CharacterCompareType::RangeExpressionDummy) {
            if (values.last().type != CharacterCompareType::Char)
                return set_error(Error::InvalidRange);

            auto value2 = values.take_last();
            values.take_last(); // RangeExpressionDummy
            auto value1 = values.take_last();

            values.append({ CharacterCompareType::CharRange, static_cast<ByteCodeValueType>(CharRange { (u32)value1.value, (u32)value2.value }) });
        }
    }

    if (values.size())
        match_length_minimum = 1;

    if (values.first().type == CharacterCompareType::Inverse)
        match_length_minimum = 0;

    stack.insert_bytecode_compare_values(move(values));

    return !has_error();
}

ALWAYS_INLINE bool PosixExtendedParser::parse_sub_expression(ByteCode& stack, size_t& match_length_minimum)
{
    ByteCode bytecode;
    size_t length = 0;
    bool should_parse_repetition_symbol { false };

    for (;;) {
        if (match_ordinary_characters()) {
            Token start_token = m_parser_state.current_token;
            Token last_token = m_parser_state.current_token;
            for (;;) {
                if (!match_ordinary_characters())
                    break;
                ++length;
                last_token = consume();
            }

            if (length > 1) {
                // last character is inserted into 'bytecode' for duplication symbol handling
                auto new_length = length - ((match_repetition_symbol() && length > 1) ? 1 : 0);
                stack.insert_bytecode_compare_string(start_token.value(), new_length);
            }

            if ((match_repetition_symbol() && length > 1) || length == 1) // Create own compare opcode for last character before duplication symbol
                bytecode.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)last_token.value().characters_without_null_termination()[0] } });

            should_parse_repetition_symbol = true;
            break;
        }

        if (match_repetition_symbol())
            return set_error(Error::InvalidRepetitionMarker);

        if (match(TokenType::Period)) {
            length = 1;
            consume();
            bytecode.insert_bytecode_compare_values({ { CharacterCompareType::AnyChar, 0 } });
            should_parse_repetition_symbol = true;
            break;
        }

        if (match(TokenType::EscapeSequence)) {
            length = 1;
            Token t = consume();
#ifdef REGEX_DEBUG
            printf("[PARSER] EscapeSequence with substring %s\n", String(t.value()).characters());
#endif

            bytecode.insert_bytecode_compare_values({ { CharacterCompareType::Char, (u32)t.value().characters_without_null_termination()[1] } });
            should_parse_repetition_symbol = true;
            break;
        }

        if (match(TokenType::LeftBracket)) {
            consume();

            ByteCode sub_ops;
            if (!parse_bracket_expression(sub_ops, length) || !sub_ops.size())
                return set_error(Error::InvalidBracketContent);

            bytecode.append(move(sub_ops));

            consume(TokenType::RightBracket, Error::MismatchingBracket);
            should_parse_repetition_symbol = true;
            break;
        }

        if (match(TokenType::RightBracket)) {
            return set_error(Error::MismatchingBracket);
        }

        if (match(TokenType::RightCurly)) {
            return set_error(Error::MismatchingBrace);
        }

        if (match(TokenType::Circumflex)) {
            consume();
            bytecode.empend((ByteCodeValueType)OpCodeId::CheckBegin);
            break;
        }

        if (match(TokenType::Dollar)) {
            consume();
            bytecode.empend((ByteCodeValueType)OpCodeId::CheckEnd);
            break;
        }

        if (match(TokenType::RightParen))
            return false;

        if (match(TokenType::LeftParen)) {
            consume();
            Optional<StringView> capture_group_name;
            bool prevent_capture_group = false;
            if (match(TokenType::Questionmark)) {
                consume();

                if (match(TokenType::Colon)) {
                    consume();
                    prevent_capture_group = true;
                } else if (consume("<")) { // named capturing group

                    Token start_token = m_parser_state.current_token;
                    Token last_token = m_parser_state.current_token;
                    size_t capture_group_name_length = 0;
                    for (;;) {
                        if (!match_ordinary_characters())
                            return set_error(Error::InvalidNameForCaptureGroup);
                        if (match(TokenType::Char) && m_parser_state.current_token.value()[0] == '>') {
                            consume();
                            break;
                        }
                        ++capture_group_name_length;
                        last_token = consume();
                    }
                    capture_group_name = StringView(start_token.value().characters_without_null_termination(), capture_group_name_length);

                } else if (match(TokenType::EqualSign)) { // positive lookahead
                    consume();
                    ASSERT_NOT_REACHED();
                } else if (consume("!")) { // negative lookahead
                    ASSERT_NOT_REACHED();
                } else if (consume("<")) {
                    if (match(TokenType::EqualSign)) { // positive lookbehind
                        consume();
                        ASSERT_NOT_REACHED();
                    }
                    if (consume("!")) // negative lookbehind
                        ASSERT_NOT_REACHED();
                } else {
                    return set_error(Error::InvalidRepetitionMarker);
                }
            }

            if (!(m_parser_state.regex_options & AllFlags::SkipSubExprResults || prevent_capture_group)) {
                if (capture_group_name.has_value())
                    bytecode.insert_bytecode_group_capture_left(capture_group_name.value());
                else
                    bytecode.insert_bytecode_group_capture_left(m_parser_state.capture_groups_count);
            }

            ByteCode capture_group_bytecode;

            if (!parse_root(capture_group_bytecode, length))
                return set_error(Error::InvalidPattern);

            bytecode.append(move(capture_group_bytecode));

            consume(TokenType::RightParen, Error::MismatchingParen);

            if (!(m_parser_state.regex_options & AllFlags::SkipSubExprResults || prevent_capture_group)) {
                if (capture_group_name.has_value()) {
                    bytecode.insert_bytecode_group_capture_right(capture_group_name.value());
                    ++m_parser_state.named_capture_groups_count;
                } else {
                    bytecode.insert_bytecode_group_capture_right(m_parser_state.capture_groups_count);
                    ++m_parser_state.capture_groups_count;
                }
            }
            should_parse_repetition_symbol = true;
            break;
        }

        return false;
    }

    if (match_repetition_symbol()) {
        if (should_parse_repetition_symbol)
            parse_repetition_symbol(bytecode, length);
        else
            return set_error(Error::InvalidRepetitionMarker);
    }

    stack.append(move(bytecode));
    match_length_minimum += length;

    return true;
}

bool PosixExtendedParser::parse_root(ByteCode& stack, size_t& match_length_minimum)
{
    ByteCode bytecode_left;
    size_t match_length_minimum_left { 0 };

    if (match_repetition_symbol())
        return set_error(Error::InvalidRepetitionMarker);

    for (;;) {
        if (!parse_sub_expression(bytecode_left, match_length_minimum_left))
            break;

        if (match(TokenType::Pipe)) {
            consume();

            ByteCode bytecode_right;
            size_t match_length_minimum_right { 0 };

            if (!parse_root(bytecode_right, match_length_minimum_right) || bytecode_right.is_empty())
                return set_error(Error::InvalidPattern);

            ByteCode new_bytecode;
            new_bytecode.insert_bytecode_alternation(move(bytecode_left), move(bytecode_right));
            bytecode_left = move(new_bytecode);
            match_length_minimum_left = min(match_length_minimum_right, match_length_minimum_left);
        }
    }

    if (bytecode_left.is_empty())
        set_error(Error::EmptySubExpression);

    stack.append(move(bytecode_left));
    match_length_minimum = match_length_minimum_left;
    return !has_error();
}

}
