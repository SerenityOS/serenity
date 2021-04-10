/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 * Copyright (c) 2020-2021, the SerenityOS developers.
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
#include <AK/StringUtils.h>

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

ALWAYS_INLINE bool Parser::match(char ch) const
{
    return m_parser_state.current_token.type() == TokenType::Char && m_parser_state.current_token.value().length() == 1 && m_parser_state.current_token.value()[0] == ch;
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
        dbgln("[PARSER] Error: Unexpected token {}. Expected: {}", m_parser_state.current_token.name(), Token::name(type));
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

ALWAYS_INLINE bool Parser::try_skip(StringView str)
{
    if (str.starts_with(m_parser_state.current_token.value()))
        str = str.substring_view(m_parser_state.current_token.value().length(), str.length() - m_parser_state.current_token.value().length());
    else
        return false;

    size_t potentially_go_back { 0 };
    for (auto ch : str) {
        if (!m_parser_state.lexer.try_skip(ch)) {
            m_parser_state.lexer.back(potentially_go_back);
            return false;
        }
        ++potentially_go_back;
    }

    m_parser_state.current_token = m_parser_state.lexer.next();
    return true;
}

ALWAYS_INLINE bool Parser::lookahead_any(StringView str)
{
    for (auto ch : str) {
        if (match(ch))
            return true;
    }

    return false;
}

ALWAYS_INLINE char Parser::skip()
{
    char ch;
    if (m_parser_state.current_token.value().length() == 1) {
        ch = m_parser_state.current_token.value()[0];
    } else {
        m_parser_state.lexer.back(m_parser_state.current_token.value().length());
        ch = m_parser_state.lexer.skip();
    }

    m_parser_state.current_token = m_parser_state.lexer.next();
    return ch;
}

ALWAYS_INLINE void Parser::back(size_t count)
{
    m_parser_state.lexer.back(count);
    m_parser_state.current_token = m_parser_state.lexer.next();
}

ALWAYS_INLINE void Parser::reset()
{
    m_parser_state.bytecode.clear();
    m_parser_state.lexer.reset();
    m_parser_state.current_token = m_parser_state.lexer.next();
    m_parser_state.error = Error::NoError;
    m_parser_state.error_token = { TokenType::Eof, 0, StringView(nullptr) };
    m_parser_state.capture_group_minimum_lengths.clear();
    m_parser_state.capture_groups_count = 0;
    m_parser_state.named_capture_groups_count = 0;
    m_parser_state.named_capture_group_minimum_lengths.clear();
    m_parser_state.named_capture_groups.clear();
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

#if REGEX_DEBUG
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

ALWAYS_INLINE bool Parser::match_ordinary_characters()
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

        // Note: don't touch match_length_minimum, it's already correct
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
                VERIFY_NOT_REACHED();

                consume(TokenType::Period, Error::InvalidCollationElement);
                consume(TokenType::RightBracket, Error::MismatchingBracket);

            } else if (match(TokenType::EqualSign)) {
                consume();
                // FIXME: Parse collating element, this is needed when we have locale support
                //        This could have impact on length parameter, I guess.
                VERIFY_NOT_REACHED();

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
        } else {
            values.append({ CharacterCompareType::Char, (ByteCodeValueType)skip() });
        }

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
                stack.insert_bytecode_compare_string({ start_token.value().characters_without_null_termination(), new_length });
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
#if REGEX_DEBUG
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
                    VERIFY_NOT_REACHED();
                } else if (consume("!")) { // negative lookahead
                    VERIFY_NOT_REACHED();
                } else if (consume("<")) {
                    if (match(TokenType::EqualSign)) { // positive lookbehind
                        consume();
                        VERIFY_NOT_REACHED();
                    }
                    if (consume("!")) // negative lookbehind
                        VERIFY_NOT_REACHED();
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

// =============================
// ECMA262 Parser
// =============================

bool ECMA262Parser::parse_internal(ByteCode& stack, size_t& match_length_minimum)
{
    if (m_parser_state.regex_options.has_flag_set(AllFlags::Unicode)) {
        return parse_pattern(stack, match_length_minimum, true, true);
    } else {
        ByteCode new_stack;
        size_t new_match_length = 0;
        auto res = parse_pattern(new_stack, new_match_length, false, false);
        if (m_parser_state.named_capture_groups_count > 0) {
            reset();
            return parse_pattern(stack, match_length_minimum, false, true);
        }

        if (!res)
            return false;

        stack.append(new_stack);
        match_length_minimum = new_match_length;
        return res;
    }
}

bool ECMA262Parser::parse_pattern(ByteCode& stack, size_t& match_length_minimum, bool unicode, bool named)
{
    return parse_disjunction(stack, match_length_minimum, unicode, named);
}

bool ECMA262Parser::parse_disjunction(ByteCode& stack, size_t& match_length_minimum, bool unicode, bool named)
{
    ByteCode left_alternative_stack;
    size_t left_alternative_min_length = 0;
    auto alt_ok = parse_alternative(left_alternative_stack, left_alternative_min_length, unicode, named);
    if (!alt_ok)
        return false;

    if (!match(TokenType::Pipe)) {
        stack.append(left_alternative_stack);
        match_length_minimum = left_alternative_min_length;
        return alt_ok;
    }

    consume();
    ByteCode right_alternative_stack;
    size_t right_alternative_min_length = 0;
    auto continuation_ok = parse_disjunction(right_alternative_stack, right_alternative_min_length, unicode, named);
    if (!continuation_ok)
        return false;

    stack.insert_bytecode_alternation(move(left_alternative_stack), move(right_alternative_stack));
    match_length_minimum = min(left_alternative_min_length, right_alternative_min_length);
    return continuation_ok;
}

bool ECMA262Parser::parse_alternative(ByteCode& stack, size_t& match_length_minimum, bool unicode, bool named)
{
    for (;;) {
        if (match(TokenType::Eof))
            return true;

        if (parse_term(stack, match_length_minimum, unicode, named))
            continue;

        return !has_error();
    }
}

bool ECMA262Parser::parse_term(ByteCode& stack, size_t& match_length_minimum, bool unicode, bool named)
{
    if (parse_assertion(stack, match_length_minimum, unicode, named))
        return true;

    ByteCode atom_stack;
    size_t minimum_atom_length = 0;
    auto parse_with_quantifier = [&] {
        bool did_parse_one = false;
        if (m_should_use_browser_extended_grammar)
            did_parse_one = parse_extended_atom(atom_stack, minimum_atom_length, named);

        if (!did_parse_one)
            did_parse_one = parse_atom(atom_stack, minimum_atom_length, unicode, named);

        if (!did_parse_one)
            return false;

        VERIFY(did_parse_one);
        if (!parse_quantifier(atom_stack, minimum_atom_length, unicode, named))
            return false;

        return true;
    };

    if (!parse_with_quantifier())
        return false;

    stack.append(move(atom_stack));
    match_length_minimum += minimum_atom_length;
    return true;
}

bool ECMA262Parser::parse_assertion(ByteCode& stack, [[maybe_unused]] size_t& match_length_minimum, bool unicode, bool named)
{
    if (match(TokenType::Circumflex)) {
        consume();
        stack.empend((ByteCodeValueType)OpCodeId::CheckBegin);
        return true;
    }

    if (match(TokenType::Dollar)) {
        consume();
        stack.empend((ByteCodeValueType)OpCodeId::CheckEnd);
        return true;
    }

    if (try_skip("\\b")) {
        stack.insert_bytecode_check_boundary(BoundaryCheckType::Word);
        return true;
    }

    if (try_skip("\\B")) {
        stack.insert_bytecode_check_boundary(BoundaryCheckType::NonWord);
        return true;
    }

    if (match(TokenType::LeftParen)) {
        if (!try_skip("(?"))
            return false;

        if (done()) {
            set_error(Error::InvalidCaptureGroup);
            return false;
        }

        ByteCode assertion_stack;
        size_t length_dummy = 0;

        bool should_parse_forward_assertion = m_should_use_browser_extended_grammar ? unicode : true;
        if (should_parse_forward_assertion && try_skip("=")) {
            if (!parse_inner_disjunction(assertion_stack, length_dummy, unicode, named))
                return false;
            stack.insert_bytecode_lookaround(move(assertion_stack), ByteCode::LookAroundType::LookAhead);
            return true;
        }
        if (should_parse_forward_assertion && try_skip("!")) {
            if (!parse_inner_disjunction(assertion_stack, length_dummy, unicode, named))
                return false;
            stack.insert_bytecode_lookaround(move(assertion_stack), ByteCode::LookAroundType::NegatedLookAhead);
            return true;
        }
        if (m_should_use_browser_extended_grammar) {
            if (!unicode) {
                if (parse_quantifiable_assertion(assertion_stack, match_length_minimum, named)) {
                    stack.append(move(assertion_stack));
                    return true;
                }
            }
        }
        if (try_skip("<=")) {
            if (!parse_inner_disjunction(assertion_stack, length_dummy, unicode, named))
                return false;
            // FIXME: Somehow ensure that this assertion regexp has a fixed length.
            stack.insert_bytecode_lookaround(move(assertion_stack), ByteCode::LookAroundType::LookBehind, length_dummy);
            return true;
        }
        if (try_skip("<!")) {
            if (!parse_inner_disjunction(assertion_stack, length_dummy, unicode, named))
                return false;
            stack.insert_bytecode_lookaround(move(assertion_stack), ByteCode::LookAroundType::NegatedLookBehind, length_dummy);
            return true;
        }

        // If none of these matched, put the '(?' back.
        m_parser_state.lexer.back(3);
        m_parser_state.current_token = m_parser_state.lexer.next();
        return false;
    }

    return false;
}

bool ECMA262Parser::parse_inner_disjunction(ByteCode& bytecode_stack, size_t& length, bool unicode, bool named)
{
    auto disjunction_ok = parse_disjunction(bytecode_stack, length, unicode, named);
    if (!disjunction_ok)
        return false;
    consume(TokenType::RightParen, Error::MismatchingParen);
    return true;
}

bool ECMA262Parser::parse_quantifiable_assertion(ByteCode& stack, size_t&, bool named)
{
    VERIFY(m_should_use_browser_extended_grammar);
    ByteCode assertion_stack;
    size_t match_length_minimum = 0;

    if (try_skip("=")) {
        if (!parse_inner_disjunction(assertion_stack, match_length_minimum, false, named))
            return false;

        stack.insert_bytecode_lookaround(move(assertion_stack), ByteCode::LookAroundType::LookAhead);
        return true;
    }
    if (try_skip("!")) {
        if (!parse_inner_disjunction(assertion_stack, match_length_minimum, false, named))
            return false;

        stack.insert_bytecode_lookaround(move(assertion_stack), ByteCode::LookAroundType::NegatedLookAhead);
        return true;
    }

    return false;
}

StringView ECMA262Parser::read_digits_as_string(ReadDigitsInitialZeroState initial_zero, ReadDigitFollowPolicy follow_policy, bool hex, int max_count)
{
    if (!match(TokenType::Char))
        return {};

    if (initial_zero == ReadDigitsInitialZeroState::Disallow && m_parser_state.current_token.value() == "0")
        return {};

    int count = 0;
    size_t offset = 0;
    auto start_token = m_parser_state.current_token;
    while (match(TokenType::Char)) {
        auto c = m_parser_state.current_token.value();

        if (follow_policy == ReadDigitFollowPolicy::DisallowNonDigit) {
            if (hex && !AK::StringUtils::convert_to_uint_from_hex(c).has_value())
                break;
            if (!hex && !c.to_uint().has_value())
                break;
        }

        if (max_count > 0 && count >= max_count)
            break;

        offset += consume().value().length();
        ++count;
    }

    return StringView { start_token.value().characters_without_null_termination(), offset };
}

Optional<unsigned> ECMA262Parser::read_digits(ECMA262Parser::ReadDigitsInitialZeroState initial_zero, ECMA262Parser::ReadDigitFollowPolicy follow_policy, bool hex, int max_count)
{
    auto str = read_digits_as_string(initial_zero, follow_policy, hex, max_count);
    if (str.is_empty())
        return {};
    if (hex)
        return AK::StringUtils::convert_to_uint_from_hex(str);
    return str.to_uint();
}

bool ECMA262Parser::parse_quantifier(ByteCode& stack, size_t& match_length_minimum, bool, bool)
{
    enum class Repetition {
        OneOrMore,
        ZeroOrMore,
        Optional,
        Explicit,
        None,
    } repetition_mark { Repetition::None };

    bool ungreedy = false;
    Optional<size_t> repeat_min, repeat_max;

    if (match(TokenType::Asterisk)) {
        consume();
        repetition_mark = Repetition::ZeroOrMore;
    } else if (match(TokenType::Plus)) {
        consume();
        repetition_mark = Repetition::OneOrMore;
    } else if (match(TokenType::Questionmark)) {
        consume();
        repetition_mark = Repetition::Optional;
    } else if (match(TokenType::LeftCurly)) {
        consume();
        repetition_mark = Repetition::Explicit;

        auto low_bound = read_digits();

        if (!low_bound.has_value()) {
            set_error(Error::InvalidBraceContent);
            return false;
        }

        repeat_min = low_bound.value();

        if (match(TokenType::Comma)) {
            consume();
            auto high_bound = read_digits();
            if (high_bound.has_value())
                repeat_max = high_bound.value();
        } else {
            repeat_max = repeat_min;
        }

        if (!match(TokenType::RightCurly)) {
            set_error(Error::MismatchingBrace);
            return false;
        }
        consume();

        if (repeat_max.has_value()) {
            if (repeat_min.value() > repeat_max.value())
                set_error(Error::InvalidBraceContent);
        }
    } else {
        return true;
    }

    if (match(TokenType::Questionmark)) {
        consume();
        ungreedy = true;
    }

    ByteCode new_bytecode;
    switch (repetition_mark) {
    case Repetition::OneOrMore:
        new_bytecode.insert_bytecode_repetition_min_one(stack, !ungreedy);
        break;
    case Repetition::ZeroOrMore:
        new_bytecode.insert_bytecode_repetition_any(stack, !ungreedy);
        match_length_minimum = 0;
        break;
    case Repetition::Optional:
        new_bytecode.insert_bytecode_repetition_zero_or_one(stack, !ungreedy);
        match_length_minimum = 0;
        break;
    case Repetition::Explicit:
        new_bytecode.insert_bytecode_repetition_min_max(stack, repeat_min.value(), repeat_max, !ungreedy);
        match_length_minimum *= repeat_min.value();
        break;
    case Repetition::None:
        VERIFY_NOT_REACHED();
    }

    return true;
}

bool ECMA262Parser::parse_atom(ByteCode& stack, size_t& match_length_minimum, bool unicode, bool named)
{
    if (match(TokenType::EscapeSequence)) {
        // Also part of AtomEscape.
        auto token = consume();
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)token.value()[1] } });
        return true;
    }
    if (try_skip("\\")) {
        // AtomEscape.
        return parse_atom_escape(stack, match_length_minimum, unicode, named);
    }

    if (match(TokenType::LeftBracket)) {
        // Character class.
        return parse_character_class(stack, match_length_minimum, unicode && !m_should_use_browser_extended_grammar, named);
    }

    if (match(TokenType::LeftParen)) {
        // Non-capturing group, or a capture group.
        return parse_capture_group(stack, match_length_minimum, unicode && !m_should_use_browser_extended_grammar, named);
    }

    if (match(TokenType::Period)) {
        consume();
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::AnyChar, 0 } });
        return true;
    }

    if (match(TokenType::Circumflex) || match(TokenType::Dollar) || match(TokenType::RightParen)
        || match(TokenType::Pipe) || match(TokenType::Plus) || match(TokenType::Asterisk)
        || match(TokenType::Questionmark)) {

        return false;
    }

    if (match(TokenType::RightBracket) || match(TokenType::RightCurly) || match(TokenType::LeftCurly)) {
        if (m_should_use_browser_extended_grammar) {
            auto token = consume();
            match_length_minimum += 1;
            stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)token.value()[0] } });
            return true;
        } else {
            return false;
        }
    }

    if (match_ordinary_characters()) {
        auto token = consume().value();
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)token[0] } });
        return true;
    }

    set_error(Error::InvalidPattern);
    return false;
}

