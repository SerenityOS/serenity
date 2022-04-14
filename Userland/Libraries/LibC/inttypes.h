/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/stdint.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#ifdef __i386__
#    define __PRI64_PREFIX "ll"
#    define __PRIPTR_PREFIX
#else
#    define __PRI64_PREFIX "l"
#    define __PRIPTR_PREFIX "l"
#endif

#define PRId8 "d"
#define PRId16 "d"
#define PRId32 "d"
#define PRId64 __PRI64_PREFIX "d"
#define PRIdPTR __PRIPTR_PREFIX "d"
#define PRIdMAX __PRI64_PREFIX "d"

#define PRIi8 "d"
#define PRIi16 "d"
#define PRIi32 "d"
#define PRIi64 __PRI64_PREFIX "d"
#define PRIiPTR __PRIPTR_PREFIX "d"
#define PRIiMAX __PRI64_PREFIX "d"

#define PRIo8 "o"
#define PRIo16 "o"
#define PRIo32 "o"
#define PRIo64 __PRI64_PREFIX "o"
#define PRIoPTR __PRIPTR_PREFIX "o"
#define PRIoMAX __PRI64_PREFIX "o"

#define PRIu8 "u"
#define PRIu16 "u"
#define PRIu32 "u"
#define PRIu64 __PRI64_PREFIX "u"
#define PRIuPTR __PRIPTR_PREFIX "u"
#define PRIuMAX __PRI64_PREFIX "u"

#define PRIx8 "x"
#define PRIx16 "x"
#define PRIx32 "x"
#define PRIx64 __PRI64_PREFIX "x"
#define PRIxPTR __PRIPTR_PREFIX "x"
#define PRIxMAX __PRI64_PREFIX "x"

#define PRIX8 "X"
#define PRIX16 "X"
#define PRIX32 "X"
#define PRIX64 __PRI64_PREFIX "X"
#define PRIXPTR __PRIPTR_PREFIX "X"
#define PRIXMAX __PRI64_PREFIX "X"

#define SCNd8 "hhd"
#define SCNd16 "hd"
#define SCNd32 "ld"
#define SCNd64 __PRI64_PREFIX "d"
#define SCNdPTR __PRIPTR_PREFIX "d"
#define SCNdMAX __PRI64_PREFIX "d"

#define SCNi8 "hhi"
#define SCNi16 "hi"
#define SCNi32 "li"
#define SCNi64 __PRI64_PREFIX "i"
#define SCNiPTR __PRIPTR_PREFIX "i"
#define SCNiMAX __PRI64_PREFIX "i"

#define SCNu8 "hhu"
#define SCNu16 "hu"
#define SCNu32 "lu"
#define SCNu64 __PRI64_PREFIX "u"
#define SCNuPTR __PRIPTR_PREFIX "u"
#define SCNuMAX __PRI64_PREFIX "u"

#define SCNo8 "hho"
#define SCNo16 "ho"
#define SCNo32 "lo"
#define SCNo64 __PRI64_PREFIX "o"
#define SCNoPTR __PRIPTR_PREFIX "o"
#define SCNoMAX __PRI64_PREFIX "o"

#define SCNx8 "hhx"
#define SCNx16 "hx"
#define SCNx32 "lx"
#define SCNx64 __PRI64_PREFIX "x"
#define SCNxPTR __PRIPTR_PREFIX "x"
#define SCNxMAX __PRI64_PREFIX "x"

typedef struct imaxdiv_t {
    intmax_t quot;
    intmax_t rem;
} imaxdiv_t;
imaxdiv_t imaxdiv(intmax_t, intmax_t);

intmax_t strtoimax(char const*, char** endptr, int base);
uintmax_t strtoumax(char const*, char** endptr, int base);

__END_DECLS
