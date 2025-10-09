/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>

#include <Kernel/EFIPrekernel/DebugOutput.h>
#include <Kernel/EFIPrekernel/Panic.h>
#include <Kernel/EFIPrekernel/Runtime.h>

namespace Kernel {

[[noreturn]] void __panic(char const* file, unsigned int line, char const* function)
{
    dbgln("at {}:{} in {}", file, line, function);

    halt();
}

}
