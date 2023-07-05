/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibRegex/RegexDefs.h>
#include <stddef.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

typedef ssize_t regoff_t;

typedef struct {
    void* __data;
    // Number of capture groups, Dr.POSIX requires this.
    size_t re_nsub;
} regex_t;

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

typedef struct {
    regoff_t rm_so;  // byte offset from start of string to start of substring
    regoff_t rm_eo;  // byte offset from start of string of the first character after the end of substring
    regoff_t rm_cnt; // number of matches
} regmatch_t;

// Values for the cflags parameter to the regcomp() function:
#define REG_EXTENDED __Regex_Extended                // Use Extended Regular Expressions.
#define REG_ICASE __Regex_Insensitive                // Ignore case in match.
#define REG_NOSUB __Regex_SkipSubExprResults         // Report only success or fail in regexec().
#define REG_GLOBAL __Regex_Global                    // Don't stop searching for more match
#define REG_NEWLINE (__Regex_Multiline | REG_GLOBAL) // Change the handling of newline.

// Values for the eflags parameter to the regexec() function:
#define REG_NOTBOL __Regex_MatchNotBeginOfLine // The circumflex character (^), when taken as a special character, will not match the beginning of string.
#define REG_NOTEOL __Regex_MatchNotEndOfLine   // The dollar sign ($), when taken as a special character, will not match the end of string.

#define REG_SEARCH __Regex_Last << 1

int regcomp(regex_t*, char const*, int);
int regexec(regex_t const*, char const*, size_t, regmatch_t[], int);
size_t regerror(int, regex_t const*, char*, size_t);
void regfree(regex_t*);

__END_DECLS
