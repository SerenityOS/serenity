#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

enum {
    LC_ALL,
    LC_NUMERIC,
    LC_CTYPE,
    LC_COLLATE,
};

struct lconv {
};

struct lconv* localeconv();
char* setlocale(int category, const char* locale);

__END_DECLS

