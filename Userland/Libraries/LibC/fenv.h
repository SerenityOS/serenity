/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdint.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

struct __x87_floating_point_environment {
    uint16_t __control_word;
    uint16_t __reserved1;
    uint16_t __status_word;
    uint16_t __reserved2;
    uint16_t __tag_word;
    uint16_t __reserved3;
    uint32_t __fpu_ip_offset;
    uint16_t __fpu_ip_selector;
    uint16_t __opcode : 11;
    uint16_t __reserved4 : 5;
    uint32_t __fpu_data_offset;
    uint16_t __fpu_data_selector;
    uint16_t __reserved5;
};

typedef struct fenv_t {
    struct __x87_floating_point_environment __x87_fpu_env;
    uint32_t __mxcsr;
} fenv_t;

#define FE_DFL_ENV ((fenv_t const*)-1)

int fegetenv(fenv_t*);
int fesetenv(fenv_t const*);
int feholdexcept(fenv_t*);
int feupdateenv(fenv_t const*);

#define FE_INVALID 1u << 0
#define FE_DIVBYZERO 1u << 2
#define FE_OVERFLOW 1u << 3
#define FE_UNDERFLOW 1u << 4
#define FE_INEXACT 1u << 5
#define FE_ALL_EXCEPT (FE_DIVBYZERO | FE_INEXACT | FE_INVALID | FE_OVERFLOW | FE_UNDERFLOW)

typedef uint16_t fexcept_t;
int fegetexceptflag(fexcept_t*, int exceptions);
int fesetexceptflag(fexcept_t const*, int exceptions);

int feclearexcept(int exceptions);
int fetestexcept(int exceptions);
int feraiseexcept(int exceptions);

#define FE_TONEAREST 0
#define FE_DOWNWARD 1
#define FE_UPWARD 2
#define FE_TOWARDZERO 3

int fesetround(int round);
int fegetround(void);

__END_DECLS
