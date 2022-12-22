/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Types.h>

#include <Kernel/Arch/Delay.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/kstdio.h>

namespace Kernel {

ProcessID g_init_pid { 0 };

bool Process::has_tracee_thread(ProcessID)
{
    TODO_AARCH64();
}

ErrorOr<void> Process::exec(NonnullOwnPtr<KString>, NonnullOwnPtrVector<KString>, NonnullOwnPtrVector<KString>, Thread*&, u32&, int)
{
    TODO_AARCH64();
}

}

// Delay.cpp
namespace Kernel {

void microseconds_delay(u32)
{
    TODO_AARCH64();
}

}

// Initializer.cpp
namespace Kernel::PCI {

bool g_pci_access_io_probe_failed { false };
bool g_pci_access_is_disabled_from_commandline { false };

}

// x86 init

multiboot_module_entry_t multiboot_copy_boot_modules_array[16];
size_t multiboot_copy_boot_modules_count;

extern "C" {
READONLY_AFTER_INIT PhysicalAddress start_of_prekernel_image;
READONLY_AFTER_INIT PhysicalAddress end_of_prekernel_image;
READONLY_AFTER_INIT size_t physical_to_virtual_offset;
// READONLY_AFTER_INIT FlatPtr kernel_mapping_base;
READONLY_AFTER_INIT FlatPtr kernel_load_base;
#if ARCH(X86_64)
READONLY_AFTER_INIT PhysicalAddress boot_pml4t;
#endif
READONLY_AFTER_INIT PhysicalAddress boot_pdpt;
READONLY_AFTER_INIT PhysicalAddress boot_pd0;
READONLY_AFTER_INIT PhysicalAddress boot_pd_kernel;
READONLY_AFTER_INIT Kernel::PageTableEntry* boot_pd_kernel_pt1023;
READONLY_AFTER_INIT char const* kernel_cmdline;
READONLY_AFTER_INIT u32 multiboot_flags;
READONLY_AFTER_INIT multiboot_memory_map_t* multiboot_memory_map;
READONLY_AFTER_INIT size_t multiboot_memory_map_count;
READONLY_AFTER_INIT multiboot_module_entry_t* multiboot_modules;
READONLY_AFTER_INIT size_t multiboot_modules_count;
READONLY_AFTER_INIT PhysicalAddress multiboot_framebuffer_addr;
READONLY_AFTER_INIT u32 multiboot_framebuffer_pitch;
READONLY_AFTER_INIT u32 multiboot_framebuffer_width;
READONLY_AFTER_INIT u32 multiboot_framebuffer_height;
READONLY_AFTER_INIT u8 multiboot_framebuffer_bpp;
READONLY_AFTER_INIT u8 multiboot_framebuffer_type;
}

extern "C" {
FlatPtr kernel_mapping_base;
}
