/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Bootloader/BootInfo.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Sections.h>
#include <Kernel/VirtualAddress.h>
#include <LibC/elf.h>

// Defined in the linker script
extern size_t __stack_chk_guard;
size_t __stack_chk_guard;
extern "C" [[noreturn]] void __stack_chk_fail();

extern "C" u8 start_of_bootloader_image[];
extern "C" u8 end_of_bootloader_image[];

extern "C" void* gdt64ptr;
extern "C" __attribute__((section(".boot_text"))) void* code64_sel;
extern "C" u64 boot_pml4t[512];
extern "C" u64 boot_pdpt[512];
extern "C" u64 boot_pd0[512];
extern "C" u64 boot_pd_kernel[512];
extern "C" u64 boot_pd_kernel_pts[512 * (KERNEL_PD_OFFSET >> 21)];
extern "C" u64 boot_pd_kernel_pt1023[512];
extern "C" const char kernel_cmdline[4096];

extern "C" void reload_cr3();

extern "C" {
multiboot_info_t* multiboot_info_ptr;
}

void __stack_chk_fail()
{
    asm("ud2");
    __builtin_unreachable();
}

namespace Bootloader {

// boot.S expects these functions to exactly have the following signatures.
// We declare them here to ensure their signatures don't accidentally change.
extern "C" [[noreturn]] void init();

static void halt()
{
    asm volatile("hlt");
}

// SerenityOS Pre-Kernel Environment C++ entry point :^)
//
// This is where C++ execution begins, after boot.S transfers control here.
//

extern "C" [[noreturn]] void init()
{
    halt();
    __builtin_unreachable();
}

// Define some Itanium C++ ABI methods to stop the linker from complaining.
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

}
