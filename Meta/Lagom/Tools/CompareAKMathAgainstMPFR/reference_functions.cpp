/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "reference_functions.h"
#include <math.h>
#include <mpfr.h>

namespace CORE_MATH {
// These functions are taken from the CORE-MATH project.
// https://gitlab.inria.fr/core-math/core-math/-/blob/master/src/binary64/

double exp(double x)
{
    mpfr_t y;
    mpfr_exp_t emin = mpfr_get_emin();
    mpfr_set_emin(-1073);
    mpfr_init2(y, 53);
    mpfr_set_d(y, x, MPFR_RNDN);
    int inex = mpfr_exp(y, y, MPFR_RNDN);
    mpfr_subnormalize(y, inex, MPFR_RNDN);
    double ret = mpfr_get_d(y, MPFR_RNDN);
    mpfr_clear(y);
    mpfr_set_emin(emin);
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

}
