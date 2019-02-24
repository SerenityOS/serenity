#include "KSyms.h"
#include "Process.h"
#include "Scheduler.h"

static KSym* s_ksyms;
dword ksym_lowest_address;
dword ksym_highest_address;
dword ksym_count;
bool ksyms_ready;

static byte parse_hex_digit(char nibble)
{
    if (nibble >= '0' && nibble <= '9')
        return nibble - '0';
    ASSERT(nibble >= 'a' && nibble <= 'f');
    return 10 + (nibble - 'a');
}

const KSym* ksymbolicate(dword address)
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
    auto* bufptr = (const char*)buffer.pointer();
    auto* start_of_name = bufptr;
    dword address = 0;

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

[[gnu::noinline]] void dump_backtrace_impl(dword ebp, bool use_ksyms)
{
    if (!current) {
        hang();
        return;
    }
    if (use_ksyms && !ksyms_ready) {
        hang();
        return;
    }
    struct RecognizedSymbol {
        dword address;
        const KSym* ksym;
    };
    Vector<RecognizedSymbol> recognized_symbols;
    if (use_ksyms) {
        for (dword* stack_ptr = (dword*)ebp; current->validate_read_from_kernel(LinearAddress((dword)stack_ptr)); stack_ptr = (dword*)*stack_ptr) {
            dword retaddr = stack_ptr[1];
            if (auto* ksym = ksymbolicate(retaddr))
                recognized_symbols.append({ retaddr, ksym });
        }
    } else{
        for (dword* stack_ptr = (dword*)ebp; current->validate_read_from_kernel(LinearAddress((dword)stack_ptr)); stack_ptr = (dword*)*stack_ptr) {
            dword retaddr = stack_ptr[1];
            kprintf("%x (next: %x)\n", retaddr, stack_ptr ? (dword*)*stack_ptr : 0);
        }
        return;
    }
    size_t bytes_needed = 0;
    for (auto& symbol : recognized_symbols) {
        bytes_needed += strlen(symbol.ksym->name) + 8 + 16;
    }
    for (auto& symbol : recognized_symbols) {
        unsigned offset = symbol.address - symbol.ksym->address;
        kprintf("%p  %s +%u\n", symbol.address, symbol.ksym->name, offset);
    }
}

void dump_backtrace(bool use_ksyms)
{
    dword ebp;
    asm volatile("movl %%ebp, %%eax":"=a"(ebp));
    dump_backtrace_impl(ebp, use_ksyms);
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
    int error;
    auto descriptor = VFS::the().open("/kernel.map", error, 0, 0, *VFS::the().root_inode());
    if (!descriptor) {
        kprintf("Failed to open /kernel.map\n");
    } else {
        auto buffer = descriptor->read_entire_file(*current);
        ASSERT(buffer);
        load_ksyms_from_data(buffer);
    }
}

