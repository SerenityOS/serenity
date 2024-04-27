/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <arch/fenv.h>
#include <stdint.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

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
// Only exists in RISC-V at the moment; on other architectures this is replaced with FE_TONEAREST.
#define FE_TOMAXMAGNITUDE 4

int fesetround(int round);
int fegetround(void);

__END_DECLS
