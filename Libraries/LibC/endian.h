#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#define __LITTLE_ENDIAN	1234
#define __BIG_ENDIAN	4321
#define __PDP_ENDIAN	3412

#if defined(__GNUC__) && defined(__BYTE_ORDER__)
#define __BYTE_ORDER	__BYTE_ORDER__
#else
#include <bits/endian.h>
#endif

#if defined(_GNU_SOURCE) || defined(_BSD_SOURCE)

#include <stdint.h>

#define LITTLE_ENDIAN	__LITTLE_ENDIAN
#define BIG_ENDIAN	__BIG_ENDIAN
#define PDP_ENDIAN	__PDP_ENDIAN
#define BYTE_ORDER	__BYTE_ORDER

#if __BYTE_ORDER == __LITTLE_ENDIAN
#define htole16(x) ((uint16_t)(x))
#define le16toh(x) ((uint16_t)(x))
#define letoh16(x) ((uint16_t)(x))
#define htole32(x) ((uint32_t)(x))
#define le32toh(x) ((uint32_t)(x))
#define letoh32(x) ((uint32_t)(x))
#define htole64(x) ((uint64_t)(x))
#define le64toh(x) ((uint64_t)(x))
#define letoh64(x) ((uint64_t)(x))
#define htobe16(x) (__builtin_bswap16(x))
#define be16toh(x) (__builtin_bswap16(x))
#define betoh16(x) (__builtin_bswap16(x))
#define htobe32(x) (__builtin_bswap32(x))
#define be32toh(x) (__builtin_bswap32(x))
#define betoh32(x) (__builtin_bswap32(x))
#define htobe64(x) (__builtin_bswap64(x))
#define be64toh(x) (__builtin_bswap64(x))
#define betoh64(x) (__builtin_bswap64(x))
#else
#define ltobe16(x) ((uint16_t)(x))
#define le16toh(x) ((uint16_t)(x))
#define letoh16(x) ((uint16_t)(x))
#define htole32(x) ((uint32_t)(x))
#define le32toh(x) ((uint32_t)(x))
#define letoh32(x) ((uint32_t)(x))
#define htole64(x) ((uint64_t)(x))
#define le64toh(x) ((uint64_t)(x))
#define letoh64(x) ((uint64_t)(x))
#define htole16(x) (__builtin_bswap16(x))
#define le16toh(x) (__builtin_bswap16(x))
#define letoh16(x) (__builtin_bswap16(x))
#define htole32(x) (__builtin_bswap32(x))
#define le32toh(x) (__builtin_bswap32(x))
#define letoh32(x) (__builtin_bswap32(x))
#define htole64(x) (__builtin_bswap64(x))
#define le64toh(x) (__builtin_bswap64(x))
#define letoh64(x) (__builtin_bswap64(x))
#endif

#endif

__END_DECLS
