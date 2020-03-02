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
#include <LibELF/ELFLoader.h>

namespace Kernel {

static KSym* s_ksyms;
u32 ksym_lowest_address = 0xffffffff;
u32 ksym_highest_address = 0;
u32 ksym_count = 0;
bool ksyms_ready = false;

static u8 parse_hex_digit(char nibble)
{
    if (nibble >= '0' && nibble <= '9')
        return nibble - '0';
    ASSERT(nibble >= 'a' && nibble <= 'f');
    return 10 + (nibble - 'a');
}

u32 address_for_kernel_symbol(const StringView& name)
{
    for (unsigned i = 0; i < ksym_count; ++i) {
        if (!strncmp(name.characters_without_null_termination(), s_ksyms[i].name, name.length()))
            return s_ksyms[i].address;
    }
    return 0;
}

const KSym* ksymbolicate(u32 address)
{
    if (address < ksym_lowest_address || address > ksym_highest_address)
        return nullptr;
    for (unsigned i = 0; i < ksym_count; ++i) {
        if (address < s_ksyms[i + 1].address)
            return &s_ksyms[i];
    }
    return nullptr;
}

static void load_ksyms_from_data(const ByteBuffer& buffer)
{
    ksym_lowest_address = 0xffffffff;
    ksym_highest_address = 0;
    auto* bufptr = (const char*)buffer.data();
    auto* start_of_name = bufptr;
    u32 address = 0;

    for (unsigned i = 0; i < 8; ++i)
        ksym_count = (ksym_count << 4) | parse_hex_digit(*(bufptr++));
    s_ksyms = static_cast<KSym*>(kmalloc_eternal(sizeof(KSym) * ksym_count));
    ++bufptr; // skip newline

    kprintf("Loading ksyms...");

    unsigned current_ksym_index = 0;

    while (bufptr < buffer.end_pointer()) {
        for (unsigned i = 0; i < 8; ++i)
            address = (address << 4) | parse_hex_digit(*(bufptr++));
        bufptr += 3;
        start_of_name = bufptr;
        while (*(++bufptr)) {
            if (*bufptr == '\n') {
                break;
            }
        }
        auto& ksym = s_ksyms[current_ksym_index];
        ksym.address = address;
        char* name = static_cast<char*>(kmalloc_eternal((bufptr - start_of_name) + 1));
        memcpy(name, start_of_name, bufptr - start_of_name);
        name[bufptr - start_of_name] = '\0';
        ksym.name = name;

        if (ksym.address < ksym_lowest_address)
            ksym_lowest_address = ksym.address;
        if (ksym.address > ksym_highest_address)
            ksym_highest_address = ksym.address;

        ++bufptr;
        ++current_ksym_index;
    }
    kprintf("ok\n");
    ksyms_ready = true;
}

[[gnu::noinline]] void dump_backtrace_impl(u32 ebp, bool use_ksyms)
{
    SmapDisabler disabler;
#if 0
    if (!current) {
        //hang();
        return;
    }
#endif
    if (use_ksyms && !ksyms_ready) {
        hang();
        return;
    }

    OwnPtr<Process::ELFBundle> elf_bundle;
    if (Process::current)
        elf_bundle = Process::current->elf_bundle();

    struct RecognizedSymbol {
        u32 address;
        const KSym* ksym;
    };
    int max_recognized_symbol_count = 256;
    RecognizedSymbol recognized_symbols[max_recognized_symbol_count];
    int recognized_symbol_count = 0;
    if (use_ksyms) {
        for (u32* stack_ptr = (u32*)ebp;
             (Process::current ? Process::current->validate_read_from_kernel(VirtualAddress(stack_ptr), sizeof(void*) * 2) : 1) && recognized_symbol_count < max_recognized_symbol_count; stack_ptr = (u32*)*stack_ptr) {
            u32 retaddr = stack_ptr[1];
            recognized_symbols[recognized_symbol_count++] = { retaddr, ksymbolicate(retaddr) };
        }
    } else {
        for (u32* stack_ptr = (u32*)ebp;
             (Process::current ? Process::current->validate_read_from_kernel(VirtualAddress(stack_ptr), sizeof(void*) * 2) : 1); stack_ptr = (u32*)*stack_ptr) {
            u32 retaddr = stack_ptr[1];
            dbg() << String::format("%x", retaddr) << " (next: " << String::format("%x", (stack_ptr ? (u32*)*stack_ptr : 0)) << ")";
        }
        return;
    }
    ASSERT(recognized_symbol_count <= max_recognized_symbol_count);
    for (int i = 0; i < recognized_symbol_count; ++i) {
        auto& symbol = recognized_symbols[i];
        if (!symbol.address)
            break;
        if (!symbol.ksym) {
            if (elf_bundle && elf_bundle->elf_loader->has_symbols()) {
                dbg() << String::format("%p", symbol.address) << "  " << elf_bundle->elf_loader->symbolicate(symbol.address);
            } else {
                dbg() << String::format("%p", symbol.address) << " (no ELF symbols for process)";
            }
            continue;
        }
        unsigned offset = symbol.address - symbol.ksym->address;
        if (symbol.ksym->address == ksym_highest_address && offset > 4096)
            dbg() << String::format("%p", symbol.address);
        else
            dbg() << String::format("%p", symbol.address) << "  " << demangle(symbol.ksym->name) << " +" << offset;
    }
}

void dump_backtrace()
{
    static bool in_dump_backtrace = false;
    if (in_dump_backtrace)
        return;
    TemporaryChange change(in_dump_backtrace, true);
    TemporaryChange disable_kmalloc_stacks(g_dump_kmalloc_stacks, false);
    u32 ebp;
    asm volatile("movl %%ebp, %%eax"
                 : "=a"(ebp));
    dump_backtrace_impl(ebp, ksyms_ready);
}

void load_ksyms()
{
    auto result = VFS::the().open("/res/kernel.map", O_RDONLY, 0, VFS::the().root_custody());
    ASSERT(!result.is_error());
    auto description = result.value();
    auto buffer = description->read_entire_file();
    ASSERT(buffer);
    load_ksyms_from_data(buffer);
}

}
