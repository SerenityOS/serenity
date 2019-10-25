#include "CppLexer.h"
#include <AK/HashTable.h>
#include <AK/String.h>
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
    static HashTable<String> keywords;
    if (keywords.is_empty()) {
        keywords.set("alignas");
        keywords.set("alignof");
        keywords.set("and");
        keywords.set("and_eq");
        keywords.set("asm");
        keywords.set("auto");
        keywords.set("bitand");
        keywords.set("bitor");
        keywords.set("bool");
        keywords.set("break");
        keywords.set("case");
        keywords.set("catch");
        keywords.set("char");
        keywords.set("char8_t");
        keywords.set("char16_t");
        keywords.set("char32_t");
        keywords.set("class");
        keywords.set("compl");
        keywords.set("const");
        keywords.set("constexpr");
        keywords.set("const_cast");
        keywords.set("continue");
        keywords.set("decltype");
        keywords.set("default");
        keywords.set("delete");
        keywords.set("do");
        keywords.set("double");
        keywords.set("dynamic_cast");
        keywords.set("else");
        keywords.set("enum");
        keywords.set("explicit");
        keywords.set("export");
        keywords.set("extern");
        keywords.set("false");
        keywords.set("float");
        keywords.set("for");
        keywords.set("friend");
        keywords.set("goto");
        keywords.set("if");
        keywords.set("inline");
        keywords.set("int");
        keywords.set("long");
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
        keywords.set("private");
        keywords.set("protected");
        keywords.set("public");
        keywords.set("register");
        keywords.set("reinterpret_cast");
        keywords.set("return");
        keywords.set("short");
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
        keywords.set("unsigned");
        keywords.set("using");
        keywords.set("virtual");
        keywords.set("void");
        keywords.set("volatile");
        keywords.set("wchar_t");
        keywords.set("while");
        keywords.set("xor");
        keywords.set("xor_eq");
    }
    return keywords.contains(string);
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
            commit_token(CppToken::Type::Comment);
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
        emit_token(CppToken::Type::Unknown);
    }
    return tokens;
}
