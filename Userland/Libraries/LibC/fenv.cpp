/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <fenv.h>

// This is the size of the floating point environment image in protected mode
static_assert(sizeof(__x87_floating_point_environment) == 28);

extern "C" {

int feupdateenv(fenv_t const* env)
{
    auto currently_raised_exceptions = fetestexcept(FE_ALL_EXCEPT);

    fesetenv(env);
    feraiseexcept(currently_raised_exceptions);

    return 0;
}

int fegetexceptflag(fexcept_t* except, int exceptions)
{
    if (!except)
        return 1;
    *except = (uint16_t)fetestexcept(exceptions);
    return 0;
}
}
