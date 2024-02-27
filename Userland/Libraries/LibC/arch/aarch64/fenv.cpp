/*
 * Copyright (c) 2024, Tom Finet <tom.codeninja@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <fenv.h>

extern "C" {

int fegetenv(fenv_t* env)
{
    if (!env)
        return 1;

    (void)env;
    TODO_AARCH64();
    return 0;
}

int fesetenv(fenv_t const* env)
{
    if (!env)
        return 1;

    (void)env;
    TODO_AARCH64();
    return 0;
}

int feholdexcept(fenv_t* env)
{
    fegetenv(env);

    fenv_t current_env;
    fegetenv(&current_env);

    (void)env;
    TODO_AARCH64();

    fesetenv(&current_env);
    return 0;
}

int fesetexceptflag(fexcept_t const* except, int exceptions)
{
    if (!except)
        return 1;

    fenv_t current_env;
    fegetenv(&current_env);

    exceptions &= FE_ALL_EXCEPT;

    (void)exceptions;
    (void)except;
    TODO_AARCH64();

    fesetenv(&current_env);
    return 0;
}

int fegetround()
{
    TODO_AARCH64();
}

int fesetround(int rounding_mode)
{
    if (rounding_mode < FE_TONEAREST || rounding_mode > FE_TOMAXMAGNITUDE)
        return 1;

    if (rounding_mode == FE_TOMAXMAGNITUDE)
        rounding_mode = FE_TONEAREST;

    TODO_AARCH64();
    return 0;
}

int feclearexcept(int exceptions)
{
    exceptions &= FE_ALL_EXCEPT;

    fenv_t current_env;
    fegetenv(&current_env);

    (void)exceptions;
    TODO_AARCH64();

    fesetenv(&current_env);
    return 0;
}

int fetestexcept(int exceptions)
{
    (void)exceptions;
    TODO_AARCH64();
}

int feraiseexcept(int exceptions)
{
    fenv_t env;
    fegetenv(&env);

    exceptions &= FE_ALL_EXCEPT;

    (void)exceptions;
    TODO_AARCH64();
}
}
