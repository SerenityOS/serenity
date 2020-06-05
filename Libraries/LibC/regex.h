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

#include <AK/OwnPtr.h>
#include <LibRegex/Regex.h>
#include <LibRegex/RegexOptions.h>
#include <LibRegex/RegexParser.h>
#include <stddef.h>

__BEGIN_DECLS

// The following constants are defined as error return values:
enum ReError {
    REG_NOERR = (FlagsUnderlyingType)Error::NoError,
    REG_BADPAT = (FlagsUnderlyingType)Error::InvalidPattern,            // Invalid regular expression.
    REG_ECOLLATE = (FlagsUnderlyingType)Error::InvalidCollationElement, // Invalid collating element referenced.
    REG_ECTYPE = (FlagsUnderlyingType)Error::InvalidCharacterClass,     // Invalid character class type referenced.
    REG_EESCAPE = (FlagsUnderlyingType)Error::InvalidTrailingEscape,    // Trailing \ in pattern.
    REG_ESUBREG = (FlagsUnderlyingType)Error::InvalidNumber,            // Number in \digit invalid or in error.
    REG_EBRACK = (FlagsUnderlyingType)Error::MismatchingBracket,        // [ ] imbalance.
    REG_EPAREN = (FlagsUnderlyingType)Error::MismatchingParen,          // \( \) or ( ) imbalance.
    REG_EBRACE = (FlagsUnderlyingType)Error::MismatchingBrace,          // \{ \} imbalance.
    REG_BADBR = (FlagsUnderlyingType)Error::InvalidBraceContent,        // Content of \{ \} invalid: not a number, number too large, more than two numbers, first larger than second.
    REG_ERANGE = (FlagsUnderlyingType)Error::InvalidRange,              // Invalid endpoint in range expression.
    REG_BADRPT = (FlagsUnderlyingType)Error::InvalidRepetitionMarker,   // ?, * or + not preceded by valid regular expression.
    REG_EMPTY_EXPR = (FlagsUnderlyingType)Error::EmptySubExpression,    // Empty expression
    REG_ENOSYS,                                                         // The implementation does not support the function.
    REG_ESPACE,                                                         // Out of memory.
    REG_NOMATCH,                                                        // regexec() failed to match.
};

struct regex_t {
    u8 cflags;
    u8 eflags;
    OwnPtr<Regex<PosixExtended>> re;
    size_t re_pat_errpos;
    ReError re_pat_err;
    String re_pat;
    size_t re_nsub;
};

struct regmatch_t {
    ssize_t rm_so;  // byte offset from start of string to start of substring
    ssize_t rm_eo;  // byte offset from start of string of the first character after the end of substring
    ssize_t rm_cnt; // number of matches
};

// Values for the cflags parameter to the regcomp() function:
#define REG_EXTENDED (int)regex::PosixFlags::Extended              // Use Extended Regular Expressions.
#define REG_ICASE (int)regex::PosixFlags::Insensitive              // Ignore case in match.
#define REG_NOSUB (int)regex::PosixFlags::SkipSubExprResults       // Report only success or fail in regexec().
#define REG_GLOBAL (int)regex::PosixFlags::Global                  // Don't stop searching for more match
#define REG_NEWLINE (int)regex::PosixFlags::Multiline | REG_GLOBAL // Change the handling of newline.

// Values for the eflags parameter to the regexec() function:
#define REG_NOTBOL (int)regex::PosixFlags::MatchNotBeginOfLine // The circumflex character (^), when taken as a special character, will not match the beginning of string.
#define REG_NOTEOL (int)regex::PosixFlags::MatchNotEndOfLine   // The dollar sign ($), when taken as a special character, will not match the end of string.

//static_assert (sizeof(FlagsUnderlyingType) * 8 >= regex::POSIXFlags::Last << 1), "flags type too small")
#define REG_SEARCH (int)regex::AllFlags::Last << 1

int regcomp(regex_t*, const char*, int);
int regexec(const regex_t*, const char*, size_t, regmatch_t[], int);
size_t regerror(int, const regex_t*, char*, size_t);
void regfree(regex_t*);

__END_DECLS
