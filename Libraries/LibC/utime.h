#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

int utime(const char* pathname, const struct utimbuf*);

__END_DECLS
