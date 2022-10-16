/*
 * Copyright (c) 2022, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64) || ARCH(I386)
#    include <Kernel/Arch/x86/TrapFrame.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/TrapFrame.h>
#else
#    error "Unknown architecture"
#endif
