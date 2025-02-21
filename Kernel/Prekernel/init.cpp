/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Boot/Multiboot.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/VirtualAddress.h>
#include <Kernel/Prekernel/DebugOutput.h>
#include <Kernel/Prekernel/Prekernel.h>
#include <Kernel/Prekernel/Random.h>
#include <Kernel/Prekernel/Runtime.h>
#include <LibELF/ELFABI.h>
#include <LibELF/Relocation.h>

// Defined in the linker script
extern uintptr_t __stack_chk_guard;
uintptr_t __stack_chk_guard __attribute__((used));
extern "C" [[noreturn]] void __stack_chk_fail();

extern "C" u8 start_of_prekernel_image[];
extern "C" u8 end_of_prekernel_image[];

extern "C" u8 _binary_Kernel_standalone_start[];
extern "C" u8 end_of_prekernel_image_after_kernel_image[];

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

void __stack_chk_fail()
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

static void memmove_virt(void* dest_virt, FlatPtr dest_phys, void* src, size_t n)
{
    if (dest_phys < (FlatPtr)src) {
        u8* pd = (u8*)dest_virt;
        u8 const* ps = (u8 const*)src;
        for (; n--;)
            *pd++ = *ps++;
        return;
    }

    u8* pd = (u8*)dest_virt;
    u8 const* ps = (u8 const*)src;
    for (pd += n, ps += n; n--;)
        *--pd = *--ps;
}

