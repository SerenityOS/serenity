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

#include <stddef.h>
#include <sys/types.h>

__BEGIN_DECLS

typedef ssize_t regoff_t;

struct regex_t {
    void* __data;
};

enum __Regex_Error {
    __Regex_NoError,
    __Regex_InvalidPattern,             // Invalid regular expression.
    __Regex_InvalidCollationElement,    // Invalid collating element referenced.
    __Regex_InvalidCharacterClass,      // Invalid character class type referenced.
    __Regex_InvalidTrailingEscape,      // Trailing \ in pattern.
    __Regex_InvalidNumber,              // Number in \digit invalid or in error.
    __Regex_MismatchingBracket,         // [ ] imbalance.
    __Regex_MismatchingParen,           // ( ) imbalance.
    __Regex_MismatchingBrace,           // { } imbalance.
    __Regex_InvalidBraceContent,        // Content of {} invalid: not a number, number too large, more than two numbers, first larger than second.
    __Regex_InvalidBracketContent,      // Content of [] invalid.
    __Regex_InvalidRange,               // Invalid endpoint in range expression.
    __Regex_InvalidRepetitionMarker,    // ?, * or + not preceded by valid regular expression.
    __Regex_ReachedMaxRecursion,        // MaximumRecursion has been reached.
    __Regex_EmptySubExpression,         // Sub expression has empty content.
    __Regex_InvalidCaptureGroup,        // Content of capture group is invalid.
    __Regex_InvalidNameForCaptureGroup, // Name of capture group is invalid.
};

enum ReError {
    REG_NOERR = __Regex_NoError,
    REG_BADPAT = __Regex_InvalidPattern,            // Invalid regular expression.
    REG_ECOLLATE = __Regex_InvalidCollationElement, // Invalid collating element referenced.
    REG_ECTYPE = __Regex_InvalidCharacterClass,     // Invalid character class type referenced.
    REG_EESCAPE = __Regex_InvalidTrailingEscape,    // Trailing \ in pattern.
    REG_ESUBREG = __Regex_InvalidNumber,            // Number in \digit invalid or in error.
    REG_EBRACK = __Regex_MismatchingBracket,        // [ ] imbalance.
    REG_EPAREN = __Regex_MismatchingParen,          // \( \) or ( ) imbalance.
    REG_EBRACE = __Regex_MismatchingBrace,          // \{ \} imbalance.
    REG_BADBR = __Regex_InvalidBraceContent,        // Content of \{ \} invalid: not a number, number too large, more than two numbers, first larger than second.
    REG_ERANGE = __Regex_InvalidRange,              // Invalid endpoint in range expression.
    REG_BADRPT = __Regex_InvalidRepetitionMarker,   // ?, * or + not preceded by valid regular expression.
    REG_EMPTY_EXPR = __Regex_EmptySubExpression,    // Empty expression
    REG_ENOSYS,                                     // The implementation does not support the function.
    REG_ESPACE,                                     // Out of memory.
    REG_NOMATCH,                                    // regexec() failed to match.
};

struct regmatch_t {
    regoff_t rm_so;  // byte offset from start of string to start of substring
    regoff_t rm_eo;  // byte offset from start of string of the first character after the end of substring
    regoff_t rm_cnt; // number of matches
};

enum __RegexAllFlags {
    __Regex_Global = 1,                                      // All matches (don't return after first match)
    __Regex_Insensitive = __Regex_Global << 1,               // Case insensitive match (ignores case of [a-zA-Z])
    __Regex_Ungreedy = __Regex_Global << 2,                  // The match becomes lazy by default. Now a ? following a quantifier makes it greedy
    __Regex_Unicode = __Regex_Global << 3,                   // Enable all unicode features and interpret all unicode escape sequences as such
    __Regex_Extended = __Regex_Global << 4,                  // Ignore whitespaces. Spaces and text after a # in the pattern are ignored
    __Regex_Extra = __Regex_Global << 5,                     // Disallow meaningless escapes. A \ followed by a letter with no special meaning is faulted
    __Regex_MatchNotBeginOfLine = __Regex_Global << 6,       // Pattern is not forced to ^ -> search in whole string!
    __Regex_MatchNotEndOfLine = __Regex_Global << 7,         // Don't Force the dollar sign, $, to always match end of the string, instead of end of the line. This option is ignored if the Multiline-flag is set
    __Regex_SkipSubExprResults = __Regex_Global << 8,        // Do not return sub expressions in the result
    __Regex_StringCopyMatches = __Regex_Global << 9,         // Do explicitly copy results into new allocated string instead of StringView to original string.
    __Regex_SingleLine = __Regex_Global << 10,               // Dot matches newline characters
    __Regex_Sticky = __Regex_Global << 11,                   // Force the pattern to only match consecutive matches from where the previous match ended.
    __Regex_Multiline = __Regex_Global << 12,                // Handle newline characters. Match each line, one by one.
    __Regex_SkipTrimEmptyMatches = __Regex_Global << 13,     // Do not remove empty capture group results.
    __Regex_Internal_Stateful = __Regex_Global << 14,        // Internal flag; enables stateful matches.
    __Regex_Internal_BrowserExtended = __Regex_Global << 15, // Internal flag; enable browser-specific ECMA262 extensions.
    __Regex_Last = __Regex_SkipTrimEmptyMatches
};

// Values for the cflags parameter to the regcomp() function:
#define REG_EXTENDED __Regex_Extended                // Use Extended Regular Expressions.
#define REG_ICASE __Regex_Insensitive                // Ignore case in match.
#define REG_NOSUB __Regex_SkipSubExprResults         // Report only success or fail in regexec().
#define REG_GLOBAL __Regex_Global                    // Don't stop searching for more match
#define REG_NEWLINE (__Regex_Multiline | REG_GLOBAL) // Change the handling of newline.

// Values for the eflags parameter to the regexec() function:
#define REG_NOTBOL __Regex_MatchNotBeginOfLine // The circumflex character (^), when taken as a special character, will not match the beginning of string.
#define REG_NOTEOL __Regex_MatchNotEndOfLine   // The dollar sign ($), when taken as a special character, will not match the end of string.

//static_assert (sizeof(FlagsUnderlyingType) * 8 >= regex::POSIXFlags::Last << 1), "flags type too small")
#define REG_SEARCH __Regex_Last << 1

int regcomp(regex_t*, const char*, int);
int regexec(const regex_t*, const char*, size_t, regmatch_t[], int);
size_t regerror(int, const regex_t*, char*, size_t);
void regfree(regex_t*);

__END_DECLS
