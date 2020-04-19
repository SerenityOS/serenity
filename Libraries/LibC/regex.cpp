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
#include <AK/Regex.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <ctype.h>
#include <stdio.h>

extern "C" {

int regcomp(regex_t* preg, const char* pattern, int cflags)
{
    *preg = { 0, 0, 0, 0, nullptr, 0, REG_NOERR, "" };

    if (!(cflags & REG_EXTENDED))
        return REG_ENOSYS;

    preg->cflags = cflags;

    String pattern_str(pattern);
    AK::regex::Lexer lexer(pattern);

#ifdef REGEX_DEBUG
    printf("[LEXER] Tokens for pattern '%s':\n", pattern);
    while (true) {
        AK::regex::Token token = lexer.next();
        if (token.type() == AK::regex::TokenType::Eof)
            break;

        String a(token.value());
        printf("[LEXER] %s -> %s\n", token.name(), a.characters());
    }
    lexer.reset();
#endif

    AK::regex::Parser parser(move(lexer));
    auto result = parser.parse(cflags);

#ifdef REGEX_DEBUG
    int i = 0;
    for (auto& item : result.m_bytes) {
        printf("[PARSER] [%i]: %i\n", i, item.number);
        i++;
    }
#endif

    if (result.m_error != AK::regex::RegexError::NoError) {
        preg->re_pat_errpos = result.m_error_token.position();
        preg->re_pat_err = (ReError)result.m_error;
        preg->re_pat = pattern;

        return (ReError)result.m_error;
    }

    preg->re_nsub = result.m_match_groups;
    preg->re_minlength = result.m_min_match_length;

#ifdef REGEX_DEBUG
    printf("Minlength for pattern '%s' = %lu\n", pattern, preg->re_minlength);
#endif

    preg->matcher = make<AK::regex::Matcher>(result.m_bytes, move(pattern_str), (u8)cflags);
    return REG_NOERR;
}


int regexec(const regex_t* preg, const char* string, size_t nmatch, regmatch_t pmatch[], int eflags)
{
    if (!preg->matcher || preg->re_pat_err) {
        if (preg->re_pat_err)
            return preg->re_pat_err;
        return REG_BADPAT;
    }

    AK::regex::MatchResult result;
    auto& matcher = preg->matcher;
    result = matcher->match(string, nmatch, preg->re_nsub, preg->re_minlength, eflags);

    if (result.match_count) {
        auto size = result.matches.size();
        for (size_t i = 0; i < nmatch; ++i) {
            if (i < size) {
                pmatch[i].rm_eo = result.matches.at(i).rm_eo;
                pmatch[i].rm_so = result.matches.at(i).rm_so;
                pmatch[i].match_count = result.matches.at(i).match_count;
            } else
                pmatch[i] = { -1, -1, 0 };
        }
    }

    if (nmatch && pmatch)
        pmatch[0].match_count = result.match_count;

    if (result.match_count) {
        if (eflags & REG_STATS) {
            if (eflags & REG_MATCHALL)
                printf("[regexec] match_all successful, found %lu occurences, took %lu operations.\n", result.match_count, result.ops);
            else
                printf("[regexec] match successful, took %lu operations.\n", result.ops);
        }

        return REG_NOERR;
    }

    for (size_t i = 0; i < nmatch; ++i)
        pmatch[i] = { -1, -1, 0 };

    if (eflags & REG_STATS) {
        if (eflags & REG_MATCHALL)
            printf("[regexec] match_all not successful, found %lu occurences, took %lu operations.\n", result.match_count, result.ops);
        else
            printf("[regexec] match not successful, took %lu operations.\n", result.ops);
    }

    return REG_NOMATCH;
}

inline static String get_error(ReError errcode)
{
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
    return error;
}

size_t regerror(int errcode, const regex_t* preg, char* errbuf, size_t errbuf_size)
{
    String error;

    if (preg && preg->re_pat != nullptr && preg->re_pat_err != REG_NOERR && preg->re_pat_errpos) {
        StringBuilder eb;
        eb.appendf("Error in Regular Expression:\n");
        eb.appendf("    %s\n    ", preg->re_pat.characters());
        for (size_t i = 0; i < preg->re_pat_errpos - 1; ++i)
            eb.append(" ");
        eb.appendf("^---- %s\n", get_error(preg->re_pat_err).characters());
        error = eb.build();
    } else
        error = get_error((ReError)errcode);

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
    preg->matcher.clear();
}
}
