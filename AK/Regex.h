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
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <stddef.h>

namespace AK {
namespace regex {

enum class RegexError : u8 {
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

enum class CompilationFlags {
    Extended = 1,
    IgnoreCase = 2,
    NoSubExpressions = 4,
    HandleNewLine = 8
};

enum class MatchFlags {
    NoBeginOfLine = 1,
    NoEndOfLine = 2,
    MatchAll = 4,
    Search = 8,
    Stats = 16
};

#define REG_MAX_RECURSE 5000

#define ENUMERATE_OPCODES              \
    __ENUMERATE_OPCODE(Compare)        \
    __ENUMERATE_OPCODE(Jump)           \
    __ENUMERATE_OPCODE(ForkJump)       \
    __ENUMERATE_OPCODE(ForkStay)       \
    __ENUMERATE_OPCODE(SaveLeftGroup)  \
    __ENUMERATE_OPCODE(SaveRightGroup) \
    __ENUMERATE_OPCODE(CheckBegin)     \
    __ENUMERATE_OPCODE(CheckEnd)       \
    __ENUMERATE_OPCODE(Exit)

enum class OpCode : u8 {
#define __ENUMERATE_OPCODE(x) x,
    ENUMERATE_OPCODES
#undef __ENUMERATE_OPCODE
};

enum class CompareType : u8 {
    Undefined,
    Inverse,
    AnySingleCharacter,
    OrdinaryCharacter,
    OrdinaryCharacters,
    CharacterClass,
    RangeExpression,
    RangeExpressionDummy,
};

enum class CharacterClass : u8 {
    Alnum,
    Cntrl,
    Lower,
    Space,
    Alpha,
    Digit,
    Print,
    Upper,
    Blank,
    Graph,
    Punct,
    Xdigit,
};

class StackValue {
public:
    union CompareValue {
        CompareValue(const CharacterClass value)
            : character_class(value)
        {
        }
        CompareValue(const char value1, const char value2)
            : range_values { value1, value2 }
        {
        }
        const CharacterClass character_class;
        const struct {
            const char from;
            const char to;
        } range_values;
    };

    union {
        const OpCode op_code;
        const char* string;
        const char ch;
        const int number;
        const size_t positive_number;
        const CompareValue compare_value;
        const CompareType compare_type;
    };

    const char* name() const;
    static const char* name(OpCode);

    StackValue(const OpCode value)
        : op_code(value)
    {
    }
    StackValue(const char* value)
        : string(value)
    {
    }
    StackValue(const char value)
        : ch(value)
    {
    }
    StackValue(const int value)
        : number(value)
    {
    }
    StackValue(const size_t value)
        : positive_number(value)
    {
    }
    StackValue(const CharacterClass value)
        : compare_value(value)
    {
    }
    StackValue(const char value1, const char value2)
        : compare_value(value1, value2)
    {
    }
    StackValue(const CompareType value)
        : compare_type(value)
    {
    }

    ~StackValue() = default;
};

struct CompareTypeAndValue {
    CompareType type;
    StackValue value;
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
    size_t position() const { return m_position; }

private:
    TokenType m_type;
    size_t m_position;
    StringView m_value;
};

class Lexer {
public:
    explicit Lexer(StringView source);
    Token next();
    void reset();
    void back(size_t offset);

private:
    char peek(size_t offset = 0) const;
    void consume();

    StringView m_source;
    size_t m_position = 0;
    size_t m_previous_position = 0;
    Token m_current_token;
    int m_current_char;
};

class Parser {
public:
    explicit Parser(Lexer lexer);

    struct ParserResult {
        Vector<StackValue> m_bytes;
        size_t m_match_groups;
        size_t m_min_match_length;
        RegexError m_error;
        Token m_error_token;
    };

    ParserResult parse(u8 compilation_flags);
    bool has_error() const { return m_parser_state.m_error != RegexError::REG_NOERR; }

private:
    bool match(TokenType type) const;
    bool match(char ch) const;
    bool done() const;
    void expected(const char* what);
    Token consume();
    Token consume(TokenType type);
    bool consume(StringView view);

    void save_state();
    void load_state();
    bool match_meta_chars();
    bool match_ere_quoted_chars();
    bool match_ere_dupl_symbol();
    bool parse_ere_dupl_symbol(Vector<StackValue>&, size_t& min_length);
    bool parse_ere_expression(Vector<StackValue>&, size_t& min_length);
    bool parse_extended_reg_exp(Vector<StackValue>&, size_t& min_length);
    bool parse_bracket_expression(Vector<StackValue>&, size_t& min_length);
    void reset();

    bool set_error(RegexError error);

    struct ParserState {
        Lexer m_lexer;
        Token m_current_token;
        RegexError m_error = RegexError::REG_NOERR;
        Token m_error_token { TokenType::Eof, 0, StringView(nullptr) };
        Vector<StackValue> m_bytes;
        size_t m_match_groups { 0 };
        size_t m_min_match_length { 0 };
        u8 m_compilation_flags { 0 };
        explicit ParserState(Lexer);
    };

    ParserState m_parser_state;
    Optional<ParserState> m_saved_state;
};

class VM {
public:
    explicit VM(const Vector<StackValue>& bytecode, const String&& pattern, const u8 compilation_flags)
        : m_bytecode(bytecode)
        , m_pattern(move(pattern))
        , m_compilation_flags(compilation_flags) {};

    struct Match {
        ptrdiff_t rm_so;    // byte offset from start of string to start of substring
        ptrdiff_t rm_eo;    // byte offset from start of string of the first character after the end of substring
        size_t match_count; // number of matches, normally 1, could be greater if REG_NEWLINE or REG_MATCHALL set.
        StringView view;    // view of the match into the string
    };

    struct MatchResult {
        size_t m_match_count { 0 };
        Vector<Match> m_matches {};
        size_t m_ops { 0 };
    };

    MatchResult match(const StringView view, const size_t max_matches_result, const size_t match_groups, const size_t min_length, const u8 match_flags) const;
    const Vector<StackValue>& bytes() const { return m_bytecode; }

private:
    struct ForkStayTuple {
        size_t m_instructionp { 0 };
        size_t m_stringp { 0 };
    };

    struct MatchState {
        StringView m_view;
        size_t m_instructionp { 0 };
        size_t m_stringp { 0 };
        size_t m_ops { 0 };
        size_t m_matches_offset { 0 };
        Vector<Match> m_matches;
        Vector<ptrdiff_t> m_left;
        u8 m_match_flags;

        MatchState() = default;
    };

    bool match_recurse(MatchState& state, size_t recursion_level = 0) const;
    const StackValue get(MatchState& state, size_t offset = 0) const;
    const StackValue get_and_increment(MatchState& state, size_t value = 1) const;

    const Vector<StackValue> m_bytecode;
    const String& m_pattern;
    const u8 m_compilation_flags;
};
}
}

//using AK::regex::;
