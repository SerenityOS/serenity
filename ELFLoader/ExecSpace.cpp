#include "ExecSpace.h"
#include "ELFLoader.h"
#include <AK/Types.h>

//#define EXECSPACE_DEBUG

ExecSpace::ExecSpace()
{
}

ExecSpace::~ExecSpace()
{
}

bool ExecSpace::loadELF(ByteBuffer&& file)
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
