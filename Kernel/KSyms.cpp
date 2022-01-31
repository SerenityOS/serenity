/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/Arch/x86/SafeMem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>

namespace Kernel {

FlatPtr g_lowest_kernel_symbol_address = 0xffffffff;
FlatPtr g_highest_kernel_symbol_address = 0;
bool g_kernel_symbols_available = false;

extern "C" {
__attribute__((section(".kernel_symbols"))) char kernel_symbols[5 * MiB] {};
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
        const auto& symbol = s_symbols[i];
        if (name == symbol.name)
            return symbol.address;
    }
    return 0;
}

const KernelSymbol* symbolicate_kernel_address(FlatPtr address)
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
        ksym.address = kernel_load_base + address;
        ksym.name = start_of_name;

        *bufptr = '\0';

        if (ksym.address < g_lowest_kernel_symbol_address)
            g_lowest_kernel_symbol_address = ksym.address;
        if (ksym.address > g_highest_kernel_symbol_address)
            g_highest_kernel_symbol_address = ksym.address;

        ++bufptr;
        ++current_symbol_index;
    }
    g_kernel_symbols_available = true;
}

NEVER_INLINE static void dump_backtrace_impl(FlatPtr base_pointer, bool use_ksyms, PrintToScreen print_to_screen)
{
#define PRINT_LINE(fmtstr, ...)                    \
    do {                                           \
        if (print_to_screen == PrintToScreen::No)  \
            dbgln(fmtstr, __VA_ARGS__);            \
        else                                       \
            critical_dmesgln(fmtstr, __VA_ARGS__); \
    } while (0)

    SmapDisabler disabler;
    if (use_ksyms && !g_kernel_symbols_available)
        Processor::halt();

    struct RecognizedSymbol {
        FlatPtr address;
        const KernelSymbol* symbol { nullptr };
    };
    constexpr size_t max_recognized_symbol_count = 256;
    RecognizedSymbol recognized_symbols[max_recognized_symbol_count];
    size_t recognized_symbol_count = 0;
    if (use_ksyms) {
        FlatPtr copied_stack_ptr[2];
        for (FlatPtr* stack_ptr = (FlatPtr*)base_pointer; stack_ptr && recognized_symbol_count < max_recognized_symbol_count; stack_ptr = (FlatPtr*)copied_stack_ptr[0]) {
            if ((FlatPtr)stack_ptr < kernel_load_base)
                break;

            void* fault_at;
            if (!safe_memcpy(copied_stack_ptr, stack_ptr, sizeof(copied_stack_ptr), fault_at))
                break;
            FlatPtr retaddr = copied_stack_ptr[1];
            recognized_symbols[recognized_symbol_count++] = { retaddr, symbolicate_kernel_address(retaddr) };
        }
    } else {
        void* fault_at;
        FlatPtr copied_stack_ptr[2];
        FlatPtr* stack_ptr = (FlatPtr*)base_pointer;
        while (stack_ptr && safe_memcpy(copied_stack_ptr, stack_ptr, sizeof(copied_stack_ptr), fault_at)) {
            FlatPtr retaddr = copied_stack_ptr[1];
            PRINT_LINE("{:p} (next: {:p})", retaddr, stack_ptr ? (FlatPtr*)copied_stack_ptr[0] : 0);
            stack_ptr = (FlatPtr*)copied_stack_ptr[0];
        }
        return;
    }
    VERIFY(recognized_symbol_count <= max_recognized_symbol_count);
    for (size_t i = 0; i < recognized_symbol_count; ++i) {
        auto& symbol = recognized_symbols[i];
        if (!symbol.address)
            break;
        if (!symbol.symbol) {
            PRINT_LINE("Kernel + {:p}", symbol.address - kernel_load_base);
            continue;
        }
        size_t offset = symbol.address - symbol.symbol->address;
        if (symbol.symbol->address == g_highest_kernel_symbol_address && offset > 4096)
            PRINT_LINE("Kernel + {:p}", symbol.address - kernel_load_base);
        else
            PRINT_LINE("Kernel + {:p}  {} +{:#x}", symbol.address - kernel_load_base, symbol.symbol->name, offset);
    }
}

void dump_backtrace(PrintToScreen print_to_screen)
{
    static bool in_dump_backtrace = false;
    if (in_dump_backtrace)
        return;
    TemporaryChange change(in_dump_backtrace, true);
    TemporaryChange disable_kmalloc_stacks(g_dump_kmalloc_stacks, false);
    FlatPtr base_pointer;
#if ARCH(I386)
    asm volatile("movl %%ebp, %%eax"
                 : "=a"(base_pointer));
#else
    asm volatile("movq %%rbp, %%rax"
                 : "=a"(base_pointer));
#endif
    dump_backtrace_impl(base_pointer, g_kernel_symbols_available, print_to_screen);
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
