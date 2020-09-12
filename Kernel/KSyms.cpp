/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Demangle.h>
#include <AK/TemporaryChange.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>
#include <LibELF/Loader.h>

namespace Kernel {

FlatPtr g_lowest_kernel_symbol_address = 0xffffffff;
FlatPtr g_highest_kernel_symbol_address = 0;
bool g_kernel_symbols_available = false;

static KernelSymbol* s_symbols;
static size_t s_symbol_count = 0;

static u8 parse_hex_digit(char nibble)
{
    if (nibble >= '0' && nibble <= '9')
        return nibble - '0';
    ASSERT(nibble >= 'a' && nibble <= 'f');
    return 10 + (nibble - 'a');
}

u32 address_for_kernel_symbol(const StringView& name)
{
    for (size_t i = 0; i < s_symbol_count; ++i) {
        if (!strncmp(name.characters_without_null_termination(), s_symbols[i].name, name.length()))
            return s_symbols[i].address;
    }
    return 0;
}

const KernelSymbol* symbolicate_kernel_address(u32 address)
{
    if (address < g_lowest_kernel_symbol_address || address > g_highest_kernel_symbol_address)
        return nullptr;
    for (unsigned i = 0; i < s_symbol_count; ++i) {
        if (address < s_symbols[i + 1].address)
            return &s_symbols[i];
    }
    return nullptr;
}

static void load_kernel_sybols_from_data(const KBuffer& buffer)
{
    g_lowest_kernel_symbol_address = 0xffffffff;
    g_highest_kernel_symbol_address = 0;

    auto* bufptr = (const char*)buffer.data();
    auto* start_of_name = bufptr;
    FlatPtr address = 0;

    for (size_t i = 0; i < 8; ++i)
        s_symbol_count = (s_symbol_count << 4) | parse_hex_digit(*(bufptr++));
    s_symbols = static_cast<KernelSymbol*>(kmalloc_eternal(sizeof(KernelSymbol) * s_symbol_count));
    ++bufptr; // skip newline

    klog() << "Loading kernel symbol table...";

    size_t current_symbol_index = 0;

    while (bufptr < buffer.end_pointer()) {
        for (size_t i = 0; i < 8; ++i)
            address = (address << 4) | parse_hex_digit(*(bufptr++));
        bufptr += 3;
        start_of_name = bufptr;
        while (*(++bufptr)) {
            if (*bufptr == '\n') {
                break;
            }
        }
        auto& ksym = s_symbols[current_symbol_index];
        ksym.address = address;
        char* name = static_cast<char*>(kmalloc_eternal((bufptr - start_of_name) + 1));
        memcpy(name, start_of_name, bufptr - start_of_name);
        name[bufptr - start_of_name] = '\0';
        ksym.name = name;

        if (ksym.address < g_lowest_kernel_symbol_address)
            g_lowest_kernel_symbol_address = ksym.address;
        if (ksym.address > g_highest_kernel_symbol_address)
            g_highest_kernel_symbol_address = ksym.address;

        ++bufptr;
        ++current_symbol_index;
    }
    g_kernel_symbols_available = true;
}

NEVER_INLINE static void dump_backtrace_impl(FlatPtr base_pointer, bool use_ksyms)
{
    SmapDisabler disabler;
#if 0
    if (!current) {
        //hang();
        return;
    }
#endif
    if (use_ksyms && !g_kernel_symbols_available) {
        Processor::halt();
        return;
    }

    OwnPtr<Process::ELFBundle> elf_bundle;
    auto current_process = Process::current();
    if (current_process)
        elf_bundle = current_process->elf_bundle();

    struct RecognizedSymbol {
        FlatPtr address;
        const KernelSymbol* symbol { nullptr };
    };
    size_t max_recognized_symbol_count = 256;
    RecognizedSymbol recognized_symbols[max_recognized_symbol_count];
    size_t recognized_symbol_count = 0;
    if (use_ksyms) {
        FlatPtr copied_stack_ptr[2];
        for (FlatPtr* stack_ptr = (FlatPtr*)base_pointer; stack_ptr && recognized_symbol_count < max_recognized_symbol_count; stack_ptr = (FlatPtr*)copied_stack_ptr[0]) {
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
            dbg() << String::format("%x", retaddr) << " (next: " << String::format("%x", (stack_ptr ? (u32*)copied_stack_ptr[0] : 0)) << ")";
            stack_ptr = (FlatPtr*)copied_stack_ptr[0];
        }
        return;
    }
    ASSERT(recognized_symbol_count <= max_recognized_symbol_count);
    for (size_t i = 0; i < recognized_symbol_count; ++i) {
        auto& symbol = recognized_symbols[i];
        if (!symbol.address)
            break;
        if (!symbol.symbol) {
            if (elf_bundle && elf_bundle->elf_loader->has_symbols()) {
                dbg() << String::format("%p", symbol.address) << "  " << elf_bundle->elf_loader->symbolicate(symbol.address);
            } else {
                dbg() << String::format("%p", symbol.address) << " (no ELF symbols for process)";
            }
            continue;
        }
        size_t offset = symbol.address - symbol.symbol->address;
        if (symbol.symbol->address == g_highest_kernel_symbol_address && offset > 4096)
            dbg() << String::format("%p", symbol.address);
        else
            dbg() << String::format("%p", symbol.address) << "  " << demangle(symbol.symbol->name) << " +" << offset;
    }
}

void dump_backtrace()
{
    static bool in_dump_backtrace = false;
    if (in_dump_backtrace)
        return;
    TemporaryChange change(in_dump_backtrace, true);
    TemporaryChange disable_kmalloc_stacks(g_dump_kmalloc_stacks, false);
    FlatPtr ebp;
    asm volatile("movl %%ebp, %%eax"
                 : "=a"(ebp));
    dump_backtrace_impl(ebp, g_kernel_symbols_available);
}

void load_kernel_symbol_table()
{
    auto result = VFS::the().open("/res/kernel.map", O_RDONLY, 0, VFS::the().root_custody());
    ASSERT(!result.is_error());
    auto description = result.value();
    auto buffer = description->read_entire_file();
    ASSERT(!buffer.is_error());
    load_kernel_sybols_from_data(buffer.value());
}

}
