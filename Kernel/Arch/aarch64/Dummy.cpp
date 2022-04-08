/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Types.h>

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KString.h>
#include <Kernel/KSyms.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Panic.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/Sections.h>
#include <Kernel/UserOrKernelBuffer.h>

// Scheduler
namespace Kernel {

READONLY_AFTER_INIT Thread* g_finalizer;

}

// Panic
namespace Kernel {

void __panic(char const*, unsigned int, char const*)
{
    for (;;) { }
}

}

// Random
namespace Kernel {

void get_fast_random_bytes(Bytes)
{
    VERIFY_NOT_REACHED();
}

}

// Inode
namespace Kernel {

static Singleton<SpinlockProtected<Inode::AllInstancesList>> s_all_instances;

SpinlockProtected<Inode::AllInstancesList>& Inode::all_instances()
{
    VERIFY_NOT_REACHED();
    return s_all_instances;
}

RefPtr<Memory::SharedInodeVMObject> Inode::shared_vmobject() const
{
    VERIFY_NOT_REACHED();
    return RefPtr<Memory::SharedInodeVMObject>(nullptr);
}

void Inode::will_be_destroyed()
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> Inode::set_shared_vmobject(Memory::SharedInodeVMObject&)
{
    VERIFY_NOT_REACHED();
    return {};
}

}

// UserOrKernelBuffer.cpp
namespace Kernel {

ErrorOr<void> UserOrKernelBuffer::write(void const*, size_t, size_t)
{
    VERIFY_NOT_REACHED();
    return {};
}

ErrorOr<void> UserOrKernelBuffer::read(void*, size_t, size_t) const
{
    VERIFY_NOT_REACHED();
    return {};
}

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

// KSyms.cpp
namespace Kernel {
bool g_kernel_symbols_available = false;
}

namespace Kernel {

void dump_backtrace(PrintToScreen)
{
    VERIFY_NOT_REACHED();
}

// KString.cpp
ErrorOr<NonnullOwnPtr<KString>> KString::try_create_uninitialized(size_t, char*&)
{
    VERIFY_NOT_REACHED();
    return ENOMEM;
}
ErrorOr<NonnullOwnPtr<KString>> KString::try_create(StringView)
{
    VERIFY_NOT_REACHED();
    return ENOMEM;
}
void KString::operator delete(void*)
{
    VERIFY_NOT_REACHED();
}

// SafeMem.h
bool safe_memset(void*, int, size_t, void*&);
bool safe_memset(void*, int, size_t, void*&)
{
    VERIFY_NOT_REACHED();
    return false;
}

ssize_t safe_strnlen(char const*, unsigned long, void*&);
ssize_t safe_strnlen(char const*, unsigned long, void*&)
{
    VERIFY_NOT_REACHED();
    return 0;
}

bool safe_memcpy(void*, void const*, unsigned long, void*&);
bool safe_memcpy(void*, void const*, unsigned long, void*&)
{
    VERIFY_NOT_REACHED();
    return false;
}

Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile*, u32&, u32);
Optional<bool> safe_atomic_compare_exchange_relaxed(u32 volatile*, u32&, u32)
{
    VERIFY_NOT_REACHED();
    return {};
}

Optional<u32> safe_atomic_load_relaxed(u32 volatile*);
Optional<u32> safe_atomic_load_relaxed(u32 volatile*)
{
    VERIFY_NOT_REACHED();
    return {};
}

Optional<u32> safe_atomic_fetch_add_relaxed(u32 volatile*, u32);
Optional<u32> safe_atomic_fetch_add_relaxed(u32 volatile*, u32)
{
    VERIFY_NOT_REACHED();
    return {};
}

Optional<u32> safe_atomic_exchange_relaxed(u32 volatile*, u32);
Optional<u32> safe_atomic_exchange_relaxed(u32 volatile*, u32)
{
    VERIFY_NOT_REACHED();
    return {};
}

bool safe_atomic_store_relaxed(u32 volatile*, u32);
bool safe_atomic_store_relaxed(u32 volatile*, u32)
{
    VERIFY_NOT_REACHED();
    return {};
}

}

extern "C" {

FlatPtr kernel_mapping_base;

void kernelputstr(char const*, size_t);
void kernelputstr(char const*, size_t)
{
    VERIFY_NOT_REACHED();
}

void kernelcriticalputstr(char const*, size_t);
void kernelcriticalputstr(char const*, size_t)
{
    VERIFY_NOT_REACHED();
}

void kernelearlyputstr(char const*, size_t);
void kernelearlyputstr(char const*, size_t)
{
    VERIFY_NOT_REACHED();
}
}
