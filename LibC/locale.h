#pragma once

#include <sys/cdefs.h>

enum {
    LC_ALL,
    LC_NUMERIC,
};

__BEGIN_DECLS

char* setlocale(int category, const char* locale);

__END_DECLS

