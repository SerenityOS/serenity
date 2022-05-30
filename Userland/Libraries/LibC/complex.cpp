/*
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <complex.h>
#include <math.h>

extern "C" {

// Function definitions of this form "type (name)(args)" are intentional, to
// prevent macro versions of "name" from being incorrectly expanded. These
// functions are here to provide external linkage to their macro implementations.

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/creal.html
float(crealf)(float complex z)
{
    return crealf(z);
}

double(creal)(double complex z)
{
    return creal(z);
}

long double(creall)(long double complex z)
{
    return creall(z);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/cimag.html
double(cimag)(double complex z)
{
    return cimag(z);
}

float(cimagf)(float complex z)
{
    return cimagf(z);
}

long double(cimagl)(long double complex z)
{
    return cimagl(z);
}


// https://pubs.opengroup.org/onlinepubs/9699919799.2018edition/functions/cabs.html
float cabsf(float complex z)
{
    return hypotf(crealf(z), cimagf(z));
}

double cabs(double complex z)
{
    return hypot(creal(z), cimag(z));
}

long double cabsl(long double complex z)
{
    return hypotl(creall(z), cimagl(z));
}

// https://pubs.opengroup.org/onlinepubs/9699919799.2018edition/functions/carg.html
float cargf(float complex z)
{
    return atan2f(cimagf(z), crealf(z));
}

double carg(double complex z)
{
    return atan2(cimag(z), creal(z));
}

long double cargl(long double complex z)
{
    return atan2l(cimagl(z), creall(z));
}

// https://pubs.opengroup.org/onlinepubs/9699919799.2018edition/functions/clog.html
float complex clogf(float complex z)
{
    return CMPLXF(logf(cabsf(z)), cargf(z));
}

double complex clog(double complex z)
{
    return CMPLX(log(cabs(z)), carg(z));
}

long double complex clogl(long double complex z)
{
    return CMPLXL(logl(cabsl(z)), cargl(z));
}

// https://pubs.opengroup.org/onlinepubs/9699919799.2018edition/functions/conj.html
float complex conjf(float complex z)
{
    return CMPLXF(crealf(z), -cimagf(z));
}

double complex conj(double complex z)
{
    return CMPLX(creal(z), -cimag(z));
}

long double complex conj(long double complex z)
{
    return CMPLXL(creall(z), -cimagl(z));
}

// https://pubs.opengroup.org/onlinepubs/9699919799.2018edition/functions/cproj.html
float complex cprojf(float complex z)
{
    if (isinf(crealf(z)) || isinf(cimagf(z))) {
        return CMPLXF(INFINITY, copysignf(0.0f, cimagf(z)));
    }
    return z;
}

double complex cproj(double complex z)
{
    if (isinf(creal(z)) || isinf(cimag(z))) {
        return CMPLX(INFNINITY, copysign(0.0, cimag(z)));
    }
    return z;
}

long double complex cprojl(long double complex z)
{
    if (isinf(creall(z)) || isinf(cimagl(z))) {
        return CMPLXL(INFINITY, copysignl(0.0L, cimagl(z)));
    }
    return z;
}

}
