/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <fenv.h>

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
