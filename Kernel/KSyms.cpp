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
    // FIXME: It's gross that this vector grows dynamically rather than being sized-to-fit.
    //        We're wasting that eternal kmalloc memory.
    auto* bufptr = (const char*)buffer.pointer();
    auto* start_of_name = bufptr;
    dword address = 0;

    for (unsigned i = 0; i < 8; ++i)
        ksym_count = (ksym_count << 4) | parse_hex_digit(*(bufptr++));
    s_ksyms = static_cast<KSym*>(kmalloc_eternal(sizeof(KSym) * ksym_count));
    ++bufptr; // skip newline

    kprintf("Loading ksyms: \033[s");

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

        if ((current_ksym_index % 10) == 0 || ksym_count == current_ksym_index)
            kprintf("\033[u\033[s%u/%u", current_ksym_index, ksym_count);
        ++bufptr;
        ++current_ksym_index;
    }
    kprintf("\n");
    ksyms_ready = true;
}

void dump_backtrace(bool use_ksyms)
{
    if (!current) {
        HANG;
        return;
    }
    if (use_ksyms && !ksyms_ready) {
        HANG;
        return;
    }
    struct RecognizedSymbol {
        dword address;
        const KSym* ksym;
    };
    Vector<RecognizedSymbol> recognized_symbols;
    if (use_ksyms) {
        for (dword* stackPtr = (dword*)&use_ksyms; current->validate_read_from_kernel(LinearAddress((dword)stackPtr)); stackPtr = (dword*)*stackPtr) {
            dword retaddr = stackPtr[1];
            if (auto* ksym = ksymbolicate(retaddr))
                recognized_symbols.append({ retaddr, ksym });
        }
    } else{
        for (dword* stackPtr = (dword*)&use_ksyms; current->validate_read_from_kernel(LinearAddress((dword)stackPtr)); stackPtr = (dword*)*stackPtr) {
            dword retaddr = stackPtr[1];
            kprintf("%x (next: %x)\n", retaddr, stackPtr ? (dword*)*stackPtr : 0);
        }
        return;
    }
    size_t bytesNeeded = 0;
    for (auto& symbol : recognized_symbols) {
        bytesNeeded += strlen(symbol.ksym->name) + 8 + 16;
    }
    for (auto& symbol : recognized_symbols) {
        unsigned offset = symbol.address - symbol.ksym->address;
        dbgprintf("%p  %s +%u\n", symbol.address, symbol.ksym->name, offset);
    }
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
    auto descriptor = VFS::the().open("/kernel.map", error);
    if (!descriptor) {
        kprintf("Failed to open /kernel.map\n");
    } else {
        auto buffer = descriptor->read_entire_file();
        ASSERT(buffer);
        load_ksyms_from_data(buffer);
    }
}