bool ECMA262Parser::parse_extended_atom(ByteCode&, size_t&, bool)
{
    // Note: This includes only rules *not* present in parse_atom()
    VERIFY(m_should_use_browser_extended_grammar);

    if (parse_invalid_braced_quantifier())
        return true; // FAIL FAIL FAIL

    return false;
}

bool ECMA262Parser::parse_invalid_braced_quantifier()
{
    if (!match(TokenType::LeftCurly))
        return false;
    consume();
    size_t chars_consumed = 1;
    auto low_bound = read_digits_as_string();
    StringView high_bound;

    if (low_bound.is_empty()) {
        back(chars_consumed + 1);
        return false;
    }
    chars_consumed += low_bound.length();
    if (match(TokenType::Comma)) {
        consume();
        ++chars_consumed;

        high_bound = read_digits_as_string();
        chars_consumed += high_bound.length();
    }

    if (!match(TokenType::RightCurly)) {
        back(chars_consumed + 1);
        return false;
    }

    consume();
    set_error(Error::InvalidPattern);
    return true;
}

bool ECMA262Parser::parse_atom_escape(ByteCode& stack, size_t& match_length_minimum, bool unicode, bool named)
{
    if (auto escape_str = read_digits_as_string(ReadDigitsInitialZeroState::Disallow, ReadDigitFollowPolicy::DisallowNonDigit); !escape_str.is_empty()) {
        if (auto escape = escape_str.to_uint(); escape.has_value()) {
            // See if this is a "back"-reference (we've already parsed the group it refers to)
            auto maybe_length = m_parser_state.capture_group_minimum_lengths.get(escape.value());
            if (maybe_length.has_value()) {
                match_length_minimum += maybe_length.value();
                stack.insert_bytecode_compare_values({ { CharacterCompareType::Reference, (ByteCodeValueType)escape.value() } });
                return true;
            }
            // It's not a pattern seen before, so we have to see if it's a valid reference to a future group.
            if (escape.value() <= ensure_total_number_of_capturing_parenthesis()) {
                // This refers to a future group, and it will _always_ be matching an empty string
                // So just match nothing and move on.
                return true;
            }
            if (!m_should_use_browser_extended_grammar) {
                set_error(Error::InvalidNumber);
                return false;
            }
        }

        // If not, put the characters back.
        back(escape_str.length());
    }

    // CharacterEscape > ControlEscape
    if (try_skip("f")) {
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'\f' } });
        return true;
    }

    if (try_skip("n")) {
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'\n' } });
        return true;
    }

    if (try_skip("r")) {
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'\r' } });
        return true;
    }

    if (try_skip("t")) {
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'\t' } });
        return true;
    }

    if (try_skip("v")) {
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'\v' } });
        return true;
    }

    // CharacterEscape > ControlLetter
    if (try_skip("c")) {
        for (auto c = 'A'; c <= 'z'; ++c) {
            if (try_skip({ &c, 1 })) {
                match_length_minimum += 1;
                stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)(c % 32) } });
                return true;
            }
        }

        if (m_should_use_browser_extended_grammar) {
            back(2);
            stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'\\' } });
            match_length_minimum += 1;
            return true;
        }

        if (unicode) {
            set_error(Error::InvalidPattern);
            return false;
        }

        // Allow '\c' in non-unicode mode, just matches 'c'.
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'c' } });
        return true;
    }

    // LegacyOctalEscapeSequence
    if (m_should_use_browser_extended_grammar) {
        if (!unicode) {
            if (auto escape = parse_legacy_octal_escape(); escape.has_value()) {
                stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)escape.value() } });
                match_length_minimum += 1;
                return true;
            }
        }
    }

    // '\0'
    if (try_skip("0")) {
        match_length_minimum += 1;
        stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)0 } });
        return true;
    }

    // HexEscape
    if (try_skip("x")) {
        if (auto hex_escape = read_digits(ReadDigitsInitialZeroState::Allow, ReadDigitFollowPolicy::Any, true, 2); hex_escape.has_value()) {
            match_length_minimum += 1;
            stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)hex_escape.value() } });
            return true;
        } else if (!unicode) {
            // '\x' is allowed in non-unicode mode, just matches 'x'.
            match_length_minimum += 1;
            stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'x' } });
            return true;
        } else {
            set_error(Error::InvalidPattern);
            return false;
        }
    }

    if (try_skip("u")) {
        if (auto code_point = read_digits(ReadDigitsInitialZeroState::Allow, ReadDigitFollowPolicy::Any, true, 4); code_point.has_value()) {
            // FIXME: The minimum length depends on the mode - should be utf8-length in u8 mode.
            match_length_minimum += 1;
            StringBuilder builder;
            builder.append_code_point(code_point.value());
            // FIXME: This isn't actually correct for ECMAScript.
            auto u8_encoded = builder.string_view();
            stack.insert_bytecode_compare_string(u8_encoded);
            return true;
        } else if (!unicode) {
            // '\u' is allowed in non-unicode mode, just matches 'u'.
            match_length_minimum += 1;
            stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'u' } });
            return true;
        } else {
            set_error(Error::InvalidPattern);
            return false;
        }
    }

    // IdentityEscape
    auto source_characters = m_should_use_browser_extended_grammar ? "^$\\.*+?()[|"sv : "^$\\.*+?()[]{}|"sv;
    for (auto ch : source_characters) {
        if (try_skip({ &ch, 1 })) {
            match_length_minimum += 1;
            stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)ch } });
            return true;
        }
    }

    if (unicode) {
        if (try_skip("/")) {
            match_length_minimum += 1;
            stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)'/' } });
            return true;
        }
    }

    if (named && try_skip("k")) {
        auto name = read_capture_group_specifier(true);
        if (name.is_empty()) {
            set_error(Error::InvalidNameForCaptureGroup);
            return false;
        }
        auto maybe_length = m_parser_state.named_capture_group_minimum_lengths.get(name);
        if (!maybe_length.has_value()) {
            set_error(Error::InvalidNameForCaptureGroup);
            return false;
        }
        match_length_minimum += maybe_length.value();

        stack.insert_bytecode_compare_named_reference(name);
        return true;
    }

    if (unicode) {
        if (try_skip("p{")) {
            // FIXME: Implement this path, Unicode property match.
            TODO();
        }
        if (try_skip("P{")) {
            // FIXME: Implement this path, Unicode property match.
            TODO();
        }
    }

    if (done())
        return set_error(Error::InvalidTrailingEscape);

    bool negate = false;
    auto ch = parse_character_class_escape(negate);
    if (!ch.has_value()) {
        if (!unicode) {
            // Allow all SourceCharacter's as escapes here.
            auto token = consume();
            match_length_minimum += 1;
            stack.insert_bytecode_compare_values({ { CharacterCompareType::Char, (ByteCodeValueType)token.value()[0] } });
            return true;
        }

        set_error(Error::InvalidCharacterClass);
        return false;
    }

    Vector<CompareTypeAndValuePair> compares;
    if (negate)
        compares.empend(CompareTypeAndValuePair { CharacterCompareType::Inverse, 0 });
    compares.empend(CompareTypeAndValuePair { CharacterCompareType::CharClass, (ByteCodeValueType)ch.value() });
    match_length_minimum += 1;
    stack.insert_bytecode_compare_values(move(compares));
    return true;
}
Optional<u8> ECMA262Parser::parse_legacy_octal_escape()
{
    constexpr auto all_octal_digits = "01234567";
    auto read_octal_digit = [&](auto start, auto end, bool should_ensure_no_following_octal_digit) -> Optional<u8> {
        for (char c = '0' + start; c <= '0' + end; ++c) {
            if (try_skip({ &c, 1 })) {
                if (!should_ensure_no_following_octal_digit || !lookahead_any(all_octal_digits))
                    return c - '0';
                back(2);
                return {};
            }
        }
        return {};
    };

    // OctalDigit(1)
    if (auto digit = read_octal_digit(0, 7, true); digit.has_value()) {
        return digit.value();
    }

    // OctalDigit(2)
    if (auto left_digit = read_octal_digit(0, 3, false); left_digit.has_value()) {
        if (auto right_digit = read_octal_digit(0, 7, true); right_digit.has_value()) {
            return left_digit.value() * 8 + right_digit.value();
        }

        back(2);
    }

    // OctalDigit(2)
    if (auto left_digit = read_octal_digit(4, 7, false); left_digit.has_value()) {
        if (auto right_digit = read_octal_digit(0, 7, false); right_digit.has_value()) {
            return left_digit.value() * 8 + right_digit.value();
        }

        back(2);
    }

    // OctalDigit(3)
    if (auto left_digit = read_octal_digit(0, 3, false); left_digit.has_value()) {
        size_t chars_consumed = 1;
        if (auto mid_digit = read_octal_digit(0, 7, false); mid_digit.has_value()) {
            ++chars_consumed;
            if (auto right_digit = read_octal_digit(0, 7, false); right_digit.has_value()) {
                return left_digit.value() * 64 + mid_digit.value() * 8 + right_digit.value();
            }
        }

        back(chars_consumed);
    }

    return {};
}

