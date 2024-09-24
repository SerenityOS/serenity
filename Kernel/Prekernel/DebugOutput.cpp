/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <AK/kstdio.h>
#include <Kernel/Arch/DebugOutput.h>
#include <Kernel/Prekernel/DebugOutput.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/BochsDebugOutput.h>
#endif

void debug_write_string(StringView str)
{
    if (str.is_null())
        return;
    for (u8 ch : str.bytes()) {
        Kernel::debug_output(ch);
#if ARCH(X86_64)
        Kernel::bochs_debug_output(ch);
#endif
    }
}

extern "C" void dbgputstr(char const* characters, size_t length)
{
    if (!characters)
        return;
    debug_write_string(StringView { characters, length });
}
