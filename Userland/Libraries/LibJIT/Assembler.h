/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <LibJIT/Aarch64/Assembler.h>
#include <LibJIT/X86_64/Assembler.h>

namespace JIT {

#if ARCH(X86_64)
#    define JIT_ARCH_SUPPORTED 1
using Assembler = X86_64Assembler;
#elif ARCH(AARCH64)
#    define JIT_ARCH_SUPPORTED 1
using Assembler = Aarch64Assembler;
#else
#    undef JIT_ARCH_SUPPORTED
#endif

}