extern "C" [[noreturn]] void init()
{
    u32 initrd_module_start = 0;
    u32 initrd_module_end = 0;
    if (multiboot_info_ptr->mods_count > 0) {
        // We only consider the first specified multiboot module, and ignore
        // the rest of the modules.
        multiboot_module_entry_t* initrd_module = (multiboot_module_entry_t*)(FlatPtr)multiboot_info_ptr->mods_addr;
        VERIFY(initrd_module->start < initrd_module->end);

        initrd_module_start = initrd_module->start;
        initrd_module_end = initrd_module->end;
    }

    u8* kernel_image = _binary_Kernel_standalone_start;
    // copy the ELF header and program headers because we might end up overwriting them
    Elf_Ehdr kernel_elf_header = *(Elf_Ehdr*)kernel_image;
    Elf_Phdr kernel_program_headers[16];
    VERIFY(kernel_elf_header.e_phnum < array_size(kernel_program_headers));
    __builtin_memcpy(kernel_program_headers, kernel_image + kernel_elf_header.e_phoff, sizeof(Elf_Phdr) * kernel_elf_header.e_phnum);

    FlatPtr kernel_physical_base = (FlatPtr)0x200000;
    FlatPtr default_kernel_load_base = KERNEL_MAPPING_BASE + kernel_physical_base;

    FlatPtr kernel_load_base = default_kernel_load_base;

    if (__builtin_strstr(kernel_cmdline, "disable_kaslr") == nullptr) {
        FlatPtr maximum_offset = (FlatPtr)KERNEL_PD_SIZE - MAX_KERNEL_SIZE - 2 * MiB; // The first 2 MiB are used for mapping the pre-kernel
#ifdef KERNEL_ADDRESS_SANITIZER_ENABLED
        // To allow for easy mapping between the kernel virtual addresses and KASAN shadow memory,
        // we map shadow memory at the very end of the virtual range, so that we can index into it
        // using just an offset. To ensure this range is free when needed, we restrict the possible
        // KASLR range when KASAN is enabled to make sure we don't use the end of the virtual range.
        maximum_offset -= ceil_div(maximum_offset, 9ul);
#endif
        kernel_load_base += (generate_secure_seed() % maximum_offset);
        kernel_load_base &= ~(2 * MiB - 1);
    }

    FlatPtr kernel_load_end = 0;
    for (size_t i = 0; i < kernel_elf_header.e_phnum; i++) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        auto start = kernel_load_base + kernel_program_header.p_vaddr;
        auto end = start + kernel_program_header.p_memsz;
        VERIFY(start > (FlatPtr)end_of_prekernel_image);
        VERIFY(kernel_physical_base + kernel_program_header.p_paddr > (FlatPtr)end_of_prekernel_image);
        if (end > kernel_load_end)
            kernel_load_end = end;
    }

    // align to 1GB
    FlatPtr kernel_mapping_base = kernel_load_base & ~(FlatPtr)0x3fffffff;

    VERIFY(kernel_load_base % 0x1000 == 0);
    VERIFY(kernel_load_base >= kernel_mapping_base + kernel_physical_base);

    int pdpt_flags = 0x3;

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

    // Fill-in multiboot-related info before loading kernel as to avoid accidentally
    // overwriting mbi end as to avoid to check whether it's mapped after reloading page tables.
    BootInfo info {};

    auto adjust_by_mapping_base = [kernel_mapping_base](auto ptr) {
        return (decltype(ptr))((FlatPtr)ptr + kernel_mapping_base);
    };

    info.boot_method = BootMethod::Multiboot1;
    info.boot_method_specific.pre_init.~PreInitBootInfo();
    new (&info.boot_method_specific.multiboot1) Multiboot1BootInfo;

    info.boot_method_specific.multiboot1.flags = multiboot_info_ptr->flags;
    info.boot_method_specific.multiboot1.memory_map = bit_cast<multiboot_memory_map_t const*>(adjust_by_mapping_base((FlatPtr)multiboot_info_ptr->mmap_addr));
    info.boot_method_specific.multiboot1.memory_map_count = multiboot_info_ptr->mmap_length / sizeof(multiboot_memory_map_t);

    if (initrd_module_start != 0 && initrd_module_end != 0) {
        info.boot_method_specific.multiboot1.module_physical_ptr = PhysicalAddress { initrd_module_start };
        info.boot_method_specific.multiboot1.module_length = initrd_module_end - initrd_module_start;
    }

    if ((multiboot_info_ptr->flags & MULTIBOOT_INFO_FRAMEBUFFER_INFO) != 0) {
        info.boot_framebuffer.paddr = PhysicalAddress { multiboot_info_ptr->framebuffer_addr };
        info.boot_framebuffer.pitch = multiboot_info_ptr->framebuffer_pitch;
        info.boot_framebuffer.width = multiboot_info_ptr->framebuffer_width;
        info.boot_framebuffer.height = multiboot_info_ptr->framebuffer_height;
        info.boot_framebuffer.bpp = multiboot_info_ptr->framebuffer_bpp;

        if (multiboot_info_ptr->framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_RGB)
            info.boot_framebuffer.type = BootFramebufferType::BGRx8888;
        else
            info.boot_framebuffer.type = BootFramebufferType::None;
    }

    reload_cr3();

    int backwards = kernel_physical_base >= (FlatPtr)kernel_image;

    for (ssize_t i = 0; i < kernel_elf_header.e_phnum; i++) {
        auto& kernel_program_header = kernel_program_headers[backwards ? kernel_elf_header.e_phnum - 1 - i : i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        memmove_virt((u8*)kernel_load_base + kernel_program_header.p_vaddr,
            kernel_physical_base + kernel_program_header.p_vaddr,
            kernel_image + kernel_program_header.p_offset, kernel_program_header.p_filesz);
    }

    for (ssize_t i = kernel_elf_header.e_phnum - 1; i >= 0; i--) {
        auto& kernel_program_header = kernel_program_headers[i];
        if (kernel_program_header.p_type != PT_LOAD)
            continue;
        __builtin_memset((u8*)kernel_load_base + kernel_program_header.p_vaddr + kernel_program_header.p_filesz, 0, kernel_program_header.p_memsz - kernel_program_header.p_filesz);
    }

    info.boot_method_specific.multiboot1.start_of_prekernel_image = PhysicalAddress { bit_cast<PhysicalPtr>(+start_of_prekernel_image) };
    info.boot_method_specific.multiboot1.end_of_prekernel_image = PhysicalAddress { bit_cast<PhysicalPtr>(+end_of_prekernel_image) };
    info.physical_to_virtual_offset = kernel_load_base - kernel_physical_base;
    info.kernel_mapping_base = kernel_mapping_base;
    info.kernel_load_base = kernel_load_base;
#if ARCH(X86_64)
    info.boot_pml4t = PhysicalAddress { bit_cast<PhysicalPtr>(+boot_pml4t) };
#endif
    info.boot_pdpt = PhysicalAddress { bit_cast<PhysicalPtr>(+boot_pdpt) };
    info.boot_method_specific.multiboot1.boot_pd0 = PhysicalAddress { bit_cast<PhysicalPtr>(+boot_pd0) };
    info.boot_pd_kernel = PhysicalAddress { bit_cast<PhysicalPtr>(+boot_pd_kernel) };
    info.boot_pd_kernel_pt1023 = bit_cast<Memory::PageTableEntry*>(adjust_by_mapping_base(boot_pd_kernel_pt1023));

    char const* cmdline_ptr = bit_cast<char const*>(adjust_by_mapping_base(kernel_cmdline));
    info.cmdline = StringView { cmdline_ptr, __builtin_strlen(cmdline_ptr) };

    asm(
        "mov %0, %%rax\n"
        "add %%rax, %%rsp" ::"g"(kernel_mapping_base)
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
