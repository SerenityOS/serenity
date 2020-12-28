/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <ctype.h>

namespace Cpp {

Lexer::Lexer(const StringView& input)
    : m_input(input)
{
}

char Lexer::peek(size_t offset) const
{
    if ((m_index + offset) >= m_input.length())
        return 0;
    return m_input[m_index + offset];
}

char Lexer::consume()
{
    ASSERT(m_index < m_input.length());
    char ch = m_input[m_index++];
    m_previous_position = m_position;
    if (ch == '\n') {
        m_position.line++;
        m_position.column = 0;
    } else {
        m_position.column++;
    }
    m_position.index = m_index;
    return ch;
}

static bool is_valid_first_character_of_identifier(char ch)
{
    return isalpha(ch) || ch == '_' || ch == '$';
}

static bool is_valid_nonfirst_character_of_identifier(char ch)
{
    return is_valid_first_character_of_identifier(ch) || isdigit(ch);
}

#define __KNOWN_KEYWORD(keyword, string) { string, Token::KnownKeyword::keyword },

static const HashMap<StringView, Cpp::Token::KnownKeyword> s_known_keywords = {
    FOR_EACH_CPP_KNOWN_KEYWORD
};

#define __KNOWN_TYPE(type, string) { string, Token::KnownType::type },

static const HashMap<StringView, Cpp::Token::KnownType> s_known_types = {
    FOR_EACH_CPP_KNOWN_TYPE
};
#undef __KNOWN_TYPE

Token Lexer::lex_one_token()
{
    size_t token_start_index = 0;
    Position token_start_position;

    auto emit_token = [&](auto type) {
        Token token;
        token.m_type = type;
        token.m_start = m_position;
        token.m_end = m_position;
        consume();
        return token;
    };

    auto begin_token = [&] {
        token_start_index = m_index;
        token_start_position = m_position;
    };
    auto commit_token = [&](auto type) {
        Token token;
        token.m_type = type;
        token.m_start = token_start_position;
        token.m_end = m_position;
        return token;
    };

    auto emit_token_equals = [&](auto type, auto equals_type) {
        if (peek(1) == '=') {
            begin_token();
            consume();
            consume();
            return commit_token(equals_type);
        }
        return emit_token(type);
    };

    auto match_escape_sequence = [&]() -> size_t {
        switch (peek(1)) {
        case '\'':
        case '"':
        case '?':
        case '\\':
        case 'a':
        case 'b':
        case 'f':
        case 'n':
        case 'r':
        case 't':
        case 'v':
            return 2;
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7': {
            size_t octal_digits = 1;
            for (size_t i = 0; i < 2; ++i) {
                char next = peek(2 + i);
                if (next < '0' || next > '7')
                    break;
                ++octal_digits;
            }
            return 1 + octal_digits;
        }
        case 'x': {
            size_t hex_digits = 0;
            while (isxdigit(peek(2 + hex_digits)))
                ++hex_digits;
            return 2 + hex_digits;
        }
        case 'u':
        case 'U': {
            bool is_unicode = true;
            size_t number_of_digits = peek(1) == 'u' ? 4 : 8;
            for (size_t i = 0; i < number_of_digits; ++i) {
                if (!isxdigit(peek(2 + i))) {
                    is_unicode = false;
                    break;
                }
            }
            return is_unicode ? 2 + number_of_digits : 0;
        }
        default:
            return 0;
        }
    };

    auto match_string_prefix = [&](char quote) -> size_t {
        if (peek() == quote)
            return 1;
        if (peek() == 'L' && peek(1) == quote)
            return 2;
        if (peek() == 'u') {
            if (peek(1) == quote)
                return 2;
            if (peek(1) == '8' && peek(2) == quote)
                return 3;
        }
        if (peek() == 'U' && peek(1) == quote)
            return 2;
        return 0;
    };

    auto ch = peek();

    if (m_index >= m_input.length()) {
        return commit_token(Token::Type::EndOfFile);
    }

