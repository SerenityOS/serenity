/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>

#if !defined(KERNEL) && defined(NDEBUG)
extern "C" {

void ak_verification_failed(char const* message)
{
    dbgln("VERIFICATION FAILED: {}", message);
    __builtin_trap();
}
}

#endif
