#include "ELFLoader.h"
#include <cstdio>
#include <cstring>
#include <cstdlib>

ELFLoader::ELFLoader(ExecSpace& execSpace, MappedFile&& file)
    : m_execSpace(execSpace)
{
    m_image = make<ELFImage>(std::move(file));
}

ELFLoader::~ELFLoader()
{
}

bool ELFLoader::load()
{
    m_image->dump();
    if (!m_image->isValid())
        return false;

    layout();
    exportSymbols();
    performRelocations();

    return true;
}

void ELFLoader::layout()
{
    printf("[ELFLoader] Layout\n");
    m_image->forEachSectionOfType(SHT_PROGBITS, [this] (const ELFImage::Section& section) {
        printf("[ELFLoader] Allocating progbits section: %s\n", section.name());
        char* ptr = m_execSpace.allocateArea(section.name(), section.size());
        memcpy(ptr, section.rawData(), section.size());
        m_sections.set(section.name(), std::move(ptr));
    });
}

void* ELFLoader::lookup(const ELFImage::Symbol& symbol)
{
    if (symbol.section().isUndefined())
        return m_execSpace.symbolPtr(symbol.name());
    return areaForSection(symbol.section()) + symbol.value();
}

char* ELFLoader::areaForSection(const ELFImage::Section& section)
{
    return areaForSectionName(section.name());
}

char* ELFLoader::areaForSectionName(const char* name)
{
    if (auto it = m_sections.find(name); it != m_sections.end())
        return (*it).value;
    ASSERT_NOT_REACHED();
    return nullptr;
}

void ELFLoader::performRelocations()
{
    printf("[ELFLoader] Performing relocations\n");

    m_image->forEachSectionOfType(SHT_PROGBITS, [this] (const ELFImage::Section& section) {
        auto& relocations = section.relocations();
        if (relocations.isUndefined())
            return;
        relocations.forEachRelocation([this, section] (const ELFImage::Relocation& relocation) {
            auto symbol = relocation.symbol();
            auto& patchPtr = *reinterpret_cast<ptrdiff_t*>(areaForSection(section) + relocation.offset());

            switch (relocation.type()) {
            case R_386_PC32: {
                char* targetPtr = (char*)lookup(symbol);
                ptrdiff_t relativeOffset = (char*)targetPtr - ((char*)&patchPtr + 4);
                printf("[ELFLoader] Relocate PC32:  offset=%08x, symbol=%u(%s) value=%08x target=%p, offset=%d\n",
                        relocation.offset(),
                        symbol.index(),
                        symbol.name(),
                        symbol.value(),
                        targetPtr,
                        relativeOffset
                );
                patchPtr = relativeOffset;
                break;
            }
            case R_386_32: {
                printf("[ELFLoader] Relocate Abs32: symbol=%u(%s), value=%08x, section=%s\n",
                    symbol.index(),
                    symbol.name(),
                    symbol.value(),
                    symbol.section().name()
                );
                char* targetPtr = areaForSection(symbol.section()) + symbol.value();
                patchPtr += (ptrdiff_t)targetPtr;
                break;
            }
            default:
                ASSERT_NOT_REACHED();
                break;
            }
        });
    });
}

void ELFLoader::exportSymbols()
{
    m_image->forEachSymbol([&] (const ELFImage::Symbol symbol) {
        printf("symbol: %u, type=%u, name=%s\n", symbol.index(), symbol.type(), symbol.name());
        if (symbol.type() == STT_FUNC)
            m_execSpace.addSymbol(symbol.name(), areaForSectionName(".text") + symbol.value(), symbol.size());
        // FIXME: What about other symbol types?
    });
}

