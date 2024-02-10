/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(AARCH64)
#    include <Userland/Libraries/LibELF/Arch/aarch64/GenericDynamicRelocationType.h>
#elif ARCH(RISCV64)
#    include <Userland/Libraries/LibELF/Arch/riscv64/GenericDynamicRelocationType.h>
#elif ARCH(X86_64)
#    include <Userland/Libraries/LibELF/Arch/x86_64/GenericDynamicRelocationType.h>
#else
#    error Unknown architecture
#endif
