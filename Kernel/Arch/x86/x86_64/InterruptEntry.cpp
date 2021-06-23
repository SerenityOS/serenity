/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/DescriptorTable.h>
#include <Kernel/Arch/x86/TrapFrame.h>

// clang-format off
asm(
".globl interrupt_common_asm_entry\n"
"interrupt_common_asm_entry: \n"
"    int3 \n" // FIXME
".globl common_trap_exit \n"
"common_trap_exit: \n"
// another thread may have handled this trap at this point, so don't
// make assumptions about the stack other than there's a TrapFrame
// and a pointer to it.
"    call exit_trap \n"
"    int3 \n" // FIXME
);
// clang-format on
