#include "CppLexer.h"
#include <AK/LogStream.h>
#include <ctype.h>

CppLexer::CppLexer(const StringView& input)
    : m_input(input)
{
}

char CppLexer::peek(int offset) const
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
    if (string == "int" || string == "char" || string == "return")
        return true;
    return false;
}

Vector<CppToken> CppLexer::lex()
{
    Vector<CppToken> tokens;

    int token_start_index = 0;
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
            while (peek()) {
                if (peek() == '*' && peek(1) == '/')
                    break;
                consume();
            }
            consume();
            consume();
            emit_token(CppToken::Type::Comment);
            continue;
        }
        if (ch == '"') {
            begin_token();
            consume();
            while (peek()) {
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
                if (consume() == '\'')
                    break;
            }
            commit_token(CppToken::Type::SingleQuotedString);
            continue;
        }
        if (isdigit(ch)) {
            begin_token();
            while (peek() && isdigit(peek())) {
                consume();
            }
            commit_token(CppToken::Type::Number);
            continue;
        }
        if (is_valid_first_character_of_identifier(ch)) {
            begin_token();
            while (peek() && is_valid_nonfirst_character_of_identifier(peek()))
                consume();
            auto token_view = StringView(m_input.characters_without_null_termination() + token_start_index, m_index - token_start_index);
            if (is_keyword(token_view))
                commit_token(CppToken::Type::Keyword);
            else
                commit_token(CppToken::Type::Identifier);
            continue;
        }
        dbg() << "Unimplemented token character: " << ch;
        ASSERT_NOT_REACHED();
    }
    return tokens;
}
