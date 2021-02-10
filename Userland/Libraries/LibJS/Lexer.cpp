/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include "Lexer.h"
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <ctype.h>
#include <stdio.h>

namespace JS {

HashMap<String, TokenType> Lexer::s_keywords;
HashMap<String, TokenType> Lexer::s_three_char_tokens;
HashMap<String, TokenType> Lexer::s_two_char_tokens;
HashMap<char, TokenType> Lexer::s_single_char_tokens;

Lexer::Lexer(StringView source)
    : m_source(source)
    , m_current_token(TokenType::Eof, {}, StringView(nullptr), StringView(nullptr), 0, 0)
{
    if (s_keywords.is_empty()) {
        s_keywords.set("await", TokenType::Await);
        s_keywords.set("break", TokenType::Break);
        s_keywords.set("case", TokenType::Case);
        s_keywords.set("catch", TokenType::Catch);
        s_keywords.set("class", TokenType::Class);
        s_keywords.set("const", TokenType::Const);
        s_keywords.set("continue", TokenType::Continue);
        s_keywords.set("debugger", TokenType::Debugger);
        s_keywords.set("default", TokenType::Default);
        s_keywords.set("delete", TokenType::Delete);
        s_keywords.set("do", TokenType::Do);
        s_keywords.set("else", TokenType::Else);
        s_keywords.set("enum", TokenType::Enum);
        s_keywords.set("export", TokenType::Export);
        s_keywords.set("extends", TokenType::Extends);
        s_keywords.set("false", TokenType::BoolLiteral);
        s_keywords.set("finally", TokenType::Finally);
        s_keywords.set("for", TokenType::For);
        s_keywords.set("function", TokenType::Function);
        s_keywords.set("if", TokenType::If);
        s_keywords.set("import", TokenType::Import);
        s_keywords.set("in", TokenType::In);
        s_keywords.set("instanceof", TokenType::Instanceof);
        s_keywords.set("let", TokenType::Let);
        s_keywords.set("new", TokenType::New);
        s_keywords.set("null", TokenType::NullLiteral);
        s_keywords.set("return", TokenType::Return);
        s_keywords.set("super", TokenType::Super);
        s_keywords.set("switch", TokenType::Switch);
        s_keywords.set("this", TokenType::This);
        s_keywords.set("throw", TokenType::Throw);
        s_keywords.set("true", TokenType::BoolLiteral);
        s_keywords.set("try", TokenType::Try);
        s_keywords.set("typeof", TokenType::Typeof);
        s_keywords.set("var", TokenType::Var);
        s_keywords.set("void", TokenType::Void);
        s_keywords.set("while", TokenType::While);
        s_keywords.set("with", TokenType::With);
        s_keywords.set("yield", TokenType::Yield);
    }

    if (s_three_char_tokens.is_empty()) {
        s_three_char_tokens.set("===", TokenType::EqualsEqualsEquals);
        s_three_char_tokens.set("!==", TokenType::ExclamationMarkEqualsEquals);
        s_three_char_tokens.set("**=", TokenType::DoubleAsteriskEquals);
        s_three_char_tokens.set("<<=", TokenType::ShiftLeftEquals);
        s_three_char_tokens.set(">>=", TokenType::ShiftRightEquals);
        s_three_char_tokens.set("&&=", TokenType::DoubleAmpersandEquals);
        s_three_char_tokens.set("||=", TokenType::DoublePipeEquals);
        s_three_char_tokens.set("\?\?=", TokenType::DoubleQuestionMarkEquals);
        s_three_char_tokens.set(">>>", TokenType::UnsignedShiftRight);
        s_three_char_tokens.set("...", TokenType::TripleDot);
    }

    if (s_two_char_tokens.is_empty()) {
        s_two_char_tokens.set("=>", TokenType::Arrow);
        s_two_char_tokens.set("+=", TokenType::PlusEquals);
        s_two_char_tokens.set("-=", TokenType::MinusEquals);
        s_two_char_tokens.set("*=", TokenType::AsteriskEquals);
        s_two_char_tokens.set("/=", TokenType::SlashEquals);
        s_two_char_tokens.set("%=", TokenType::PercentEquals);
        s_two_char_tokens.set("&=", TokenType::AmpersandEquals);
        s_two_char_tokens.set("|=", TokenType::PipeEquals);
        s_two_char_tokens.set("^=", TokenType::CaretEquals);
        s_two_char_tokens.set("&&", TokenType::DoubleAmpersand);
        s_two_char_tokens.set("||", TokenType::DoublePipe);
        s_two_char_tokens.set("??", TokenType::DoubleQuestionMark);
        s_two_char_tokens.set("**", TokenType::DoubleAsterisk);
        s_two_char_tokens.set("==", TokenType::EqualsEquals);
        s_two_char_tokens.set("<=", TokenType::LessThanEquals);
        s_two_char_tokens.set(">=", TokenType::GreaterThanEquals);
        s_two_char_tokens.set("!=", TokenType::ExclamationMarkEquals);
        s_two_char_tokens.set("--", TokenType::MinusMinus);
        s_two_char_tokens.set("++", TokenType::PlusPlus);
        s_two_char_tokens.set("<<", TokenType::ShiftLeft);
        s_two_char_tokens.set(">>", TokenType::ShiftRight);
        s_two_char_tokens.set("?.", TokenType::QuestionMarkPeriod);
    }

    if (s_single_char_tokens.is_empty()) {
        s_single_char_tokens.set('&', TokenType::Ampersand);
        s_single_char_tokens.set('*', TokenType::Asterisk);
        s_single_char_tokens.set('[', TokenType::BracketOpen);
        s_single_char_tokens.set(']', TokenType::BracketClose);
        s_single_char_tokens.set('^', TokenType::Caret);
        s_single_char_tokens.set(':', TokenType::Colon);
        s_single_char_tokens.set(',', TokenType::Comma);
        s_single_char_tokens.set('{', TokenType::CurlyOpen);
        s_single_char_tokens.set('}', TokenType::CurlyClose);
        s_single_char_tokens.set('=', TokenType::Equals);
        s_single_char_tokens.set('!', TokenType::ExclamationMark);
        s_single_char_tokens.set('-', TokenType::Minus);
        s_single_char_tokens.set('(', TokenType::ParenOpen);
        s_single_char_tokens.set(')', TokenType::ParenClose);
        s_single_char_tokens.set('%', TokenType::Percent);
        s_single_char_tokens.set('.', TokenType::Period);
        s_single_char_tokens.set('|', TokenType::Pipe);
        s_single_char_tokens.set('+', TokenType::Plus);
        s_single_char_tokens.set('?', TokenType::QuestionMark);
        s_single_char_tokens.set(';', TokenType::Semicolon);
        s_single_char_tokens.set('/', TokenType::Slash);
        s_single_char_tokens.set('~', TokenType::Tilde);
        s_single_char_tokens.set('<', TokenType::LessThan);
        s_single_char_tokens.set('>', TokenType::GreaterThan);
    }
    consume();
}

void Lexer::consume()
{
    auto did_reach_eof = [this] {
        if (m_position != m_source.length())
            return false;
        m_position++;
        m_line_column++;
        m_current_char = EOF;
        return true;
    };

    if (m_position > m_source.length())
        return;

    if (did_reach_eof())
        return;

    if (is_line_terminator()) {
#if LEXER_DEBUG
        String type;
        if (m_current_char == '\n')
            type = "LINE FEED";
        else if (m_current_char == '\r')
            type = "CARRIAGE RETURN";
        else if (m_source[m_position + 1] == (char)0xa8)
            type = "LINE SEPARATOR";
        else
            type = "PARAGRAPH SEPARATOR";
        dbgln("Found a line terminator: {}", type);
#endif
        // This is a three-char line terminator, we need to increase m_position some more.
        // We might reach EOF and need to check again.
        if (m_current_char != '\n' && m_current_char != '\r') {
            m_position += 2;
            if (did_reach_eof())
                return;
        }

        // If the previous character is \r and the current one \n we already updated line number
        // and column - don't do it again. From https://tc39.es/ecma262/#sec-line-terminators:
        //   The sequence <CR><LF> is commonly used as a line terminator.
        //   It should be considered a single SourceCharacter for the purpose of reporting line numbers.
        auto second_char_of_crlf = m_position > 1 && m_source[m_position - 2] == '\r' && m_current_char == '\n';

        if (!second_char_of_crlf) {
            m_line_number++;
            m_line_column = 1;
#if LEXER_DEBUG
            dbgln("Incremented line number, now at: line {}, column 1", m_line_number);
        } else {
            dbgln("Previous was CR, this is LF - not incrementing line number again.");
#endif
        }
    } else {
        m_line_column++;
    }

    m_current_char = m_source[m_position++];
}

bool Lexer::consume_exponent()
{
    consume();
    if (m_current_char == '-' || m_current_char == '+')
        consume();

    if (!isdigit(m_current_char))
        return false;

    while (isdigit(m_current_char)) {
        consume();
    }
    return true;
}

bool Lexer::consume_octal_number()
{
    consume();
    if (!(m_current_char >= '0' && m_current_char <= '7'))
        return false;

    while (m_current_char >= '0' && m_current_char <= '7') {
        consume();
    }

    return true;
}

bool Lexer::consume_hexadecimal_number()
{
    consume();
    if (!isxdigit(m_current_char))
        return false;

    while (isxdigit(m_current_char))
        consume();

    return true;
}

bool Lexer::consume_binary_number()
{
    consume();
    if (!(m_current_char == '0' || m_current_char == '1'))
        return false;

    while (m_current_char == '0' || m_current_char == '1')
        consume();

    return true;
}

bool Lexer::match(char a, char b) const
{
    if (m_position >= m_source.length())
        return false;

    return m_current_char == a
        && m_source[m_position] == b;
}

bool Lexer::match(char a, char b, char c) const
{
    if (m_position + 1 >= m_source.length())
        return false;

    return m_current_char == a
        && m_source[m_position] == b
        && m_source[m_position + 1] == c;
}

bool Lexer::match(char a, char b, char c, char d) const
{
    if (m_position + 2 >= m_source.length())
        return false;

    return m_current_char == a
        && m_source[m_position] == b
        && m_source[m_position + 1] == c
        && m_source[m_position + 2] == d;
}

bool Lexer::is_eof() const
{
    return m_current_char == EOF;
}

bool Lexer::is_line_terminator() const
{
    if (m_current_char == '\n' || m_current_char == '\r')
        return true;
    if (m_position > 0 && m_position + 1 < m_source.length()) {
        auto three_chars_view = m_source.substring_view(m_position - 1, 3);
        return (three_chars_view == LINE_SEPARATOR) || (three_chars_view == PARAGRAPH_SEPARATOR);
    }
    return false;
}

bool Lexer::is_identifier_start() const
{
    return isalpha(m_current_char) || m_current_char == '_' || m_current_char == '$';
}

bool Lexer::is_identifier_middle() const
{
    return is_identifier_start() || isdigit(m_current_char);
}

bool Lexer::is_line_comment_start(bool line_has_token_yet) const
{
    return match('/', '/')
        || match('<', '!', '-', '-')
        // "-->" is considered a line comment start if the current line is only whitespace and/or
        // other block comment(s); or in other words: the current line does not have a token or
        // ongoing line comment yet
        || (match('-', '-', '>') && !line_has_token_yet);
}

bool Lexer::is_block_comment_start() const
{
    return match('/', '*');
}

bool Lexer::is_block_comment_end() const
{
    return match('*', '/');
}

bool Lexer::is_numeric_literal_start() const
{
    return isdigit(m_current_char) || (m_current_char == '.' && m_position < m_source.length() && isdigit(m_source[m_position]));
}

bool Lexer::slash_means_division() const
{
    auto type = m_current_token.type();
    return type == TokenType::BigIntLiteral
        || type == TokenType::BoolLiteral
        || type == TokenType::BracketClose
        || type == TokenType::CurlyClose
        || type == TokenType::Identifier
        || type == TokenType::NullLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::ParenClose
        || type == TokenType::RegexLiteral
        || type == TokenType::StringLiteral
        || type == TokenType::TemplateLiteralEnd
        || type == TokenType::This;
}

Token Lexer::next()
{
    size_t trivia_start = m_position;
    auto in_template = !m_template_states.is_empty();
    bool line_has_token_yet = m_line_column > 1;
    bool unterminated_comment = false;

    if (!in_template || m_template_states.last().in_expr) {
        // consume whitespace and comments
        while (true) {
            if (is_line_terminator()) {
                line_has_token_yet = false;
                do {
                    consume();
                } while (is_line_terminator());
            } else if (isspace(m_current_char)) {
                do {
                    consume();
                } while (isspace(m_current_char));
            } else if (is_line_comment_start(line_has_token_yet)) {
                consume();
                do {
                    consume();
                } while (!is_eof() && !is_line_terminator());
            } else if (is_block_comment_start()) {
                consume();
                do {
                    consume();
                } while (!is_eof() && !is_block_comment_end());
                if (is_eof())
                    unterminated_comment = true;
                consume(); // consume *
                if (is_eof())
                    unterminated_comment = true;
                consume(); // consume /
            } else {
                break;
            }
        }
    }

    size_t value_start = m_position;
    size_t value_start_line_number = m_line_number;
    size_t value_start_column_number = m_line_column;
    auto token_type = TokenType::Invalid;
    // This is being used to communicate info about invalid tokens to the parser, which then
    // can turn that into more specific error messages - instead of us having to make up a
    // bunch of Invalid* tokens (bad numeric literals, unterminated comments etc.)
    String token_message;

    if (m_current_token.type() == TokenType::RegexLiteral && !is_eof() && isalpha(m_current_char)) {
        token_type = TokenType::RegexFlags;
        while (!is_eof() && isalpha(m_current_char))
            consume();
    } else if (m_current_char == '`') {
        consume();

        if (!in_template) {
            token_type = TokenType::TemplateLiteralStart;
            m_template_states.append({ false, 0 });
        } else {
            if (m_template_states.last().in_expr) {
                m_template_states.append({ false, 0 });
                token_type = TokenType::TemplateLiteralStart;
            } else {
                m_template_states.take_last();
                token_type = TokenType::TemplateLiteralEnd;
            }
        }
    } else if (in_template && m_template_states.last().in_expr && m_template_states.last().open_bracket_count == 0 && m_current_char == '}') {
        consume();
        token_type = TokenType::TemplateLiteralExprEnd;
        m_template_states.last().in_expr = false;
    } else if (in_template && !m_template_states.last().in_expr) {
        if (is_eof()) {
            token_type = TokenType::UnterminatedTemplateLiteral;
            m_template_states.take_last();
        } else if (match('$', '{')) {
            token_type = TokenType::TemplateLiteralExprStart;
            consume();
            consume();
            m_template_states.last().in_expr = true;
        } else {
            while (!match('$', '{') && m_current_char != '`' && !is_eof()) {
                if (match('\\', '$') || match('\\', '`'))
                    consume();
                consume();
            }
            if (is_eof() && !m_template_states.is_empty())
                token_type = TokenType::UnterminatedTemplateLiteral;
            else
                token_type = TokenType::TemplateLiteralString;
        }
    } else if (is_identifier_start()) {
        // identifier or keyword
        do {
            consume();
        } while (is_identifier_middle());

        StringView value = m_source.substring_view(value_start - 1, m_position - value_start);
        auto it = s_keywords.find(value.hash(), [&](auto& entry) { return entry.key == value; });
        if (it == s_keywords.end()) {
            token_type = TokenType::Identifier;
        } else {
            token_type = it->value;
        }
    } else if (is_numeric_literal_start()) {
        token_type = TokenType::NumericLiteral;
        bool is_invalid_numeric_literal = false;
        if (m_current_char == '0') {
            consume();
            if (m_current_char == '.') {
                // decimal
                consume();
                while (isdigit(m_current_char))
                    consume();
                if (m_current_char == 'e' || m_current_char == 'E')
                    is_invalid_numeric_literal = !consume_exponent();
            } else if (m_current_char == 'e' || m_current_char == 'E') {
                is_invalid_numeric_literal = !consume_exponent();
            } else if (m_current_char == 'o' || m_current_char == 'O') {
                // octal
                is_invalid_numeric_literal = !consume_octal_number();
            } else if (m_current_char == 'b' || m_current_char == 'B') {
                // binary
                is_invalid_numeric_literal = !consume_binary_number();
            } else if (m_current_char == 'x' || m_current_char == 'X') {
                // hexadecimal
                is_invalid_numeric_literal = !consume_hexadecimal_number();
            } else if (m_current_char == 'n') {
                consume();
                token_type = TokenType::BigIntLiteral;
            } else if (isdigit(m_current_char)) {
                // octal without '0o' prefix. Forbidden in 'strict mode'
                do {
                    consume();
                } while (isdigit(m_current_char));
            }
        } else {
            // 1...9 or period
            while (isdigit(m_current_char))
                consume();
            if (m_current_char == 'n') {
                consume();
                token_type = TokenType::BigIntLiteral;
            } else {
                if (m_current_char == '.') {
                    consume();
                    while (isdigit(m_current_char))
                        consume();
                }
                if (m_current_char == 'e' || m_current_char == 'E')
                    is_invalid_numeric_literal = !consume_exponent();
            }
        }
        if (is_invalid_numeric_literal) {
            token_type = TokenType::Invalid;
            token_message = "Invalid numeric literal";
        }
    } else if (m_current_char == '"' || m_current_char == '\'') {
        char stop_char = m_current_char;
        consume();
        // Note: LS/PS line terminators are allowed in string literals.
        while (m_current_char != stop_char && m_current_char != '\r' && m_current_char != '\n' && !is_eof()) {
            if (m_current_char == '\\') {
                consume();
            }
            consume();
        }
        if (m_current_char != stop_char) {
            token_type = TokenType::UnterminatedStringLiteral;
        } else {
            consume();
            token_type = TokenType::StringLiteral;
        }
    } else if (m_current_char == '/' && !slash_means_division()) {
        consume();
        token_type = TokenType::RegexLiteral;

        while (!is_eof()) {
            if (m_current_char == '[') {
                m_regex_is_in_character_class = true;
            } else if (m_current_char == ']') {
                m_regex_is_in_character_class = false;
            } else if (!m_regex_is_in_character_class && m_current_char == '/') {
                break;
            }

            if (match('\\', '/') || match('\\', '[') || match('\\', '\\') || (m_regex_is_in_character_class && match('\\', ']')))
                consume();
            consume();
        }

        if (is_eof()) {
            token_type = TokenType::UnterminatedRegexLiteral;
        } else {
            consume();
        }
    } else if (m_current_char == EOF) {
        if (unterminated_comment) {
            token_type = TokenType::Invalid;
            token_message = "Unterminated multi-line comment";
        } else {
            token_type = TokenType::Eof;
        }
    } else {
        // There is only one four-char operator: >>>=
        bool found_four_char_token = false;
        if (match('>', '>', '>', '=')) {
            found_four_char_token = true;
            consume();
            consume();
            consume();
            consume();
            token_type = TokenType::UnsignedShiftRightEquals;
        }

        bool found_three_char_token = false;
        if (!found_four_char_token && m_position + 1 < m_source.length()) {
            auto three_chars_view = m_source.substring_view(m_position - 1, 3);
            auto it = s_three_char_tokens.find(three_chars_view.hash(), [&](auto& entry) { return entry.key == three_chars_view; });
            if (it != s_three_char_tokens.end()) {
                found_three_char_token = true;
                consume();
                consume();
                consume();
                token_type = it->value;
            }
        }

        bool found_two_char_token = false;
        if (!found_four_char_token && !found_three_char_token && m_position < m_source.length()) {
            auto two_chars_view = m_source.substring_view(m_position - 1, 2);
            auto it = s_two_char_tokens.find(two_chars_view.hash(), [&](auto& entry) { return entry.key == two_chars_view; });
            if (it != s_two_char_tokens.end()) {
                // OptionalChainingPunctuator :: ?. [lookahead ∉ DecimalDigit]
                if (!(it->value == TokenType::QuestionMarkPeriod && m_position + 1 < m_source.length() && isdigit(m_source[m_position + 1]))) {
                    found_two_char_token = true;
                    consume();
                    consume();
                    token_type = it->value;
                }
            }
        }

        bool found_one_char_token = false;
        if (!found_four_char_token && !found_three_char_token && !found_two_char_token) {
            auto it = s_single_char_tokens.find(m_current_char);
            if (it != s_single_char_tokens.end()) {
                found_one_char_token = true;
                consume();
                token_type = it->value;
            }
        }

        if (!found_four_char_token && !found_three_char_token && !found_two_char_token && !found_one_char_token) {
            consume();
            token_type = TokenType::Invalid;
        }
    }

    if (!m_template_states.is_empty() && m_template_states.last().in_expr) {
        if (token_type == TokenType::CurlyOpen) {
            m_template_states.last().open_bracket_count++;
        } else if (token_type == TokenType::CurlyClose) {
            m_template_states.last().open_bracket_count--;
        }
    }

    m_current_token = Token(
        token_type,
        token_message,
        m_source.substring_view(trivia_start - 1, value_start - trivia_start),
        m_source.substring_view(value_start - 1, m_position - value_start),
        value_start_line_number,
        value_start_column_number);

#if LEXER_DEBUG
    dbgln("------------------------------");
    dbgln("Token: {}", m_current_token.name());
    dbgln("Trivia: _{}_", m_current_token.trivia());
    dbgln("Value: _{}_", m_current_token.value());
    dbgln("Line: {}, Column: {}", m_current_token.line_number(), m_current_token.line_column());
    dbgln("------------------------------");
#endif

    return m_current_token;
}

}
