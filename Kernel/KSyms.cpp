/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StackUnwinder.h>
#include <AK/TemporaryChange.h>
#include <Kernel/Arch/SafeMem.h>
#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/KSyms.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel {

FlatPtr g_lowest_kernel_symbol_address = 0xffffffff;
FlatPtr g_highest_kernel_symbol_address = 0;
SetOnce g_kernel_symbols_available;

extern "C" {
__attribute__((section(".kernel_symbols"))) char kernel_symbols[8 * MiB] {};
}

static KernelSymbol* s_symbols;
static size_t s_symbol_count = 0;

UNMAP_AFTER_INIT static u8 parse_hex_digit(char nibble)
{
    if (nibble >= '0' && nibble <= '9')
        return nibble - '0';
    VERIFY(nibble >= 'a' && nibble <= 'f');
    return 10 + (nibble - 'a');
}

FlatPtr address_for_kernel_symbol(StringView name)
{
    for (size_t i = 0; i < s_symbol_count; ++i) {
        auto const& symbol = s_symbols[i];
        if (name == symbol.name)
            return symbol.address;
    }
    return 0;
}

KernelSymbol const* symbolicate_kernel_address(FlatPtr address)
{
    if (address < g_lowest_kernel_symbol_address || address > g_highest_kernel_symbol_address)
        return nullptr;
    for (unsigned i = 0; i < s_symbol_count; ++i) {
        if (address < s_symbols[i + 1].address)
            return &s_symbols[i];
    }
    return nullptr;
}

UNMAP_AFTER_INIT static void load_kernel_symbols_from_data(Bytes buffer)
{
    g_lowest_kernel_symbol_address = 0xffffffff;
    g_highest_kernel_symbol_address = 0;

    auto* bufptr = (char*)buffer.data();
    auto* start_of_name = bufptr;
    FlatPtr address = 0;

    for (size_t i = 0; i < 8; ++i)
        s_symbol_count = (s_symbol_count << 4) | parse_hex_digit(*(bufptr++));
    s_symbols = static_cast<KernelSymbol*>(kmalloc(sizeof(KernelSymbol) * s_symbol_count));
    ++bufptr; // skip newline

    dmesgln("Loading kernel symbol table...");

    size_t current_symbol_index = 0;

    while ((u8 const*)bufptr < buffer.data() + buffer.size()) {
        for (size_t i = 0; i < sizeof(void*) * 2; ++i)
            address = (address << 4) | parse_hex_digit(*(bufptr++));
        bufptr += 3;
        start_of_name = bufptr;
        while (*(++bufptr)) {
            if (*bufptr == '\n') {
                break;
            }
        }
        auto& ksym = s_symbols[current_symbol_index];

#if ARCH(AARCH64)
        // Currently, the AArch64 kernel is linked at a high virtual memory address, instead
        // of zero, so the address of a symbol does not need to be offset by the g_boot_info.kernel_load_base.
        ksym.address = address;
#else
        ksym.address = g_boot_info.kernel_load_base + address;
#endif

        ksym.name = start_of_name;

        *bufptr = '\0';

        if (ksym.address < g_lowest_kernel_symbol_address)
            g_lowest_kernel_symbol_address = ksym.address;
        if (ksym.address > g_highest_kernel_symbol_address)
            g_highest_kernel_symbol_address = ksym.address;

        ++bufptr;
        ++current_symbol_index;
    }
    g_kernel_symbols_available.set();
}

NEVER_INLINE static void dump_backtrace_impl(FlatPtr frame_pointer, bool use_ksyms, PrintToScreen print_to_screen)
{
#define PRINT_LINE(fmtstr, ...)                    \
    do {                                           \
        if (print_to_screen == PrintToScreen::No)  \
            dbgln(fmtstr, __VA_ARGS__);            \
        else                                       \
            critical_dmesgln(fmtstr, __VA_ARGS__); \
    } while (0)

    SmapDisabler disabler;
    if (use_ksyms && !g_kernel_symbols_available.was_set())
        Processor::halt();

    struct RecognizedSymbol {
        FlatPtr address;
        KernelSymbol const* symbol { nullptr };
    };

    constexpr size_t max_recognized_symbol_count = 256;
    RecognizedSymbol recognized_symbols[max_recognized_symbol_count];
    size_t recognized_symbol_count = 0;

    MUST(AK::unwind_stack_from_frame_pointer(
        frame_pointer,
        [](FlatPtr address) -> ErrorOr<FlatPtr> {
            if (address < g_boot_info.kernel_mapping_base)
                return EINVAL;

            FlatPtr value;
            void* fault_at;
            if (!safe_memcpy(&value, bit_cast<FlatPtr*>(address), sizeof(FlatPtr), fault_at))
                return EFAULT;

            return value;
        },
        [use_ksyms, print_to_screen, &recognized_symbol_count, &recognized_symbols](AK::StackFrame stack_frame) -> ErrorOr<IterationDecision> {
            if (use_ksyms) {
                if (recognized_symbol_count >= max_recognized_symbol_count)
                    return IterationDecision::Break;

                recognized_symbols[recognized_symbol_count++] = { stack_frame.return_address, symbolicate_kernel_address(stack_frame.return_address) };
            } else {
                PRINT_LINE("{:p}", stack_frame.return_address);
            }
            return IterationDecision::Continue;
        }));

    if (!use_ksyms)
        return;

    VERIFY(recognized_symbol_count <= max_recognized_symbol_count);
    for (size_t i = 0; i < recognized_symbol_count; ++i) {
        auto& symbol = recognized_symbols[i];
        if (!symbol.address)
            break;
        if (!symbol.symbol) {
            PRINT_LINE("Kernel + {:p}", symbol.address - g_boot_info.kernel_load_base);
            continue;
        }
        size_t offset = symbol.address - symbol.symbol->address;
        if (symbol.symbol->address == g_highest_kernel_symbol_address && offset > 4096)
            PRINT_LINE("Kernel + {:p}", symbol.address - g_boot_info.kernel_load_base);
        else
            PRINT_LINE("Kernel + {:p}  {} +{:#x}", symbol.address - g_boot_info.kernel_load_base, symbol.symbol->name, offset);
    }
}

void dump_backtrace_from_base_pointer(FlatPtr base_pointer)
{
    // FIXME: Change signature of dump_backtrace_impl to use an enum instead of a bool.
    dump_backtrace_impl(base_pointer, /*use_ksym=*/false, PrintToScreen::No);
}

void dump_backtrace(PrintToScreen print_to_screen)
{
    static bool in_dump_backtrace = false;
    if (in_dump_backtrace)
        return;
    TemporaryChange change(in_dump_backtrace, true);
    TemporaryChange disable_kmalloc_stacks(g_dump_kmalloc_stacks, false);

    FlatPtr base_pointer = (FlatPtr)__builtin_frame_address(0);
    dump_backtrace_impl(base_pointer, g_kernel_symbols_available.was_set(), print_to_screen);
}

UNMAP_AFTER_INIT void load_kernel_symbol_table()
{
    auto kernel_symbols_size = strnlen(kernel_symbols, sizeof(kernel_symbols));
    // If we're hitting this VERIFY the kernel symbol file has grown beyond
    // the array size of kernel_symbols. Try making the array larger.
    VERIFY(kernel_symbols_size != sizeof(kernel_symbols));
    load_kernel_symbols_from_data({ kernel_symbols, kernel_symbols_size });
}

}
