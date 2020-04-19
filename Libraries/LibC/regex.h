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
#include <AK/Regex.h>
#include <stddef.h>

namespace AK {
namespace regex {
class Matcher;
class Token;
}
}

__BEGIN_DECLS

// The following constants are defined as error return values:
enum ReError {
    REG_NOERR = (u8)RegexError::NoError,
    REG_NOMATCH = (u8)RegexError::NoMatch,                  // regexec() failed to match.
    REG_BADPAT = (u8)RegexError::InvalidPattern,            // Invalid regular expression.
    REG_ECOLLATE = (u8)RegexError::InvalidCollationElement, // Invalid collating element referenced.
    REG_ECTYPE = (u8)RegexError::InvalidCharacterClass,     // Invalid character class type referenced.
    REG_EESCAPE = (u8)RegexError::InvalidTrailingEscape,    // Trailing \ in pattern.
    REG_ESUBREG = (u8)RegexError::InvalidNumber,            // Number in \digit invalid or in error.
    REG_EBRACK = (u8)RegexError::BracketMismatch,           // [ ] imbalance.
    REG_EPAREN = (u8)RegexError::ParenMismatch,             // \( \) or ( ) imbalance.
    REG_EBRACE = (u8)RegexError::BraceMismatch,             // \{ \} imbalance.
    REG_BADBR = (u8)RegexError::InvalidBraceContent,        // Content of \{ \} invalid: not a number, number too large, more than two numbers, first larger than second.
    REG_ERANGE = (u8)RegexError::InvalidRange,              // Invalid endpoint in range expression.
    REG_ESPACE = (u8)RegexError::OutOfMemory,               // Out of memory.
    REG_BADRPT = (u8)RegexError::InvalidRepetitionMarker,   // ?, * or + not preceded by valid regular expression.
    REG_ENOSYS = (u8)RegexError::NotImplemented,            // The implementation does not support the function.
};

struct regex_t {
    size_t re_nsub;
    u8 cflags;
    u8 eflags;
    size_t re_minlength;
    OwnPtr<AK::regex::Matcher> matcher;
    size_t re_pat_errpos { 0 };
    ReError re_pat_err;
    String re_pat;
};

typedef ptrdiff_t regoff_t;

struct regmatch_t {
    regoff_t rm_so;     // byte offset from start of string to start of substring
    regoff_t rm_eo;     // byte offset from start of string of the first character after the end of substring
    size_t match_count; // number of matches, normally 1, could be greater if REG_NEWLINE or REG_MATCHALL set.
};

// Values for the cflags parameter to the regcomp() function:
#define REG_EXTENDED (u8) AK::regex::CompilationFlags::Extended      // Use Extended Regular Expressions.
#define REG_ICASE (u8) AK::regex::CompilationFlags::IgnoreCase       // Ignore case in match.
#define REG_NOSUB (u8) AK::regex::CompilationFlags::NoSubExpressions // Report only success or fail in regexec().
#define REG_NEWLINE (u8) AK::regex::CompilationFlags::HandleNewLine  // Change the handling of newline.

// Values for the eflags parameter to the regexec() function:
#define REG_NOTBOL (u8) AK::regex::MatchFlags::NoBeginOfLine // The circumflex character (^), when taken as a special character, will not match the beginning of string.
#define REG_NOTEOL (u8) AK::regex::MatchFlags::NoEndOfLine   // The dollar sign ($), when taken as a special character, will not match the end of string.
#define REG_MATCHALL (u8) AK::regex::MatchFlags::MatchAll    // Match all occurences of the character - extension to posix
#define REG_SEARCH (u8) AK::regex::MatchFlags::Search        // Do try to match the pattern anyhwere in the string
#define REG_STATS (u8) AK::regex::MatchFlags::Stats          // Print stats for a match to stdout

int regcomp(regex_t*, const char*, int);
int regexec(const regex_t*, const char*, size_t, regmatch_t[], int);
size_t regerror(int, const regex_t*, char*, size_t);
void regfree(regex_t*);

__END_DECLS
