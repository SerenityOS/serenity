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
#include <AK/StringBuilder.h>
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
    , m_current_token(TokenType::Eof, 0, StringView(nullptr))
{
}

char Lexer::peek(size_t offset) const
{
    if ((m_position + offset) >= m_source.length())
        return EOF;
    return m_source[m_position + offset];
}

void Lexer::consume()
{
    m_previous_position = m_position;

    if (m_position >= m_source.length()) {
        m_position = m_source.length() + 1;
        m_current_char = EOF;
        return;
    }

    m_current_char = m_source[m_position++];
}

void Lexer::reset()
{
    m_position = 0;
    m_current_token = { TokenType::Eof, 0, StringView(nullptr) };
    m_has_errors = false;
    m_current_char = 0;
    m_previous_position = 0;
}

Token Lexer::next()
{
    size_t token_start_position;

    auto begin_token = [&] {
        token_start_position = m_position;
    };

    auto commit_token = [&](auto type) {
        auto substring = m_source.substring_view(token_start_position, m_previous_position - token_start_position + 1);
        m_current_token = Token(type, token_start_position, substring);
    };

    auto emit_token = [&](auto type) {
        m_current_token = Token(type, m_position, m_source.substring_view(m_position, 1));
        consume();
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

    while (m_position <= m_source.length()) {

        auto ch = peek();
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
        }

        if (ch == EOF)
            break;

        emit_token(TokenType::OrdinaryCharacter);
        return m_current_token;
    }

    return Token(TokenType::Eof, m_position, m_source.substring_view(0, 0));
}

Parser::ParserState::ParserState(Lexer lexer)
    : m_lexer(move(lexer))
    , m_current_token(m_lexer.next())
{
}

Parser::Parser(Lexer lexer)
    : m_parser_state(move(lexer))
{
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
    return (type == TokenType::Circumflex
        || type == TokenType::Period
        || type == TokenType::LeftBracket
        || type == TokenType::Dollar
        || type == TokenType::LeftParen
        || type == TokenType::RightParen
        || type == TokenType::Pipe
        || type == TokenType::Asterisk
        || type == TokenType::Plus
        || type == TokenType::Questionmark
        || type == TokenType::LeftCurly
        || type == TokenType::EscapeSequence);
}

bool Parser::match_ere_dupl_symbol()
{
    auto type = m_parser_state.m_current_token.type();
    return (type == TokenType::Asterisk
        || type == TokenType::Plus
        || type == TokenType::Questionmark
        || type == TokenType::LeftCurly);
}

bool Parser::parse_ere_dupl_symbol()
{
    if (match(TokenType::LeftCurly)) {
        consume();

        StringBuilder number1_builder;
        while (match(TokenType::OrdinaryCharacter)) {
            number1_builder.append(consume().value());
        }
        bool ok;
        u64 number1 = number1_builder.build().to_uint(ok);
        ASSERT(ok);
        UNUSED_PARAM(number1);

        if (match(TokenType::Comma)) {
            consume();
        }

        StringBuilder number2_builder;
        while (match(TokenType::OrdinaryCharacter)) {
            number2_builder.append(consume().value());
        }
        bool ok2;
        u64 number2 = number2_builder.build().to_uint(ok2);
        ASSERT(ok2);
        UNUSED_PARAM(number2);

        consume(TokenType::RightCurly);

        // FIXME: Add OpCode for range based duplication
        return true;

    } else if (match(TokenType::Plus)) {
        consume();

        m_bytes.empend(OpCode::ForkJump);
        m_bytes.empend(last_label_offset());
        return true;

    } else if (match(TokenType::Asterisk)) {
        consume();

        m_bytes.empend(OpCode::ForkJump);
        m_bytes.empend(last_label_offset());
        return true;

    } else if (match(TokenType::Questionmark)) {
        consume();

        m_bytes.empend(OpCode::ForkStay);
        m_bytes.empend(last_label_offset());
        return true;
    }

    return false;
}

int Parser::last_label_offset()
{
    return label_offset(m_parser_state.m_last_label);
}

int Parser::label_offset(size_t label)
{
    return label - m_bytes.size();
}

size_t Parser::get_label()
{
    return m_bytes.size();
}

