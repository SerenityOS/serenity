/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/stdint.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#define PRId8 "d"
#define PRId16 "d"
#define PRId32 "d"
#define PRId64 "lld"
#define PRIi8 "d"
#define PRIi16 "d"
#define PRIi32 "d"
#ifndef __x86_64__
#    define PRIi64 "lld"
#else
#    define PRIi64 "ld"
#endif
#define PRIu8 "u"
#define PRIo8 "o"
#define PRIo16 "o"
#define PRIo32 "o"
#ifndef __x86_64__
#    define PRIo64 "llo"
#else
#    define PRIo64 "lo"
#endif
#define PRIu16 "u"
#define PRIu32 "u"
#ifndef __x86_64__
#    define PRIu64 "llu"
#    define PRIuPTR "x"
#else
#    define PRIu64 "lu"
#    define PRIuPTR "lx"
#endif
#define PRIx8 "b"
#define PRIX8 "hhX"
#define PRIx16 "w"
#define PRIX16 "hX"
#define PRIx32 "x"
#define PRIX32 "X"
#ifndef __x86_64__
#    define PRIx64 "llx"
#    define PRIX64 "llX"
#    define PRIxPTR "x"
#else
#    define PRIx64 "lx"
#    define PRIX64 "lX"
#    define PRIxPTR "lx"
#endif

#define __PRI64_PREFIX "ll"
#define __PRIPTR_PREFIX

#define PRIdPTR __PRIPTR_PREFIX "d"
#define PRIiPTR __PRIPTR_PREFIX "i"
#define PRIXPTR __PRIPTR_PREFIX "X"

#define PRIdMAX __PRI64_PREFIX "d"
#define PRIoMAX __PRI64_PREFIX "o"
#define PRIuMAX __PRI64_PREFIX "u"

#define SCNdMAX __PRI64_PREFIX "d"
#define SCNoMAX __PRI64_PREFIX "o"
#define SCNuMAX __PRI64_PREFIX "u"

#define SCNu64 __PRI64_PREFIX "u"
#define SCNd64 __PRI64_PREFIX "d"
#define SCNi64 __PRI64_PREFIX "i"
#define SCNx64 __PRI64_PREFIX "x"

#define SCNd8 "hhd"
#define SCNd16 "hd"
#define SCNd32 "ld"

#define SCNi8 "hhi"
#define SCNi16 "hi"
#define SCNi32 "li"

#define SCNu8 "hhu"
#define SCNu16 "hu"
#define SCNu32 "lu"

#define SCNx8 "hhx"
#define SCNx16 "hx"
#define SCNx32 "lx"

typedef struct imaxdiv_t {
    intmax_t quot;
    intmax_t rem;
} imaxdiv_t;
imaxdiv_t imaxdiv(intmax_t, intmax_t);

intmax_t strtoimax(const char*, char** endptr, int base);
uintmax_t strtoumax(const char*, char** endptr, int base);

__END_DECLS
