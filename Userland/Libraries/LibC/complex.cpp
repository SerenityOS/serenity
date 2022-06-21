/*
 * Copyright (c) 2022, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <complex.h>

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
}
