#include <sys/cdefs.h>

#pragma once

__BEGIN_DECLS

#define bswap_16(x) (__builtin_bswap16(x))
#define bswap_32(x) (__builtin_bswap32(x))
#define bswap_64(x) (__builtin_bswap64(x))

__END_DECLS
