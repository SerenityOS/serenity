/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

enum {
    LC_ALL,
#define LC_ALL LC_ALL
    LC_NUMERIC,
#define LC_NUMERIC LC_NUMERIC
    LC_CTYPE,
#define LC_CTYPE LC_CTYPE
    LC_COLLATE,
#define LC_COLLATE LC_COLLATE
    LC_TIME,
#define LC_TIME LC_TIME
    LC_MONETARY,
#define LC_MONETARY LC_MONETARY
    LC_MESSAGES,
#define LC_MESSAGES LC_MESSAGES
};

struct lconv {
    char* decimal_point;
    char* thousands_sep;
    char* grouping;
    char* int_curr_symbol;
    char* currency_symbol;
    char* mon_decimal_point;
    char* mon_thousands_sep;
    char* mon_grouping;
    char* positive_sign;
    char* negative_sign;
    char int_frac_digits;
    char frac_digits;
    char p_cs_precedes;
    char p_sep_by_space;
    char n_cs_precedes;
    char n_sep_by_space;
    char p_sign_posn;
    char n_sign_posn;
    char int_p_cs_precedes;
    char int_p_sep_by_space;
    char int_n_cs_precedes;
    char int_n_sep_by_space;
    char int_p_sign_posn;
    char int_n_sign_posn;
};

struct lconv* localeconv(void);
char* setlocale(int category, char const* locale);

__END_DECLS
