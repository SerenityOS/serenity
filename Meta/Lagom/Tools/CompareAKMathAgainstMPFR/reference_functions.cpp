/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "reference_functions.h"
#include <math.h>
#include <mpfr.h>

namespace CORE_MATH {

void set_exponent_limits()
{
    mpfr_set_emin(-1073);
    mpfr_set_emax(1024);
}

// These functions are taken from the CORE-MATH project.
// https://gitlab.inria.fr/core-math/core-math/-/blob/master/src/binary64/

double exp(double x)
{
    mpfr_t y;
    mpfr_init2(y, 53);
    mpfr_set_d(y, x, MPFR_RNDN);
    int inex = mpfr_exp(y, y, MPFR_RNDN);
    mpfr_subnormalize(y, inex, MPFR_RNDN);
    double ret = mpfr_get_d(y, MPFR_RNDN);
    mpfr_clear(y);
    return ret;
}

double log(double x)
{
    mpfr_t y;
    mpfr_init2(y, 53);
    mpfr_set_d(y, x, MPFR_RNDN);
    mpfr_log(y, y, MPFR_RNDN);
    double ret = mpfr_get_d(y, MPFR_RNDN);
    mpfr_clear(y);
    return ret;
}

double cosh(double x)
{
    mpfr_t y;
    mpfr_init2(y, 53);
    mpfr_set_d(y, x, MPFR_RNDN);
    mpfr_cosh(y, y, MPFR_RNDN);
    /* no need to call mpfr_subnormalize(), since cosh(x) >= 1 */
    double ret = mpfr_get_d(y, MPFR_RNDN);
    mpfr_clear(y);
    return ret;
}

double sinh(double x)
{
    mpfr_t y;
    mpfr_init2(y, 53);
    mpfr_set_d(y, x, MPFR_RNDN);
    int inex = mpfr_sinh(y, y, MPFR_RNDN);
    mpfr_subnormalize(y, inex, MPFR_RNDN);
    double ret = mpfr_get_d(y, MPFR_RNDN);
    mpfr_clear(y);
    return ret;
}

double tanh(double x)
{
    mpfr_t y;
    mpfr_init2(y, 53);
    mpfr_set_d(y, x, MPFR_RNDN);
    int inex = mpfr_tanh(y, y, MPFR_RNDN);
    mpfr_subnormalize(y, inex, MPFR_RNDN);
    double ret = mpfr_get_d(y, MPFR_RNDN);
    mpfr_clear(y);
    return ret;
}
}
