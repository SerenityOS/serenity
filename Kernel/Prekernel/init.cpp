/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Multiboot.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/VirtualAddress.h>
#include <LibC/elf.h>
#include <LibELF/Relocation.h>

// Defined in the linker script
extern size_t __stack_chk_guard;
size_t __stack_chk_guard __attribute__((used));
extern "C" [[noreturn]] void __stack_chk_fail();

extern "C" u8 start_of_prekernel_image[];
extern "C" u8 end_of_prekernel_image[];

extern "C" u8 gdt64ptr[];
extern "C" u16 code64_sel;
extern "C" u64 boot_pml4t[512];
extern "C" u64 boot_pdpt[512];
extern "C" u64 boot_pd0[512];
extern "C" u64 boot_pd0_pts[512 * (MAX_KERNEL_SIZE >> 21 & 0x1ff)];
extern "C" u64 boot_pd_kernel[512];
extern "C" u64 boot_pd_kernel_pt0[512];
extern "C" u64 boot_pd_kernel_image_pts[512 * (MAX_KERNEL_SIZE >> 21 & 0x1ff)];
extern "C" u64 boot_pd_kernel_pt1023[512];
extern "C" char const kernel_cmdline[4096];

extern "C" void reload_cr3();

extern "C" {
multiboot_info_t* multiboot_info_ptr;
}

[[noreturn]] static void halt()
{
    asm volatile("hlt");
    __builtin_unreachable();
}

void __stack_chk_fail()
{
    halt();
}

void __assertion_failed(char const*, char const*, unsigned int, char const*)
{
    halt();
}

namespace Kernel {

// boot.S expects these functions to exactly have the following signatures.
// We declare them here to ensure their signatures don't accidentally change.
extern "C" [[noreturn]] void init();

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
    // copy the ELF header and program headers because we might end up overwriting them
    ElfW(Ehdr) kernel_elf_header = *(ElfW(Ehdr)*)kernel_image;
    ElfW(Phdr) kernel_program_headers[16];
    if (kernel_elf_header.e_phnum > array_size(kernel_program_headers))
        halt();
    __builtin_memcpy(kernel_program_headers, kernel_image + kernel_elf_header.e_phoff, sizeof(ElfW(Phdr)) * kernel_elf_header.e_phnum);

    FlatPtr kernel_physical_base = 0x200000;
#if ARCH(I386)
    FlatPtr kernel_load_base = 0xc0200000;
#else
    FlatPtr kernel_load_base = 0x2000200000;
#endif

    FlatPtr kernel_load_end = 0;
    for (size_t i = 0; i < kernel_elf_header.e_phnum; i++) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        auto start = kernel_load_base + kernel_program_header.p_vaddr;
        auto end = start + kernel_program_header.p_memsz;
        if (start < (FlatPtr)end_of_prekernel_image)
            halt();
        if (kernel_physical_base + kernel_program_header.p_paddr < (FlatPtr)end_of_prekernel_image)
            halt();
        if (end > kernel_load_end)
            kernel_load_end = end;
    }

    // align to 1GB
    FlatPtr kernel_mapping_base = kernel_load_base & ~(FlatPtr)0x3fffffff;

    VERIFY(kernel_load_base % 0x1000 == 0);
    VERIFY(kernel_load_base >= kernel_mapping_base + 0x200000);

#if ARCH(I386)
    int pdpt_flags = 0x1;
#else
    int pdpt_flags = 0x3;
