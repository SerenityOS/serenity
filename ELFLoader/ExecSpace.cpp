#include "ExecSpace.h"
#include "ELFLoader.h"
#include <AK/Types.h>

#ifndef SERENITY
#include <AK/TemporaryFile.h>
#endif

//#define EXECSPACE_DEBUG

ExecSpace::ExecSpace()
{
    initializeBuiltins();
}

ExecSpace::~ExecSpace()
{
}

#ifdef SERENITY
int puts(const char* str)
{
    kprintf("%s\n", str);
    return 0;
}
#endif

void ExecSpace::initializeBuiltins()
{
#ifndef SERENITY
    m_symbols.set("puts", { (char*)puts, 0 });
#endif
}

#ifdef SERENITY
bool ExecSpace::loadELF(ByteBuffer&& file)
#else
bool ExecSpace::loadELF(MappedFile&& file)
#endif
{
    ELFLoader loader(*this, move(file));
    if (!loader.load())
        return false;
#ifdef EXECSPACE_DEBUG
    kprintf("ExecSpace: ELF loaded, symbol map now:\n");
    for (auto& s : m_symbols) {
        kprintf("> %p: %s (%u)\n",
                s.value.ptr,
                s.key.characters(),
                s.value.size);
    }
#endif
    return true;
}

#ifdef EXECSPACE_DEBUG
static void disassemble(const char* data, size_t length)
{
    if (!length)
        return;

#ifdef SERENITY
    for (unsigned i = 0; i < length; ++i) {
        kprintf("%b ", (unsigned char)data[i]);
    }
    kprintf("\n");
#else
    TemporaryFile temp;
    if (!temp.isValid()) {
        fprintf(stderr, "Unable to create temp file for disassembly.\n");
        return;
    }
    fprintf(temp.stream(), "db ");
    for (unsigned i = 0; i < length; ++i) {
        fprintf(temp.stream(), "0x%02x, ", (unsigned char)data[i]);
    }
    fprintf(temp.stream(), "\n");
    temp.sync();

    char cmdbuf[128];
    ksprintf(cmdbuf, "nasm -f bin -o /dev/stdout %s | ndisasm -b32 -", temp.fileName().characters());
    system(cmdbuf);
#endif
}
#endif

char* ExecSpace::symbolPtr(const char* name)
{
    if (auto it = m_symbols.find(name); it != m_symbols.end()) {
        auto& symbol = (*it).value;
#ifdef EXECSPACE_DEBUG
        kprintf("[ELFLoader] symbolPtr(%s) dump:\n", name);
        disassemble(symbol.ptr, symbol.size);
#endif
        return symbol.ptr;
    }
    return nullptr;
}

void ExecSpace::allocateUniverse(size_t size)
{
    ASSERT(!m_universe);
    if (hookableAlloc)
        m_universe = static_cast<char*>(hookableAlloc("elf-sec", size));
    else
        m_universe = static_cast<char*>(kmalloc(size));
}

bool ExecSpace::allocate_section(LinearAddress laddr, size_t size, size_t alignment, bool is_readable, bool is_writable)
{
    ASSERT(alloc_section_hook);
    char namebuf[16];
    ksprintf(namebuf, "elf-%s%s", is_readable ? "r" : "", is_writable ? "w" : "");
    auto* ptr = static_cast<char*>(alloc_section_hook(laddr, size, alignment, is_readable, is_writable, namebuf));
    m_allocated_regions.append(ptr);
    return true;
}

void ExecSpace::addSymbol(String&& name, char* ptr, unsigned size)
{
    m_symbols.set(move(name), { ptr, size });
}
