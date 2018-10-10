#include "ELFImage.h"
#include <cstdio>
#include <cstring>
#include <cstdlib>

ELFImage::ELFImage(MappedFile&& file)
    : m_file(std::move(file))
{
    m_isValid = parse();
}

ELFImage::~ELFImage()
{
}

static const char* objectFileTypeToString(Elf32_Half type)
{
    switch (type) {
    case ET_NONE: return "None";
    case ET_REL: return "Relocatable";
    case ET_EXEC: return "Executable";
    case ET_DYN: return "Shared object";
    case ET_CORE: return "Core";
    default: return "(?)";
    }
}

const char* ELFImage::sectionIndexToString(unsigned index)
{
    if (index == SHN_UNDEF)
        return "Undefined";
    if (index >= SHN_LORESERVE)
        return "Reserved";
    return section(index).name();
}

unsigned ELFImage::symbolCount() const
{
    return section(m_symbolTableSectionIndex).entryCount();
}

void ELFImage::dump()
{
    printf("AK::ELFImage{%p} {\n", this);
    printf("    isValid: %u\n", isValid());

    if (!isValid()) {
        printf("}\n");
        return;
    }

    printf("    type:    %s\n", objectFileTypeToString(header().e_type));
    printf("    machine: %u\n", header().e_machine);
    printf("    entry:   %08x\n", header().e_entry);
    printf("    shoff:   %u\n", header().e_shoff);
    printf("    shnum:   %u\n", header().e_shnum);
    printf(" shstrndx:   %u\n", header().e_shstrndx);

    for (unsigned i = 0; i < header().e_shnum; ++i) {
        auto& section = this->section(i);
        printf("    Section %u: {\n", i);
        printf("        name: %s\n", section.name());
        printf("        type: %x\n", section.type());
        printf("      offset: %x\n", section.offset());
        printf("        size: %u\n", section.size());
        printf("        \n");
        printf("    }\n");
    }

    printf("Symbol count: %u (table is %u)\n", symbolCount(), m_symbolTableSectionIndex);
    for (unsigned i = 1; i < symbolCount(); ++i) {
        auto& sym = symbol(i);
        printf("Symbol @%u:\n", i);
        printf("    Name: %s\n", sym.name());
        printf("    In section: %s\n", sectionIndexToString(sym.sectionIndex()));
        printf("    Value: %08x\n", sym.value());
        printf("    Size: %u\n", sym.size());
    }

    printf("}\n");
}

unsigned ELFImage::sectionCount() const
{
    return header().e_shnum;
}

bool ELFImage::parse()
{
    // We only support i386.
    if (header().e_machine != 3)
        return false;

    // First locate the string tables.
    for (unsigned i = 0; i < sectionCount(); ++i) {
        auto& sh = sectionHeader(i);
        if (sh.sh_type == SHT_SYMTAB) {
            ASSERT(!m_symbolTableSectionIndex);
            m_symbolTableSectionIndex = i;
        }
        if (sh.sh_type == SHT_STRTAB && i != header().e_shstrndx) {
            ASSERT(!m_stringTableSectionIndex);
            m_stringTableSectionIndex = i;
        }
    }

    // Then create a name-to-index map.
    for (unsigned i = 0; i < sectionCount(); ++i) {
        auto& section = this->section(i);
        m_sections.set(section.name(), std::move(i));
    }
    return true;
}

const char* ELFImage::sectionHeaderTableString(unsigned offset) const
{
    auto& sh = sectionHeader(header().e_shstrndx);
    if (sh.sh_type != SHT_STRTAB)
        return nullptr;
    return rawData(sh.sh_offset + offset);
}

const char* ELFImage::tableString(unsigned offset) const
{
    auto& sh = sectionHeader(m_stringTableSectionIndex);
    if (sh.sh_type != SHT_STRTAB)
        return nullptr;
    return rawData(sh.sh_offset + offset);
}

const char* ELFImage::rawData(unsigned offset) const
{
    return reinterpret_cast<const char*>(m_file.pointer()) + offset;
}

const Elf32_Ehdr& ELFImage::header() const
{
    return *reinterpret_cast<const Elf32_Ehdr*>(rawData(0));
}

const Elf32_Shdr& ELFImage::sectionHeader(unsigned index) const
{
    ASSERT(index < header().e_shnum);
    return *reinterpret_cast<const Elf32_Shdr*>(rawData(header().e_shoff + (index * sizeof(Elf32_Shdr))));
}

const ELFImage::Symbol ELFImage::symbol(unsigned index) const
{
    ASSERT(index < symbolCount());
    auto* rawSyms = reinterpret_cast<const Elf32_Sym*>(rawData(section(m_symbolTableSectionIndex).offset()));
    return Symbol(*this, index, rawSyms[index]);
}

const ELFImage::Section ELFImage::section(unsigned index) const
{
    ASSERT(index < sectionCount());
    return Section(*this, index);
}

const ELFImage::Relocation ELFImage::RelocationSection::relocation(unsigned index) const
{
    ASSERT(index < relocationCount());
    auto* rels = reinterpret_cast<const Elf32_Rel*>(m_image.rawData(offset()));
    return Relocation(m_image, rels[index]);
}

const ELFImage::RelocationSection ELFImage::Section::relocations() const
{
    // FIXME: This is ugly.
    char relocationSectionName[128];
    sprintf(relocationSectionName, ".rel%s", name());

    printf("looking for '%s'\n", relocationSectionName);
    auto relocationSection = m_image.lookupSection(relocationSectionName);
    if (relocationSection.type() != SHT_REL)
        return static_cast<const RelocationSection>(m_image.section(0));

    printf("Found relocations for %s in %s\n", name(), relocationSection.name());
    return static_cast<const RelocationSection>(relocationSection);
}

const ELFImage::Section ELFImage::lookupSection(const char* name) const
{
    if (auto it = m_sections.find(name); it != m_sections.end())
        return section((*it).value);
    return section(0);
}

