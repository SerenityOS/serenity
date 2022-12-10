/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 * Copyright (c) 2022, Keegan Saunders <keegan@undefinedbehaviour.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Platform.h>
#include <Kernel/AddressSanitizer.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/BootInfo.h>
#include <Kernel/KSyms.h>

using namespace Kernel;
using namespace Kernel::AddressSanitizer;

constexpr uintptr_t KASAN_SHADOW_OFFSET = 0x6fc0000000;
constexpr u32 KASAN_SCALE = 3;
constexpr uintptr_t KASAN_GRANULE = 1UL << KASAN_SCALE;

Atomic<bool> Kernel::AddressSanitizer::g_kasan_is_deadly { true };

enum class Access {
    Load,
    Store,
    __Count
};

static constexpr StringView AccessNames[] {
    "load"sv,
    "store"sv
};

static_assert(array_size(AccessNames) == to_underlying(Access::__Count));

static inline u8* kasan_shadow_address(unsigned long address)
{
    return reinterpret_cast<u8*>((address >> KASAN_SCALE) + KASAN_SHADOW_OFFSET);
}

NO_SANITIZE_ADDRESS void Kernel::AddressSanitizer::poison(unsigned long address, size_t size, Poison value)
{
    (void)address;
    (void)size;
    (void)value;
#ifdef ENABLE_KERNEL_ADDRESS_SANITIZER
    // TODO: If the process page tables have a mapping for `address` and `size` that is
    // the `kasan_zero` page: unmap those specific pages and allocate new physical
    // pages which are writable into the location, then apply the poison.
    // Otherwise, apply the poison to the existing region.
#endif
}

NO_SANITIZE_ADDRESS static inline void shadow_va_check(unsigned long address, size_t size, Access access, void* return_address)
{
    (void)address;
    (void)size;
    (void)access;
    (void)return_address;
#ifdef ENABLE_KERNEL_ADDRESS_SANITIZER
    address = align_down_to(address, KASAN_GRANULE);
    auto shadow_size = align_up_to(size, KASAN_GRANULE) >> KASAN_SCALE;

    auto shadow_address = kasan_shadow_address(address);

    bool poisoned = false;
    for (size_t i = 0; i < shadow_size; i++) {
        if (shadow_address[i] != to_underlying(Poison::None)) {
            poisoned = true;
            break;
        }
    }

    if (poisoned) {
        critical_dmesgln("KASAN: invalid {} of size {} on address {:p}", AccessNames[to_underlying(access)], size, address);
        dump_backtrace(g_kasan_is_deadly ? PrintToScreen::Yes : PrintToScreen::No);
        if (g_kasan_is_deadly) {
            critical_dmesgln("KASAN is configured to be deadly, halting the system.");
            Processor::halt();
        }
    }
#endif
}

extern "C" {

// Define a macro to easily declare the KASAN load and store callbacks for
// the various sizes of data type.
//
#define ADDRESS_SANITIZER_LOAD_STORE(size)                                          \
    void __asan_load##size(unsigned long);                                          \
    void __asan_load##size(unsigned long address)                                   \
    {                                                                               \
        shadow_va_check(address, size, Access::Load, __builtin_return_address(0));  \
    }                                                                               \
    void __asan_load##size##_noabort(unsigned long);                                \
    void __asan_load##size##_noabort(unsigned long address)                         \
    {                                                                               \
        shadow_va_check(address, size, Access::Load, __builtin_return_address(0));  \
    }                                                                               \
    void __asan_store##size(unsigned long);                                         \
    void __asan_store##size(unsigned long address)                                  \
    {                                                                               \
        shadow_va_check(address, size, Access::Store, __builtin_return_address(0)); \
    }                                                                               \
    void __asan_store##size##_noabort(unsigned long);                               \
    void __asan_store##size##_noabort(unsigned long address)                        \
    {                                                                               \
        shadow_va_check(address, size, Access::Store, __builtin_return_address(0)); \
    }

ADDRESS_SANITIZER_LOAD_STORE(1);
ADDRESS_SANITIZER_LOAD_STORE(2);
ADDRESS_SANITIZER_LOAD_STORE(4);
ADDRESS_SANITIZER_LOAD_STORE(8);
ADDRESS_SANITIZER_LOAD_STORE(16);

#undef ADDRESS_SANITIZER_LOAD_STORE

void __asan_loadN(unsigned long, size_t);
void __asan_loadN(unsigned long address, size_t size)
{
    shadow_va_check(address, size, Access::Load, __builtin_return_address(0));
}

void __asan_loadN_noabort(unsigned long, size_t);
void __asan_loadN_noabort(unsigned long address, size_t size)
{
    shadow_va_check(address, size, Access::Load, __builtin_return_address(0));
}

void __asan_storeN(unsigned long, size_t);
void __asan_storeN(unsigned long address, size_t size)
{
    shadow_va_check(address, size, Access::Store, __builtin_return_address(0));
}

void __asan_storeN_noabort(unsigned long, size_t);
void __asan_storeN_noabort(unsigned long address, size_t size)
{
    shadow_va_check(address, size, Access::Store, __builtin_return_address(0));
}

// Performs shadow memory cleanup of the current thread's stack before a
// function marked with the [[noreturn]] attribute is called.
//
void __asan_handle_no_return(void);
void __asan_handle_no_return(void)
{
}

void __asan_before_dynamic_init(char const*);
void __asan_before_dynamic_init(char const* /* module_name */)
{
}

void __asan_after_dynamic_init();
void __asan_after_dynamic_init()
{
}
}
