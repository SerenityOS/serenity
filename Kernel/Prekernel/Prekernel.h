/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __cplusplus
#    include <Kernel/Multiboot.h>
#endif

#define MAX_KERNEL_SIZE 0x3000000

#ifdef __cplusplus
namespace Kernel {

struct [[gnu::packed]] BootInfo {
    u8 const* start_of_prekernel_image;
    u8 const* end_of_prekernel_image;
    FlatPtr kernel_base;
    multiboot_info* multiboot_info_ptr;
#    if ARCH(X86_64)
    u32 gdt64ptr;
    u16 code64_sel;
    FlatPtr boot_pml4t;
#    endif
    FlatPtr boot_pdpt;
    FlatPtr boot_pd0;
    FlatPtr boot_pd_kernel;
    FlatPtr boot_pd_kernel_pt1023;
    char const* kernel_cmdline;
};
}
#endif
