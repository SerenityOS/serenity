/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <assert.h>
#include <locale.h>
#include <stdio.h>
#include <string.h>

extern "C" {

static char default_decimal_point[] = ".";
static char default_thousands_sep[] = ",";
static char default_grouping[] = "\x03\x03";

static char default_empty_string[] = "";
static char default_empty_value = 127;

static struct lconv default_locale = {
    default_decimal_point,
    default_thousands_sep,
    default_grouping,
    default_empty_string,
    default_empty_string,
    default_empty_string,
    default_empty_string,
    default_empty_string,
    default_empty_string,
    default_empty_string,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value,
    default_empty_value
};

struct __locale_t_impl {
    int category_mask {};
};

char* setlocale(int, char const* locale)
{
    static char c_locale_string[2];
    memcpy(c_locale_string, "C", 2);

    // If we get a null pointer, return the current locale as per POSIX spec.
    if (locale == nullptr)
        return c_locale_string;

    if (strcmp(locale, "POSIX") == 0 || strcmp(locale, "C") == 0 || strcmp(locale, "") == 0)
        return c_locale_string;

    return nullptr;
}

struct lconv* localeconv()
{
    return &default_locale;
}

// https://pubs.opengroup.org/onlinepubs/9799919799/functions/freelocale.html
void freelocale(locale_t locale)
{
    delete locale;
}

char const* getlocalename_l(int, locale_t)
{
    // FIXME: Implement this.
    return "C";
}

// https://pubs.opengroup.org/onlinepubs/9799919799/functions/newlocale.html
locale_t newlocale(int category_mask, char const* locale, locale_t base)
{
    if (!base && (locale == "POSIX"sv || locale == "C"sv || locale == ""sv))
        return new __locale_t_impl { category_mask };

    return nullptr;
}

locale_t uselocale(locale_t loc)
{
    // FIXME: Implement this.
    return loc;
}
}