    if (isspace(ch)) {
        begin_token();
        while (isspace(peek()))
            consume();
        return commit_token(Token::Type::Whitespace);
    }
    if (ch == '(') {
        return emit_token(Token::Type::LeftParen);
    }
    if (ch == ')') {
        return emit_token(Token::Type::RightParen);
    }
    if (ch == '{') {
        return emit_token(Token::Type::LeftCurly);
    }
    if (ch == '}') {
        return emit_token(Token::Type::RightCurly);
    }
    if (ch == '[') {
        return emit_token(Token::Type::LeftBracket);
    }
    if (ch == ']') {
        return emit_token(Token::Type::RightBracket);
    }
    if (ch == '<') {
        begin_token();
        consume();
        if (peek() == '<') {
            consume();
            if (peek() == '=') {
                consume();
                return commit_token(Token::Type::LessLessEquals);
            }
            return commit_token(Token::Type::LessLess);
        }
        if (peek() == '=') {
            consume();
            return commit_token(Token::Type::LessEquals);
        }
        if (peek() == '>') {
            consume();
            return commit_token(Token::Type::LessGreater);
        }
        return commit_token(Token::Type::Less);
    }
    if (ch == '>') {
        begin_token();
        consume();
        if (peek() == '>') {
            consume();
            if (peek() == '=') {
                consume();
                return commit_token(Token::Type::GreaterGreaterEquals);
            }
            return commit_token(Token::Type::GreaterGreater);
        }
        if (peek() == '=') {
            consume();
            return commit_token(Token::Type::GreaterEquals);
        }
        return commit_token(Token::Type::Greater);
    }
    if (ch == ',') {
        return emit_token(Token::Type::Comma);
    }
    if (ch == '+') {
        begin_token();
        consume();
        if (peek() == '+') {
            consume();
            return commit_token(Token::Type::PlusPlus);
        }
        if (peek() == '=') {
            consume();
            return commit_token(Token::Type::PlusEquals);
        }
        return commit_token(Token::Type::Plus);
    }
    if (ch == '-') {
        begin_token();
        consume();
        if (peek() == '-') {
            consume();
            return commit_token(Token::Type::MinusMinus);
        }
        if (peek() == '=') {
            consume();
            return commit_token(Token::Type::MinusEquals);
        }
        if (peek() == '>') {
            consume();
            if (peek() == '*') {
                consume();
                return commit_token(Token::Type::ArrowAsterisk);
            }
            return commit_token(Token::Type::Arrow);
        }
        return commit_token(Token::Type::Minus);
    }
    if (ch == '*') {
        return emit_token_equals(Token::Type::Asterisk, Token::Type::AsteriskEquals);
    }
    if (ch == '%') {
        return emit_token_equals(Token::Type::Percent, Token::Type::PercentEquals);
    }
    if (ch == '^') {
        return emit_token_equals(Token::Type::Caret, Token::Type::CaretEquals);
    }
    if (ch == '!') {
        return emit_token_equals(Token::Type::ExclamationMark, Token::Type::ExclamationMarkEquals);
    }
    if (ch == '=') {
        return emit_token_equals(Token::Type::Equals, Token::Type::EqualsEquals);
    }
    if (ch == '&') {
        begin_token();
        consume();
        if (peek() == '&') {
            consume();
            return commit_token(Token::Type::AndAnd);
        }
        if (peek() == '=') {
            consume();
            return commit_token(Token::Type::AndEquals);
        }
        return commit_token(Token::Type::And);
    }
    if (ch == '|') {
        begin_token();
        consume();
        if (peek() == '|') {
            consume();
            return commit_token(Token::Type::PipePipe);
        }
        if (peek() == '=') {
            consume();
            return commit_token(Token::Type::PipeEquals);
        }
        return commit_token(Token::Type::Pipe);
    }
    if (ch == '~') {
        return emit_token(Token::Type::Tilde);
    }
    if (ch == '?') {
        return emit_token(Token::Type::QuestionMark);
    }
    if (ch == ':') {
        begin_token();
        consume();
        if (peek() == ':') {
            consume();
            if (peek() == '*') {
                consume();
                return commit_token(Token::Type::ColonColonAsterisk);
            }
            return commit_token(Token::Type::ColonColon);
        }
        return commit_token(Token::Type::Colon);
    }
    if (ch == ';') {
        return emit_token(Token::Type::Semicolon);
    }
    if (ch == '.') {
        begin_token();
        consume();
        if (peek() == '*') {
            consume();
            return commit_token(Token::Type::DotAsterisk);
        }
        return commit_token(Token::Type::Dot);
    }
    if (ch == '#') {
        begin_token();
        consume();

        if (is_valid_first_character_of_identifier(peek()))
            while (peek() && is_valid_nonfirst_character_of_identifier(peek()))
                consume();

        auto directive = StringView(m_input.characters_without_null_termination() + token_start_index, m_index - token_start_index);
        if (directive == "#include") {
            commit_token(Token::Type::IncludeStatement);

            begin_token();
            while (isspace(peek()))
                consume();
            commit_token(Token::Type::Whitespace);

            begin_token();
            if (peek() == '<' || peek() == '"') {
                char closing = consume() == '<' ? '>' : '"';
                while (peek() && peek() != closing && peek() != '\n')
                    consume();

                if (peek() && consume() == '\n') {
                    return commit_token(Token::Type::IncludePath);
                }

                commit_token(Token::Type::IncludePath);
                begin_token();
            }
        }

        while (peek() && peek() != '\n')
            consume();

        return commit_token(Token::Type::PreprocessorStatement);
    }
    if (ch == '/' && peek(1) == '/') {
        begin_token();
        while (peek() && peek() != '\n')
            consume();
        return commit_token(Token::Type::Comment);
    }
    if (ch == '/' && peek(1) == '*') {
        begin_token();
        consume();
        consume();
        while (peek()) {
            if (peek() == '*' && peek(1) == '/') {
                consume();
                consume();
                return commit_token(Token::Type::Comment);
            }
            consume();
        }
        //TODO: error
        return commit_token(Token::Type::Unknown);
    }
    if (ch == '/') {
        return emit_token_equals(Token::Type::Slash, Token::Type::SlashEquals);
    }
    if (size_t prefix = match_string_prefix('"'); prefix > 0) {
        begin_token();
        for (size_t i = 0; i < prefix; ++i)
            consume();
        while (peek()) {
            if (peek() == '\\') {
                if (size_t escape = match_escape_sequence(); escape > 0) {
                    //TODO: see what to do. we should not discard that...
                    commit_token(Token::Type::DoubleQuotedString);
                    begin_token();
                    for (size_t i = 0; i < escape; ++i)
                        consume();
                    //TODO: see what to do. we should not discard that...
                    commit_token(Token::Type::EscapeSequence);
                    begin_token();
                    continue;
                }
            }

            if (consume() == '"')
                break;
        }
        return commit_token(Token::Type::DoubleQuotedString);
    }
    if (size_t prefix = match_string_prefix('R'); prefix > 0 && peek(prefix) == '"') {
        begin_token();
        for (size_t i = 0; i < prefix + 1; ++i)
            consume();
        size_t prefix_start = m_index;
        while (peek() && peek() != '(')
            consume();
        StringView prefix_string = m_input.substring_view(prefix_start, m_index - prefix_start);
        while (peek()) {
            if (consume() == '"') {
                ASSERT(m_index >= prefix_string.length() + 2);
                ASSERT(m_input[m_index - 1] == '"');
                if (m_input[m_index - 1 - prefix_string.length() - 1] == ')') {
                    StringView suffix_string = m_input.substring_view(m_index - 1 - prefix_string.length(), prefix_string.length());
                    if (prefix_string == suffix_string)
                        break;
                }
            }
        }
        return commit_token(Token::Type::RawString);
    }
    if (size_t prefix = match_string_prefix('\''); prefix > 0) {
        begin_token();
        for (size_t i = 0; i < prefix; ++i)
            consume();
        while (peek()) {
            if (peek() == '\\') {
                if (size_t escape = match_escape_sequence(); escape > 0) {
                    //TODO: see what to do we discard that...
                    commit_token(Token::Type::SingleQuotedString);
                    begin_token();
                    for (size_t i = 0; i < escape; ++i)
                        consume();
                    //TODO: see what to do we discard that...
                    commit_token(Token::Type::EscapeSequence);
                    begin_token();
                    continue;
                }
            }

            if (consume() == '\'')
                break;
        }
        return commit_token(Token::Type::SingleQuotedString);
    }
    if (isdigit(ch) || (ch == '.' && isdigit(peek(1)))) {
        begin_token();
        consume();

        auto type = ch == '.' ? Token::Type::Float : Token::Type::Integer;
        bool is_hex = false;
        bool is_binary = false;

        auto match_exponent = [&]() -> size_t {
            char ch = peek();
            if (ch != 'e' && ch != 'E' && ch != 'p' && ch != 'P')
                return 0;

            type = Token::Type::Float;
            size_t length = 1;
            ch = peek(length);
            if (ch == '+' || ch == '-') {
                ++length;
            }
            for (ch = peek(length); isdigit(ch); ch = peek(length)) {
                ++length;
            }
            return length;
        };

        auto match_type_literal = [&]() -> size_t {
            size_t length = 0;
            for (;;) {
                char ch = peek(length);
                if ((ch == 'u' || ch == 'U') && type == Token::Type::Integer) {
                    ++length;
                } else if ((ch == 'f' || ch == 'F') && !is_binary) {
                    type = Token::Type::Float;
                    ++length;
                } else if (ch == 'l' || ch == 'L') {
                    ++length;
                } else
                    return length;
            }
        };

        if (peek() == 'b' || peek() == 'B') {
            consume();
            is_binary = true;
            for (char ch = peek(); ch == '0' || ch == '1' || (ch == '\'' && peek(1) != '\''); ch = peek()) {
                consume();
            }
        } else {
            if (peek() == 'x' || peek() == 'X') {
                consume();
                is_hex = true;
            }

            for (char ch = peek(); (is_hex ? isxdigit(ch) : isdigit(ch)) || (ch == '\'' && peek(1) != '\'') || ch == '.'; ch = peek()) {
                if (ch == '.') {
                    if (type == Token::Type::Integer) {
                        type = Token::Type::Float;
                    } else
                        break;
                };
                consume();
            }
        }

        if (!is_binary) {
            size_t length = match_exponent();
            for (size_t i = 0; i < length; ++i)
                consume();
        }

        size_t length = match_type_literal();
        for (size_t i = 0; i < length; ++i)
            consume();

        return commit_token(type);
    }
    if (is_valid_first_character_of_identifier(ch)) {
        begin_token();
        while (peek() && is_valid_nonfirst_character_of_identifier(peek()))
            consume();
        auto token_view = StringView(m_input.characters_without_null_termination() + token_start_index, m_index - token_start_index);
        if (auto known_keyword = s_known_keywords.get(token_view); known_keyword.has_value()) {
            auto token = commit_token(Token::Type::Keyword);

            token.m_known_keyword = known_keyword.value();
            return token;
        } else if (auto known_type = s_known_types.get(token_view); known_type.has_value()) {
            auto token = commit_token(Token::Type::KnownType);
            token.m_known_type = known_type.value();
            return token;
        } else {
            auto token = commit_token(Token::Type::Identifier);
            token.m_identifier = token_view;
            return token;
        }
    }
    dbg() << "Unimplemented token character: " << ch;
    return emit_token(Token::Type::Unknown);
}

Vector<Token> Lexer::lex()
{
    Vector<Token> tokens;

    while (m_index < m_input.length())
        tokens.append(lex_one_token());
    return tokens;
}

}
