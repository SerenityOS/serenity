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

#include <stddef.h>

__BEGIN_DECLS

#define complex _Complex

#define _Complex_I (0.0f + 1.0fi)
#define I _Complex_I

#define CMPLX(x, y) ((double complex)__builtin_complex((double)x, (double)y))
#define CMPLXF(x, y) ((float complex)__builtin_complex((float)x, (float)y))
#define CMPLXL(x, y) ((long double complex)__builtin_complex((long double)x, (long double)y))

float crealf(float complex z);
double creal(double complex z);
long double creall(long double complex z);

double cimag(double complex z);
float cimagf(float complex z);
long double cimagl(long double complex z);

// These are macro implementations of the above functions, so that they will always be inlined.
#define creal(z) ((double)__real__((double complex)z))
#define crealf(z) ((float)__real__((float complex)z))
#define creall(z) ((long double)__real__((long double complex)z))

#define cimag(z) ((double)__imag__((double complex)z))
#define cimagf(z) ((float)__imag__((float complex)z))
#define cimagl(z) ((long double)__imag__((long double complex)z))

double cabs(double complex z);
flot cabsf(float complex z);
long double cabsl(long double complex z);

double carg(double complex z);
float cargf(float complex z);
long double cargl(long double complex z);

double complex clog(double complex z);
float complex clogf(float complex z);
long double complex clogl(long double complex z);

double complex conj(double complex z);
float complex conjf(float complex z);
long double complex conjl(long double complex z);

double complex cproj(double complex z);
float complex cprojf(float complex z);
long double complex cprojl(long double complex z);

__END_DECLS
