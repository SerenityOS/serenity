/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/UnicodeUtils.h>

#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Firmware/EFI/Protocols/LoadedImage.h>
#include <Kernel/Firmware/EFI/Protocols/MediaAccess.h>
#include <Kernel/Firmware/EFI/Protocols/RISCVBootProtocol.h>
#include <Kernel/Firmware/EFI/SystemTable.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/VirtualAddress.h>
#include <Kernel/Prekernel/Prekernel.h>

#include <Kernel/EFIPrekernel/Arch/Boot.h>
#include <Kernel/EFIPrekernel/Arch/MMU.h>
#include <Kernel/EFIPrekernel/DebugOutput.h>
#include <Kernel/EFIPrekernel/DeviceTree.h>
#include <Kernel/EFIPrekernel/EFIPrekernel.h>
#include <Kernel/EFIPrekernel/Error.h>
#include <Kernel/EFIPrekernel/GOP.h>
#include <Kernel/EFIPrekernel/Panic.h>
#include <Kernel/EFIPrekernel/Relocation.h>
#include <Kernel/EFIPrekernel/Runtime.h>
#include <Kernel/EFIPrekernel/VirtualMemoryLayout.h>

#include <LibELF/ELFABI.h>
#include <LibELF/Relocation.h>

// FIXME: Initialize the __stack_chk_guard with a random value via the EFI_RNG_PROTOCOL or other arch-specific methods.
uintptr_t __stack_chk_guard __attribute__((used));

extern "C" u8 pe_image_base[];

extern "C" u8 start_of_kernel_image[];
extern "C" u8 end_of_kernel_image[];

