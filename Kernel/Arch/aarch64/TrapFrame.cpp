/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/aarch64/TrapFrame.h>
#include <Kernel/InterruptDisabler.h>

namespace Kernel {

extern "C" void exit_trap(TrapFrame* trap)
{
    return Processor::current().exit_trap(*trap);
}

}