#endif
    boot_pdpt[(kernel_mapping_base >> 30) & 0x1ffu] = (FlatPtr)boot_pd_kernel | pdpt_flags;

    boot_pd_kernel[0] = (FlatPtr)boot_pd_kernel_pt0 | 0x3;

    for (FlatPtr vaddr = kernel_load_base; vaddr <= kernel_load_end; vaddr += PAGE_SIZE * 512)
        boot_pd_kernel[(vaddr - kernel_mapping_base) >> 21] = (FlatPtr)(&boot_pd_kernel_image_pts[(vaddr - kernel_load_base) >> 12]) | 0x3;

    __builtin_memset(boot_pd_kernel_pt0, 0, sizeof(boot_pd_kernel_pt0));

    VERIFY((size_t)end_of_prekernel_image < array_size(boot_pd_kernel_pt0) * PAGE_SIZE);

    /* pseudo-identity map 0M - end_of_prekernel_image */
    for (size_t i = 0; i < (FlatPtr)end_of_prekernel_image / PAGE_SIZE; i++)
        boot_pd_kernel_pt0[i] = i * PAGE_SIZE | 0x3;

    __builtin_memset(boot_pd_kernel_image_pts, 0, sizeof(boot_pd_kernel_image_pts));

    for (size_t i = 0; i < kernel_elf_header.e_phnum; i++) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        for (FlatPtr offset = 0; offset < kernel_program_header.p_memsz; offset += PAGE_SIZE) {
            auto pte_index = ((kernel_load_base & 0x1fffff) + kernel_program_header.p_vaddr + offset) >> 12;
            boot_pd_kernel_image_pts[pte_index] = (kernel_physical_base + kernel_program_header.p_paddr + offset) | 0x3;
        }
    }

    boot_pd_kernel[511] = (FlatPtr)boot_pd_kernel_pt1023 | 0x3;

    reload_cr3();

    for (ssize_t i = kernel_elf_header.e_phnum - 1; i >= 0; i--) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        __builtin_memmove((u8*)kernel_load_base + kernel_program_header.p_vaddr, kernel_image + kernel_program_header.p_offset, kernel_program_header.p_filesz);
    }

    for (ssize_t i = kernel_elf_header.e_phnum - 1; i >= 0; i--) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        __builtin_memset((u8*)kernel_load_base + kernel_program_header.p_vaddr + kernel_program_header.p_filesz, 0, kernel_program_header.p_memsz - kernel_program_header.p_filesz);
    }

    multiboot_info_ptr->mods_count--;
    multiboot_info_ptr->mods_addr += sizeof(multiboot_module_entry_t);

    auto adjust_by_mapping_base = [kernel_mapping_base](auto ptr) {
        return (decltype(ptr))((FlatPtr)ptr + kernel_mapping_base);
    };

    BootInfo info;
    info.start_of_prekernel_image = (PhysicalPtr)start_of_prekernel_image;
    info.end_of_prekernel_image = (PhysicalPtr)end_of_prekernel_image;
    info.physical_to_virtual_offset = kernel_load_base - kernel_physical_base;
    info.kernel_mapping_base = kernel_mapping_base;
    info.kernel_load_base = kernel_load_base;
#if ARCH(X86_64)
    info.gdt64ptr = (PhysicalPtr)gdt64ptr;
    info.code64_sel = code64_sel;
    info.boot_pml4t = (PhysicalPtr)boot_pml4t;
#endif
    info.boot_pdpt = (PhysicalPtr)boot_pdpt;
    info.boot_pd0 = (PhysicalPtr)boot_pd0;
    info.boot_pd_kernel = (PhysicalPtr)boot_pd_kernel;
    info.boot_pd_kernel_pt1023 = (FlatPtr)adjust_by_mapping_base(boot_pd_kernel_pt1023);
    info.kernel_cmdline = (FlatPtr)adjust_by_mapping_base(kernel_cmdline);
    info.multiboot_flags = multiboot_info_ptr->flags;
    info.multiboot_memory_map = adjust_by_mapping_base((FlatPtr)multiboot_info_ptr->mmap_addr);
    info.multiboot_memory_map_count = multiboot_info_ptr->mmap_length / sizeof(multiboot_memory_map_t);
    info.multiboot_modules = adjust_by_mapping_base((FlatPtr)multiboot_info_ptr->mods_addr);
    info.multiboot_modules_count = multiboot_info_ptr->mods_count;
    info.multiboot_framebuffer_addr = multiboot_info_ptr->framebuffer_addr;
    info.multiboot_framebuffer_pitch = multiboot_info_ptr->framebuffer_pitch;
    info.multiboot_framebuffer_width = multiboot_info_ptr->framebuffer_width;
    info.multiboot_framebuffer_height = multiboot_info_ptr->framebuffer_height;
    info.multiboot_framebuffer_bpp = multiboot_info_ptr->framebuffer_bpp;
    info.multiboot_framebuffer_type = multiboot_info_ptr->framebuffer_type;

    asm(
#if ARCH(I386)
        "add %0, %%esp"
#else
        "movabs %0, %%rax\n"
        "add %%rax, %%rsp"
#endif
        ::"g"(kernel_mapping_base)
        : "ax");

    // unmap the 0-1MB region
    for (size_t i = 0; i < 256; i++)
        boot_pd0_pts[i] = 0;

    // unmap the end_of_prekernel_image - MAX_KERNEL_SIZE region
    for (FlatPtr vaddr = (FlatPtr)end_of_prekernel_image; vaddr < MAX_KERNEL_SIZE; vaddr += PAGE_SIZE)
        boot_pd0_pts[vaddr >> 12] = 0;

    reload_cr3();

    ELF::perform_relative_relocations(kernel_load_base);

    void (*entry)(BootInfo const&) = (void (*)(BootInfo const&))(kernel_load_base + kernel_elf_header.e_entry);
    entry(*adjust_by_mapping_base(&info));

    __builtin_unreachable();
}

// Define some Itanium C++ ABI methods to stop the linker from complaining.
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));

}
