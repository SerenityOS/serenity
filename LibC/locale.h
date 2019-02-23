#pragma once

#include <sys/cdefs.h>

enum {
    LC_ALL,
    LC_NUMERIC,
    LC_CTYPE,
    LC_COLLATE,
};

__BEGIN_DECLS

char* setlocale(int category, const char* locale);

__END_DECLS

