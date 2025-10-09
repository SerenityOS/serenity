/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

// https://github.com/riscv-non-isa/riscv-c-api-doc/blob/main/src/c-api.adoc#extension-bitmask

struct RISCVFeatureBits {
    unsigned length;
    unsigned long long features[];
};

struct RISCVCPUModel {
    unsigned mvendorid;
    unsigned long long marchid;
    unsigned long long mimpid;
};

// 1st argument: extension name (used as our kernel CPUFeature flag name)
// 2nd and 3rd argument: groupid and bit position from https://github.com/riscv-non-isa/riscv-c-api-doc/blob/main/src/c-api.adoc#extension-bitmask-definitions
#define ENUMERATE_RISCV_EXTENSION_BITMASK_BITS(E) \
    E(A, 0, 0)                                    \
    E(C, 0, 2)                                    \
    E(D, 0, 3)                                    \
    E(F, 0, 5)                                    \
    E(I, 0, 8)                                    \
    E(M, 0, 12)                                   \
    E(V, 0, 21)                                   \
    E(Zacas, 0, 26)                               \
    E(Zba, 0, 27)                                 \
    E(Zbb, 0, 28)                                 \
    E(Zbc, 0, 29)                                 \
    E(Zbkb, 0, 30)                                \
    E(Zbkc, 0, 31)                                \
    E(Zbkx, 0, 32)                                \
    E(Zbs, 0, 33)                                 \
    E(Zfa, 0, 34)                                 \
    E(Zfh, 0, 35)                                 \
    E(Zfhmin, 0, 36)                              \
    E(Zicboz, 0, 37)                              \
    E(Zicond, 0, 38)                              \
    E(Zihintntl, 0, 39)                           \
    E(Zihintpause, 0, 40)                         \
    E(Zknd, 0, 41)                                \
    E(Zkne, 0, 42)                                \
    E(Zknh, 0, 43)                                \
    E(Zksed, 0, 44)                               \
    E(Zksh, 0, 45)                                \
    E(Zkt, 0, 46)                                 \
    E(Ztso, 0, 47)                                \
    E(Zvbb, 0, 48)                                \
    E(Zvbc, 0, 49)                                \
    E(Zvfh, 0, 50)                                \
    E(Zvfhmin, 0, 51)                             \
    E(Zvkb, 0, 52)                                \
    E(Zvkg, 0, 53)                                \
    E(Zvkned, 0, 54)                              \
    E(Zvknha, 0, 55)                              \
    E(Zvknhb, 0, 56)                              \
    E(Zvksed, 0, 57)                              \
    E(Zvksh, 0, 58)                               \
    E(Zvkt, 0, 59)                                \
    E(Zve32x, 0, 60)                              \
    E(Zve32f, 0, 61)                              \
    E(Zve64x, 0, 62)                              \
    E(Zve64f, 0, 63)                              \
    E(Zve64d, 1, 0)                               \
    E(Zimop, 1, 1)                                \
    E(Zca, 1, 2)                                  \
    E(Zcb, 1, 3)                                  \
    E(Zcd, 1, 4)                                  \
    E(Zcf, 1, 5)                                  \
    E(Zcmop, 1, 6)                                \
    E(Zawrs, 1, 7)

// Update this variable when a new groupid is used.
static constexpr size_t EXTENSION_BITMASK_GROUP_COUNT = 2;
