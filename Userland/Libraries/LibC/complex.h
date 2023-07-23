/*
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

/* complex arithmetic
 *
 * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/complex.h.html
 */

#pragma once

#ifdef __cplusplus
#    error "C++ code must not include complex.h. Use AK/Complex.h instead."
#endif

#include <stddef.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#define complex _Complex

#define _Complex_I (0.0f + 1.0fi)
#define I _Complex_I

#define CMPLX(x, y) ((double complex)__builtin_complex((double)x, (double)y))
#define CMPLXF(x, y) ((float complex)__builtin_complex((float)x, (float)y))
#define CMPLXL(x, y) ((long double complex)__builtin_complex((long double)x, (long double)y))

// These are macro implementations of the above functions, so that they will always be inlined.
#define creal(z) ((double)__real__((double complex)z))
#define crealf(z) ((float)__real__((float complex)z))
#define creall(z) ((long double)__real__((long double complex)z))

#define cimag(z) ((double)__imag__((double complex)z))
#define cimagf(z) ((float)__imag__((float complex)z))
#define cimagl(z) ((long double)__imag__((long double complex)z))

// Function definitions of this form "type (name)(args)" are intentional, to
// prevent macro versions of "name" from being incorrectly expanded. These
// functions are here to provide external linkage to their macro implementations.

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/creal.html
static inline float(crealf)(float complex z)
{
    return crealf(z);
}

static inline double(creal)(double complex z)
{
    return creal(z);
}

static inline long double(creall)(long double complex z)
{
    return creall(z);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/cimag.html
static inline double(cimag)(double complex z)
{
    return cimag(z);
}

static inline float(cimagf)(float complex z)
{
    return cimagf(z);
}

static inline long double(cimagl)(long double complex z)
{
    return cimagl(z);
}

__END_DECLS
