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

constexpr const char* s_known_keywords[] = {
    "alignas",
    "alignof",
    "and",
    "and_eq",
    "asm",
    "bitand",
    "bitor",
    "bool",
    "break",
    "case",
    "catch",
    "class",
    "compl",
    "const",
    "const_cast",
    "constexpr",
    "continue",
    "decltype",
    "default",
    "delete",
    "do",
    "dynamic_cast",
    "else",
    "enum",
    "explicit",
    "export",
    "extern",
    "false",
    "final",
    "for",
    "friend",
    "goto",
    "if",
    "inline",
    "mutable",
    "namespace",
    "new",
    "noexcept",
    "not",
    "not_eq",
    "nullptr",
    "operator",
    "or",
    "or_eq",
    "override",
    "private",
    "protected",
    "public",
    "register",
    "reinterpret_cast",
    "return",
    "signed",
    "sizeof",
    "static",
    "static_assert",
    "static_cast",
    "struct",
    "switch",
    "template",
    "this",
    "thread_local",
    "throw",
    "true",
    "try",
    "typedef",
    "typeid",
    "typename",
    "union",
    "using",
    "virtual",
    "volatile",
    "while",
    "xor",
    "xor_eq"
};

constexpr const char* s_known_types[] = {
    "ByteBuffer",
    "CircularDeque",
    "CircularQueue",
    "Deque",
    "DoublyLinkedList",
    "FileSystemPath",
    "Array",
    "Function",
    "HashMap",
    "HashTable",
    "IPv4Address",
    "InlineLinkedList",
    "IntrusiveList",
    "JsonArray",
    "JsonObject",
    "JsonValue",
    "MappedFile",
    "NetworkOrdered",
    "NonnullOwnPtr",
    "NonnullOwnPtrVector",
    "NonnullRefPtr",
    "NonnullRefPtrVector",
    "Optional",
    "OwnPtr",
    "RefPtr",
    "Result",
    "ScopeGuard",
    "SinglyLinkedList",
    "String",
    "StringBuilder",
    "StringImpl",
    "StringView",
    "Utf8View",
    "Vector",
    "WeakPtr",
    "auto",
    "char",
    "char16_t",
    "char32_t",
    "char8_t",
    "double",
    "float",
    "i16",
    "i32",
    "i64",
    "i8",
    "int",
    "int",
    "long",
    "short",
    "signed",
    "u16",
    "u32",
    "u64",
    "u8",
    "unsigned",
    "void",
    "wchar_t"
};

static bool is_keyword(const StringView& string)
{
    static HashTable<String> keywords(array_size(s_known_keywords));
    if (keywords.is_empty()) {
        keywords.set_from(s_known_keywords);
    }
    return keywords.contains(string);
}

static bool is_known_type(const StringView& string)
{
    static HashTable<String> types(array_size(s_known_types));
    if (types.is_empty()) {
        types.set_from(s_known_types);
    }
    return types.contains(string);
}

