/*
 * Copyright (c) 2021, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <wctype.h>

extern "C" {

int iswalnum(wint_t wc)
{
    return __inline_isalnum(wc);
}

int iswalpha(wint_t wc)
{
    return __inline_isalpha(wc);
}

int iswcntrl(wint_t wc)
{
    return __inline_iscntrl(wc);
}

int iswdigit(wint_t wc)
{
    return __inline_isdigit(wc);
}

int iswxdigit(wint_t wc)
{
    return __inline_isxdigit(wc);
}

int iswspace(wint_t wc)
{
    return __inline_isspace(wc);
}

int iswpunct(wint_t wc)
{
    return __inline_ispunct(wc);
}

int iswprint(wint_t wc)
{
    return __inline_isprint(wc);
}

int iswgraph(wint_t wc)
{
    return __inline_isgraph(wc);
}

int iswlower(wint_t wc)
{
    return __inline_islower(wc);
}

int iswupper(wint_t wc)
{
    return __inline_isupper(wc);
}

int iswblank(wint_t wc)
{
    return __inline_isblank(wc);
}

int iswctype(wint_t, wctype_t)
{
    dbgln("FIXME: Implement iswctype()");
    TODO();
}

wctype_t wctype(char const*)
{
    dbgln("FIXME: Implement wctype()");
    TODO();
}

wint_t towlower(wint_t wc)
{
    return __inline_tolower(wc);
}

wint_t towupper(wint_t wc)
{
    return __inline_toupper(wc);
}

wint_t towctrans(wint_t, wctrans_t)
{
    dbgln("FIXME: Implement towctrans()");
    TODO();
}

wctrans_t wctrans(char const*)
{
    dbgln("FIXME: Implement wctrans()");
    TODO();
}
}
