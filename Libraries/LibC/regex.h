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

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <stddef.h>

namespace regex {
class VM;
}

__BEGIN_DECLS
struct regex_t {
    size_t re_nsub;
    u8 cflags;
    u8 eflags;
    regex::VM* vm { nullptr };
};

typedef size_t regoff_t;

struct regmatch_t {
    regoff_t rm_so; // byte offset from start of string to start of substring
    regoff_t rm_eo; // byte offset from start of string of the first character after the end of substring
};

// Values for the cflags parameter to the regcomp() function:
#define REG_EXTENDED 1 // Use Extended Regular Expressions.
#define REG_ICASE (REG_EXTENDED << 1)   // Ignore case in match.
#define REG_NOSUB (REG_EXTENDED << 2)   // Report only success or fail in regexec().
#define REG_NEWLINE (REG_EXTENDED << 3) // Change the handling of newline.

// Values for the eflags parameter to the regexec() function:
#define REG_NOTBOL 1 // The circumflex character (^), when taken as a special character, will not match the beginning of string.
#define REG_NOTEOL (REG_NOTBOL << 1) // The dollar sign ($), when taken as a special character, will not match the end of string.

// The following constants are defined as error return values:
enum ReError {
    REG_NOERR = 0,
    REG_NOMATCH,  // regexec() failed to match.
    REG_BADPAT,   // Invalid regular expression.
    REG_ECOLLATE, // Invalid collating element referenced.
    REG_ECTYPE,   // Invalid character class type referenced.
    REG_EESCAPE,  // Trailing \ in pattern.
    REG_ESUBREG,  // Number in \digit invalid or in error.
    REG_EBRACK,   // [ ] imbalance.
    REG_EPAREN,   // \( \) or ( ) imbalance.
    REG_EBRACE,   // \{ \} imbalance.
    REG_BADBR,    // Content of \{ \} invalid: not a number, number too large, more than two numbers, first larger than second.
    REG_ERANGE,   // Invalid endpoint in range expression.
    REG_ESPACE,   // Out of memory.
    REG_BADRPT,   // ?, * or + not preceded by valid regular expression.
    REG_ENOSYS,   // The implementation does not support the function.
};

int regcomp(regex_t*, const char*, int);
int regexec(const regex_t*, const char*, size_t, regmatch_t[], int);
size_t regerror(int, const regex_t*, char*, size_t);
void regfree(regex_t*);

__END_DECLS

namespace regex {

enum class OpCode : size_t {
    Compare = 1,
    ForkJump,
    ForkStay,
    SaveLeftGroup,
    SaveRightGroup,
    CheckBegin,
    CheckEnd,
    Exit,
};

class StackValue {
public:
    union {
        OpCode op_code;
        char* string;
        int length;
    };
    explicit StackValue(OpCode op_code_) { op_code = op_code_; }
    explicit StackValue(char* string_) { string = string_; }
    explicit StackValue(int length_) { length = length_; }
    ~StackValue();
};

#define ENUMERATE_REGEX_TOKENS                 \
    __ENUMERATE_REGEX_TOKEN(Eof)               \
    __ENUMERATE_REGEX_TOKEN(OrdinaryCharacter) \
    __ENUMERATE_REGEX_TOKEN(Circumflex)        \
    __ENUMERATE_REGEX_TOKEN(Period)            \
    __ENUMERATE_REGEX_TOKEN(LeftParen)         \
    __ENUMERATE_REGEX_TOKEN(RightParen)        \
    __ENUMERATE_REGEX_TOKEN(LeftCurly)         \
    __ENUMERATE_REGEX_TOKEN(RightCurly)        \
    __ENUMERATE_REGEX_TOKEN(LeftBracket)       \
    __ENUMERATE_REGEX_TOKEN(RightBracket)      \
    __ENUMERATE_REGEX_TOKEN(Asterisk)          \
    __ENUMERATE_REGEX_TOKEN(EscapeSequence)    \
    __ENUMERATE_REGEX_TOKEN(Dollar)            \
    __ENUMERATE_REGEX_TOKEN(Pipe)              \
    __ENUMERATE_REGEX_TOKEN(Plus)              \
    __ENUMERATE_REGEX_TOKEN(Minus)             \
    __ENUMERATE_REGEX_TOKEN(Comma)             \
    __ENUMERATE_REGEX_TOKEN(Questionmark)

enum class TokenType {
#define __ENUMERATE_REGEX_TOKEN(x) x,
    ENUMERATE_REGEX_TOKENS
#undef __ENUMERATE_REGEX_TOKEN
};

class Token {
public:
    Token(TokenType type, size_t start_position, StringView value)
        : m_type(type)
        , m_position(start_position)
        , m_value(value)
    {
    }

    TokenType type() const { return m_type; }
    const char* name() const;
    static const char* name(TokenType);
    const StringView& value() const { return m_value; }

private:
    TokenType m_type;
    size_t m_position;
    StringView m_value;
};

class Lexer {
public:
    explicit Lexer(StringView source);
    Token next();
    bool has_errors() const { return m_has_errors; }
    void reset();

private:
    char peek(size_t offset = 0) const;
    void consume();

    StringView m_source;
    size_t m_position = 0;
    size_t m_previous_position = 0;
    Token m_current_token;
    int m_current_char;
    bool m_has_errors = false;
};

class Parser {
public:
    explicit Parser(Lexer lexer);
    Vector<StackValue>& parse();
    bool has_errors() const { return m_parser_state.m_has_errors; }

private:
    bool match(TokenType type) const;
    bool done() const;
    void expected(const char* what);
    Token consume();
    Token consume(TokenType type);
    void save_state();
    void load_state();
    bool match_meta_chars();
    bool match_ere_quoted_chars();
    bool match_ere_dupl_symbol();
    bool parse_ere_dupl_symbol();
    bool parse_ere_expression();
    bool parse_extended_reg_exp();
    int label_offset(size_t label);
    int last_label_offset();
    size_t get_label();
    void reset();

    struct ParserState {
        Lexer m_lexer;
        Token m_current_token;
        bool m_has_errors = false;
        size_t m_last_label = 0;

        explicit ParserState(Lexer);
    };

    ParserState m_parser_state;
    Optional<ParserState> m_saved_state;
    Vector<StackValue> m_bytes;
};

class VM {
public:
    explicit VM(const Vector<StackValue>& bytecode, const String&& pattern)
        : m_bytecode(bytecode)
        , m_pattern(move(pattern))
    {
    }

    bool match(const StringView view);
    bool match(const StringView view, size_t& ops_count);
    const Vector<StackValue>& bytes() const { return m_bytecode; }

private:
    struct MatchState {
        size_t m_stringp { 0 };
        size_t m_instructionp { 0 };
        StringView m_view;
        size_t m_ops { 0 };
        MatchState() = default;
        MatchState(size_t stringp, size_t instructionp, StringView view);
    };

    bool match_recurse(MatchState& state);
    const StackValue current(MatchState& state) const;
    const StackValue increment(MatchState& state) const;

    const Vector<StackValue> m_bytecode;
    const String& m_pattern;
};
}
