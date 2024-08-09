/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>

#include <Kernel/EFIPrekernel/Arch/Boot.h>
#include <Kernel/EFIPrekernel/Runtime.h>

namespace Kernel {

void arch_prepare_boot(void* root_page_table, BootInfo& boot_info)
{
    (void)root_page_table;
    (void)boot_info;
    TODO();
}

[[noreturn]] void arch_enter_kernel(void* root_page_table, FlatPtr kernel_entry_vaddr, FlatPtr kernel_stack_pointer, FlatPtr boot_info_vaddr)
{
    (void)root_page_table;
    (void)kernel_entry_vaddr;
    (void)kernel_stack_pointer;
    (void)boot_info_vaddr;
    halt();
}

}
