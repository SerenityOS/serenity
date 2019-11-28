#include "ELFImage.h"
#include <AK/kstdio.h>

ELFImage::ELFImage(const u8* buffer)
    : m_buffer(buffer)
{
    m_valid = parse();
}

ELFImage::~ELFImage()
{
}

static const char* object_file_type_to_string(Elf32_Half type)
{
    switch (type) {
    case ET_NONE:
        return "None";
    case ET_REL:
        return "Relocatable";
    case ET_EXEC:
        return "Executable";
    case ET_DYN:
        return "Shared object";
    case ET_CORE:
        return "Core";
    default:
        return "(?)";
    }
}

const char* ELFImage::section_index_to_string(unsigned index) const
{
    if (index == SHN_UNDEF)
        return "Undefined";
    if (index >= SHN_LORESERVE)
        return "Reserved";
    return section(index).name();
}

unsigned ELFImage::symbol_count() const
{
    return section(m_symbol_table_section_index).entry_count();
}

void ELFImage::dump() const
{
    kprintf("ELFImage{%p} {\n", this);
    kprintf("    is_valid: %u\n", is_valid());

    if (!is_valid()) {
        kprintf("}\n");
        return;
    }

    kprintf("    type:    %s\n", object_file_type_to_string(header().e_type));
    kprintf("    machine: %u\n", header().e_machine);
    kprintf("    entry:   %x\n", header().e_entry);
    kprintf("    shoff:   %u\n", header().e_shoff);
    kprintf("    shnum:   %u\n", header().e_shnum);
    kprintf(" shstrndx:   %u\n", header().e_shstrndx);

    for (unsigned i = 0; i < header().e_shnum; ++i) {
        auto& section = this->section(i);
        kprintf("    Section %u: {\n", i);
        kprintf("        name: %s\n", section.name());
        kprintf("        type: %x\n", section.type());
        kprintf("      offset: %x\n", section.offset());
        kprintf("        size: %u\n", section.size());
        kprintf("        \n");
        kprintf("    }\n");
    }

    kprintf("Symbol count: %u (table is %u)\n", symbol_count(), m_symbol_table_section_index);
    for (unsigned i = 1; i < symbol_count(); ++i) {
        auto& sym = symbol(i);
        kprintf("Symbol @%u:\n", i);
        kprintf("    Name: %s\n", sym.name());
        kprintf("    In section: %s\n", section_index_to_string(sym.section_index()));
        kprintf("    Value: %x\n", sym.value());
        kprintf("    Size: %u\n", sym.size());
    }

    kprintf("}\n");
}

unsigned ELFImage::section_count() const
{
    return header().e_shnum;
}

unsigned ELFImage::program_header_count() const
{
    return header().e_phnum;
}

bool ELFImage::parse()
{
    // We only support i386.
    if (header().e_machine != 3) {
        kprintf("ELFImage::parse(): e_machine=%u not supported!\n", header().e_machine);
        return false;
    }

    // First locate the string tables.
    for (unsigned i = 0; i < section_count(); ++i) {
        auto& sh = section_header(i);
        if (sh.sh_type == SHT_SYMTAB) {
            ASSERT(!m_symbol_table_section_index || m_symbol_table_section_index == i);
            m_symbol_table_section_index = i;
        }
        if (sh.sh_type == SHT_STRTAB && i != header().e_shstrndx) {
            ASSERT(!m_string_table_section_index || m_string_table_section_index == i);
            m_string_table_section_index = i;
        }
    }

    // Then create a name-to-index map.
    for (unsigned i = 0; i < section_count(); ++i) {
        auto& section = this->section(i);
        m_sections.set(section.name(), move(i));
    }

    return true;
}

const char* ELFImage::section_header_table_string(unsigned offset) const
{
    auto& sh = section_header(header().e_shstrndx);
    if (sh.sh_type != SHT_STRTAB)
        return nullptr;
    return raw_data(sh.sh_offset + offset);
}

const char* ELFImage::table_string(unsigned offset) const
{
    auto& sh = section_header(m_string_table_section_index);
    if (sh.sh_type != SHT_STRTAB)
        return nullptr;
    return raw_data(sh.sh_offset + offset);
}

const char* ELFImage::raw_data(unsigned offset) const
{
    return reinterpret_cast<const char*>(m_buffer) + offset;
}

const Elf32_Ehdr& ELFImage::header() const
{
    return *reinterpret_cast<const Elf32_Ehdr*>(raw_data(0));
}

const Elf32_Phdr& ELFImage::program_header_internal(unsigned index) const
{
    ASSERT(index < header().e_phnum);
    return *reinterpret_cast<const Elf32_Phdr*>(raw_data(header().e_phoff + (index * sizeof(Elf32_Phdr))));
}

const Elf32_Shdr& ELFImage::section_header(unsigned index) const
{
    ASSERT(index < header().e_shnum);
    return *reinterpret_cast<const Elf32_Shdr*>(raw_data(header().e_shoff + (index * sizeof(Elf32_Shdr))));
}

const ELFImage::Symbol ELFImage::symbol(unsigned index) const
{
    ASSERT(index < symbol_count());
    auto* raw_syms = reinterpret_cast<const Elf32_Sym*>(raw_data(section(m_symbol_table_section_index).offset()));
    return Symbol(*this, index, raw_syms[index]);
}

const ELFImage::Section ELFImage::section(unsigned index) const
{
    ASSERT(index < section_count());
    return Section(*this, index);
}

const ELFImage::ProgramHeader ELFImage::program_header(unsigned index) const
{
    ASSERT(index < program_header_count());
    return ProgramHeader(*this, index);
}

const ELFImage::Relocation ELFImage::RelocationSection::relocation(unsigned index) const
{
    ASSERT(index < relocation_count());
    auto* rels = reinterpret_cast<const Elf32_Rel*>(m_image.raw_data(offset()));
    return Relocation(m_image, rels[index]);
}

const ELFImage::RelocationSection ELFImage::Section::relocations() const
{
    // FIXME: This is ugly.
    char relocation_sectionName[128];
    sprintf(relocation_sectionName, ".rel%s", name());

#ifdef ELFIMAGE_DEBUG
    kprintf("looking for '%s'\n", relocation_sectionName);
#endif
    auto relocation_section = m_image.lookup_section(relocation_sectionName);
    if (relocation_section.type() != SHT_REL)
        return static_cast<const RelocationSection>(m_image.section(0));

#ifdef ELFIMAGE_DEBUG
    kprintf("Found relocations for %s in %s\n", name(), relocation_section.name());
#endif
    return static_cast<const RelocationSection>(relocation_section);
}

const ELFImage::Section ELFImage::lookup_section(const char* name) const
{
    if (auto it = m_sections.find(name); it != m_sections.end())
        return section((*it).value);
    return section(0);
}
