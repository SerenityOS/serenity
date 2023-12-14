/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>

// NOTE: Prototypes here are not correct but since they are extern "C" and we do no plan to do
//       anything except crashing either way, it doesn't matter much.

// Well, we won't find matching declarations for the following :)
#pragma GCC diagnostic ignored "-Wmissing-declarations"

extern "C" {
[[gnu::used, gnu::weak]] void __cxa_begin_catch()
{
    VERIFY_NOT_REACHED();
}

[[gnu::used, gnu::weak]] void __gxx_personality_v0()
{
    VERIFY_NOT_REACHED();
}

[[gnu::used, gnu::weak]] void _Unwind_Resume()
{
    VERIFY_NOT_REACHED();
}
}
