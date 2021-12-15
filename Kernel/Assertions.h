/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Platform.h>

#define __STRINGIFY_HELPER(x) #x
#define __STRINGIFY(x) __STRINGIFY_HELPER(x)

extern "C" {
[[noreturn]] void _abort();
[[noreturn]] void abort();
}

#if ARCH(I386) || ARCH(X86_64)
#    define VERIFY_INTERRUPTS_DISABLED() VERIFY(!(cpu_flags() & 0x200))
#    define VERIFY_INTERRUPTS_ENABLED() VERIFY(cpu_flags() & 0x200)
#else
#    define VERIFY_INTERRUPTS_DISABLED() TODO()
#    define VERIFY_INTERRUPTS_ENABLED() TODO()
#endif
