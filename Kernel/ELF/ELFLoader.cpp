#include "ELFLoader.h"
#include <AK/kstdio.h>

//#define ELFLOADER_DEBUG
//#define SUPPORT_RELOCATIONS

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
#ifdef SUPPORT_RELOCATIONS
    if (!perform_relocations())
        return false;
#endif

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
            alloc_section_hook(
                program_header.laddr(),
                program_header.size_in_memory(),
                program_header.alignment(),
                program_header.is_readable(),
                program_header.is_writable(),
                String::format("elf-alloc-%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "")
            );
            memcpy(program_header.laddr().as_ptr(), program_header.raw_data(), program_header.size_in_image());
        } else {
            map_section_hook(
                program_header.laddr(),
                program_header.size_in_memory(),
                program_header.alignment(),
                program_header.offset(),
                program_header.is_readable(),
                program_header.is_writable(),
                String::format("elf-map-%s%s", program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "")
            );
        }
    });
    return !failed;
}

#ifdef SUPPORT_RELOCATIONS
void* ELFLoader::lookup(const ELFImage::Symbol& symbol)
{
    if (symbol.section().is_undefined())
        return symbol_ptr(symbol.name());
    return area_for_section(symbol.section()) + symbol.value();
}
#endif

#ifdef SUPPORT_RELOCATIONS
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
#endif

#ifdef SUPPORT_RELOCATIONS
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
                ptrdiff_t relative_offset = (char*)target_ptr - ((char*)&patch_ptr + 4);
#ifdef ELFLOADER_DEBUG
                kprintf("ELFLoader: Relocate PC32:  offset=%x, symbol=%u(%s) value=%x target=%p, offset=%d\n",
                        relocation.offset(),
                        symbol.index(),
                        symbol.name(),
                        symbol.value(),
                        target_ptr,
                        relative_offset
                );
#endif
                patch_ptr = relative_offset;
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
#endif

char* ELFLoader::symbol_ptr(const char* name)
{
    char* found_ptr = nullptr;
    m_image.for_each_symbol([&] (const ELFImage::Symbol symbol) {
        if (symbol.type() != STT_FUNC)
            return true;
        if (strcmp(symbol.name(), name))
            return true;
        if (m_image.is_executable())
            found_ptr = (char*)symbol.value();
#ifdef SUPPORT_RELOCATIONS
        else if (m_image.is_relocatable())
            found_ptr = area_for_section(symbol.section()) + symbol.value();
#endif
        else
            ASSERT_NOT_REACHED();
        return false;
    });
    return found_ptr;
}
