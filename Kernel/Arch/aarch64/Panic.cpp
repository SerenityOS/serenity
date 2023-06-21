/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/Panic.h>

// FIXME: Merge the code in this file with Kernel/Library/Panic.cpp once the proper abstractions are in place.

// Note: This is required here, since __assertion_failed should be out of the Kernel namespace,
//       but the PANIC macro uses functions that require the Kernel namespace.
using namespace Kernel;

[[noreturn]] void __assertion_failed(char const* msg, char const* file, unsigned line, char const* func)
{
    critical_dmesgln("ASSERTION FAILED: {}", msg);
    critical_dmesgln("{}:{} in {}", file, line, func);

    // Used for printing a nice backtrace!
    PANIC("Aborted");
}
