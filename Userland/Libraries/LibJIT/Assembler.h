/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64)
#    include <LibJIT/X86_64/Assembler.h>
#    define JIT_ARCH_SUPPORTED 1
namespace JIT {
using Assembler = X86_64Assembler;
}
#else
#    undef JIT_ARCH_SUPPORTED
#endif