Optional<CharClass> ECMA262Parser::parse_character_class_escape(bool& negate, bool expect_backslash)
{
    if (expect_backslash && !try_skip("\\"))
        return {};

    // CharacterClassEscape
    CharClass ch_class;
    if (try_skip("d")) {
        ch_class = CharClass::Digit;
    } else if (try_skip("D")) {
        ch_class = CharClass::Digit;
        negate = true;
    } else if (try_skip("s")) {
        ch_class = CharClass::Space;
    } else if (try_skip("S")) {
        ch_class = CharClass::Space;
        negate = true;
    } else if (try_skip("w")) {
        ch_class = CharClass::Word;
    } else if (try_skip("W")) {
        ch_class = CharClass::Word;
        negate = true;
    } else {
        return {};
    }

    return ch_class;
}

bool ECMA262Parser::parse_character_class(ByteCode& stack, size_t& match_length_minimum, bool unicode, bool)
{
    consume(TokenType::LeftBracket, Error::InvalidPattern);

    Vector<CompareTypeAndValuePair> compares;

    if (match(TokenType::Circumflex)) {
        // Negated charclass
        consume();
        compares.empend(CompareTypeAndValuePair { CharacterCompareType::Inverse, 0 });
    }

    if (match(TokenType::RightBracket)) {
        consume();
        return true;
    }

    if (!parse_nonempty_class_ranges(compares, unicode))
        return false;

    match_length_minimum += 1;
    stack.insert_bytecode_compare_values(move(compares));
    return true;
}