bool Parser::parse_ere_expression()
{
    for (;;) {
        bool matched = false;
        if (match(TokenType::OrdinaryCharacter)) {
            m_parser_state.m_last_label = get_label();
            size_t length = 0;
            Token start_token = m_parser_state.m_current_token;
            for (;;) {
                if (!match(TokenType::OrdinaryCharacter))
                    break;
                ++length;
                consume();
            }
            m_bytes.empend(OpCode::Compare);
            m_bytes.empend((char*)start_token.value().characters_without_null_termination());
            m_bytes.empend(length);
            matched = true;
        }

        else if (match(TokenType::Period)) {
            m_parser_state.m_last_label = get_label();
            Token t = consume();
            m_bytes.empend(OpCode::Compare);
            m_bytes.empend((char*)t.value().characters_without_null_termination());
            m_bytes.empend(1);
            matched = true;
        }

        else if (match(TokenType::EscapeSequence)) {
            m_parser_state.m_last_label = get_label();
            Token t = consume();
            m_bytes.empend(OpCode::Compare);
#ifdef REGEX_DEBUG
            printf("[PARSER] EscapeSequence with substring %s\n", String(t.value()).characters());
#endif
            m_bytes.empend((char*)t.value().characters_without_null_termination() + 1);
            m_bytes.empend(1);
            matched = true;
        }

        // FIXME: Add bracket expression matching/parsing

        if (match(TokenType::Circumflex)) {
            consume(TokenType::Circumflex);
            m_bytes.empend(OpCode::CheckBegin);
            matched = true;
        }

        if (match(TokenType::Dollar)) {
            consume(TokenType::Dollar);
            m_bytes.empend(OpCode::CheckEnd);
            matched = true;
        }

        if (match(TokenType::LeftParen)) {
            consume(TokenType::LeftParen);
            m_bytes.empend(OpCode::SaveLeftGroup);
            parse_extended_reg_exp();
            consume(TokenType::RightParen);
            m_bytes.empend(OpCode::SaveRightGroup);
            matched = true;
        }

        if (match_ere_dupl_symbol()) {
            parse_ere_dupl_symbol();
            matched = true;
        }

        if (!matched)
            break;
    }

    if (match(TokenType::Eof))
        return true;

    return true;
}

bool Parser::parse_extended_reg_exp()
{
    bool matched = false;
    bool errors = false;
    for (;;) {
        errors &= !(parse_ere_expression());

        if (match(TokenType::Pipe)) {
            consume(TokenType::Pipe);
            ASSERT_NOT_REACHED();

            errors &= !(parse_extended_reg_exp());

            // FIXME: Add logical OR opcode with two parameters
            //        containing the jump offsets to both branches.
            matched = true;
        }

        if (!matched)
            break;
    }

    m_parser_state.m_has_errors = errors;
    return !errors;
}

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
    m_parser_state.m_current_token = m_parser_state.m_lexer.next();
    return old_token;
}

Token Parser::consume(TokenType type)
{
    if (m_parser_state.m_current_token.type() != type) {
        m_parser_state.m_has_errors = true;
#ifdef REGEX_DEBUG
        fprintf(stderr, "[PARSER] Error: Unexpected token %s. Expected %s\n", m_parser_state.m_current_token.name(), Token::name(type));
#endif
    }
    return consume();
}

Vector<ByteCode>& Parser::parse()
{
    m_bytes.clear();
    parse_extended_reg_exp();
    consume(TokenType::Eof);

#ifdef REGEX_DEBUG
    printf("[PARSER] Produced bytecode stack with %lu entries\n", m_bytes.size());
#endif
    return m_bytes;
}

void Parser::reset()
{
    m_bytes.clear();
    m_parser_state.m_lexer.reset();
    m_parser_state.m_current_token = m_parser_state.m_lexer.next();
}

ByteCode::~ByteCode()
{
}

VM::MatchState::MatchState(size_t stringp, size_t instructionp, StringView view)
    : m_stringp(stringp)
    , m_instructionp(instructionp)
    , m_view(view)
{
}

bool VM::match(StringView view)
{
    MatchState state;
    state.m_view = view;
    return match_recurse(state);
}

bool VM::match(StringView view, size_t& ops_count)
{
    MatchState state;
    state.m_view = view;
    auto result = match_recurse(state);
    ops_count = state.m_ops;
    return result;
}

const ByteCode VM::current(MatchState& state) const
{
    if (state.m_instructionp < m_bytecode.size())
        return m_bytecode.at(state.m_instructionp);
    else
        return ByteCode(OpCode::Exit);
}

const ByteCode VM::increment(MatchState& state) const
{
    ++state.m_instructionp;
    return current(state);
}

