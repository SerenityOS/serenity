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

#include "CppLexer.h"
#include <AK/HashTable.h>
#include <AK/String.h>
#include <ctype.h>

namespace GUI {

CppLexer::CppLexer(const StringView& input)
    : m_input(input)
{
}

char CppLexer::peek(size_t offset) const
{
    if ((m_index + offset) >= m_input.length())
        return 0;
    return m_input[m_index + offset];
}

char CppLexer::consume()
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

static bool is_keyword(const StringView& string)
{
    static HashTable<String> keywords;
    if (keywords.is_empty()) {
        keywords.set("alignas");
        keywords.set("alignof");
        keywords.set("and");
        keywords.set("and_eq");
        keywords.set("asm");
        keywords.set("bitand");
        keywords.set("bitor");
        keywords.set("bool");
        keywords.set("break");
        keywords.set("case");
        keywords.set("catch");
        keywords.set("class");
        keywords.set("compl");
        keywords.set("const");
        keywords.set("const_cast");
        keywords.set("constexpr");
        keywords.set("continue");
        keywords.set("decltype");
        keywords.set("default");
        keywords.set("delete");
        keywords.set("do");
        keywords.set("dynamic_cast");
        keywords.set("else");
        keywords.set("enum");
        keywords.set("explicit");
        keywords.set("export");
        keywords.set("extern");
        keywords.set("false");
        keywords.set("final");
        keywords.set("for");
        keywords.set("friend");
        keywords.set("goto");
        keywords.set("if");
        keywords.set("inline");
        keywords.set("mutable");
        keywords.set("namespace");
        keywords.set("new");
        keywords.set("noexcept");
        keywords.set("not");
        keywords.set("not_eq");
        keywords.set("nullptr");
        keywords.set("operator");
        keywords.set("or");
        keywords.set("or_eq");
        keywords.set("override");
        keywords.set("private");
        keywords.set("protected");
        keywords.set("public");
        keywords.set("register");
        keywords.set("reinterpret_cast");
        keywords.set("return");
        keywords.set("signed");
        keywords.set("sizeof");
        keywords.set("static");
        keywords.set("static_assert");
        keywords.set("static_cast");
        keywords.set("struct");
        keywords.set("switch");
        keywords.set("template");
        keywords.set("this");
        keywords.set("thread_local");
        keywords.set("throw");
        keywords.set("true");
        keywords.set("try");
        keywords.set("typedef");
        keywords.set("typeid");
        keywords.set("typename");
        keywords.set("union");
        keywords.set("using");
        keywords.set("virtual");
        keywords.set("volatile");
        keywords.set("while");
        keywords.set("xor");
        keywords.set("xor_eq");
    }
    return keywords.contains(string);
}

static bool is_known_type(const StringView& string)
{
    static HashTable<String> types;
    if (types.is_empty()) {
        types.set("ByteBuffer");
        types.set("CircularDeque");
        types.set("CircularQueue");
        types.set("Deque");
        types.set("DoublyLinkedList");
        types.set("FileSystemPath");
        types.set("FixedArray");
        types.set("Function");
        types.set("HashMap");
        types.set("HashTable");
        types.set("IPv4Address");
        types.set("InlineLinkedList");
        types.set("IntrusiveList");
        types.set("JsonArray");
        types.set("JsonObject");
        types.set("JsonValue");
        types.set("MappedFile");
        types.set("NetworkOrdered");
        types.set("NonnullOwnPtr");
        types.set("NonnullOwnPtrVector");
        types.set("NonnullRefPtr");
        types.set("NonnullRefPtrVector");
        types.set("Optional");
        types.set("OwnPtr");
        types.set("RefPtr");
        types.set("Result");
        types.set("ScopeGuard");
        types.set("SinglyLinkedList");
        types.set("String");
        types.set("StringBuilder");
        types.set("StringImpl");
        types.set("StringView");
        types.set("Utf8View");
        types.set("Vector");
        types.set("WeakPtr");
        types.set("auto");
        types.set("char");
        types.set("char16_t");
        types.set("char32_t");
        types.set("char8_t");
        types.set("double");
        types.set("float");
        types.set("i16");
        types.set("i32");
        types.set("i64");
        types.set("i8");
        types.set("int");
        types.set("int");
        types.set("long");
        types.set("short");
        types.set("signed");
        types.set("u16");
        types.set("u32");
        types.set("u64");
        types.set("u8");
        types.set("unsigned");
        types.set("void");
        types.set("wchar_t");
    }
    return types.contains(string);
}

Vector<CppToken> CppLexer::lex()
{
    Vector<CppToken> tokens;

    size_t token_start_index = 0;
    CppPosition token_start_position;

    auto emit_token = [&](auto type) {
        CppToken token;
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
        CppToken token;
        token.m_type = type;
        token.m_start = token_start_position;
        token.m_end = m_previous_position;
        tokens.append(token);
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
            for (size_t i = 0; i < 2; ++i) {
                if (!isxdigit(peek(2 + i)))
                    break;
                ++hex_digits;
            }
            return 2 + hex_digits;
        }
        case 'u': {
            bool is_unicode = true;
            for (size_t i = 0; i < 4; ++i) {
                if (!isxdigit(peek(2 + i))) {
                    is_unicode = false;
                    break;
                }
            }
            return is_unicode ? 6 : 0;
        }
        default:
            return 0;
        }
    };

    while (m_index < m_input.length()) {
        auto ch = peek();
        if (isspace(ch)) {
            begin_token();
            while (isspace(peek()))
                consume();
            commit_token(CppToken::Type::Whitespace);
            continue;
        }
        if (ch == '(') {
            emit_token(CppToken::Type::LeftParen);
            continue;
        }
        if (ch == ')') {
            emit_token(CppToken::Type::RightParen);
            continue;
        }
        if (ch == '{') {
            emit_token(CppToken::Type::LeftCurly);
            continue;
        }
        if (ch == '}') {
            emit_token(CppToken::Type::RightCurly);
            continue;
        }
        if (ch == '[') {
            emit_token(CppToken::Type::LeftBracket);
            continue;
        }
        if (ch == ']') {
            emit_token(CppToken::Type::RightBracket);
            continue;
        }
        if (ch == ',') {
            emit_token(CppToken::Type::Comma);
            continue;
        }
        if (ch == '*') {
            emit_token(CppToken::Type::Asterisk);
            continue;
        }
        if (ch == ';') {
            emit_token(CppToken::Type::Semicolon);
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
                commit_token(CppToken::Type::IncludeStatement);

                begin_token();
                while (isspace(peek()))
                    consume();
                commit_token(CppToken::Type::Whitespace);

                begin_token();
                if (peek() == '<' || peek() == '"') {
                    char closing = consume() == '<' ? '>' : '"';
                    while (peek() != closing && peek() != '\n')
                        consume();

                    if (consume() == '\n') {
                        commit_token(CppToken::Type::IncludePath);
                        continue;
                    } else {
                        commit_token(CppToken::Type::IncludePath);
                        begin_token();
                    }
                }
            }

            while (peek() && peek() != '\n')
                consume();

            commit_token(CppToken::Type::PreprocessorStatement);
            continue;
        }
        if (ch == '/' && peek(1) == '/') {
            begin_token();
            while (peek() && peek() != '\n')
                consume();
            commit_token(CppToken::Type::Comment);
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

            commit_token(CppToken::Type::Comment);
            continue;
        }
        if (ch == '"') {
            begin_token();
            consume();
            while (peek()) {
                if (peek() == '\\') {
                    size_t escape = match_escape_sequence();
                    if (escape > 0) {
                        commit_token(CppToken::Type::DoubleQuotedString);
                        begin_token();
                        for (size_t i = 0; i < escape; ++i)
                            consume();
                        commit_token(CppToken::Type::EscapeSequence);
                        begin_token();
                        continue;
                    }
                }

                if (consume() == '"')
                    break;
            }
            commit_token(CppToken::Type::DoubleQuotedString);
            continue;
        }
        if (ch == '\'') {
            begin_token();
            consume();
            while (peek()) {
                if (peek() == '\\') {
                    size_t escape = match_escape_sequence();
                    if (escape > 0) {
                        commit_token(CppToken::Type::SingleQuotedString);
                        begin_token();
                        for (size_t i = 0; i < escape; ++i)
                            consume();
                        commit_token(CppToken::Type::EscapeSequence);
                        begin_token();
                        continue;
                    }
                }

                if (consume() == '\'')
                    break;
            }
            commit_token(CppToken::Type::SingleQuotedString);
            continue;
        }
        if (isdigit(ch) || (ch == '.' && isdigit(peek(1)))) {
            begin_token();
            consume();

            auto type = ch == '.' ? CppToken::Type::Float : CppToken::Type::Integer;
            bool is_hex = false;
            bool is_binary = false;

            auto match_exponent = [&]() -> size_t {
                char ch = peek();
                if (ch != 'e' && ch != 'E' && ch != 'p' && ch != 'P')
                    return 0;

                type = CppToken::Type::Float;
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
                    if ((ch == 'u' || ch == 'U') && type == CppToken::Type::Integer) {
                        ++length;
                    } else if ((ch == 'f' || ch == 'F') && !is_binary) {
                        type = CppToken::Type::Float;
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
                        if (type == CppToken::Type::Integer) {
                            type = CppToken::Type::Float;
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
                commit_token(CppToken::Type::Keyword);
            else if (is_known_type(token_view))
                commit_token(CppToken::Type::KnownType);
            else
                commit_token(CppToken::Type::Identifier);
            continue;
        }
        dbg() << "Unimplemented token character: " << ch;
        emit_token(CppToken::Type::Unknown);
    }
    return tokens;
}

}
