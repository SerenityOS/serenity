/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <assert.h>
#include <ctype.h>
#include <wchar.h>

__BEGIN_DECLS

typedef long wctrans_t;

int iswalnum(wint_t wc);
int iswalpha(wint_t wc);
int iswcntrl(wint_t wc);
int iswdigit(wint_t wc);
int iswxdigit(wint_t wc);
int iswspace(wint_t wc);
int iswpunct(wint_t wc);
int iswprint(wint_t wc);
int iswgraph(wint_t wc);
int iswlower(wint_t wc);
int iswupper(wint_t wc);
int iswblank(wint_t wc);
int iswctype(wint_t, wctype_t);
wctype_t wctype(const char*);
wint_t towlower(wint_t wc);
wint_t towupper(wint_t wc);
wint_t towctrans(wint_t, wctrans_t);
wctrans_t wctrans(const char*);

__END_DECLS
