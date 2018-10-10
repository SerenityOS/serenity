#include "ExecSpace.h"
#include "ELFLoader.h"
#include <AK/TemporaryFile.h>
#include <unistd.h>

ExecSpace::ExecSpace()
{
    initializeBuiltins();
}

ExecSpace::~ExecSpace()
{
}

void ExecSpace::initializeBuiltins()
{
    m_symbols.set("puts", { (char*)puts, 0 });
}

bool ExecSpace::loadELF(MappedFile&& file)
{
    ELFLoader loader(*this, std::move(file));
    if (!loader.load())
        return false;
    printf("[ExecSpace] ELF loaded, symbol map now:\n");
    for (auto& s : m_symbols) {
        printf("> %p: %s (%u)\n",
                s.value.ptr,
                s.key.characters(),
                s.value.size);
    }
    return true;
}

static void disassemble(const char* data, size_t length)
{
    if (!length)
        return;

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
    sprintf(cmdbuf, "nasm -f bin -o /dev/stdout %s | ndisasm -b32 -", temp.fileName().characters());
    system(cmdbuf);
}

char* ExecSpace::symbolPtr(const char* name)
{
    if (auto it = m_symbols.find(name); it != m_symbols.end()) {
        printf("[ELFLoader] symbolPtr(%s) dump:\n", name);
        auto& symbol = (*it).value;
        disassemble(symbol.ptr, symbol.size);
        return symbol.ptr;
    }
    return nullptr;
}


char* ExecSpace::allocateArea(String&& name, unsigned size)
{
    char* ptr = static_cast<char*>(malloc(size));
    ASSERT(ptr);
    m_areas.append(make<Area>(std::move(name), ptr, size));
    return ptr;
}

void ExecSpace::addSymbol(String&& name, char* ptr, unsigned size)
{
    m_symbols.set(std::move(name), { ptr, size });
}