namespace Kernel {

static_assert(EFI::EFI_PAGE_SIZE == PAGE_SIZE, "The EFIPrekernel assumes that EFI_PAGE_SIZE == PAGE_SIZE");

EFI::Handle g_efi_image_handle = 0;
EFI::SystemTable* g_efi_system_table = nullptr;

static size_t pages_needed(size_t bytes)
{
    return (bytes + PAGE_SIZE - 1) / PAGE_SIZE;
}

extern "C" [[noreturn]] void __stack_chk_fail();
extern "C" [[noreturn]] void __stack_chk_fail()
{
    PANIC("Stack protector failure, stack smashing detected!");
}

static void map_kernel_image(void* root_page_table, ELF::Image const& kernel_elf_image, ReadonlyBytes kernel_elf_image_data, FlatPtr kernel_load_base)
{
    kernel_elf_image.for_each_program_header([root_page_table, kernel_elf_image_data, kernel_load_base](ELF::Image::ProgramHeader const& program_header) {
        if (program_header.type() != PT_LOAD)
            return IterationDecision::Continue;

        auto page_count = pages_needed(program_header.size_in_memory());

        auto start_vaddr = kernel_load_base + program_header.vaddr().get();
        auto start_paddr = bit_cast<PhysicalPtr>(kernel_elf_image_data.data()) + program_header.offset();

        if (program_header.size_in_memory() != program_header.size_in_image()) {
            if (program_header.size_in_image() != 0)
                PANIC("Program headers with p_memsz != p_filesz && p_filesz != 0 are not supported");

            // Allocate a zeroed memory region for the program header.
            EFI::PhysicalAddress segment_data_paddr = 0;
            if (auto status = g_efi_system_table->boot_services->allocate_pages(EFI::AllocateType::AnyPages, EFI::MemoryType::LoaderData, page_count, &segment_data_paddr); status != EFI::Status::Success)
                PANIC("Failed to allocate memory for program header {}: {}", program_header.index(), status);

            __builtin_memset(bit_cast<void*>(segment_data_paddr), 0, page_count * PAGE_SIZE);

            start_paddr = segment_data_paddr;
        }

        VERIFY(program_header.alignment() % PAGE_SIZE == 0);
        VERIFY(start_vaddr % PAGE_SIZE == 0);

        Memory::Access access;
        if (program_header.is_readable())
            access |= Memory::Access::Read;
        if (program_header.is_writable())
            access |= Memory::Access::Write;
        if (program_header.is_executable())
            access |= Memory::Access::Execute;

        if (auto result = Memory::map_pages(root_page_table, start_vaddr, start_paddr, page_count, access); result.is_error())
            PANIC("Failed to map program header {}: {}", program_header.index(), result.release_error());

        return IterationDecision::Continue;
    });
}

static void get_memory_map_and_exit_boot_services(void* root_page_table, BootInfo& boot_info)
{
    auto* boot_services = g_efi_system_table->boot_services;
    auto& memory_map = boot_info.boot_method_specific.efi.memory_map;

    // Print this message before the first call to GetMemoryMap(), as calling OutputString() could change the memory map.
    dbgln("Exiting EFI Boot Services...");

    // Get the required size for the memory map.
    if (auto status = boot_services->get_memory_map(&memory_map.descriptor_array_size, nullptr, &memory_map.map_key, &memory_map.descriptor_size, &memory_map.descriptor_version); status != EFI::Status::BufferTooSmall)
        PANIC("Failed to acquire the required size for memory map: {}", status);

    // Make room for 10 additional descriptors in the memory map, as the memory map could change between the first GetMemoryMap() and ExitBootServices().
    // This also allows us to reuse the memory map even if the first call to ExitBootServices() fails.
    // We probably shouldn't allocate memory if ExitBootServices() failed, as that might change the memory map again.
    memory_map.descriptor_array_size = round_up_to_power_of_two(memory_map.descriptor_array_size + memory_map.descriptor_size * 10, PAGE_SIZE);

    // We have to save the size here, as GetMemoryMap() overrides the value pointed to by the MemoryMap argument.
    memory_map.buffer_size = memory_map.descriptor_array_size;

    if (auto status = boot_services->allocate_pages(EFI::AllocateType::AnyPages, EFI::MemoryType::LoaderData, memory_map.buffer_size / PAGE_SIZE, &memory_map.descriptor_array_paddr); status != EFI::Status::Success)
        PANIC("Failed to allocate memory for the EFI memory map: {}", status);

    __builtin_memset(bit_cast<void*>(memory_map.descriptor_array_paddr), 0, memory_map.buffer_size);

    if (auto result = Memory::map_pages(root_page_table, EFI_MEMORY_MAP_VADDR, memory_map.descriptor_array_paddr, memory_map.buffer_size / PAGE_SIZE, Memory::Access::Read); result.is_error())
        PANIC("Failed to map the EFI memory map: {}", result.release_error());

    // Tell the kernel the location of the EFI memory map
    memory_map.descriptor_array = bit_cast<EFI::MemoryDescriptor*>(EFI_MEMORY_MAP_VADDR);

    if (auto status = boot_services->get_memory_map(&memory_map.descriptor_array_size, bit_cast<EFI::MemoryDescriptor*>(memory_map.descriptor_array_paddr), &memory_map.map_key, &memory_map.descriptor_size, &memory_map.descriptor_version); status != EFI::Status::Success)
        PANIC("Failed to get the EFI memory map: {}", status);

    // A very crude memory leak detector
    // We should check for leaks before calling ExitBootServices(), as we have no way of freeing them after that.
    // Memory that should stay allocated has to directly be allocated via Allocate{Pages,Pool}().
    kmalloc_stats stats;
    get_kmalloc_stats(stats);

    if (stats.kmalloc_call_count != stats.kfree_call_count)
        PANIC("Memory leak(s) detected! kmalloc call count: {}, kfree call count: {}", stats.kmalloc_call_count, stats.kfree_call_count);

    // From now on, we can't use any boot service or device-handle-based protocol anymore, even if ExitBootServices() failed.
    if (auto status = boot_services->exit_boot_services(g_efi_image_handle, memory_map.map_key); status == EFI::Status::InvalidParameter) {
        // We have to call GetMemoryMap() again, as the memory map changed between GetMemoryMap() and ExitBootServices().
        // Memory allocation services are still allowed to be used if ExitBootServices() failed.
        memory_map.descriptor_array_size = memory_map.buffer_size;
        if (boot_services->get_memory_map(&memory_map.descriptor_array_size, bit_cast<EFI::MemoryDescriptor*>(memory_map.descriptor_array_paddr), &memory_map.map_key, &memory_map.descriptor_size, &memory_map.descriptor_version) != EFI::Status::Success)
            halt();

        if (boot_services->exit_boot_services(g_efi_image_handle, memory_map.map_key) != EFI::Status::Success)
            halt();
    } else if (status != EFI::Status::Success) {
        halt();
    }
}

extern "C" EFIAPI EFI::Status init(EFI::Handle image_handle, EFI::SystemTable* system_table);
extern "C" EFIAPI EFI::Status init(EFI::Handle image_handle, EFI::SystemTable* system_table)
{
    // We use some EFI 1.10 functions from the System Table, so reject older versions.
    static constexpr u32 efi_version_1_10 = (1 << 16) | 10;
    if (system_table->hdr.signature != EFI::SystemTable::signature || system_table->hdr.revision < efi_version_1_10)
        return EFI::Status::Unsupported;

    g_efi_image_handle = image_handle;
    g_efi_system_table = system_table;

    auto* boot_services = system_table->boot_services;

    // clang-format off
    system_table->con_out->set_attribute(system_table->con_out, EFI::TextAttribute {
        .foreground_color = EFI::TextAttribute::ForegroundColor::White,
        .background_color = EFI::TextAttribute::BackgroundColor::Black,
    });
    // clang-format on

    // Clear the screen. This also removes the manufacturer logo, if present.
    system_table->con_out->clear_screen(system_table->con_out);

    ucs2_dbgln(u"SerenityOS EFI Prekernel");

    auto loaded_image_protocol_guid = EFI::LoadedImageProtocol::guid;
    EFI::LoadedImageProtocol* loaded_image_protocol;
    if (auto status = boot_services->handle_protocol(image_handle, &loaded_image_protocol_guid, reinterpret_cast<void**>(&loaded_image_protocol)); status != EFI::Status::Success)
        PANIC("Failed to get the loaded image protocol: {}", status);

    VERIFY(loaded_image_protocol->image_base == pe_image_base);
    VERIFY(bit_cast<FlatPtr>(loaded_image_protocol->image_base) % PAGE_SIZE == 0);

    auto kernel_image_paddr = bit_cast<PhysicalPtr>(&start_of_kernel_image);
    VERIFY(kernel_image_paddr % PAGE_SIZE == 0);
    auto kernel_image_size = bit_cast<PhysicalPtr>(&end_of_kernel_image) - kernel_image_paddr;

    auto maybe_root_page_table = Memory::allocate_empty_root_page_table();
    if (maybe_root_page_table.is_error())
        PANIC("Failed to allocate root page table: {}", maybe_root_page_table.release_error());

    auto* root_page_table = maybe_root_page_table.value();

    // Allocate pages for the kernel stack and map it to KERNEL_MAPPING_BASE
    // TODO: KASLR
    EFI::PhysicalAddress kernel_stack_paddr = 0;
    if (auto status = system_table->boot_services->allocate_pages(EFI::AllocateType::AnyPages, EFI::MemoryType::LoaderData, KERNEL_STACK_SIZE / PAGE_SIZE, &kernel_stack_paddr); status != EFI::Status::Success)
        PANIC("Failed to allocate pages for the kernel stack: {}", status);

    __builtin_memset(bit_cast<void*>(kernel_stack_paddr), 0, KERNEL_STACK_SIZE);

    if (auto result = Memory::map_pages(root_page_table, KERNEL_STACK_VADDR, kernel_stack_paddr, KERNEL_STACK_SIZE / PAGE_SIZE, Memory::Access::Read | Memory::Access::Write); result.is_error())
        PANIC("Failed to map the kernel stack: {}", result.release_error());

    // Allocate pages for the boot info struct and map it to KERNEL_MAPPING_BASE + KERNEL_STACK_SIZE
    // TODO: KASLR
    EFI::PhysicalAddress boot_info_paddr = 0;
    if (auto status = system_table->boot_services->allocate_pages(EFI::AllocateType::AnyPages, EFI::MemoryType::LoaderData, pages_needed(sizeof(BootInfo)), &boot_info_paddr); status != EFI::Status::Success)
        PANIC("Failed to allocate pages for the BootInfo struct: {}", status);

    auto* boot_info = bit_cast<BootInfo*>(boot_info_paddr);
    new (boot_info) BootInfo;

    boot_info->boot_method = BootMethod::EFI;
    boot_info->boot_method_specific.pre_init.~PreInitBootInfo();
    new (&boot_info->boot_method_specific.efi) EFIBootInfo;

    init_gop_and_fill_framebuffer_boot_info(boot_info);

    if (auto result = Memory::map_pages(root_page_table, BOOT_INFO_VADDR, boot_info_paddr, pages_needed(sizeof(BootInfo)), Memory::Access::Read); result.is_error())
        PANIC("Failed to map the BootInfo struct: {}", result.release_error());

    Bytes kernel_elf_image_data { bit_cast<u8*>(kernel_image_paddr), kernel_image_size };
    ELF::Image kernel_elf_image { kernel_elf_image_data };

    // TODO: KASLR
    FlatPtr default_kernel_load_base = KERNEL_MAPPING_BASE + 0x200000;

    boot_info->kernel_mapping_base = KERNEL_MAPPING_BASE;
    boot_info->kernel_load_base = default_kernel_load_base;
    boot_info->physical_to_virtual_offset = boot_info->kernel_load_base - kernel_image_paddr;

    // TODO: Get the cmdline from loaded_image_protocol->load_options.
    //       The EFI shell passes the cmdline as a NUL-terminated UCS-2 string with argv[0] being the executable path.
    //       efibootmgr --unicode doesn't add a NUL terminator.

    dbgln("Mapping the kernel image...");
    map_kernel_image(root_page_table, kernel_elf_image, kernel_elf_image_data, boot_info->kernel_load_base);

    dbgln("Performing relative relocations of the kernel image...");
    perform_kernel_relocations(kernel_elf_image, kernel_elf_image_data, boot_info->kernel_load_base);

    fill_flattened_devicetree_boot_info(boot_info);

    arch_prepare_boot(root_page_table, *boot_info);

    get_memory_map_and_exit_boot_services(root_page_table, *boot_info);

    auto kernel_entry_vaddr = boot_info->kernel_load_base + kernel_elf_image.entry().get();

    arch_enter_kernel(root_page_table, kernel_entry_vaddr, KERNEL_STACK_VADDR + KERNEL_STACK_SIZE, BOOT_INFO_VADDR);
}

}

void __assertion_failed(char const* msg, char const* file, unsigned int line, char const* func)
{
    dbgln("ASSERTION FAILED: {}", msg);
    dbgln("{}:{} in {}", file, line, func);
    Kernel::halt();
}
