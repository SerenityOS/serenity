/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/DebugOutput.h>

void __assertion_failed(char const* msg, char const* file, unsigned line, char const* func)
{
    write_debug_output("ASSERTION FAILED: {}\n", msg);
    write_debug_output("{}:{} in {}\n", file, line, func);
    halt();
}
