#include "KSyms.h"
#include "Process.h"
#include "Scheduler.h"
#include <AK/ELF/ELFLoader.h>
#include <AK/TemporaryChange.h>
#include <Kernel/FileSystem/FileDescription.h>

static KSym* s_ksyms;
u32 ksym_lowest_address;
u32 ksym_highest_address;
u32 ksym_count;
bool ksyms_ready;

static u8 parse_hex_digit(char nibble)
{
    if (nibble >= '0' && nibble <= '9')
        return nibble - '0';
    ASSERT(nibble >= 'a' && nibble <= 'f');
    return 10 + (nibble - 'a');
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
    auto* bufptr = (const char*)buffer.pointer();
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
    if (!current) {
        //hang();
        return;
    }
    if (use_ksyms && !ksyms_ready) {
        hang();
        return;
    }
    struct RecognizedSymbol {
        u32 address;
        const KSym* ksym;
    };
    int max_recognized_symbol_count = 256;
    RecognizedSymbol recognized_symbols[max_recognized_symbol_count];
    int recognized_symbol_count = 0;
    if (use_ksyms) {
        for (u32* stack_ptr = (u32*)ebp; current->process().validate_read_from_kernel(VirtualAddress((u32)stack_ptr)) && recognized_symbol_count < max_recognized_symbol_count; stack_ptr = (u32*)*stack_ptr) {
            u32 retaddr = stack_ptr[1];
            recognized_symbols[recognized_symbol_count++] = { retaddr, ksymbolicate(retaddr) };
        }
    } else {
        for (u32* stack_ptr = (u32*)ebp; current->process().validate_read_from_kernel(VirtualAddress((u32)stack_ptr)); stack_ptr = (u32*)*stack_ptr) {
            u32 retaddr = stack_ptr[1];
            dbgprintf("%x (next: %x)\n", retaddr, stack_ptr ? (u32*)*stack_ptr : 0);
        }
        return;
    }
    ASSERT(recognized_symbol_count <= max_recognized_symbol_count);
    size_t bytes_needed = 0;
    for (int i = 0; i < recognized_symbol_count; ++i) {
        auto& symbol = recognized_symbols[i];
        bytes_needed += (symbol.ksym ? strlen(symbol.ksym->name) : 0) + 8 + 16;
    }
    for (int i = 0; i < recognized_symbol_count; ++i) {
        auto& symbol = recognized_symbols[i];
        if (!symbol.address)
            break;
        if (!symbol.ksym) {
            if (current->process().elf_loader() && current->process().elf_loader()->has_symbols()) {
                dbgprintf("%p  %s\n", symbol.address, current->process().elf_loader()->symbolicate(symbol.address).characters());
            } else {
                dbgprintf("%p (no ELF symbols for process)\n", symbol.address);
            }
            continue;
        }
        unsigned offset = symbol.address - symbol.ksym->address;
        if (symbol.ksym->address == ksym_highest_address && offset > 4096)
            dbgprintf("%p\n", symbol.address);
        else
            dbgprintf("%p  %s +%u\n", symbol.address, symbol.ksym->name, offset);
    }
}

void dump_backtrace()
{
    static bool in_dump_backtrace = false;
    if (in_dump_backtrace) {
        dbgprintf("dump_backtrace() called from within itself, what the hell is going on!\n");
        return;
    }
    TemporaryChange change(in_dump_backtrace, true);
    u32 ebp;
    asm volatile("movl %%ebp, %%eax"
                 : "=a"(ebp));
    dump_backtrace_impl(ebp, ksyms_ready);
}

void init_ksyms()
{
    ksyms_ready = false;
    ksym_lowest_address = 0xffffffff;
    ksym_highest_address = 0;
    ksym_count = 0;
}

void load_ksyms()
{
    auto result = VFS::the().open("/kernel.map", 0, 0, VFS::the().root_custody());
    ASSERT(!result.is_error());
    auto description = result.value();
    auto buffer = description->read_entire_file();
    ASSERT(buffer);
    load_ksyms_from_data(buffer);
}
