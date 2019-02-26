#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

enum {
    LC_ALL,
    LC_NUMERIC,
    LC_CTYPE,
    LC_COLLATE,
    LC_TIME,
};

struct lconv {
    char *decimal_point;
};

struct lconv* localeconv();
char* setlocale(int category, const char* locale);

__END_DECLS

