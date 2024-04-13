/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/ArchSpecificThreadData.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/ArchSpecificThreadData.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/ArchSpecificThreadData.h>
#else
#    error Unknown architecture
#endif
