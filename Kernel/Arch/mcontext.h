/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/mcontext.h>
#elif ARCH(AARCH64)
#    error "Unknown architecture"
#endif
