/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/Types.h>

namespace ELF {

void set_thread_pointer_register(FlatPtr);

}

#if ARCH(AARCH64)
#    include <LibELF/Arch/aarch64/tls.h>
#elif ARCH(RISCV64)
#    include <LibELF/Arch/riscv64/tls.h>
#elif ARCH(X86_64)
#    include <LibELF/Arch/x86_64/tls.h>
#else
#    error Unknown architecture
#endif
