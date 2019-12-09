#include "CppLexer.h"
#include <AK/HashTable.h>
#include <AK/String.h>
#include <ctype.h>

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