bool VM::match_recurse(MatchState& state)
{
    MatchState jump_state;
    jump_state.m_view = state.m_view;
    Vector<MatchState> stay_states;

    for (;;) {
        ++state.m_ops;
        auto stack_item = current(state);

#ifdef REGEX_DEBUG
        printf("[VM] OpCode: 0x%i - instructionp: %2lu, stringp: %2lu - [%20s]", stack_item.length, state.m_instructionp, state.m_stringp,
            String(&state.m_view[state.m_stringp], state.m_view.length() - state.m_stringp).characters());
#endif

        if (stack_item.op_code == OpCode::Compare) {
            auto* str = increment(state).string;
            auto& length = increment(state).length;

#ifdef REGEX_DEBUG
            printf(" > Comparing strings: '%s' == '%s' with length: %i\n", String(str, length).characters(), String(&state.m_view[state.m_stringp], length).characters(), length);
#endif

            if (length == 1) {
                if (str[0] == '.') {
                    ++state.m_stringp;
                } else if (str[0] == state.m_view[state.m_stringp])
                    ++state.m_stringp;
                else
                    return false;
            } else {
                if ((size_t)length > state.m_view.length() - state.m_stringp)
                    return false;

                if (!strncmp(str, &state.m_view[state.m_stringp], length))
                    state.m_stringp += length;
                else
                    return false;
            }

            if (state.m_stringp > state.m_view.length())
                return false;

        } else if (stack_item.op_code == OpCode::ForkJump) {
            auto& offset = increment(state).length;
            jump_state.m_stringp = state.m_stringp;
            jump_state.m_ops = state.m_ops;
            jump_state.m_instructionp = state.m_instructionp + offset;
#ifdef REGEX_DEBUG
            printf(" > ForkJump to offset: %i, jump_state: instructionp: %lu, stringp: %lu\n", offset, jump_state.m_instructionp, jump_state.m_stringp);
#endif
            // first do the recursion
            if (match_recurse(jump_state)) {
                // found a way out...
                state.m_ops = jump_state.m_ops;
                return true;
            } else
                state.m_ops = jump_state.m_ops;
            // if not sucessful, continue in this function

        } else if (stack_item.op_code == OpCode::ForkStay) {
            auto& offset = increment(state).length;
            stay_states.empend(state.m_instructionp + offset, state.m_stringp, state.m_view);
#ifdef REGEX_DEBUG
            printf("\n");
#endif

        } else if (stack_item.op_code == OpCode::SaveLeftGroup) {
#ifdef REGEX_DEBUG
            printf("\n");
#endif

        } else if (stack_item.op_code == OpCode::SaveRightGroup) {
#ifdef REGEX_DEBUG
            printf("\n");
#endif

        } else if (stack_item.op_code == OpCode::CheckBegin) {
#ifdef REGEX_DEBUG
            printf("\n");
#endif
            if (state.m_stringp != 0)
                return false;

        } else if (stack_item.op_code == OpCode::CheckEnd) {
#ifdef REGEX_DEBUG
            printf("\n");
#endif
            if (state.m_stringp != state.m_view.length())
                return false;

        } else if (stack_item.op_code == OpCode::Exit) {
            break;
        }

        increment(state);

        if (state.m_instructionp >= m_bytecode.size()) {
#ifdef REGEX_DEBUG
            printf("Reached end of OpCodes!\n");
#endif
            break;
        }
    }

#ifdef REGEX_DEBUG
    printf("String: stringp: %lu, length: %lu\n", state.m_stringp, state.m_view.length());
    printf("Instruction: instructionp: %lu, size: %lu\n", state.m_instructionp, bytes().size());
#endif

    if (state.m_stringp == state.m_view.length()) {
        if (state.m_instructionp == bytes().size()) // last instruction done with OpCode::Exit!
            return true;
    }
    for (auto& s_state : stay_states) {
        s_state.m_ops = state.m_ops;
        if (match_recurse(s_state)) {
            state.m_ops = s_state.m_ops;
            return true;
        } else
            state.m_ops = s_state.m_ops;
    }

    return false;
}
}

int regcomp(regex_t* preg, const char* pattern, int cflags)
{
    if (!(cflags & REG_EXTENDED))
        return REG_ENOSYS;

    preg->cflags = cflags;

    String s(pattern);
    regex::Lexer lexer(s);

#ifdef REGEX_DEBUG
    printf("[LEXER] Tokens for pattern '%s':\n", pattern);
    while (true) {
        regex::Token token = lexer.next();
        if (token.type() == regex::TokenType::Eof)
            break;

        String a(token.value());
        printf("[LEXER] %s -> %s\n", token.name(), a.characters());
        fflush(stdout);
    }
    lexer.reset();
#endif

    regex::Parser parser(move(lexer));
    auto& bytes = parser.parse();

#ifdef REGEX_DEBUG
    int i = 0;
    for (auto& item : bytes) {
        printf("[PARSER] [%i]: %i\n", i, item.length);
        i++;
    }
#endif

    if (parser.has_errors())
        return REG_BADPAT;

    preg->vm = new regex::VM(bytes, move(s));
    return REG_NOERR;
}

extern "C" {

int regexec(const regex_t* preg, const char* string, size_t nmatch, regmatch_t pmatch[], int eflags)
{
    UNUSED_PARAM(nmatch);
    UNUSED_PARAM(pmatch);
    UNUSED_PARAM(eflags);

    if (!preg->vm)
        return REG_BADPAT;

    auto& vm = *preg->vm;
    size_t ops;
    if (vm.match(StringView { string }, ops)) {
        printf("[regexec] match successful, took %lu operations.\n", ops);
        return REG_NOERR;
    } else {
        printf("[regexec] match not successful, took %lu operations.\n", ops);
    }

    return REG_NOMATCH;
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
    if (preg->vm)
        delete preg->vm;
}
}
