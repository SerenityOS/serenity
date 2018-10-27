#include "ELFLoader.h"
#include <AK/kstdio.h>

//#define ELFLOADER_DEBUG

#ifdef SERENITY
ELFLoader::ELFLoader(ExecSpace& execSpace, ByteBuffer&& file)
#else
ELFLoader::ELFLoader(ExecSpace& execSpace, MappedFile&& file)
#endif
    : m_execSpace(execSpace)
{
    m_image = make<ELFImage>(move(file));
}

ELFLoader::~ELFLoader()
{
}

bool ELFLoader::load()
{
#ifdef ELFLOADER_DEBUG
    m_image->dump();
#endif
    if (!m_image->isValid())
        return false;

    if (!layout())
        return false;
    exportSymbols();
    if (!performRelocations())
        return false;

    return true;
}

bool ELFLoader::layout()
{
#ifdef ELFLOADER_DEBUG
    kprintf("[ELFLoader] Layout\n");
#endif
    bool failed = false;
    dword highestOffset = 0;
    dword sizeNeeded = 0;
    m_image->forEachSection([&] (auto& section) {
        if (section.offset() > highestOffset) {
            highestOffset = section.offset();
            sizeNeeded = highestOffset + section.size();
        }
    });
#ifdef ELFLOADER_DEBUG
    kprintf("[ELFLoader] Highest section offset: %u, Size needed: %u\n", highestOffset, sizeNeeded);
#endif
    m_execSpace.allocateUniverse(sizeNeeded);

    m_image->forEachSectionOfType(SHT_PROGBITS, [this, &failed] (const ELFImage::Section& section) {
#ifdef ELFLOADER_DEBUG
        kprintf("[ELFLoader] Allocating progbits section: %s\n", section.name());
#endif
        if (!section.size())
            return true;
        char* ptr = m_execSpace.allocateArea(section.name(), section.size(), section.offset(), LinearAddress(section.address()));
        if (!ptr) {
            kprintf("ELFLoader: failed to allocate section '%s'\n", section.name());
            failed = true;
            return false;
        }
        memcpy(ptr, section.rawData(), section.size());
        m_sections.set(section.name(), move(ptr));
        return true;
    });
    m_image->forEachSectionOfType(SHT_NOBITS, [this, &failed] (const ELFImage::Section& section) {
#ifdef ELFLOADER_DEBUG
        kprintf("[ELFLoader] Allocating nobits section: %s\n", section.name());
#endif
        if (!section.size())
            return true;
        char* ptr = m_execSpace.allocateArea(section.name(), section.size(), section.offset(), LinearAddress(section.address()));
        if (!ptr) {
            kprintf("ELFLoader: failed to allocate section '%s'\n", section.name());
            failed = true;
            return false;
        }
        memset(ptr, 0, section.size());
        m_sections.set(section.name(), move(ptr));
        return true;
    });
    return !failed;
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

bool ELFLoader::performRelocations()
{
#ifdef ELFLOADER_DEBUG
    kprintf("[ELFLoader] Performing relocations\n");
#endif

    bool failed = false;

    m_image->forEachSectionOfType(SHT_PROGBITS, [this, &failed] (const ELFImage::Section& section) -> bool {
        auto& relocations = section.relocations();
        if (relocations.isUndefined())
            return true;
        relocations.forEachRelocation([this, section, &failed] (const ELFImage::Relocation& relocation) {
            auto symbol = relocation.symbol();
            auto& patchPtr = *reinterpret_cast<ptrdiff_t*>(areaForSection(section) + relocation.offset());

            switch (relocation.type()) {
            case R_386_PC32: {
                char* targetPtr = (char*)lookup(symbol);
                if (!targetPtr) {
                    kprintf("ELFLoader: unresolved symbol '%s'\n", symbol.name());
                    failed = true;
                    return false;
                }
                ptrdiff_t relativeOffset = (char*)targetPtr - ((char*)&patchPtr + 4);
#ifdef ELFLOADER_DEBUG
                kprintf("[ELFLoader] Relocate PC32:  offset=%x, symbol=%u(%s) value=%x target=%p, offset=%d\n",
                        relocation.offset(),
                        symbol.index(),
                        symbol.name(),
                        symbol.value(),
                        targetPtr,
                        relativeOffset
                );
#endif
                patchPtr = relativeOffset;
                break;
            }
            case R_386_32: {
#ifdef ELFLOADER_DEBUG
                kprintf("[ELFLoader] Relocate Abs32: symbol=%u(%s), value=%x, section=%s\n",
                    symbol.index(),
                    symbol.name(),
                    symbol.value(),
                    symbol.section().name()
                );
#endif
                char* targetPtr = areaForSection(symbol.section()) + symbol.value();
                patchPtr += (ptrdiff_t)targetPtr;
                break;
            }
            default:
                ASSERT_NOT_REACHED();
                break;
            }
            return true;
        });
        return !failed;
    });
    return !failed;
}

void ELFLoader::exportSymbols()
{
    m_image->forEachSymbol([&] (const ELFImage::Symbol symbol) {
#ifdef ELFLOADER_DEBUG
        kprintf("symbol: %u, type=%u, name=%s, section=%u\n", symbol.index(), symbol.type(), symbol.name(), symbol.sectionIndex());
#endif
        if (symbol.type() == STT_FUNC) {
            char* ptr;
            if (m_image->isExecutable())
                ptr = (char*)symbol.value();
            else if (m_image->isRelocatable())
                ptr = areaForSection(symbol.section()) + symbol.value();
            else
                ASSERT_NOT_REACHED();
            m_execSpace.addSymbol(symbol.name(), ptr, symbol.size());
        }
        // FIXME: What about other symbol types?
        return true;
    });
}

