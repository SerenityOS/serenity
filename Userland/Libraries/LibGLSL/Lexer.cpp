/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Volodymyr V. <vvmposeydon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Lexer.h"
#include <AK/CharacterTypes.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>

namespace GLSL {

Lexer::Lexer(StringView input, size_t start_line)
    : m_input(input)
    , m_previous_position { start_line, 0 }
    , m_position { start_line, 0 }
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
    VERIFY(m_index < m_input.length());
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

constexpr bool is_valid_first_character_of_identifier(char ch)
{
    return is_ascii_alpha(ch) || ch == '_' || ch == '$';
}

constexpr bool is_valid_nonfirst_character_of_identifier(char ch)
{
    return is_valid_first_character_of_identifier(ch) || is_ascii_digit(ch);
}

// NOTE: some of these keywords are not used at the moment, however they are reserved for future use and should not be used as identifiers
constexpr Array<StringView, 66> s_known_keywords = {
    "asm"sv,
    "attribute"sv,
    "break"sv,
    "case"sv,
    "cast"sv,
    "centroid"sv,
    "class"sv,
    "common"
    "partition"sv,
    "active"sv,
    "const"sv,
    "continue"sv,
    "default"sv,
    "discard"sv,
    "do"sv,
    "else"sv,
    "enum"sv,
    "extern"sv,
    "external"sv,
    "false"sv,
    "filter"sv,
    "fixed"sv,
    "flat"sv,
    "for"sv,
    "goto"sv,
    "half"sv,
    "highp"sv,
    "if"sv,
    "in"sv,
    "inline"sv,
    "inout"sv,
    "input"sv,
    "interface"sv,
    "invariant"sv,
    "layout"sv,
    "lowp"sv,
    "mediump"sv,
    "namespace"sv,
    "noinline"sv,
    "noperspective"sv,
    "out"sv,
    "output"sv,
    "packed"sv,
    "patch"sv,
    "precision"sv,
    "public"sv,
    "return"sv,
    "row_major"sv,
    "sample"sv,
    "sizeof"sv,
    "smooth"sv,
    "static"sv,
    "struct"sv,
    "subroutine"sv,
    "superp"sv,
    "switch"sv,
    "template"sv,
    "this"sv,
    "true"sv,
    "typedef"sv,
    "uniform"sv,
    "union"sv,
    "using"sv,
    "varying"sv,
    "volatile"sv,
    "while"sv,
};

constexpr Array<StringView, 120> s_known_types = {
    "bool"sv,
    "bvec2"sv,
    "bvec3"sv,
    "bvec4"sv,
    "dmat2"sv,
    "dmat2x2"sv,
    "dmat2x3"sv,
    "dmat2x4"sv,
    "dmat3"sv,
    "dmat3x2"sv,
    "dmat3x3"sv,
    "dmat3x4"sv,
    "dmat4"sv,
    "dmat4x2"sv,
    "dmat4x3"sv,
    "dmat4x4"sv,
    "double"sv,
    "dvec2"sv,
    "dvec3"sv,
    "dvec4"sv,
    "float"sv,
    "fvec2"sv,
    "fvec3"sv,
    "fvec4"sv,
    "hvec2"sv,
    "hvec3"sv,
    "hvec4"sv,
    "iimage1D"sv,
    "iimage1DArray"sv,
    "iimage2D"sv,
    "iimage2DArray"sv,
    "iimage3D"sv,
    "iimageBuffer"sv,
    "iimageCube"sv,
    "image1D"sv,
    "image1DArray"sv,
    "image1DArrayShadow"sv,
    "image1DShadow"sv,
    "image2D"sv,
    "image2DArray"sv,
    "image2DArrayShadow"sv,
    "image2DShadow"sv,
    "image3D"sv,
    "imageBuffer"sv,
    "imageCube"sv,
    "int"sv,
    "isampler1D"sv,
    "isampler1DArray"sv,
    "isampler2D"sv,
    "isampler2DArray"sv,
    "isampler2DMS"sv,
    "isampler2DMSArray"sv,
    "isampler2DRect"sv,
    "isampler3D"sv,
    "isamplerBuffer"sv,
    "isamplerCube"sv,
    "isamplerCubeArray"sv,
    "ivec2"sv,
    "ivec3"sv,
    "ivec4"sv,
    "long"sv,
    "mat2"sv,
    "mat2x2"sv,
    "mat2x3"sv,
    "mat2x4"sv,
    "mat3"sv,
    "mat3x2"sv,
    "mat3x3"sv,
    "mat3x4"sv,
    "mat4"sv,
    "mat4x2"sv,
    "mat4x3"sv,
    "mat4x4"sv,
    "sampler1D"sv,
    "sampler1DArray"sv,
    "sampler1DArrayShadow"sv,
    "sampler1DShadow"sv,
    "sampler2D"sv,
    "sampler2DArray"sv,
    "sampler2DArrayShadow"sv,
    "sampler2DMS"sv,
    "sampler2DMSArray"sv,
    "sampler2DRect"sv,
    "sampler2DRectShadow"sv,
    "sampler2DShadow"sv,
    "sampler3D"sv,
    "sampler3DRect"sv,
    "samplerBuffer"sv,
    "samplerCube"sv,
    "samplerCubeArray"sv,
    "samplerCubeArrayShadow"sv,
    "samplerCubeShadow"sv,
    "short"sv,
    "uimage1D"sv,
    "uimage1DArray"sv,
    "uimage2D"sv,
    "uimage2DArray"sv,
    "uimage3D"sv,
    "uimageBuffer"sv,
    "uimageCube"sv,
    "uint"sv,
    "unsigned"sv,
    "usampler1D"sv,
    "usampler1DArray"sv,
    "usampler2D"sv,
    "usampler2DArray"sv,
    "usampler2DMS"sv,
    "usampler2DMSArray"sv,
    "usampler2DRect"sv,
    "usampler3D"sv,
    "usamplerBuffer"sv,
    "usamplerCube"sv,
    "usamplerCubeArray"sv,
    "uvec2"sv,
    "uvec3"sv,
    "uvec4"sv,
    "vec2"sv,
    "vec3"sv,
    "vec4"sv,
    "void"sv,
};

static bool is_keyword(StringView string)
{
    return AK::find(s_known_keywords.begin(), s_known_keywords.end(), string) != s_known_keywords.end();
}

static bool is_known_type(StringView string)
{
    return AK::find(s_known_types.begin(), s_known_types.end(), string) != s_known_types.end();
}

void Lexer::lex_impl(Function<void(Token)> callback)
{
    size_t token_start_index = 0;
    Position token_start_position;

    auto emit_single_char_token = [&](auto type) {
        callback(Token(type, m_position, m_position, m_input.substring_view(m_index, 1)));
        consume();
    };

    auto begin_token = [&] {
        token_start_index = m_index;
        token_start_position = m_position;
    };
    auto commit_token = [&](auto type) {
        if (m_options.ignore_whitespace && type == Token::Type::Whitespace)
            return;
        callback(Token(type, token_start_position, m_previous_position, m_input.substring_view(token_start_index, m_index - token_start_index)));
    };

    auto emit_token_equals = [&](auto type, auto equals_type) {
        if (peek(1) == '=') {
            begin_token();
            consume();
            consume();
            commit_token(equals_type);
            return;
        }
        emit_single_char_token(type);
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
            while (is_ascii_hex_digit(peek(2 + hex_digits)))
                ++hex_digits;
            return 2 + hex_digits;
        }
        case 'u':
        case 'U': {
            bool is_unicode = true;
            size_t number_of_digits = peek(1) == 'u' ? 4 : 8;
            for (size_t i = 0; i < number_of_digits; ++i) {
                if (!is_ascii_hex_digit(peek(2 + i))) {
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
        if (is_ascii_space(ch)) {
            begin_token();
            while (is_ascii_space(peek()))
                consume();
            commit_token(Token::Type::Whitespace);
            continue;
        }
        if (ch == '(') {
            emit_single_char_token(Token::Type::LeftParen);
            continue;
        }
        if (ch == ')') {
            emit_single_char_token(Token::Type::RightParen);
            continue;
        }
        if (ch == '{') {
            emit_single_char_token(Token::Type::LeftCurly);
            continue;
        }
        if (ch == '}') {
            emit_single_char_token(Token::Type::RightCurly);
            continue;
        }
        if (ch == '[') {
            emit_single_char_token(Token::Type::LeftBracket);
            continue;
        }
        if (ch == ']') {
            emit_single_char_token(Token::Type::RightBracket);
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
            emit_single_char_token(Token::Type::Comma);
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
            begin_token();
            consume();
            if (peek() == '^') {
                consume();
                commit_token(Token::Type::CaretCaret);
                continue;
            }
            if (peek() == '=') {
                consume();
                commit_token(Token::Type::CaretEquals);
                continue;
            }
            commit_token(Token::Type::Caret);
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
            emit_single_char_token(Token::Type::Tilde);
            continue;
        }
        if (ch == '?') {
            emit_single_char_token(Token::Type::QuestionMark);
            continue;
        }
        if (ch == ':') {
            emit_single_char_token(Token::Type::Colon);
            continue;
        }
        if (ch == ';') {
            emit_single_char_token(Token::Type::Semicolon);
            continue;
        }
        if (ch == '.') {
            emit_single_char_token(Token::Type::Dot);
            continue;
        }
        if (ch == '#') {
            begin_token();
            consume();
            while (AK::is_ascii_space(peek()))
                consume();

            size_t directive_start = m_index;
            if (is_valid_first_character_of_identifier(peek()))
                while (peek() && is_valid_nonfirst_character_of_identifier(peek()))
                    consume();

            auto directive = StringView(m_input.characters_without_null_termination() + directive_start, m_index - directive_start);
            if (directive == "include"sv) {
                commit_token(Token::Type::IncludeStatement);

                if (is_ascii_space(peek())) {
                    begin_token();
                    do {
                        consume();
                    } while (is_ascii_space(peek()));
                    commit_token(Token::Type::Whitespace);
                }

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
            } else {
                while (peek()) {
                    if (peek() == '\\' && peek(1) == '\n') {
                        consume();
                        consume();
                    } else if (peek() == '\n') {
                        break;
                    } else {
                        consume();
                    }
                }

                commit_token(Token::Type::PreprocessorStatement);
            }

            continue;
        }
        if (ch == '/' && peek(1) == '/') {
            while (peek() && peek() != '\n')
                consume();
            continue;
        }
        if (ch == '/' && peek(1) == '*') {
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

                // If string is not terminated - stop before EOF
                if (!peek(1))
                    break;

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
                    VERIFY(m_index >= prefix_string.length() + 2);
                    VERIFY(m_input[m_index - 1] == '"');
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
        if (is_ascii_digit(ch) || (ch == '.' && is_ascii_digit(peek(1)))) {
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
                for (ch = peek(length); is_ascii_digit(ch); ch = peek(length)) {
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

                for (char ch = peek(); (is_hex ? is_ascii_hex_digit(ch) : is_ascii_digit(ch)) || (ch == '\'' && peek(1) != '\'') || ch == '.'; ch = peek()) {
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

        if (ch == '\\' && peek(1) == '\n') {
            consume();
            consume();
            continue;
        }

        dbgln("Unimplemented token character: {}", ch);
        emit_single_char_token(Token::Type::Unknown);
    }
}

Vector<Token> Lexer::lex()
{
    Vector<Token> tokens;
    lex_impl([&](auto token) {
        tokens.append(move(token));
    });
    return tokens;
}

}
