#include <assert.h>
#include <locale.h>
#include <stdio.h>

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

char* setlocale(int category, const char* locale)
{
    dbgprintf("FIXME(LibC): setlocale(%d, %s)\n", category, locale);
    return nullptr;
}

struct lconv* localeconv()
{
    return &default_locale;
}
}