struct CharClassRangeElement {
    union {
        CharClass character_class;
        u32 code_point { 0 };
    };

    bool is_negated { false };
    bool is_character_class { false };
};

bool ECMA262Parser::parse_nonempty_class_ranges(Vector<CompareTypeAndValuePair>& ranges, bool unicode)
{
    auto read_class_atom_no_dash = [&]() -> Optional<CharClassRangeElement> {
        if (match(TokenType::EscapeSequence)) {
            auto token = consume().value();
            return { { .code_point = (u32)token[1], .is_character_class = false } };
        }

        if (try_skip("\\")) {
            if (done()) {
                set_error(Error::InvalidTrailingEscape);
                return {};
            }

            if (try_skip("f"))
                return { { .code_point = '\f', .is_character_class = false } };
            if (try_skip("n"))
                return { { .code_point = '\n', .is_character_class = false } };
            if (try_skip("r"))
                return { { .code_point = '\r', .is_character_class = false } };
            if (try_skip("t"))
                return { { .code_point = '\t', .is_character_class = false } };
            if (try_skip("v"))
                return { { .code_point = '\v', .is_character_class = false } };
            if (try_skip("b"))
                return { { .code_point = '\b', .is_character_class = false } };
            if (try_skip("/"))
                return { { .code_point = '/', .is_character_class = false } };

            // CharacterEscape > ControlLetter
            if (try_skip("c")) {
                for (auto c = 'A'; c <= 'z'; ++c) {
                    if (try_skip({ &c, 1 }))
                        return { { .code_point = (u32)(c % 32), .is_character_class = false } };
                }
                if (m_should_use_browser_extended_grammar) {
                    for (auto c = '0'; c <= '9'; ++c) {
                        if (try_skip({ &c, 1 }))
                            return { { .code_point = (u32)(c % 32), .is_character_class = false } };
                    }
                    if (try_skip("_"))
                        return { { .code_point = (u32)('_' % 32), .is_character_class = false } };

                    back(2);
                    return { { .code_point = '\\', .is_character_class = false } };
                }
            }

            // '\0'
            if (try_skip("0"))
                return { { .code_point = 0, .is_character_class = false } };

            // HexEscape
            if (try_skip("x")) {
                if (auto hex_escape = read_digits(ReadDigitsInitialZeroState::Allow, ReadDigitFollowPolicy::Any, true, 2); hex_escape.has_value()) {
                    return { { .code_point = hex_escape.value(), .is_character_class = false } };
                } else if (!unicode) {
                    // '\x' is allowed in non-unicode mode, just matches 'x'.
                    return { { .code_point = 'x', .is_character_class = false } };
                } else {
                    set_error(Error::InvalidPattern);
                    return {};
                }
            }

            if (try_skip("u")) {
                if (auto code_point = read_digits(ReadDigitsInitialZeroState::Allow, ReadDigitFollowPolicy::Any, true, 4); code_point.has_value()) {
                    // FIXME: While codepoint ranges are supported, codepoint matches as "Char" are not!
                    return { { .code_point = code_point.value(), .is_character_class = false } };
                } else if (!unicode) {
                    // '\u' is allowed in non-unicode mode, just matches 'u'.
                    return { { .code_point = 'u', .is_character_class = false } };
                } else {
                    set_error(Error::InvalidPattern);
                    return {};
                }
            }

            if (unicode) {
                if (try_skip("-"))
                    return { { .code_point = '-', .is_character_class = false } };
            }

            if (try_skip("p{") || try_skip("P{")) {
                // FIXME: Implement these; unicode properties.
                TODO();
            }

            if (try_skip("d"))
                return { { .character_class = CharClass::Digit, .is_character_class = true } };
            if (try_skip("s"))
                return { { .character_class = CharClass::Space, .is_character_class = true } };
            if (try_skip("w"))
                return { { .character_class = CharClass::Word, .is_character_class = true } };
            if (try_skip("D"))
                return { { .character_class = CharClass::Digit, .is_negated = true, .is_character_class = true } };
            if (try_skip("S"))
                return { { .character_class = CharClass::Space, .is_negated = true, .is_character_class = true } };
            if (try_skip("W"))
                return { { .character_class = CharClass::Word, .is_negated = true, .is_character_class = true } };

            if (!unicode) {
                // Any unrecognised escape is allowed in non-unicode mode.
                return { { .code_point = (u32)skip(), .is_character_class = false } };
            }
        }

        if (match(TokenType::RightBracket) || match(TokenType::HyphenMinus))
            return {};

        // Allow any (other) SourceCharacter.
        return { { .code_point = (u32)skip(), .is_character_class = false } };
    };
    auto read_class_atom = [&]() -> Optional<CharClassRangeElement> {
        if (match(TokenType::HyphenMinus)) {
            consume();
            return { { .code_point = '-', .is_character_class = false } };
        }

        return read_class_atom_no_dash();
    };

    while (!match(TokenType::RightBracket)) {
        if (match(TokenType::Eof)) {
            set_error(Error::MismatchingBracket);
            return false;
        }

        auto first_atom = read_class_atom();
        if (!first_atom.has_value())
            return false;

        if (match(TokenType::HyphenMinus)) {
            consume();
            if (match(TokenType::RightBracket)) {
                //  Allow '-' as the last element in a charclass, even after an atom.
                m_parser_state.lexer.back(2); // -]
                m_parser_state.current_token = m_parser_state.lexer.next();
                goto read_as_single_atom;
            }
            auto second_atom = read_class_atom();
            if (!second_atom.has_value())
                return false;

            if (first_atom.value().is_character_class || second_atom.value().is_character_class) {
                if (m_should_use_browser_extended_grammar) {
                    if (unicode) {
                        set_error(Error::InvalidRange);
                        return false;
                    }
                    // CharacterRangeOrUnion > !Unicode > CharClass
                    if (first_atom->is_character_class)
                        ranges.empend(CompareTypeAndValuePair { CharacterCompareType::CharClass, (ByteCodeValueType)first_atom->character_class });
                    else
                        ranges.empend(CompareTypeAndValuePair { CharacterCompareType::Char, (ByteCodeValueType)first_atom->code_point });

                    ranges.empend(CompareTypeAndValuePair { CharacterCompareType::Char, (ByteCodeValueType)'-' });

                    if (second_atom->is_character_class)
                        ranges.empend(CompareTypeAndValuePair { CharacterCompareType::CharClass, (ByteCodeValueType)second_atom->character_class });
                    else
                        ranges.empend(CompareTypeAndValuePair { CharacterCompareType::Char, (ByteCodeValueType)second_atom->code_point });
                    continue;
                } else {
                    set_error(Error::InvalidRange);
                    return false;
                }
            }

            if (first_atom.value().code_point > second_atom.value().code_point) {
                set_error(Error::InvalidRange);
                return false;
            }

            VERIFY(!first_atom.value().is_negated);
            VERIFY(!second_atom.value().is_negated);

            ranges.empend(CompareTypeAndValuePair { CharacterCompareType::CharRange, CharRange { first_atom.value().code_point, second_atom.value().code_point } });
            continue;
        }

    read_as_single_atom:;

        auto atom = first_atom.value();

        if (atom.is_character_class) {
            if (atom.is_negated)
                ranges.empend(CompareTypeAndValuePair { CharacterCompareType::TemporaryInverse, 0 });
            ranges.empend(CompareTypeAndValuePair { CharacterCompareType::CharClass, (ByteCodeValueType)first_atom.value().character_class });
        } else {
            VERIFY(!atom.is_negated);
            ranges.empend(CompareTypeAndValuePair { CharacterCompareType::Char, first_atom.value().code_point });
        }
    }

    consume(TokenType::RightBracket, Error::MismatchingBracket);

    return true;
}

