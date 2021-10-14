/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/Spinlock.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/Spinlock.h>
#else
#    error "Unknown architecture"
#endif
