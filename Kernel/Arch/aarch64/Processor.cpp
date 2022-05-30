/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/CPU.h>

namespace Kernel {

void Processor::wait_check()
{
    VERIFY_NOT_REACHED();
}

[[noreturn]] void Processor::halt()
{
    for (;;)
        asm volatile("wfi");
}

}
