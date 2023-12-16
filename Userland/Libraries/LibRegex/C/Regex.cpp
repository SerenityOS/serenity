/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/StringBuilder.h>
#include <AK/Variant.h>
#include <LibRegex/Regex.h>
#include <ctype.h>
#include <regex.h>
#include <stdio.h>
#include <string.h>

#ifndef AK_OS_SERENITY
#    error "This file is intended for use on Serenity only to implement POSIX regex.h"
#endif

struct internal_regex_t {
    u8 cflags;
    u8 eflags;
    Optional<Variant<NonnullOwnPtr<Regex<PosixExtended>>, NonnullOwnPtr<Regex<PosixBasic>>>> re;
    size_t re_pat_errpos;
    ReError re_pat_err;
    ByteString re_pat;
};

static internal_regex_t* impl_from(regex_t* re)
{
    if (!re)
        return nullptr;

    return reinterpret_cast<internal_regex_t*>(re->__data);
}

static internal_regex_t const* impl_from(regex_t const* re)
{
    return impl_from(const_cast<regex_t*>(re));
}

extern "C" {

int regcomp(regex_t* reg, char const* pattern, int cflags)
{
    if (!reg)
        return REG_ESPACE;

    // Note that subsequent uses of regcomp() without regfree() _will_ leak memory
    // This could've been prevented if libc provided a reginit() or similar, but it does not.
    reg->__data = new internal_regex_t { 0, 0, {}, 0, ReError::REG_NOERR, {} };

    auto* preg = impl_from(reg);
    bool is_extended = cflags & REG_EXTENDED;

    preg->cflags = cflags;

    ByteString pattern_str(pattern);
    if (is_extended)
        preg->re = make<Regex<PosixExtended>>(pattern_str, PosixOptions {} | (PosixFlags)cflags | PosixFlags::SkipTrimEmptyMatches);
    else
        preg->re = make<Regex<PosixBasic>>(pattern_str, PosixOptions {} | (PosixFlags)cflags | PosixFlags::SkipTrimEmptyMatches);

    auto parser_result = preg->re->visit([](auto& re) { return re->parser_result; });

    if (parser_result.error != regex::Error::NoError) {
        preg->re_pat_errpos = parser_result.error_token.position();
        preg->re_pat_err = (ReError)parser_result.error;
        preg->re_pat = pattern;

        dbgln("Have Error: {}", (int)parser_result.error);

        return (ReError)parser_result.error;
    }

    reg->re_nsub = parser_result.capture_groups_count;

    return REG_NOERR;
}

int regexec(regex_t const* reg, char const* string, size_t nmatch, regmatch_t pmatch[], int eflags)
{
    auto const* preg = impl_from(reg);

    if (!preg->re.has_value() || preg->re_pat_err) {
        if (preg->re_pat_err)
            return preg->re_pat_err;
        return REG_BADPAT;
    }

    RegexResult result;
    StringView string_view { string, strlen(string) };
    if (eflags & REG_SEARCH)
        result = preg->re->visit([&](auto& re) { return re->search(string_view, PosixOptions {} | (PosixFlags)eflags); });
    else
        result = preg->re->visit([&](auto& re) { return re->match(string_view, PosixOptions {} | (PosixFlags)eflags); });

    if (result.success) {
        auto capture_groups_count = preg->re->visit([](auto& re) { return re->parser_result.capture_groups_count; });
        auto size = result.matches.size();
        if (size && nmatch && pmatch) {
            pmatch[0].rm_cnt = size;

            size_t match_index { 0 };
            for (size_t i = 0; i < size; ++i) {
                pmatch[match_index].rm_so = result.matches.at(i).global_offset;
                pmatch[match_index].rm_eo = pmatch[match_index].rm_so + result.matches.at(i).view.length();
                if (match_index > 0)
                    pmatch[match_index].rm_cnt = result.capture_group_matches.size();

                ++match_index;
                if (match_index >= nmatch)
                    return REG_NOERR;

                if (i < result.capture_group_matches.size()) {
                    auto capture_groups_size = result.capture_group_matches.at(i).size();
                    for (size_t j = 0; j < capture_groups_count; ++j) {
                        if (j >= capture_groups_size || !result.capture_group_matches.at(i).at(j).view.length()) {
                            pmatch[match_index].rm_so = -1;
                            pmatch[match_index].rm_eo = -1;
                            pmatch[match_index].rm_cnt = 0;
                        } else {
                            pmatch[match_index].rm_so = result.capture_group_matches.at(i).at(j).global_offset;
                            pmatch[match_index].rm_eo = pmatch[match_index].rm_so + result.capture_group_matches.at(i).at(j).view.length();
                            pmatch[match_index].rm_cnt = 1;
                        }

                        ++match_index;
                        if (match_index >= nmatch)
                            return REG_NOERR;
                    }
                }
            }

            if (match_index < nmatch) {
                for (size_t i = match_index; i < nmatch; ++i) {
                    pmatch[i].rm_so = -1;
                    pmatch[i].rm_eo = -1;
                    pmatch[i].rm_cnt = 0;
                }
            }
        }
        return REG_NOERR;
    }

    if (nmatch && pmatch) {
        pmatch[0].rm_so = -1;
        pmatch[0].rm_eo = -1;
        pmatch[0].rm_cnt = 0;
    }

    return REG_NOMATCH;
}

static StringView get_error(ReError errcode)
{
    switch (errcode) {
    case REG_NOERR:
        return "No error"sv;
    case REG_NOMATCH:
        return "regexec() failed to match."sv;
    case REG_BADPAT:
        return "Invalid regular expression."sv;
    case REG_ECOLLATE:
        return "Invalid collating element referenced."sv;
    case REG_ECTYPE:
        return "Invalid character class type referenced."sv;
    case REG_EESCAPE:
        return "Trailing \\ in pattern."sv;
    case REG_ESUBREG:
        return "Number in \\digit invalid or in error."sv;
    case REG_EBRACK:
        return "[ ] imbalance."sv;
    case REG_EPAREN:
        return "\\( \\) or ( ) imbalance."sv;
    case REG_EBRACE:
        return "\\{ \\} imbalance."sv;
    case REG_BADBR:
        return "Content of \\{ \\} invalid: not a number, number too large, more than two numbers, first larger than second."sv;
    case REG_ERANGE:
        return "Invalid endpoint in range expression."sv;
    case REG_ESPACE:
        return "Out of memory."sv;
    case REG_BADRPT:
        return "?, * or + not preceded by valid regular expression."sv;
    case REG_ENOSYS:
        return "The implementation does not support the function."sv;
    case REG_EMPTY_EXPR:
        return "Empty expression provided"sv;
    }
    return {};
}

size_t regerror(int errcode, regex_t const* reg, char* errbuf, size_t errbuf_size)
{
    ByteString error;
    auto const* preg = impl_from(reg);

    if (!preg)
        error = get_error((ReError)errcode);
    else
        error = preg->re->visit([&](auto& re) { return re->error_string(get_error(preg->re_pat_err)); });

    if (!errbuf_size)
        return error.length();

    if (!error.copy_characters_to_buffer(errbuf, errbuf_size))
        return 0;

    return error.length();
}

void regfree(regex_t* reg)
{
    auto* preg = impl_from(reg);
    if (preg) {
        delete preg;
        reg->__data = nullptr;
    }
}
}
