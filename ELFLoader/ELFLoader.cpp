#include "ELFLoader.h"
#include <AK/kstdio.h>

//#define ELFLOADER_DEBUG

ELFLoader::ELFLoader(const byte* buffer)
    : m_image(buffer)
{
}

ELFLoader::~ELFLoader()
{
}

bool ELFLoader::load()
{
#ifdef ELFLOADER_DEBUG
    m_image.dump();
#endif
    if (!m_image.is_valid())
        return false;

    if (!layout())
        return false;
    export_symbols();
    if (!perform_relocations())
        return false;

    return true;
}

bool ELFLoader::layout()
{
#ifdef ELFLOADER_DEBUG
    kprintf("ELFLoader: Layout\n");
#endif

    bool failed = false;
    m_image.for_each_program_header([&] (const ELFImage::ProgramHeader& program_header) {
        if (program_header.type() != PT_LOAD)
            return;
#ifdef ELFLOADER_DEBUG
        kprintf("PH: L%x %u r:%u w:%u\n", program_header.laddr().get(), program_header.size_in_memory(), program_header.is_readable(), program_header.is_writable());
#endif
        if (program_header.is_writable()) {
            allocate_section(program_header.laddr(), program_header.size_in_memory(), program_header.alignment(), program_header.is_readable(), program_header.is_writable());
        } else {
            map_section(program_header.laddr(), program_header.size_in_memory(), program_header.alignment(), program_header.offset(), program_header.is_readable(), program_header.is_writable());
        }
    });

    m_image.for_each_section_of_type(SHT_PROGBITS, [this, &failed] (const ELFImage::Section& section) {
#ifdef ELFLOADER_DEBUG
        kprintf("ELFLoader: Copying progbits section: %s\n", section.name());
#endif
        if (!section.size())
            return true;
        char* ptr = (char*)section.address();
        if (!ptr) {
#ifdef ELFLOADER_DEBUG
            kprintf("ELFLoader: ignoring section '%s' with null address\n", section.name());
#endif
            return true;
        }
        memcpy(ptr, section.raw_data(), section.size());
        m_sections.set(section.name(), move(ptr));
        return true;
    });
    m_image.for_each_section_of_type(SHT_NOBITS, [this, &failed] (const ELFImage::Section& section) {
#ifdef ELFLOADER_DEBUG
        kprintf("ELFLoader: Copying nobits section: %s\n", section.name());
#endif
        if (!section.size())
            return true;
        char* ptr = (char*)section.address();
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
    if (symbol.section().is_undefined())
        return symbol_ptr(symbol.name());
    return area_for_section(symbol.section()) + symbol.value();
}

char* ELFLoader::area_for_section(const ELFImage::Section& section)
{
    return area_for_section_name(section.name());
}

char* ELFLoader::area_for_section_name(const char* name)
{
    if (auto it = m_sections.find(name); it != m_sections.end())
        return (*it).value;
    ASSERT_NOT_REACHED();
    return nullptr;
}

bool ELFLoader::perform_relocations()
{
#ifdef ELFLOADER_DEBUG
    kprintf("ELFLoader: Performing relocations\n");
#endif

    bool failed = false;

    m_image.for_each_section_of_type(SHT_PROGBITS, [this, &failed] (const ELFImage::Section& section) -> bool {
        auto& relocations = section.relocations();
        if (relocations.is_undefined())
            return true;
        relocations.for_each_relocation([this, section, &failed] (const ELFImage::Relocation& relocation) {
            auto symbol = relocation.symbol();
            auto& patch_ptr = *reinterpret_cast<ptrdiff_t*>(area_for_section(section) + relocation.offset());

            switch (relocation.type()) {
            case R_386_PC32: {
                char* target_ptr = (char*)lookup(symbol);
                if (!target_ptr) {
                    kprintf("ELFLoader: unresolved symbol '%s'\n", symbol.name());
                    failed = true;
                    return false;
                }
                ptrdiff_t relativeOffset = (char*)target_ptr - ((char*)&patch_ptr + 4);
#ifdef ELFLOADER_DEBUG
                kprintf("ELFLoader: Relocate PC32:  offset=%x, symbol=%u(%s) value=%x target=%p, offset=%d\n",
                        relocation.offset(),
                        symbol.index(),
                        symbol.name(),
                        symbol.value(),
                        target_ptr,
                        relativeOffset
                );
#endif
                patch_ptr = relativeOffset;
                break;
            }
            case R_386_32: {
#ifdef ELFLOADER_DEBUG
                kprintf("ELFLoader: Relocate Abs32: symbol=%u(%s), value=%x, section=%s\n",
                    symbol.index(),
                    symbol.name(),
                    symbol.value(),
                    symbol.section().name()
                );
#endif
                char* target_ptr = area_for_section(symbol.section()) + symbol.value();
                patch_ptr += (ptrdiff_t)target_ptr;
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

void ELFLoader::export_symbols()
{
    m_image.for_each_symbol([&] (const ELFImage::Symbol symbol) {
#ifdef ELFLOADER_DEBUG
        kprintf("symbol: %u, type=%u, name=%s, section=%u\n", symbol.index(), symbol.type(), symbol.name(), symbol.sectionIndex());
#endif
        if (symbol.type() == STT_FUNC) {
            char* ptr;
            if (m_image.is_executable())
                ptr = (char*)symbol.value();
            else if (m_image.is_relocatable())
                ptr = area_for_section(symbol.section()) + symbol.value();
            else
                ASSERT_NOT_REACHED();
            add_symbol(symbol.name(), ptr, symbol.size());
        }
        // FIXME: What about other symbol types?
        return true;
    });
}

char* ELFLoader::symbol_ptr(const char* name)
{
    if (auto it = m_symbols.find(name); it != m_symbols.end()) {
        auto& symbol = (*it).value;
#ifdef EXECSPACE_DEBUG
        kprintf("[ELFLoader] symbol_ptr(%s) dump:\n", name);
        disassemble(symbol.ptr, symbol.size);
#endif
        return symbol.ptr;
    }
    return nullptr;
}

bool ELFLoader::allocate_section(LinearAddress laddr, size_t size, size_t alignment, bool is_readable, bool is_writable)
{
    ASSERT(alloc_section_hook);
    char namebuf[16];
    ksprintf(namebuf, "elf-alloc-%s%s", is_readable ? "r" : "", is_writable ? "w" : "");
    return alloc_section_hook(laddr, size, alignment, is_readable, is_writable, namebuf);
}

bool ELFLoader::map_section(LinearAddress laddr, size_t size, size_t alignment, size_t offset_in_image, bool is_readable, bool is_writable)
{
    ASSERT(alloc_section_hook);
    char namebuf[16];
    ksprintf(namebuf, "elf-map-%s%s", is_readable ? "r" : "", is_writable ? "w" : "");
    return map_section_hook(laddr, size, alignment, offset_in_image, is_readable, is_writable, namebuf);
}

void ELFLoader::add_symbol(String&& name, char* ptr, unsigned size)
{
    m_symbols.set(move(name), { ptr, size });
}
