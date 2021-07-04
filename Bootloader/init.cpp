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

extern "C" u8 gdt64ptr[];
extern "C" __attribute__((section(".boot_text"))) void* code64_sel;
extern "C" u64 boot_pml4t[512];
extern "C" u64 boot_pdpt[512];
extern "C" u64 boot_pd0[512];
extern "C" u64 boot_pd0_pts[512 * (KERNEL_PD_OFFSET >> 21)];
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
    if (multiboot_info_ptr->mods_count < 1)
        halt();

    multiboot_module_entry_t* kernel_module = (multiboot_module_entry_t*)(FlatPtr)multiboot_info_ptr->mods_addr;

    u8* kernel_image = (u8*)(FlatPtr)kernel_module->start;
    ElfW(Ehdr)* kernel_elf_header = (ElfW(Ehdr)*)kernel_image;
    ElfW(Phdr)* kernel_program_headers = (ElfW(Phdr*))((char*)kernel_elf_header + kernel_elf_header->e_phoff);

    FlatPtr kernel_load_base = kernel_program_headers[0].p_vaddr;

    // align to 1GB
    kernel_load_base &= ~(FlatPtr)0x3fffffff;

    if (kernel_program_headers[0].p_vaddr < (FlatPtr)end_of_bootloader_image)
        halt();

    if (kernel_program_headers[0].p_paddr < (FlatPtr)end_of_bootloader_image)
        halt();

#if ARCH(I386)
    int pdpt_flags = 0x1;
#else
    int pdpt_flags = 0x3;
#endif
    boot_pdpt[(kernel_load_base >> 30) & 0x1ffu] = (FlatPtr)boot_pd_kernel | pdpt_flags;

    for (size_t i = 0; i < KERNEL_PD_OFFSET >> 21; i++)
        boot_pd_kernel[i] = (FlatPtr)&boot_pd_kernel_pts[i * 512] | 0x3;

    __builtin_memset(boot_pd_kernel_pts, 0, sizeof(boot_pd_kernel_pts));

    /* pseudo-identity map 0M - end_of_bootloader_image */
    for (size_t i = 0; i < (FlatPtr)end_of_bootloader_image / PAGE_SIZE; i++)
        boot_pd_kernel_pts[i] = i * PAGE_SIZE | 0x3;

    for (size_t i = 0; i < kernel_elf_header->e_phnum ; i++) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        for (FlatPtr offset = 0; offset < kernel_program_header.p_memsz; offset += PAGE_SIZE) {
            auto pte_index = (kernel_program_header.p_vaddr + offset - kernel_load_base) >> 12;
            boot_pd_kernel_pts[pte_index] = (kernel_program_header.p_paddr + offset) | 0x3;
        }
    }

    boot_pd_kernel[511] = (FlatPtr)boot_pd_kernel_pt1023 | 0x3;

    reload_cr3();

    for (ssize_t i = kernel_elf_header->e_phnum - 1; i >= 0; i--) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        __builtin_memmove((u8*)kernel_program_header.p_vaddr, kernel_image + kernel_program_header.p_offset, kernel_program_header.p_filesz);
    }

    for (ssize_t i = kernel_elf_header->e_phnum - 1; i >= 0; i--) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        __builtin_memset((u8*)kernel_program_header.p_vaddr + kernel_program_header.p_filesz, 0, kernel_program_header.p_memsz - kernel_program_header.p_filesz);
    }

    multiboot_info_ptr->mods_count--;
    multiboot_info_ptr->mods_addr += sizeof(multiboot_module_entry_t);

    auto adjust_by_load_base = [kernel_load_base](auto* ptr) {
        return (decltype(ptr))((FlatPtr)ptr + kernel_load_base);
    };

    BootInfo info;
    info.start_of_bootloader_image = (u8*)&start_of_bootloader_image + kernel_load_base;
    info.end_of_bootloader_image = (u8*)&end_of_bootloader_image + kernel_load_base;
    info.kernel_base = kernel_program_headers->p_vaddr;
    info.multiboot_info_ptr = adjust_by_load_base(multiboot_info_ptr);
    info.gdt64ptr = 0;   //gdt64ptr;
    info.code64_sel = 0; //code64_sel;
#if ARCH(X86_64)
    info.boot_pml4t = (FlatPtr)adjust_by_load_base(boot_pml4t);
#endif
    info.boot_pdpt = (FlatPtr)adjust_by_load_base(boot_pdpt);
    info.boot_pd0 = (FlatPtr)adjust_by_load_base(boot_pd0);
    info.boot_pd_kernel = (FlatPtr)adjust_by_load_base(boot_pd_kernel);
    info.boot_pd_kernel_pt1023 = (FlatPtr)adjust_by_load_base(boot_pd_kernel_pt1023);
    info.kernel_cmdline = adjust_by_load_base(kernel_cmdline);

    asm(
#if ARCH(I386)
        "add %0, %%esp"
#else
        "add %0, %%rsp"
#endif
        :: "g"(kernel_load_base)
    );

    // unmap the 0-1MB region
    for (size_t i = 0; i < 256; i++)
        boot_pd0_pts[i] = 0;

    void (*entry)(BootInfo const&) = (void (*)(BootInfo const&))kernel_elf_header->e_entry;
    entry(*adjust_by_load_base(&info));

    __builtin_unreachable();
}

// Define some Itanium C++ ABI methods to stop the linker from complaining.
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

}
