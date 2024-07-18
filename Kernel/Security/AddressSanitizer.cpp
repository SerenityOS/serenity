/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>

#include <Kernel/Arch/Processor.h>
#include <Kernel/Boot/BootInfo.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Security/AddressSanitizer.h>

static constexpr size_t kasan_shadow_scale_offset = 3; // We map each 8 real bytes to 1 shadow byte
static constexpr size_t kasan_shadow_scale = 1 << kasan_shadow_scale_offset;
static constexpr size_t kasan_shadow_mask = kasan_shadow_scale - 1;

// Defined in clang
static constexpr size_t kasan_alloca_redzone_size = 32;

namespace Kernel::AddressSanitizer {

enum class AccessType {
    Load,
    Store
};

static constexpr StringView to_string(AccessType shadow_type)
{
    switch (shadow_type) {
    case AccessType::Load:
        return "Load"sv;
    case AccessType::Store:
        return "Store"sv;
    default:
        return "Unknown"sv;
    }
}

static constexpr StringView to_string(ShadowType shadow_type)
{
    switch (shadow_type) {
    case ShadowType::Unpoisoned8Bytes:
        return "8 Bytes Unpoisoned"sv;
    case ShadowType::Unpoisoned1Byte:
        return "1 Byte Unpoisoned | 7 Bytes Poisoned"sv;
    case ShadowType::Unpoisoned2Bytes:
        return "2 Bytes Unpoisoned | 6 Bytes Poisoned"sv;
    case ShadowType::Unpoisoned3Bytes:
        return "3 Bytes Unpoisoned | 5 Bytes Poisoned"sv;
    case ShadowType::Unpoisoned4Bytes:
        return "4 Bytes Unpoisoned | 4 Bytes Poisoned"sv;
    case ShadowType::Unpoisoned5Bytes:
        return "5 Bytes Unpoisoned | 3 Bytes Poisoned"sv;
    case ShadowType::Unpoisoned6Bytes:
        return "6 Bytes Unpoisoned | 2 Bytes Poisoned"sv;
    case ShadowType::Unpoisoned7Bytes:
        return "7 Bytes Unpoisoned | 1 Byte Poisoned"sv;
    case ShadowType::StackLeft:
        return "Stack Left Redzone"sv;
    case ShadowType::StackMiddle:
        return "Stack Middle Redzone"sv;
    case ShadowType::StackRight:
        return "Stack Right Redzone"sv;
    case ShadowType::UseAfterReturn:
        return "Use After Return"sv;
    case ShadowType::UseAfterScope:
        return "Use After Scope"sv;
    case ShadowType::Generic:
        return "Generic Redzone"sv;
    case ShadowType::Malloc:
        return "Malloc Redzone"sv;
    case ShadowType::Free:
        return "Freed Region"sv;
    default:
        return "Unknown"sv;
    }
}

Atomic<bool> g_kasan_is_deadly { true };

static void print_violation(FlatPtr address, size_t size, AccessType access_type, ShadowType shadow_type, void* return_address)
{
    critical_dmesgln("KASAN: Invalid {}-byte {} access to {}, which is marked as '{}' [at {:p}]", size, to_string(access_type), VirtualAddress(address), to_string(shadow_type), return_address);
    dump_backtrace(g_kasan_is_deadly ? PrintToScreen::Yes : PrintToScreen::No);
    if (g_kasan_is_deadly) {
        critical_dmesgln("KASAN is configured to be deadly, halting the system.");
        Processor::halt();
    }
}

static FlatPtr kasan_shadow_base;
static FlatPtr kasan_shadow_offset;
static bool kasan_initialized = false;

void init(FlatPtr shadow_base)
{
    kasan_shadow_base = shadow_base;
    kasan_shadow_offset = shadow_base - (g_boot_info.kernel_mapping_base >> kasan_shadow_scale_offset);
    kasan_initialized = true;
}

static inline ShadowType* va_to_shadow(FlatPtr address)
{
    return (ShadowType*)((address >> kasan_shadow_scale_offset) + kasan_shadow_offset);
}

void fill_shadow(FlatPtr address, size_t size, ShadowType type)
{
    if (!kasan_initialized) [[unlikely]]
        return;
    VERIFY((address % kasan_shadow_scale) == 0);
    VERIFY((size % kasan_shadow_scale) == 0);
    auto* shadow = va_to_shadow(address);
    auto shadow_size = size >> kasan_shadow_scale_offset;
    memset(shadow, to_underlying(type), shadow_size);
}

void mark_region(FlatPtr address, size_t valid_size, size_t total_size, ShadowType type)
{
    if (!kasan_initialized) [[unlikely]]
        return;
    VERIFY((address % kasan_shadow_scale) == 0);
    VERIFY((total_size % kasan_shadow_scale) == 0);
    auto* shadow = va_to_shadow(address);
    auto valid_shadow_size = valid_size >> kasan_shadow_scale_offset;
    memset(shadow, to_underlying(ShadowType::Unpoisoned8Bytes), valid_shadow_size);
    auto unaligned_size = valid_size & kasan_shadow_mask;
    if (unaligned_size)
        *(shadow + valid_shadow_size) = static_cast<ShadowType>(unaligned_size);
    auto poisoned_shadow_size = (total_size - round_up_to_power_of_two(valid_size, kasan_shadow_scale)) >> kasan_shadow_scale_offset;
    memset(shadow + valid_shadow_size + (unaligned_size != 0), to_underlying(type), poisoned_shadow_size);
}

static bool shadow_va_check_1b(FlatPtr address, ShadowType& shadow_type)
{
    auto const shadow = *va_to_shadow(address);
    i8 const minimal_valid_shadow = (address & kasan_shadow_mask) + 1;
    if (shadow == ShadowType::Unpoisoned8Bytes || (minimal_valid_shadow <= static_cast<i8>(shadow))) [[likely]]
        return true;
    shadow_type = shadow;
    return false;
}

static bool shadow_va_check_2b(FlatPtr address, ShadowType& shadow_type)
{
    // Check for unaligned access
    if ((address >> kasan_shadow_scale_offset) != (address + 1) >> kasan_shadow_scale_offset) [[unlikely]]
        return shadow_va_check_1b(address, shadow_type) && shadow_va_check_1b(address + 1, shadow_type);

    auto const shadow = *va_to_shadow(address);
    i8 const minimal_valid_shadow = ((address + 1) & kasan_shadow_mask) + 1;
    if (shadow == ShadowType::Unpoisoned8Bytes || (minimal_valid_shadow <= static_cast<i8>(shadow))) [[likely]]
        return true;
    shadow_type = shadow;
    return false;
}

static bool shadow_va_check_4b(FlatPtr address, ShadowType& shadow_type)
{
    // Check for unaligned access
    if ((address >> kasan_shadow_scale_offset) != (address + 3) >> kasan_shadow_scale_offset) [[unlikely]]
        return shadow_va_check_2b(address, shadow_type) && shadow_va_check_2b(address + 2, shadow_type);

    auto const shadow = *va_to_shadow(address);
    i8 const minimal_valid_shadow = ((address + 3) & kasan_shadow_mask) + 1;
    if (shadow == ShadowType::Unpoisoned8Bytes || (minimal_valid_shadow <= static_cast<i8>(shadow))) [[likely]]
        return true;
    shadow_type = shadow;
    return false;
}

static bool shadow_va_check_8b(FlatPtr address, ShadowType& shadow_type)
{
    // Check for unaligned access
    if ((address >> kasan_shadow_scale_offset) != (address + 7) >> kasan_shadow_scale_offset) [[unlikely]]
        return shadow_va_check_4b(address, shadow_type) && shadow_va_check_4b(address + 4, shadow_type);

    auto const shadow = *va_to_shadow(address);
    i8 const minimal_valid_shadow = ((address + 7) & kasan_shadow_mask) + 1;
    if (shadow == ShadowType::Unpoisoned8Bytes || (minimal_valid_shadow <= static_cast<i8>(shadow))) [[likely]]
        return true;
    shadow_type = shadow;
    return false;
}

static bool shadow_va_check_Nb(FlatPtr address, size_t n, ShadowType& shadow_type)
{
    while ((address % 8) && (n > 0)) {
        if (!shadow_va_check_1b(address, shadow_type)) [[unlikely]]
            return false;
        address++;
        n--;
    }
    while (n >= 8) {
        if (!shadow_va_check_8b(address, shadow_type))
            return false;
        address += 8;
        n -= 8;
    }
    while (n > 0) {
        if (!shadow_va_check_1b(address, shadow_type)) [[unlikely]]
            return false;
        address++;
        n--;
    }
    return true;
}

static void shadow_va_check(FlatPtr address, size_t size, AccessType access_type, void* return_address)
{
    if (size == 0) [[unlikely]]
        return;
    if (!kasan_initialized) [[unlikely]]
        return;
    if (address < g_boot_info.kernel_mapping_base || address >= kasan_shadow_base) [[unlikely]]
        return;

    bool valid = false;
    ShadowType shadow_type = ShadowType::Unpoisoned8Bytes;
    switch (size) {
    case 1:
        valid = shadow_va_check_1b(address, shadow_type);
        break;
    case 2:
        valid = shadow_va_check_2b(address, shadow_type);
        break;
    case 4:
        valid = shadow_va_check_4b(address, shadow_type);
        break;
    case 8:
        valid = shadow_va_check_8b(address, shadow_type);
        break;
    default:
        valid = shadow_va_check_Nb(address, size, shadow_type);
        break;
    }

    if (valid) [[likely]]
        return;

    print_violation(address, size, access_type, shadow_type, return_address);
}

}

