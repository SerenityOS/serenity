/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Types.h>

#define RREGISTER(num)                    \
    union {                               \
        u64 r##num;                       \
        struct {                          \
            u32 _;                        \
            union {                       \
                u32 r##num##d;            \
                struct {                  \
                    u16 __;               \
                    union {               \
                        u16 r##num##w;    \
                        struct {          \
                            u8 ___;       \
                            u8 r##num##b; \
                        };                \
                    };                    \
                };                        \
            };                            \
        };                                \
    }

#define GPREGISTER(letter)                \
    union {                               \
        u64 r##letter##x;                 \
        struct                            \
        {                                 \
            u32 _;                        \
            union {                       \
                u32 e##letter##x;         \
                struct                    \
                {                         \
                    u16 __;               \
                    union {               \
                        u16 letter##x;    \
                        struct {          \
                            u8 letter##h; \
                            u8 letter##l; \
                        };                \
                    };                    \
                };                        \
            };                            \
        };                                \
    }

#define SPREGISTER(name)                \
    union {                             \
        u64 r##name;                    \
        struct                          \
        {                               \
            u32 _;                      \
            union {                     \
                u32 e##name;            \
                struct                  \
                {                       \
                    u16 __;             \
                    union {             \
                        u16 name;       \
                        struct {        \
                            u8 ___;     \
                            u8 name##l; \
                        };              \
                    };                  \
                };                      \
            };                          \
        };                              \
    }

struct [[gnu::packed]] PtraceRegisters {
    GPREGISTER(a);
    GPREGISTER(b);
    GPREGISTER(c);
    GPREGISTER(d);

    SPREGISTER(sp);
    SPREGISTER(bp);
    SPREGISTER(si);
    SPREGISTER(di);
    SPREGISTER(ip); // technically there is no ipl, but what ever

    RREGISTER(8);
    RREGISTER(9);
    RREGISTER(10);
    RREGISTER(11);
    RREGISTER(12);
    RREGISTER(13);
    RREGISTER(14);
    RREGISTER(15);
    // flags
    union {
        u64 rflags;
        struct {
            u32 _;
            union {
                u32 eflags;
                struct {
                    u16 __;
                    u16 flags;
                };
            };
        };
    };

    // These may not be used, unless we go back into compatability mode
    u32 cs;
    u32 ss;
    u32 ds;
    u32 es;
    u32 fs;
    u32 gs;

    // FIXME: Add FPU registers and Flags
    // FIXME: Add Ymm Xmm etc.
};