StringView ECMA262Parser::read_capture_group_specifier(bool take_starting_angle_bracket)
{
    if (take_starting_angle_bracket && !consume("<"))
        return {};

    auto start_token = m_parser_state.current_token;
    size_t offset = 0;
    while (match(TokenType::Char)) {
        auto c = m_parser_state.current_token.value();
        if (c == ">")
            break;
        offset += consume().value().length();
    }

    StringView name { start_token.value().characters_without_null_termination(), offset };
    if (!consume(">") || name.is_empty())
        set_error(Error::InvalidNameForCaptureGroup);

    return name;
}

bool ECMA262Parser::parse_capture_group(ByteCode& stack, size_t& match_length_minimum, bool unicode, bool named)
{
    consume(TokenType::LeftParen, Error::InvalidPattern);

    if (match(TokenType::Questionmark)) {
        // Non-capturing group or group with specifier.
        consume();

        if (match(TokenType::Colon)) {
            consume();
            ByteCode noncapture_group_bytecode;
            size_t length = 0;
            if (!parse_disjunction(noncapture_group_bytecode, length, unicode, named))
                return set_error(Error::InvalidPattern);

            consume(TokenType::RightParen, Error::MismatchingParen);

            stack.append(move(noncapture_group_bytecode));
            match_length_minimum += length;
            return true;
        }

        if (consume("<")) {
            ++m_parser_state.named_capture_groups_count;
            auto group_index = ++m_parser_state.capture_groups_count; // Named capture groups count as normal capture groups too.
            auto name = read_capture_group_specifier();

            if (name.is_empty()) {
                set_error(Error::InvalidNameForCaptureGroup);
                return false;
            }

            ByteCode capture_group_bytecode;
            size_t length = 0;
            if (!parse_disjunction(capture_group_bytecode, length, unicode, named))
                return set_error(Error::InvalidPattern);

            consume(TokenType::RightParen, Error::MismatchingParen);

            stack.insert_bytecode_group_capture_left(name);
            stack.insert_bytecode_group_capture_left(group_index);
            stack.append(move(capture_group_bytecode));
            stack.insert_bytecode_group_capture_right(name);
            stack.insert_bytecode_group_capture_right(group_index);

            match_length_minimum += length;

            m_parser_state.named_capture_group_minimum_lengths.set(name, length);
            m_parser_state.capture_group_minimum_lengths.set(group_index, length);
            return true;
        }

        set_error(Error::InvalidCaptureGroup);
        return false;
    }

    auto group_index = ++m_parser_state.capture_groups_count;
    stack.insert_bytecode_group_capture_left(group_index);

    ByteCode capture_group_bytecode;
    size_t length = 0;

    if (!parse_disjunction(capture_group_bytecode, length, unicode, named))
        return set_error(Error::InvalidPattern);

    stack.append(move(capture_group_bytecode));

    m_parser_state.capture_group_minimum_lengths.set(group_index, length);

    consume(TokenType::RightParen, Error::MismatchingParen);

    stack.insert_bytecode_group_capture_right(group_index);

    match_length_minimum += length;

    return true;
}

size_t ECMA262Parser::ensure_total_number_of_capturing_parenthesis()
{
    if (m_total_number_of_capturing_parenthesis.has_value())
        return m_total_number_of_capturing_parenthesis.value();

    GenericLexer lexer { m_parser_state.lexer.source() };
    size_t count = 0;
    while (!lexer.is_eof()) {
        switch (lexer.peek()) {
        case '\\':
            lexer.consume(2);
            continue;
        case '[':
            while (!lexer.is_eof()) {
                if (lexer.consume_specific('\\'))
                    lexer.consume();
                else if (lexer.consume_specific(']'))
                    break;
                lexer.consume();
            }
            break;
        case '(':
            if (lexer.consume_specific('?')) {
                // non-capturing group '(?:', lookaround '(?<='/'(?<!', or named capture '(?<'
                if (!lexer.consume_specific('<'))
                    break;

                if (lexer.next_is(is_any_of("=!")))
                    break;

                ++count;
            } else {
                ++count;
            }
            break;
        }
        lexer.consume();
    }

    m_total_number_of_capturing_parenthesis = count;
    return count;
}
}
