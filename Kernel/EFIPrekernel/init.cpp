/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/ScopeGuard.h>
#include <AK/UnicodeUtils.h>
#include <Kernel/Firmware/EFI/EFI.h>
#include <Kernel/Firmware/EFI/Protocols/LoadedImage.h>
#include <Kernel/Firmware/EFI/Protocols/MediaAccess.h>
#include <Kernel/Firmware/EFI/Protocols/RISCVBootProtocol.h>
#include <Kernel/Firmware/EFI/SystemTable.h>

// FIXME: Merge the EFI Prekernel with the x86 Prekernel once the EFI Prekernel works on x86.
//        Making this Prekernel work on x86 requires refactoring the x86 boot info to not rely on multiboot.
//        And for AArch64 we need to make the Kernel bootable from any load address.

// FIXME: We should introduce another Kernel entry point for AArch64 and RISC-V, so we can pass UEFI-related info to the kernel.
//        This is required to be able to use UEFI runtime services and the EFI_GRAPHICS_OUTPUT_PROTOCOL.

// FIXME: Initialize the __stack_chk_guard with a random value via the EFI_RNG_PROTOCOL or other arch-specific methods.
uintptr_t __stack_chk_guard __attribute__((used)) = 0xc6c7c8c9;

namespace Kernel {

static EFI::Handle s_image_handle;
static EFI::SystemTable* s_system_table;

static void efi_dbgln(char16_t const* message)
{
    if (s_system_table == nullptr || s_system_table->con_out == nullptr)
        return;

    s_system_table->con_out->output_string(s_system_table->con_out, const_cast<char16_t*>(message));
    s_system_table->con_out->output_string(s_system_table->con_out, const_cast<char16_t*>(u"\r\n"));
}

[[noreturn]] static void halt()
{
    for (;;) {
#if ARCH(AARCH64)
        asm volatile("msr daifset, #2; wfi");
#elif ARCH(RISCV64)
        asm volatile("csrw sie, zero; wfi");
#elif ARCH(X86_64)
        asm volatile("cli; hlt");
#else
#    error Unknown architecture
#endif
    }
}

extern "C" [[noreturn]] void __stack_chk_fail();
extern "C" [[noreturn]] void __stack_chk_fail()
{
    halt();
}

struct KernelImageRange {
    EFI::PhysicalAddress address;
    FlatPtr page_count;
};

static ErrorOr<KernelImageRange, EFI::Status> load_kernel(EFI::Handle device_handle)
{
    auto simple_file_system_protocol_guid = EFI::SimpleFileSystemProtocol::guid;
    EFI::SimpleFileSystemProtocol* simple_file_system_interface;
    if (auto status = s_system_table->boot_services->handle_protocol(device_handle, &simple_file_system_protocol_guid, reinterpret_cast<void**>(&simple_file_system_interface)); status != EFI::Status::Success) {
        efi_dbgln(u"Error: The boot device doesn't support the Simple Filesystem Protocol");
        return status;
    }

    EFI::FileProtocol* volume;
    if (auto status = simple_file_system_interface->open_volume(simple_file_system_interface, &volume); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to open the boot volume");
        return status;
    }

    ScopeGuard volume_closer([volume]() { volume->close(volume); });

    // FIXME: Get the kernel file name from the command line.
#if ARCH(RISCV64)
    char16_t const kernel_file_name[] = u"boot\\Kernel.bin";
#else
    char16_t const kernel_file_name[] = u"Kernel";
#endif

    EFI::FileProtocol* kernel_file;
    if (auto status = volume->open(volume, &kernel_file, const_cast<char16_t*>(kernel_file_name), EFI::FileOpenMode::Read, EFI::FileAttribute::None); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to open the kernel image file");
        return status;
    }

    ScopeGuard kernel_file_closer([kernel_file]() { kernel_file->close(kernel_file); });

    auto file_info_guid = EFI::FileInfo::guid;
    alignas(EFI::FileInfo) u8 info_buffer[sizeof(EFI::FileInfo) + sizeof(kernel_file_name)];
    FlatPtr info_size = sizeof(info_buffer);
    if (auto status = kernel_file->get_info(kernel_file, &file_info_guid, &info_size, &info_buffer); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to get info for the kernel image file");
        return status;
    }

    auto const* info = reinterpret_cast<EFI::FileInfo const*>(info_buffer);
    FlatPtr kernel_size = info->file_size;

#if ARCH(AARCH64) || ARCH(RISCV64)
    // The AArch64 and RISC-V kernel use some memory after the kernel image for the stack and initial page tables.
    // FIXME: Don't hardcode additional padding after the kernel.
    //        Either directly jump to the kernel init() like on x86 (and therefore don't use pre_init)
    //        or add some kind of header to the kernel image?
    kernel_size += 12 * MiB;
#endif

    KernelImageRange kernel_image_range {
        .address = 0,
        .page_count = (kernel_size + PAGE_SIZE - 1) / PAGE_SIZE,
    };

    if (auto status = s_system_table->boot_services->allocate_pages(EFI::AllocateType::AnyPages, EFI::MemoryType::LoaderData, kernel_image_range.page_count, &kernel_image_range.address); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to allocate pages for the kernel image");
        return status;
    }

    ArmedScopeGuard free_kernel_image_on_failure([kernel_image_range]() {
        s_system_table->boot_services->free_pages(kernel_image_range.address, kernel_image_range.page_count);
    });

    // FIXME: Load the kernel in chunks. Loading the entire kernel at once is quite slow on edk2 running on x86.
    efi_dbgln(u"Loading the kernel image...");
    if (auto status = kernel_file->read(kernel_file, &kernel_size, bit_cast<void*>(kernel_image_range.address)); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to read the kernel image file");
        return status;
    }
    efi_dbgln(u"Done");

    free_kernel_image_on_failure.disarm();

    return kernel_image_range;
}

extern "C" EFIAPI EFI::Status init(EFI::Handle image_handle, EFI::SystemTable* system_table);
extern "C" EFIAPI EFI::Status init(EFI::Handle image_handle, EFI::SystemTable* system_table)
{
    // We use some EFI 1.10 functions from the System Table, so reject older versions.
    static constexpr u32 efi_version_1_10 = (1 << 16) | 10;
    if (system_table->hdr.signature != EFI::SystemTable::signature || system_table->hdr.revision < efi_version_1_10)
        return EFI::Status::Unsupported;

    s_image_handle = image_handle;
    s_system_table = system_table;

    auto* boot_services = system_table->boot_services;

    // clang-format off
    system_table->con_out->set_attribute(system_table->con_out, EFI::TextAttribute {
        .foreground_color = EFI::TextAttribute::ForegroundColor::White,
        .background_color = EFI::TextAttribute::BackgroundColor::Black,
    });
    // clang-format on

    // Clear the screen. This also removes the manufacturer logo, if present.
    system_table->con_out->clear_screen(system_table->con_out);

    auto loaded_image_protocol_guid = EFI::LoadedImageProtocol::guid;
    EFI::LoadedImageProtocol* loaded_image_interface;
    if (auto status = boot_services->handle_protocol(image_handle, &loaded_image_protocol_guid, reinterpret_cast<void**>(&loaded_image_interface)); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to get the loaded image protocol");
        return status;
    }

    // TODO: Get the cmdline from loaded_image_interface->load_options.
    //       The EFI shell passes the cmdline as a NUL-terminated UCS-2 string with argv[0] being the executable path.
    //       efibootmgr --unicode doesn't add a NUL terminator.

    auto kernel_image_range = TRY(load_kernel(loaded_image_interface->device_handle));

    // We only return on failure.
    ScopeGuard free_kernel_image_on_failure([system_table, &kernel_image_range]() {
        system_table->boot_services->free_pages(kernel_image_range.address, kernel_image_range.page_count);
    });

#if ARCH(RISCV64)
    // Get the boot hart ID.
    auto riscv_boot_protocol_guid = EFI::RISCVBootProtocol::guid;
    EFI::RISCVBootProtocol* riscv_boot_protocol = nullptr;

    if (auto status = boot_services->locate_protocol(&riscv_boot_protocol_guid, nullptr, reinterpret_cast<void**>(&riscv_boot_protocol)); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to locate the RISC-V boot protocol");
        efi_dbgln(u"RISC-V systems that don't support RISCV_EFI_BOOT_PROTOCOL are not supported.");
        return status;
    }

    FlatPtr boot_hart_id = 0;
    if (auto status = riscv_boot_protocol->get_boot_hart_id(riscv_boot_protocol, &boot_hart_id); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to get the RISC-V boot hart ID");
        return status;
    }

    FlatPtr fdt_addr = 0;
    // Get the flattened devicetree from the configuration table.
    for (FlatPtr i = 0; i < system_table->number_of_table_entries; ++i) {
        if (system_table->configuration_table[i].vendor_guid == EFI::DTB_TABLE_GUID) {
            fdt_addr = bit_cast<FlatPtr>(system_table->configuration_table[i].vendor_table);
        }
    }

    if (fdt_addr == 0) {
        efi_dbgln(u"Error: Failed to find the devicetree configuration table");
        efi_dbgln(u"RISC-V systems without a devicetree UEFI configuration table are not supported.");
        return EFI::Status::LoadError;
    }
#endif

    struct EFIMemoryMap {
        EFI::MemoryDescriptor* descriptor_array = nullptr;
        FlatPtr descriptor_array_size = 0;
        FlatPtr descriptor_size = 0;
        FlatPtr buffer_size = 0;
        FlatPtr map_key = 0;
        u32 descriptor_version = 0;
    } efi_memory_map;

    // Get the required size for the memory map.
    if (auto status = boot_services->get_memory_map(&efi_memory_map.descriptor_array_size, nullptr, &efi_memory_map.map_key, &efi_memory_map.descriptor_size, &efi_memory_map.descriptor_version); status != EFI::Status::BufferTooSmall) {
        efi_dbgln(u"Error: Failed to acquire the required size for memory map");
        return status;
    }

    // Make room for 10 additional descriptors in the memory map, as the memory map might be changed by allocating the memory for it.
    // This also allows us to reuse the memory map even if the first call to ExitBootServices() fails.
    // We probably shouldn't allocate memory if ExitBootServices() failed, as that might change the memory map again.
    efi_memory_map.descriptor_array_size += efi_memory_map.descriptor_size * 10;

    // We have to save the size here, as GetMemoryMap() overrides the value pointed to by the MemoryMap argument.
    efi_memory_map.buffer_size = efi_memory_map.descriptor_array_size;

    if (auto status = boot_services->allocate_pool(EFI::MemoryType::LoaderData, efi_memory_map.buffer_size, reinterpret_cast<void**>(&efi_memory_map.descriptor_array)); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to allocate memory for the memory map");
        return status;
    }

    // We only return on failure.
    ScopeGuard free_memory_map_on_failure([boot_services, &efi_memory_map]() {
        boot_services->free_pool(efi_memory_map.descriptor_array);
    });

    if (auto status = boot_services->get_memory_map(&efi_memory_map.descriptor_array_size, efi_memory_map.descriptor_array, &efi_memory_map.map_key, &efi_memory_map.descriptor_size, &efi_memory_map.descriptor_version); status != EFI::Status::Success) {
        efi_dbgln(u"Error: Failed to get the memory map");
        return status;
    }

    efi_dbgln(u"Exiting EFI Boot Services...");
    // From now on, we can't use any boot service or device-handle-based protocols anymore, even if ExitBootServices() failed.
    if (auto status = boot_services->exit_boot_services(image_handle, efi_memory_map.map_key); status == EFI::Status::InvalidParameter) {
        // We have to call GetMemoryMap() again, as the memory map changed between GetMemoryMap() and ExitBootServices().
        // Memory allocation services are still allowed to be used if ExitBootServices() failed.
        efi_memory_map.descriptor_array_size = efi_memory_map.buffer_size;
        if (auto status = boot_services->get_memory_map(&efi_memory_map.descriptor_array_size, efi_memory_map.descriptor_array, &efi_memory_map.map_key, &efi_memory_map.descriptor_size, &efi_memory_map.descriptor_version); status != EFI::Status::Success) {
            halt();
        }

        if (auto status = boot_services->exit_boot_services(image_handle, efi_memory_map.map_key); status != EFI::Status::Success) {
            halt();
        }
    } else if (status != EFI::Status::Success) {
        halt();
    }

#if ARCH(RISCV64)
    using RISCVEntry = void(FlatPtr boot_hart_id, FlatPtr fdt_phys_addr);
    auto* entry = bit_cast<RISCVEntry*>(kernel_image_range.address);

    // The RISC-V kernel requires the MMU to be disabled on entry.
    // We are identity mapped, so we can safely disable it.
    asm volatile("csrw satp, zero");

    // FIXME: Use the UEFI memory map on RISC-V and pass the UEFI command line to the kernel.
    entry(boot_hart_id, fdt_addr);
#else
    (void)kernel_image_range;
#endif

    halt();
}

}

void __assertion_failed(char const*, char const*, unsigned int, char const*)
{
    Kernel::halt();
}

// Define some Itanium C++ ABI methods to stop the linker from complaining.
// If we actually call these something has gone horribly wrong
void* __dso_handle __attribute__((visibility("hidden")));
