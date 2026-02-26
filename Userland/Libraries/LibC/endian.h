/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

// https://pubs.opengroup.org/onlinepubs/9799919799/basedefs/endian.h.html

// "Inclusion of the <endian.h> header may also make visible all symbols from <stdint.h>."
#include <stdint.h>

__BEGIN_DECLS

// "LITTLE_ENDIAN
//      If BYTE_ORDER == LITTLE_ENDIAN, the host byte order is from least significant to most significant."
#define LITTLE_ENDIAN __ORDER_LITTLE_ENDIAN__

// "BIG_ENDIAN
//      If BYTE_ORDER == BIG_ENDIAN, the host byte order is from most significant to least significant."
#define BIG_ENDIAN __ORDER_BIG_ENDIAN__

// "BYTE_ORDER
//      This macro shall have a value equal to one of the *_ENDIAN macros in this header."
#define BYTE_ORDER __BYTE_ORDER__

// These definitions are not required by POSIX, but are assumed to be present by some ports.
#define __BYTE_ORDER BYTE_ORDER
#define __LITTLE_ENDIAN LITTLE_ENDIAN
#define __BIG_ENDIAN BIG_ENDIAN

#if BYTE_ORDER == LITTLE_ENDIAN
#    define htole16(x) ((uint16_t)(x))
#    define le16toh(x) ((uint16_t)(x))
#    define htole32(x) ((uint32_t)(x))
#    define le32toh(x) ((uint32_t)(x))
#    define htole64(x) ((uint64_t)(x))
#    define le64toh(x) ((uint64_t)(x))
#    define htobe16(x) (__builtin_bswap16(x))
#    define be16toh(x) (__builtin_bswap16(x))
#    define htobe32(x) (__builtin_bswap32(x))
#    define be32toh(x) (__builtin_bswap32(x))
#    define htobe64(x) (__builtin_bswap64(x))
#    define be64toh(x) (__builtin_bswap64(x))
#else
#    define htole16(x) (__builtin_bswap16(x))
#    define le16toh(x) (__builtin_bswap16(x))
#    define htole32(x) (__builtin_bswap32(x))
#    define le32toh(x) (__builtin_bswap32(x))
#    define htole64(x) (__builtin_bswap64(x))
#    define le64toh(x) (__builtin_bswap64(x))
#    define htobe16(x) ((uint16_t)(x))
#    define be16toh(x) ((uint16_t)(x))
#    define htobe32(x) ((uint32_t)(x))
#    define be32toh(x) ((uint32_t)(x))
#    define htobe64(x) ((uint64_t)(x))
#    define be64toh(x) ((uint64_t)(x))
#endif

__END_DECLS
