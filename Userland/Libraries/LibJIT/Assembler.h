/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJIT/X86_64/Assembler.h>

namespace JIT {
#if ARCH(X86_64)
#    define JIT_ARCH_SUPPORTED 1
typedef X86_64Assembler Assembler;
#else
#    undef JIT_ARCH_SUPPORTED
#endif
}
