/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>

#include <Kernel/EFIPrekernel/Runtime.h>

namespace Kernel {

[[noreturn]] void halt()
{
    for (;;) {
#if ARCH(AARCH64)
        asm volatile("msr daifset, #2; wfi");
#elif ARCH(RISCV64)
        asm volatile("csrw sie, zero; wfi");
#elif ARCH(X86_64)
        asm volatile("cli; hlt");
#else
#    error Unknown architecture
#endif
    }
}

}
