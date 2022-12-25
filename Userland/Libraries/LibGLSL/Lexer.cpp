/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Melody Goad <mszoopers@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Lexer.h"
#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/GenericLexer.h>
#include <AK/HashMap.h>
#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>
#include <stdio.h>

namespace GLSL {

HashMap<FlyString, TokenType> Lexer::s_keywords;
HashMap<DeprecatedString, TokenType> Lexer::s_three_char_tokens;
HashMap<DeprecatedString, TokenType> Lexer::s_two_char_tokens;
HashMap<char, TokenType> Lexer::s_single_char_tokens;

Lexer::Lexer(StringView source, size_t line_number, size_t line_column)
    : m_source(source)
    , m_current_token(TokenType::Eof, {}, {}, {}, 0, 0, 0)
    , m_line_number(line_number)
    , m_line_column(line_column)
    , m_parsed_identifiers(adopt_ref(*new ParsedIdentifiers))
{
    if (s_keywords.is_empty()) {
        s_keywords.set("asm", TokenType::Asm);
        s_keywords.set("attribute", TokenType::Attribute);
        s_keywords.set("break", TokenType::Break);
        s_keywords.set("bvec2", TokenType::Bvec2);
        s_keywords.set("bvec3", TokenType::Bvec3);
        s_keywords.set("bvec4", TokenType::Bvec4);
        s_keywords.set("cast", TokenType::Cast);
        s_keywords.set("centroid", TokenType::Centroid);
        s_keywords.set("class", TokenType::Class);
        s_keywords.set("const", TokenType::Const);
        s_keywords.set("continue", TokenType::Continue);
        s_keywords.set("default", TokenType::Default);
        s_keywords.set("discard", TokenType::Discard);
        s_keywords.set("do", TokenType::Do);
        s_keywords.set("double", TokenType::Double);
        s_keywords.set("dvec2", TokenType::Dvec2);
        s_keywords.set("dvec3", TokenType::Dvec3);
        s_keywords.set("dvec4", TokenType::Dvec4);
        s_keywords.set("else", TokenType::Else);
        s_keywords.set("enum", TokenType::Enum);
        s_keywords.set("extern", TokenType::Extern);
        s_keywords.set("external", TokenType::External);
        s_keywords.set("false", TokenType::BoolLiteral);
        s_keywords.set("float", TokenType::Float);
        s_keywords.set("fixed", TokenType::Fixed);
        s_keywords.set("for", TokenType::For);
        s_keywords.set("fvec2", TokenType::Fvec2);
        s_keywords.set("fvec3", TokenType::Fvec3);
        s_keywords.set("fvec4", TokenType::Fvec4);
        s_keywords.set("goto", TokenType::Goto);
        s_keywords.set("half", TokenType::Half);
        s_keywords.set("highp", TokenType::Highp);
        s_keywords.set("hvec2", TokenType::Hvec2);
        s_keywords.set("hvec3", TokenType::Hvec3);
        s_keywords.set("hvec4", TokenType::Hvec4);
        s_keywords.set("if", TokenType::If);
        s_keywords.set("in", TokenType::In);
        s_keywords.set("inline", TokenType::Inline);
        s_keywords.set("inout", TokenType::Inout);
        s_keywords.set("input", TokenType::Input);
        s_keywords.set("int", TokenType::Int);
        s_keywords.set("interface", TokenType::Interface);
        s_keywords.set("long", TokenType::Long);
        s_keywords.set("mat2", TokenType::Mat2);
        s_keywords.set("mat2x2", TokenType::Mat2x2);
        s_keywords.set("mat2x3", TokenType::Mat2x3);
        s_keywords.set("mat2x4", TokenType::Mat2x4);
        s_keywords.set("mat3", TokenType::Mat3);
        s_keywords.set("mat3x2", TokenType::Mat3x2);
        s_keywords.set("mat3x3", TokenType::Mat3x3);
        s_keywords.set("mat3x4", TokenType::Mat3x4);
        s_keywords.set("mat4", TokenType::Mat4);
        s_keywords.set("mat4x2", TokenType::Mat4x2);
        s_keywords.set("mat4x3", TokenType::Mat4x3);
        s_keywords.set("matx4", TokenType::Mat4x4);
        s_keywords.set("mediump", TokenType::Mediump);
        s_keywords.set("namespace", TokenType::Namespace);
        s_keywords.set("noinline", TokenType::Noinline);
        s_keywords.set("out", TokenType::Out);
        s_keywords.set("output", TokenType::Output);
        s_keywords.set("packed", TokenType::Packed);
        s_keywords.set("precision", TokenType::Precision);
        s_keywords.set("public", TokenType::Public);
        s_keywords.set("return", TokenType::Return);
        s_keywords.set("sampler1D", TokenType::Sampler1D);
        s_keywords.set("sampler1DShadow", TokenType::Sampler1DShadow);
        s_keywords.set("sampler2D", TokenType::Sampler2D);
        s_keywords.set("sampler2DRect", TokenType::Sampler2DRect);
        s_keywords.set("sampler2DRectShadow", TokenType::Sampler2DRectShadow);
        s_keywords.set("sampler2DShadow", TokenType::Sampler2DShadow);
        s_keywords.set("sampler3D", TokenType::Sampler3D);
        s_keywords.set("sampler3DRect", TokenType::Sampler3DRect);
        s_keywords.set("samplerCube", TokenType::SamplerCube);
        s_keywords.set("short", TokenType::Short);
        s_keywords.set("static", TokenType::Static);
        s_keywords.set("struct", TokenType::Struct);
        s_keywords.set("switch", TokenType::Switch);
        s_keywords.set("template", TokenType::Template);
        s_keywords.set("this", TokenType::This);
        s_keywords.set("typedef", TokenType::Typedef);
        s_keywords.set("true", TokenType::BoolLiteral);
        s_keywords.set("uniform", TokenType::Uniform);
        s_keywords.set("union", TokenType::Union);
        s_keywords.set("unsigned", TokenType::Unsigned);
        s_keywords.set("varying", TokenType::Varying);
        s_keywords.set("vec2", TokenType::Vec2);
        s_keywords.set("vec3", TokenType::Vec3);
        s_keywords.set("vec4", TokenType::Vec4);
        s_keywords.set("void", TokenType::Void);
        s_keywords.set("volatile", TokenType::Volatile);
        s_keywords.set("while", TokenType::While);
    }

    if (s_three_char_tokens.is_empty()) {
        s_three_char_tokens.set("<<=", TokenType::ShiftLeftEquals);
        s_three_char_tokens.set(">>=", TokenType::ShiftRightEquals);
    }

    if (s_two_char_tokens.is_empty()) {
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
        s_two_char_tokens.set("==", TokenType::EqualsEquals);
        s_two_char_tokens.set("<=", TokenType::LessThanEquals);
        s_two_char_tokens.set(">=", TokenType::GreaterThanEquals);
        s_two_char_tokens.set("!=", TokenType::ExclamationMarkEquals);
        s_two_char_tokens.set("--", TokenType::MinusMinus);
        s_two_char_tokens.set("++", TokenType::PlusPlus);
        s_two_char_tokens.set("<<", TokenType::ShiftLeft);
        s_two_char_tokens.set(">>", TokenType::ShiftRight);
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
        if (m_position < m_source.length())
            return false;
        m_eof = true;
        m_current_char = '\0';
        m_position = m_source.length() + 1;
        m_line_column++;
        return true;
    };

    if (m_position > m_source.length())
        return;

    if (did_reach_eof())
        return;

    if (is_line_terminator()) {
        if constexpr (LEXER_DEBUG) {
            DeprecatedString type;
            if (m_current_char == '\n')
                type = "LINE FEED";
            else if (m_current_char == '\r')
                type = "CARRIAGE RETURN";
            dbgln("Found a line terminator: {}", type);
        }

        // If the previous character is \r and the current one \n we already updated line number
        // and column - don't do it again. From https://tc39.es/ecma262/#sec-line-terminators:
        //   The sequence <CR><LF> is commonly used as a line terminator.
        //   It should be considered a single SourceCharacter for the purpose of reporting line numbers.
        auto second_char_of_crlf = m_position > 1 && m_source[m_position - 2] == '\r' && m_current_char == '\n';

        if (!second_char_of_crlf) {
            m_line_number++;
            m_line_column = 1;
            dbgln_if(LEXER_DEBUG, "Incremented line number, now at: line {}, column 1", m_line_number);
        } else {
            dbgln_if(LEXER_DEBUG, "Previous was CR, this is LF - not incrementing line number again.");
        }
    } else if (is_ascii(m_current_char)) {
        m_position++;

        if (did_reach_eof())
            return;

        m_line_column++;
    } else {
        m_line_column++;
    }

    m_current_char = m_source[m_position++];
}

bool Lexer::consume_decimal_number()
{
    if (!is_ascii_digit(m_current_char))
        return false;

    while (is_ascii_digit(m_current_char) || match_numeric_literal_separator_followed_by(is_ascii_digit)) {
        consume();
    }
    return true;
}

bool Lexer::consume_exponent()
{
    consume();
    if (m_current_char == '-' || m_current_char == '+')
        consume();

    if (!is_ascii_digit(m_current_char))
        return false;

    return consume_decimal_number();
}

static constexpr bool is_octal_digit(char ch)
{
    return ch >= '0' && ch <= '7';
}

bool Lexer::consume_octal_number()
{
    consume();
    if (!is_octal_digit(m_current_char))
        return false;

    while (is_octal_digit(m_current_char) || match_numeric_literal_separator_followed_by(is_octal_digit))
        consume();

    return true;
}

bool Lexer::consume_hexadecimal_number()
{
    consume();
    if (!is_ascii_hex_digit(m_current_char))
        return false;

    while (is_ascii_hex_digit(m_current_char) || match_numeric_literal_separator_followed_by(is_ascii_hex_digit))
        consume();

    return true;
}

static constexpr bool is_binary_digit(char ch)
{
    return ch == '0' || ch == '1';
}

bool Lexer::consume_binary_number()
{
    consume();
    if (!is_binary_digit(m_current_char))
        return false;

    while (is_binary_digit(m_current_char) || match_numeric_literal_separator_followed_by(is_binary_digit))
        consume();

    return true;
}

template<typename Callback>
bool Lexer::match_numeric_literal_separator_followed_by(Callback callback) const
{
    if (m_position >= m_source.length())
        return false;
    return m_current_char == '_'
        && callback(m_source[m_position]);
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
    return m_eof;
}

ALWAYS_INLINE bool Lexer::is_line_terminator() const
{
    if (m_current_char == '\n' || m_current_char == '\r')
        return true;
    return false;
}

ALWAYS_INLINE u32 Lexer::current_code_point() const
{
    static constexpr const u32 REPLACEMENT_CHARACTER = 0xFFFD;
    if (m_position == 0)
        return REPLACEMENT_CHARACTER;
    auto substring = m_source.substring_view(m_position - 1);
    if (substring.is_empty())
        return REPLACEMENT_CHARACTER;
    if (is_ascii(substring[0]))
        return substring[0];
    Utf8View utf_8_view { substring };
    return *utf_8_view.begin();
}

bool Lexer::is_whitespace() const
{
    if (is_ascii_space(m_current_char))
        return true;
    return false;
}

Optional<u32> Lexer::is_identifier_start(size_t& identifier_length) const
{
    u32 code_point = current_code_point();
    identifier_length = 1;

    if (is_ascii_alpha(code_point) || code_point == '_')
        return code_point;

    return {};
}

Optional<u32> Lexer::is_identifier_middle(size_t& identifier_length) const
{
    u32 code_point = current_code_point();
    identifier_length = 1;

    if (is_ascii_alphanumeric(code_point))
        return code_point;

    return {};
}

bool Lexer::is_line_comment_start() const
{
    return match('/', '/');
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
    return is_ascii_digit(m_current_char) || (m_current_char == '.' && m_position < m_source.length() && is_ascii_digit(m_source[m_position]));
}

bool Lexer::slash_means_division() const
{
    auto type = m_current_token.type();
    return type == TokenType::BoolLiteral
        || type == TokenType::BracketClose
        || type == TokenType::CurlyClose
        || type == TokenType::Identifier
        || type == TokenType::MinusMinus
        || type == TokenType::NumericLiteral
        || type == TokenType::ParenClose
        || type == TokenType::PlusPlus
        || type == TokenType::This;
}

Token Lexer::next()
{
    size_t trivia_start = m_position;
    bool unterminated_comment = false;
    // consume whitespace and comments
    while (true) {
        if (is_line_terminator()) {
            do {
                consume();
            } while (is_line_terminator());
        } else if (is_whitespace()) {
            do {
                consume();
            } while (is_whitespace());
        } else if (is_line_comment_start()) {
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

    size_t value_start = m_position;
    size_t value_start_line_number = m_line_number;
    size_t value_start_column_number = m_line_column;
    auto token_type = TokenType::Invalid;
    // This is being used to communicate info about invalid tokens to the parser, which then
    // can turn that into more specific error messages - instead of us having to make up a
    // bunch of Invalid* tokens (bad numeric literals, unterminated comments etc.)
    DeprecatedString token_message;

    Optional<FlyString> identifier;
    size_t identifier_length = 0;

    if (m_current_char == '#') {
        // FIXME: Implement the GLSL preprocessor lexing.
        consume();
    } else if (auto code_point = is_identifier_start(identifier_length); code_point.has_value()) {
        // identifier or keyword
        StringBuilder builder;
        do {
            builder.append_code_point(*code_point);
            for (size_t i = 0; i < identifier_length; ++i)
                consume();

            code_point = is_identifier_middle(identifier_length);
        } while (code_point.has_value());

        identifier = builder.string_view();
        m_parsed_identifiers->identifiers.set(*identifier);

        auto it = s_keywords.find(identifier->hash(), [&](auto& entry) { return entry.key == identifier; });
        if (it == s_keywords.end())
            token_type = TokenType::Identifier;
        else
            token_type = it->value;
    } else if (is_numeric_literal_start()) {
        token_type = TokenType::NumericLiteral;
        bool is_invalid_numeric_literal = false;
        if (m_current_char == '0') {
            consume();
            if (m_current_char == '.') {
                // decimal
                consume();
                while (is_ascii_digit(m_current_char))
                    consume();
                if (m_current_char == 'e' || m_current_char == 'E')
                    is_invalid_numeric_literal = !consume_exponent();
            } else if (m_current_char == 'e' || m_current_char == 'E') {
                is_invalid_numeric_literal = !consume_exponent();
            } else if (m_current_char == 'o' || m_current_char == 'O') {
                // octal
                is_invalid_numeric_literal = !consume_octal_number();
                if (m_current_char == 'n') {
                    consume();
                    token_type = TokenType::NumericLiteral;
                }
            } else if (m_current_char == 'b' || m_current_char == 'B') {
                // binary
                is_invalid_numeric_literal = !consume_binary_number();
                if (m_current_char == 'n') {
                    consume();
                    token_type = TokenType::NumericLiteral;
                }
            } else if (m_current_char == 'x' || m_current_char == 'X') {
                // hexadecimal
                is_invalid_numeric_literal = !consume_hexadecimal_number();
                if (m_current_char == 'n') {
                    consume();
                    token_type = TokenType::NumericLiteral;
                }
            } else if (m_current_char == 'n') {
                consume();
                token_type = TokenType::NumericLiteral;
            } else if (is_ascii_digit(m_current_char)) {
                // octal without '0o' prefix. Forbidden in 'strict mode'
                do {
                    consume();
                } while (is_ascii_digit(m_current_char));
            }
        } else {
            // 1...9 or period
            while (is_ascii_digit(m_current_char) || match_numeric_literal_separator_followed_by(is_ascii_digit))
                consume();
            if (m_current_char == 'n') {
                consume();
                token_type = TokenType::NumericLiteral;
            } else {
                if (m_current_char == '.') {
                    consume();
                    if (m_current_char == '_')
                        is_invalid_numeric_literal = true;

                    while (is_ascii_digit(m_current_char) || match_numeric_literal_separator_followed_by(is_ascii_digit)) {
                        consume();
                    }
                }
                if (m_current_char == 'e' || m_current_char == 'E')
                    is_invalid_numeric_literal = is_invalid_numeric_literal || !consume_exponent();
            }
        }
        if (is_invalid_numeric_literal) {
            token_type = TokenType::Invalid;
            token_message = "Invalid numeric literal";
        }
    } else if (m_eof) {
        if (unterminated_comment) {
            token_type = TokenType::Invalid;
            token_message = "Unterminated multi-line comment";
        } else {
            token_type = TokenType::Eof;
        }
    } else {
        bool found_three_char_token = false;
        if (m_position + 1 < m_source.length()) {
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
        if (!found_three_char_token && m_position < m_source.length()) {
            auto two_chars_view = m_source.substring_view(m_position - 1, 2);
            auto it = s_two_char_tokens.find(two_chars_view.hash(), [&](auto& entry) { return entry.key == two_chars_view; });
            if (it != s_two_char_tokens.end()) {
                // OptionalChainingPunctuator :: ?. [lookahead ∉ DecimalDigit]
                found_two_char_token = true;
                consume();
                consume();
                token_type = it->value;
            }
        }

        bool found_one_char_token = false;
        if (!found_three_char_token && !found_two_char_token) {
            auto it = s_single_char_tokens.find(m_current_char);
            if (it != s_single_char_tokens.end()) {
                found_one_char_token = true;
                consume();
                token_type = it->value;
            }
        }

        if (!found_three_char_token && !found_two_char_token && !found_one_char_token) {
            consume();
            token_type = TokenType::Invalid;
        }
    }

    m_current_token = Token(
        token_type,
        token_message,
        m_source.substring_view(trivia_start - 1, value_start - trivia_start),
        m_source.substring_view(value_start - 1, m_position - value_start),
        value_start_line_number,
        value_start_column_number,
        value_start - 1);

    if (identifier.has_value())
        m_current_token.set_identifier_value(identifier.release_value());

    if constexpr (LEXER_DEBUG) {
        dbgln("------------------------------");
        dbgln("Token: {}", m_current_token.name());
        dbgln("Trivia: _{}_", m_current_token.trivia());
        dbgln("Value: _{}_", m_current_token.value());
        dbgln("Line: {}, Column: {}", m_current_token.line_number(), m_current_token.line_column());
        dbgln("------------------------------");
    }

    return m_current_token;
}
}