using namespace Kernel;
using namespace Kernel::AddressSanitizer;

extern "C" {

// Define a macro to easily declare the KASAN load and store callbacks for
// the various sizes of data type.
//
#define ADDRESS_SANITIZER_LOAD_STORE(size)                                                                   \
    void __asan_load##size(FlatPtr);                                                                         \
    void __asan_load##size(FlatPtr address)                                                                  \
    {                                                                                                        \
        shadow_va_check(address, size, AccessType::Load, __builtin_return_address(0));                       \
    }                                                                                                        \
    void __asan_load##size##_noabort(FlatPtr);                                                               \
    void __asan_load##size##_noabort(FlatPtr address)                                                        \
    {                                                                                                        \
        shadow_va_check(address, size, AccessType::Load, __builtin_return_address(0));                       \
    }                                                                                                        \
    void __asan_store##size(FlatPtr);                                                                        \
    void __asan_store##size(FlatPtr address)                                                                 \
    {                                                                                                        \
        shadow_va_check(address, size, AccessType::Store, __builtin_return_address(0));                      \
    }                                                                                                        \
    void __asan_store##size##_noabort(FlatPtr);                                                              \
    void __asan_store##size##_noabort(FlatPtr address)                                                       \
    {                                                                                                        \
        shadow_va_check(address, size, AccessType::Store, __builtin_return_address(0));                      \
    }                                                                                                        \
    void __asan_report_load##size(FlatPtr);                                                                  \
    void __asan_report_load##size(FlatPtr address)                                                           \
    {                                                                                                        \
        print_violation(address, size, AccessType::Load, ShadowType::Generic, __builtin_return_address(0));  \
    }                                                                                                        \
    void __asan_report_load##size##_noabort(FlatPtr);                                                        \
    void __asan_report_load##size##_noabort(FlatPtr address)                                                 \
    {                                                                                                        \
        print_violation(address, size, AccessType::Load, ShadowType::Generic, __builtin_return_address(0));  \
    }                                                                                                        \
    void __asan_report_store##size(FlatPtr);                                                                 \
    void __asan_report_store##size(FlatPtr address)                                                          \
    {                                                                                                        \
        print_violation(address, size, AccessType::Store, ShadowType::Generic, __builtin_return_address(0)); \
    }                                                                                                        \
    void __asan_report_store##size##_noabort(FlatPtr);                                                       \
    void __asan_report_store##size##_noabort(FlatPtr address)                                                \
    {                                                                                                        \
        print_violation(address, size, AccessType::Store, ShadowType::Generic, __builtin_return_address(0)); \
    }

ADDRESS_SANITIZER_LOAD_STORE(1);
ADDRESS_SANITIZER_LOAD_STORE(2);
ADDRESS_SANITIZER_LOAD_STORE(4);
ADDRESS_SANITIZER_LOAD_STORE(8);
ADDRESS_SANITIZER_LOAD_STORE(16);

#undef ADDRESS_SANITIZER_LOAD_STORE

void __asan_loadN(FlatPtr, size_t);
void __asan_loadN(FlatPtr address, size_t size)
{
    shadow_va_check(address, size, AccessType::Load, __builtin_return_address(0));
}

void __asan_loadN_noabort(FlatPtr, size_t);
void __asan_loadN_noabort(FlatPtr address, size_t size)
{
    shadow_va_check(address, size, AccessType::Load, __builtin_return_address(0));
}

void __asan_storeN(FlatPtr, size_t);
void __asan_storeN(FlatPtr address, size_t size)
{
    shadow_va_check(address, size, AccessType::Store, __builtin_return_address(0));
}

void __asan_storeN_noabort(FlatPtr, size_t);
void __asan_storeN_noabort(FlatPtr address, size_t size)
{
    shadow_va_check(address, size, AccessType::Store, __builtin_return_address(0));
}

void __asan_report_load_n(FlatPtr, size_t);
void __asan_report_load_n(FlatPtr address, size_t size)
{
    print_violation(address, size, AccessType::Load, ShadowType::Generic, __builtin_return_address(0));
}

void __asan_report_load_n_noabort(FlatPtr, size_t);
void __asan_report_load_n_noabort(FlatPtr address, size_t size)
{
    print_violation(address, size, AccessType::Load, ShadowType::Generic, __builtin_return_address(0));
}

void __asan_report_store_n(FlatPtr, size_t);
void __asan_report_store_n(FlatPtr address, size_t size)
{
    print_violation(address, size, AccessType::Store, ShadowType::Generic, __builtin_return_address(0));
}

void __asan_report_store_n_noabort(FlatPtr, size_t);
void __asan_report_store_n_noabort(FlatPtr address, size_t size)
{
    print_violation(address, size, AccessType::Store, ShadowType::Generic, __builtin_return_address(0));
}

// As defined in the compiler
struct __asan_global_source_location {
    char const* filename;
    int line_number;
    int column_number;
};
struct __asan_global {
    uintptr_t address;
    size_t valid_size;
    size_t total_size;
    char const* name;
    char const* module_name;
    size_t has_dynamic_init;
    struct __asan_global_source_location* location;
    size_t odr_indicator;
};

void __asan_register_globals(struct __asan_global*, size_t);
void __asan_register_globals(struct __asan_global* globals, size_t count)
{
    for (auto i = 0u; i < count; ++i)
        mark_region(globals[i].address, globals[i].valid_size, globals[i].total_size, ShadowType::Generic);
}

void __asan_unregister_globals(struct __asan_global*, size_t);
void __asan_unregister_globals(struct __asan_global* globals, size_t count)
{
    for (auto i = 0u; i < count; ++i)
        mark_region(globals[i].address, globals[i].total_size, globals[i].total_size, ShadowType::Unpoisoned8Bytes);
}

void __asan_alloca_poison(FlatPtr, size_t);
void __asan_alloca_poison(FlatPtr address, size_t size)
{
    VERIFY(address % kasan_alloca_redzone_size == 0);
    auto rounded_size = round_up_to_power_of_two(size, kasan_alloca_redzone_size);
    fill_shadow(address - kasan_alloca_redzone_size, kasan_alloca_redzone_size, ShadowType::StackLeft);
    mark_region(address, size, rounded_size, Kernel::AddressSanitizer::ShadowType::StackMiddle);
    fill_shadow(address + rounded_size, kasan_alloca_redzone_size, Kernel::AddressSanitizer::ShadowType::StackRight);
}

void __asan_allocas_unpoison(FlatPtr, size_t);
void __asan_allocas_unpoison(FlatPtr start, size_t end)
{
    VERIFY(start >= end);
    auto size = end - start;
    VERIFY(size % kasan_shadow_scale == 0);
    fill_shadow(start, size, Kernel::AddressSanitizer::ShadowType::Unpoisoned8Bytes);
}

void __asan_poison_stack_memory(FlatPtr, size_t);
void __asan_poison_stack_memory(FlatPtr address, size_t size)
{
    fill_shadow(address, round_up_to_power_of_two(size, kasan_shadow_scale), Kernel::AddressSanitizer::ShadowType::UseAfterScope);
}

void __asan_unpoison_stack_memory(FlatPtr, size_t);
void __asan_unpoison_stack_memory(FlatPtr address, size_t size)
{
    fill_shadow(address, round_up_to_power_of_two(size, kasan_shadow_scale), Kernel::AddressSanitizer::ShadowType::Unpoisoned8Bytes);
}

void __asan_handle_no_return(void);
void __asan_handle_no_return(void)
{
}

void __asan_before_dynamic_init(char const*);
void __asan_before_dynamic_init(char const*)
{
}

void __asan_after_dynamic_init();
void __asan_after_dynamic_init()
{
}
}
