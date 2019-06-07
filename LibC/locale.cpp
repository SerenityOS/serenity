#include <assert.h>
#include <locale.h>
#include <stdio.h>

extern "C" {

static struct lconv default_locale = {
    ".",
    ",",
    "\x03\x03",
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
