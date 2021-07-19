/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __cplusplus
#    include <Kernel/Multiboot.h>
#    include <Kernel/PhysicalAddress.h>
#    include <Kernel/VirtualAddress.h>
#endif

#define MAX_KERNEL_SIZE 0x3000000

#ifdef __cplusplus
namespace Kernel {

struct [[gnu::packed]] BootInfo {
    u32 start_of_prekernel_image;
    u32 end_of_prekernel_image;
    u64 kernel_base;
    u64 multiboot_info_ptr;
#    if ARCH(X86_64)
    u32 gdt64ptr;
    u16 code64_sel;
    u32 boot_pml4t;
#    endif
    u32 boot_pdpt;
    u32 boot_pd0;
    u32 boot_pd_kernel;
    u64 boot_pd_kernel_pt1023;
    u64 kernel_cmdline;
};
}
#endif
