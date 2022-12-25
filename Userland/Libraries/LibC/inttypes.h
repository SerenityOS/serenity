/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/stdint.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#define __PRI64_PREFIX "l"
#define __PRIPTR_PREFIX "l"

#define PRId8 "d"
#define PRId16 "d"
#define PRId32 "d"
#define PRId64 __PRI64_PREFIX "d"
#define PRIdPTR __PRIPTR_PREFIX "d"
#define PRIdMAX __PRI64_PREFIX "d"

#define PRIdLEAST8 PRId8
#define PRIdLEAST16 PRId16
#define PRIdLEAST32 PRId32
#define PRIdLEAST64 PRId64
#define PRIdFAST8 PRId8
#define PRIdFAST16 PRId16
#define PRIdFAST32 PRId32
#define PRIdFAST64 PRId64

#define PRIi8 "d"
#define PRIi16 "d"
#define PRIi32 "d"
#define PRIi64 __PRI64_PREFIX "d"
#define PRIiPTR __PRIPTR_PREFIX "d"
#define PRIiMAX __PRI64_PREFIX "d"

#define PRIiLEAST8 PRIi8
#define PRIiLEAST16 PRIi16
#define PRIiLEAST32 PRIi32
#define PRIiLEAST64 PRIi64
#define PRIiFAST8 PRIi8
#define PRIiFAST16 PRIi16
#define PRIiFAST32 PRIi32
#define PRIiFAST64 PRIi64

#define PRIo8 "o"
#define PRIo16 "o"
#define PRIo32 "o"
#define PRIo64 __PRI64_PREFIX "o"
#define PRIoPTR __PRIPTR_PREFIX "o"
#define PRIoMAX __PRI64_PREFIX "o"

#define PRIoLEAST8 PRIo8
#define PRIoLEAST16 PRIo16
#define PRIoLEAST32 PRIo32
#define PRIoLEAST64 PRIo64
#define PRIoFAST8 PRIo8
#define PRIoFAST16 PRIo16
#define PRIoFAST32 PRIo32
#define PRIoFAST64 PRIo64

#define PRIu8 "u"
#define PRIu16 "u"
#define PRIu32 "u"
#define PRIu64 __PRI64_PREFIX "u"
#define PRIuPTR __PRIPTR_PREFIX "u"
#define PRIuMAX __PRI64_PREFIX "u"

#define PRIuLEAST8 PRIu8
#define PRIuLEAST16 PRIu16
#define PRIuLEAST32 PRIu32
#define PRIuLEAST64 PRIu64
#define PRIuFAST8 PRIu8
#define PRIuFAST16 PRIu16
#define PRIuFAST32 PRIu32
#define PRIuFAST64 PRIu64

#define PRIx8 "x"
#define PRIx16 "x"
#define PRIx32 "x"
#define PRIx64 __PRI64_PREFIX "x"
#define PRIxPTR __PRIPTR_PREFIX "x"
#define PRIxMAX __PRI64_PREFIX "x"

#define PRIxLEAST8 PRIx8
#define PRIxLEAST16 PRIx16
#define PRIxLEAST32 PRIx32
#define PRIxLEAST64 PRIx64
#define PRIxFAST8 PRIx8
#define PRIxFAST16 PRIx16
#define PRIxFAST32 PRIx32
#define PRIxFAST64 PRIx64

#define PRIX8 "X"
#define PRIX16 "X"
#define PRIX32 "X"
#define PRIX64 __PRI64_PREFIX "X"
#define PRIXPTR __PRIPTR_PREFIX "X"
#define PRIXMAX __PRI64_PREFIX "X"

#define PRIXLEAST8 PRIX8
#define PRIXLEAST16 PRIX16
#define PRIXLEAST32 PRIX32
#define PRIXLEAST64 PRIX64
#define PRIXFAST8 PRIX8
#define PRIXFAST16 PRIX16
#define PRIXFAST32 PRIX32
#define PRIXFAST64 PRIX64

#define SCNd8 "hhd"
#define SCNd16 "hd"
#define SCNd32 "ld"
#define SCNd64 __PRI64_PREFIX "d"
#define SCNdPTR __PRIPTR_PREFIX "d"
#define SCNdMAX __PRI64_PREFIX "d"

#define SCNdLEAST8 SCNd8
#define SCNdLEAST16 SCNd16
#define SCNdLEAST32 SCNd32
#define SCNdLEAST64 SCNd64
#define SCNdFAST8 SCNd8
#define SCNdFAST16 SCNd16
#define SCNdFAST32 SCNd32
#define SCNdFAST64 SCNd64

#define SCNi8 "hhi"
#define SCNi16 "hi"
#define SCNi32 "li"
#define SCNi64 __PRI64_PREFIX "i"
#define SCNiPTR __PRIPTR_PREFIX "i"
#define SCNiMAX __PRI64_PREFIX "i"

#define SCNiLEAST8 SCNi8
#define SCNiLEAST16 SCNi16
#define SCNiLEAST32 SCNi32
#define SCNiLEAST64 SCNi64
#define SCNiFAST8 SCNi8
#define SCNiFAST16 SCNi16
#define SCNiFAST32 SCNi32
#define SCNiFAST64 SCNi64

#define SCNu8 "hhu"
#define SCNu16 "hu"
#define SCNu32 "lu"
#define SCNu64 __PRI64_PREFIX "u"
#define SCNuPTR __PRIPTR_PREFIX "u"
#define SCNuMAX __PRI64_PREFIX "u"

#define SCNuLEAST8 SCNu8
#define SCNuLEAST16 SCNu16
#define SCNuLEAST32 SCNu32
#define SCNuLEAST64 SCNu64
#define SCNuFAST8 SCNu8
#define SCNuFAST16 SCNu16
#define SCNuFAST32 SCNu32
#define SCNuFAST64 SCNu64

#define SCNo8 "hho"
#define SCNo16 "ho"
#define SCNo32 "lo"
#define SCNo64 __PRI64_PREFIX "o"
#define SCNoPTR __PRIPTR_PREFIX "o"
#define SCNoMAX __PRI64_PREFIX "o"

#define SCNoLEAST8 SCNo8
#define SCNoLEAST16 SCNo16
#define SCNoLEAST32 SCNo32
#define SCNoLEAST64 SCNo64
#define SCNoFAST8 SCNo8
#define SCNoFAST16 SCNo16
#define SCNoFAST32 SCNo32
#define SCNoFAST64 SCNo64

#define SCNx8 "hhx"
#define SCNx16 "hx"
#define SCNx32 "lx"
#define SCNx64 __PRI64_PREFIX "x"
#define SCNxPTR __PRIPTR_PREFIX "x"
#define SCNxMAX __PRI64_PREFIX "x"

#define SCNxLEAST8 SCNx8
#define SCNxLEAST16 SCNx16
#define SCNxLEAST32 SCNx32
#define SCNxLEAST64 SCNx64
#define SCNxFAST8 SCNx8
#define SCNxFAST16 SCNx16
#define SCNxFAST32 SCNx32
#define SCNxFAST64 SCNx64

typedef struct imaxdiv_t {
    intmax_t quot;
    intmax_t rem;
} imaxdiv_t;
imaxdiv_t imaxdiv(intmax_t, intmax_t);

intmax_t strtoimax(char const*, char** endptr, int base);
uintmax_t strtoumax(char const*, char** endptr, int base);

__END_DECLS
