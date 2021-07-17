/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <Kernel/Multiboot.h>

namespace Bootloader {

struct [[gnu::packed]] BootInfo {
    u8* start_of_bootloader_image;
    u8* end_of_bootloader_image;
    FlatPtr kernel_base;
    multiboot_info* multiboot_info_ptr;
    void* gdt64ptr;
    void* code64_sel;
#if ARCH(X86_64)
    FlatPtr boot_pml4t;
#endif
    FlatPtr boot_pdpt;
    FlatPtr boot_pd0;
    FlatPtr boot_pd_kernel;
    FlatPtr boot_pd_kernel_pt1023;
    const char* kernel_cmdline;
};
}
