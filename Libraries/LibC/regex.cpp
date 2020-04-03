/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include "regex.h"
#include <AK/String.h>
#include <ctype.h>
#include <stdio.h>

namespace regex {

const char* Token::name(TokenType type)
{
    switch (type) {
#define __ENUMERATE_REGEX_TOKEN(x) \
    case TokenType::x:             \
        return #x;
        ENUMERATE_REGEX_TOKENS
#undef __ENUMERATE_REGEX_TOKEN
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* Token::name() const
{
    return name(m_type);
}

Lexer::Lexer(const StringView source)
    : m_source(source)
    , m_current_token(TokenType::Eof, StringView(nullptr))
{
}

char Lexer::peek(size_t offset) const
{
    if ((m_position + offset) >= m_source.length())
        return 0;
    return m_source[m_position + offset];
}

void Lexer::consume()
{
    m_previous_position = m_position;

    if (m_position >= m_source.length()) {
        printf("EOF\n");
        m_position = m_source.length() + 1;
        m_current_char = EOF;
        return;
    }

    m_current_char = m_source[m_position++];
}

Optional<Token> Lexer::next()
{
    size_t token_start_position;
    bool ordinary_character { false };

    auto commit_token = [&](auto type) {
        m_current_token = Token(type, m_source.substring_view(token_start_position, m_previous_position - token_start_position + 1));
    };

    auto begin_token = [&] {
        if (ordinary_character) {
            commit_token(TokenType::OrdinaryCharacters);
        } else {
            token_start_position = m_position;
        }
    };

    auto emit_token = [&](auto type) {
        if (ordinary_character) {
            commit_token(TokenType::OrdinaryCharacters);
        } else {
            m_current_token = Token(type, m_source.substring_view(m_position, 1));
            consume();
        }
    };

    auto match_escape_sequence = [&]() -> size_t {
        switch (peek(1)) {
        case '^':
        case '.':
        case '[':
        case '$':
        case '(':
        case ')':
        case '|':
        case '*':
        case '+':
        case '?':
        case '{':
        case '\\':
            return 2;
        default:
            return 0;
        }
    };

    while (m_position < m_source.length()) {

        auto ch = peek();
        if (isspace(ch)) {
            begin_token();
            while (isspace(peek()))
                consume();
            commit_token(TokenType::Whitespace);
            return m_current_token;
        }
        if (ch == '(') {
            emit_token(TokenType::LeftParen);
            return m_current_token;
        }
        if (ch == ')') {
            emit_token(TokenType::RightParen);
            return m_current_token;
        }
        if (ch == '{') {
            emit_token(TokenType::LeftCurly);
            return m_current_token;
        }
        if (ch == '}') {
            emit_token(TokenType::RightCurly);
            return m_current_token;
        }
        if (ch == '[') {
            emit_token(TokenType::LeftBracket);
            return m_current_token;
        }
        if (ch == ']') {
            emit_token(TokenType::RightBracket);
            return m_current_token;
        }
        if (ch == '.') {
            emit_token(TokenType::Period);
            return m_current_token;
        }
        if (ch == '*') {
            emit_token(TokenType::Asterisk);
            return m_current_token;
        }
        if (ch == '+') {
            emit_token(TokenType::Plus);
            return m_current_token;
        }
        if (ch == '$') {
            emit_token(TokenType::Dollar);
            return m_current_token;
        }
        if (ch == '^') {
            emit_token(TokenType::Circumflex);
            return m_current_token;
        }
        if (ch == '|') {
            emit_token(TokenType::Pipe);
            return m_current_token;
        }
        if (ch == '?') {
            emit_token(TokenType::Questionmark);
            return m_current_token;
        }
        if (ch == ',') {
            emit_token(TokenType::Comma);
            return m_current_token;
        }
        if (ch == '-') {
            emit_token(TokenType::Minus);
            return m_current_token;
        }

        if (ch == '\\') {
            size_t escape = match_escape_sequence();
            if (escape > 0) {
                begin_token();
                for (size_t i = 0; i < escape; ++i)
                    consume();
                commit_token(TokenType::EscapeSequence);
                return m_current_token;
            }
        } else if (m_current_char == EOF) {
            emit_token(TokenType::Eof);
            return m_current_token;
        }

        // Everything else goes to ordinary characters
        if (!ordinary_character) {
            begin_token();
            ordinary_character = true;
        }
        consume();
    }

    return {};
}

Parser::ParserState::ParserState(Lexer lexer)
    : m_lexer(move(lexer))
    , m_current_token(m_lexer.next().value())
{
}

Parser::Parser(Lexer lexer)
    : m_parser_state(move(lexer))
{
}

bool Parser::match_ord_chars()
{
    auto type = m_parser_state.m_current_token.type();
    return type == TokenType::OrdinaryCharacters;
}

bool Parser::match_meta_chars()
{
    auto type = m_parser_state.m_current_token.type();
    return type == TokenType::Circumflex
        || type == TokenType::Minus
        || type == TokenType::RightBracket;
}

bool Parser::match_ere_quoted_chars()
{
    auto type = m_parser_state.m_current_token.type();
    return type == TokenType::EscapeSequence;
}

Optional<Token> Parser::parse_ere_quoted_chars()
{
    consume(TokenType::EscapeSequence);

    Token token = m_parser_state.m_lexer.next().value();
    if (token.type() == TokenType::Circumflex
        || token.type() == TokenType::Period
        || token.type() == TokenType::LeftBracket
        || token.type() == TokenType::Dollar
        || token.type() == TokenType::LeftParen
        || token.type() == TokenType::RightParen
        || token.type() == TokenType::Pipe
        || token.type() == TokenType::Asterisk
        || token.type() == TokenType::Plus
        || token.type() == TokenType::Questionmark
        || token.type() == TokenType::LeftCurly
        || token.type() == TokenType::EscapeSequence)
        return token;
    else {
        fprintf(stderr, "Error: Unexpected token %s in Escape sequence.\n", m_parser_state.m_current_token.name());
        return {};
    }
}

//void Parser::parse_ere()
//{
//    Vector<ByteCode> result;

//    return result;
//}

//bool Parser::match_branch()
//{
//}

bool Parser::done() const
{
    return match(TokenType::Eof);
}

bool Parser::match(TokenType type) const
{
    return m_parser_state.m_current_token.type() == type;
}

Token Parser::consume()
{
    auto old_token = m_parser_state.m_current_token;
    m_parser_state.m_current_token = m_parser_state.m_lexer.next().value();
    return old_token;
}

Token Parser::consume(TokenType type)
{
    if (m_parser_state.m_current_token.type() != type) {
        m_parser_state.m_has_errors = true;
        fprintf(stderr, "Error: Unexpected token %s. Expected %s\n", m_parser_state.m_current_token.name(), Token::name(type));
    }
    return consume();
}

Vector<ByteCode> Parser::parse_next()
{
    Vector<ByteCode> bytes;

    if (done())
        return {};

    //    if (match_characters()) {

    //    } else if (match_) {

    //    } else if (match_statement()) {
    //        program->append(parse_statement());
    //    } else {
    //        expected("statement");
    //        consume();
    //    }

    return bytes;
}

}

//Instructions:

//* Match groups: SAVEL, SAVER
//* Compare: CMP (string, char, ...?)
//* Jumps:
//  - FORKSTAY (higher priority on the thread that stays)
//  - FORKJUMP (higher priority on the thread that jumps)
//*

int regcomp(regex_t* preg, const char* pattern, int cflags)
{
    UNUSED_PARAM(preg);
    UNUSED_PARAM(cflags);

    if (!(cflags & REG_EXTENDED))
        return REG_ENOSYS;

    preg->cflags = cflags;

    regex::Lexer lexer(pattern);

    printf("Tokens for pattern '%s':\n", pattern);

    Optional<regex::Token> token;
    while (true) {
        token = lexer.next();
        if (!token.has_value())
            break;

        String a(token.value().value());
        printf("%s -> %s\n", token.value().name(), a.characters());
    }

    return REG_NOERR;
}

extern "C" {

int regexec(const regex_t* preg, const char* string, size_t nmatch, regmatch_t pmatch[], int eflags)
{
    UNUSED_PARAM(preg);
    UNUSED_PARAM(string);
    UNUSED_PARAM(nmatch);
    UNUSED_PARAM(pmatch);
    UNUSED_PARAM(eflags);
    return REG_NOERR;
}

size_t regerror(int errcode, const regex_t* preg, char* errbuf, size_t errbuf_size)
{
    UNUSED_PARAM(preg);

    String error;
    switch ((ReError)errcode) {
    case REG_NOERR:
        error = "No error";
        break;
    case REG_NOMATCH:
        error = "regexec() failed to match.";
        break;
    case REG_BADPAT:
        error = "Invalid regular expression.";
        break;
    case REG_ECOLLATE:
        error = "Invalid collating element referenced.";
        break;
    case REG_ECTYPE:
        error = "Invalid character class type referenced.";
        break;
    case REG_EESCAPE:
        error = "Trailing \\ in pattern.";
        break;
    case REG_ESUBREG:
        error = "Number in \\digit invalid or in error.";
        break;
    case REG_EBRACK:
        error = "[ ] imbalance.";
        break;
    case REG_EPAREN:
        error = "\\( \\) or ( ) imbalance.";
        break;
    case REG_EBRACE:
        error = "\\{ \\} imbalance.";
        break;
    case REG_BADBR:
        error = "Content of \\{ \\} invalid: not a number, number too large, more than two numbers, first larger than second.";
        break;
    case REG_ERANGE:
        error = "Invalid endpoint in range expression.";
        break;
    case REG_ESPACE:
        error = "Out of memory.";
        break;
    case REG_BADRPT:
        error = "?, * or + not preceded by valid regular expression.";
        break;
    case REG_ENOSYS:
        error = "The implementation does not support the function.";
        break;
    }

    if (!errbuf_size)
        return error.length();
    else if (error.length() > errbuf_size) {
        strncpy(errbuf, error.characters(), errbuf_size - 1);
        errbuf[errbuf_size - 1] = '\0';
    } else {
        strncpy(errbuf, error.characters(), error.length());
    }
    return error.length();
}

void regfree(regex_t* preg)
{
    preg->re_nsub = 0;
    preg->cflags = 0;
    preg->eflags = 0;
}
}