Vector<Token> Lexer::lex()
{
    Vector<Token> tokens;

    size_t token_start_index = 0;
    Position token_start_position;

    auto emit_token = [&](auto type) {
        Token token;
        token.m_type = type;
        token.m_start = m_position;
        token.m_end = m_position;
        tokens.append(token);
        consume();
    };

    auto begin_token = [&] {
        token_start_index = m_index;
        token_start_position = m_position;
    };
    auto commit_token = [&](auto type) {
        Token token;
        token.m_type = type;
        token.m_start = token_start_position;
        token.m_end = m_previous_position;
        tokens.append(token);
    };

    auto emit_token_equals = [&](auto type, auto equals_type) {
        if (peek(1) == '=') {
            begin_token();
            consume();
            consume();
            commit_token(equals_type);
            return;
        }
        emit_token(type);
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

    while (m_index < m_input.length()) {
        auto ch = peek();
        if (isspace(ch)) {
            begin_token();
            while (isspace(peek()))
                consume();
            commit_token(Token::Type::Whitespace);
            continue;
        }
        if (ch == '(') {
            emit_token(Token::Type::LeftParen);
            continue;
        }
        if (ch == ')') {
            emit_token(Token::Type::RightParen);
            continue;
        }
        if (ch == '{') {
            emit_token(Token::Type::LeftCurly);
            continue;
        }
        if (ch == '}') {
            emit_token(Token::Type::RightCurly);
            continue;
        }
        if (ch == '[') {
            emit_token(Token::Type::LeftBracket);
            continue;
        }
        if (ch == ']') {
            emit_token(Token::Type::RightBracket);
            continue;
        }
        if (ch == '<') {
            begin_token();
            consume();
            if (peek() == '<') {
                consume();
                if (peek() == '=') {
                    consume();
                    commit_token(Token::Type::LessLessEquals);
                    continue;
                }
                commit_token(Token::Type::LessLess);
                continue;
            }
            if (peek() == '=') {
                consume();
                commit_token(Token::Type::LessEquals);
                continue;
            }
            if (peek() == '>') {
                consume();
                commit_token(Token::Type::LessGreater);
                continue;
            }
            commit_token(Token::Type::Less);
            continue;
        }
        if (ch == '>') {
            begin_token();
            consume();
            if (peek() == '>') {
                consume();
                if (peek() == '=') {
                    consume();
                    commit_token(Token::Type::GreaterGreaterEquals);
                    continue;
                }
                commit_token(Token::Type::GreaterGreater);
                continue;
            }
            if (peek() == '=') {
                consume();
                commit_token(Token::Type::GreaterEquals);
                continue;
            }
            commit_token(Token::Type::Greater);
            continue;
        }
        if (ch == ',') {
            emit_token(Token::Type::Comma);
            continue;
        }
        if (ch == '+') {
            begin_token();
            consume();
            if (peek() == '+') {
                consume();
                commit_token(Token::Type::PlusPlus);
                continue;
            }
            if (peek() == '=') {
                consume();
                commit_token(Token::Type::PlusEquals);
                continue;
            }
            commit_token(Token::Type::Plus);
            continue;
        }
        if (ch == '-') {
            begin_token();
            consume();
            if (peek() == '-') {
                consume();
                commit_token(Token::Type::MinusMinus);
                continue;
            }
            if (peek() == '=') {
                consume();
                commit_token(Token::Type::MinusEquals);
                continue;
            }
            if (peek() == '>') {
                consume();
                if (peek() == '*') {
                    consume();
                    commit_token(Token::Type::ArrowAsterisk);
                    continue;
                }
                commit_token(Token::Type::Arrow);
                continue;
            }
            commit_token(Token::Type::Minus);
            continue;
        }
        if (ch == '*') {
            emit_token_equals(Token::Type::Asterisk, Token::Type::AsteriskEquals);
            continue;
        }
        if (ch == '%') {
            emit_token_equals(Token::Type::Percent, Token::Type::PercentEquals);
            continue;
        }
        if (ch == '^') {
            emit_token_equals(Token::Type::Caret, Token::Type::CaretEquals);
            continue;
        }
        if (ch == '!') {
            emit_token_equals(Token::Type::ExclamationMark, Token::Type::ExclamationMarkEquals);
            continue;
        }
        if (ch == '=') {
            emit_token_equals(Token::Type::Equals, Token::Type::EqualsEquals);
            continue;
        }
        if (ch == '&') {
            begin_token();
            consume();
            if (peek() == '&') {
                consume();
                commit_token(Token::Type::AndAnd);
                continue;
            }
            if (peek() == '=') {
                consume();
                commit_token(Token::Type::AndEquals);
                continue;
            }
            commit_token(Token::Type::And);
            continue;
        }
        if (ch == '|') {
            begin_token();
            consume();
            if (peek() == '|') {
                consume();
                commit_token(Token::Type::PipePipe);
                continue;
            }
            if (peek() == '=') {
                consume();
                commit_token(Token::Type::PipeEquals);
                continue;
            }
            commit_token(Token::Type::Pipe);
            continue;
        }
        if (ch == '~') {
            emit_token(Token::Type::Tilde);
            continue;
        }
        if (ch == '?') {
            emit_token(Token::Type::QuestionMark);
            continue;
        }
        if (ch == ':') {
            begin_token();
            consume();
            if (peek() == ':') {
                consume();
                if (peek() == '*') {
                    consume();
                    commit_token(Token::Type::ColonColonAsterisk);
                    continue;
                }
                commit_token(Token::Type::ColonColon);
                continue;
            }
            commit_token(Token::Type::Colon);
            continue;
        }
        if (ch == ';') {
            emit_token(Token::Type::Semicolon);
            continue;
        }
        if (ch == '.') {
            begin_token();
            consume();
            if (peek() == '*') {
                consume();
                commit_token(Token::Type::DotAsterisk);
                continue;
            }
            commit_token(Token::Type::Dot);
            continue;
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
                        commit_token(Token::Type::IncludePath);
                        continue;
                    }

                    commit_token(Token::Type::IncludePath);
                    begin_token();
                }
            }

            while (peek() && peek() != '\n')
                consume();

            commit_token(Token::Type::PreprocessorStatement);
            continue;
        }
        if (ch == '/' && peek(1) == '/') {
            begin_token();
            while (peek() && peek() != '\n')
                consume();
            commit_token(Token::Type::Comment);
            continue;
        }
        if (ch == '/' && peek(1) == '*') {
            begin_token();
            consume();
            consume();
            bool comment_block_ends = false;
            while (peek()) {
                if (peek() == '*' && peek(1) == '/') {
                    comment_block_ends = true;
                    break;
                }

                consume();
            }

            if (comment_block_ends) {
                consume();
                consume();
            }

            commit_token(Token::Type::Comment);
            continue;
        }
        if (ch == '/') {
            emit_token_equals(Token::Type::Slash, Token::Type::SlashEquals);
            continue;
        }
        if (size_t prefix = match_string_prefix('"'); prefix > 0) {
            begin_token();
            for (size_t i = 0; i < prefix; ++i)
                consume();
            while (peek()) {
                if (peek() == '\\') {
                    if (size_t escape = match_escape_sequence(); escape > 0) {
                        commit_token(Token::Type::DoubleQuotedString);
                        begin_token();
                        for (size_t i = 0; i < escape; ++i)
                            consume();
                        commit_token(Token::Type::EscapeSequence);
                        begin_token();
                        continue;
                    }
                }

                if (consume() == '"')
                    break;
            }
            commit_token(Token::Type::DoubleQuotedString);
            continue;
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
            commit_token(Token::Type::RawString);
            continue;
        }
        if (size_t prefix = match_string_prefix('\''); prefix > 0) {
            begin_token();
            for (size_t i = 0; i < prefix; ++i)
                consume();
            while (peek()) {
                if (peek() == '\\') {
                    if (size_t escape = match_escape_sequence(); escape > 0) {
                        commit_token(Token::Type::SingleQuotedString);
                        begin_token();
                        for (size_t i = 0; i < escape; ++i)
                            consume();
                        commit_token(Token::Type::EscapeSequence);
                        begin_token();
                        continue;
                    }
                }

                if (consume() == '\'')
                    break;
            }
            commit_token(Token::Type::SingleQuotedString);
            continue;
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

            commit_token(type);
            continue;
        }
        if (is_valid_first_character_of_identifier(ch)) {
            begin_token();
            while (peek() && is_valid_nonfirst_character_of_identifier(peek()))
                consume();
            auto token_view = StringView(m_input.characters_without_null_termination() + token_start_index, m_index - token_start_index);
            if (is_keyword(token_view))
                commit_token(Token::Type::Keyword);
            else if (is_known_type(token_view))
                commit_token(Token::Type::KnownType);
            else
                commit_token(Token::Type::Identifier);
            continue;
        }
        dbg() << "Unimplemented token character: " << ch;
        emit_token(Token::Type::Unknown);
    }
    return tokens;
}

}
