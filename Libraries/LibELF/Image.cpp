/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Memory.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>

namespace ELF {

Image::Image(const u8* buffer, size_t size, bool verbose_logging)
    : m_buffer(buffer)
    , m_size(size)
    , m_verbose_logging(verbose_logging)
{
    parse();
}

Image::~Image()
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

StringView Image::section_index_to_string(unsigned index) const
{
    ASSERT(m_valid);
    if (index == SHN_UNDEF)
        return "Undefined";
    if (index >= SHN_LORESERVE)
        return "Reserved";
    return section(index).name();
}

unsigned Image::symbol_count() const
{
    ASSERT(m_valid);
    return section(m_symbol_table_section_index).entry_count();
}

void Image::dump() const
{
    dbgprintf("Image{%p} {\n", this);
    dbgprintf("    is_valid: %u\n", is_valid());

    if (!is_valid()) {
        dbgprintf("}\n");
        return;
    }

    dbgprintf("    type:    %s\n", object_file_type_to_string(header().e_type));
    dbgprintf("    machine: %u\n", header().e_machine);
    dbgprintf("    entry:   %x\n", header().e_entry);
    dbgprintf("    shoff:   %u\n", header().e_shoff);
    dbgprintf("    shnum:   %u\n", header().e_shnum);
    dbgprintf("    phoff:   %u\n", header().e_phoff);
    dbgprintf("    phnum:   %u\n", header().e_phnum);
    dbgprintf(" shstrndx:   %u\n", header().e_shstrndx);

    for_each_program_header([&](const ProgramHeader& program_header) {
        dbgprintf("    Program Header %d: {\n", program_header.index());
        dbgprintf("        type: %x\n", program_header.type());
        dbgprintf("      offset: %x\n", program_header.offset());
        dbgprintf("       flags: %x\n", program_header.flags());
        dbgprintf("        \n");
        dbgprintf("    }\n");
    });

    for (unsigned i = 0; i < header().e_shnum; ++i) {
        auto& section = this->section(i);
        dbgprintf("    Section %u: {\n", i);
        dbgprintf("        name: %.*s\n", (int)section.name().length(), section.name().characters_without_null_termination());
        dbgprintf("        type: %x\n", section.type());
        dbgprintf("      offset: %x\n", section.offset());
        dbgprintf("        size: %u\n", section.size());
        dbgprintf("        \n");
        dbgprintf("    }\n");
    }

    dbgprintf("Symbol count: %u (table is %u)\n", symbol_count(), m_symbol_table_section_index);
    for (unsigned i = 1; i < symbol_count(); ++i) {
        auto& sym = symbol(i);
        dbgprintf("Symbol @%u:\n", i);
        dbgprintf("    Name: %.*s\n", (int)sym.name().length(), sym.name().characters_without_null_termination());
        StringView section_index_string = section_index_to_string(sym.section_index());
        dbgprintf("    In section: %.*s\n", (int)section_index_string.length(), section_index_string.characters_without_null_termination());
        dbgprintf("    Value: %x\n", sym.value());
        dbgprintf("    Size: %u\n", sym.size());
    }

    dbgprintf("}\n");
}

unsigned Image::section_count() const
{
    ASSERT(m_valid);
    return header().e_shnum;
}

unsigned Image::program_header_count() const
{
    ASSERT(m_valid);
    return header().e_phnum;
}

bool Image::parse()
{
    if (m_size < sizeof(Elf32_Ehdr) || !validate_elf_header(header(), m_size, m_verbose_logging)) {
        if (m_verbose_logging)
            dbgputstr("Image::parse(): ELF Header not valid\n");
        return m_valid = false;
    }

    m_valid = true;

    // First locate the string tables.
    for (unsigned i = 0; i < section_count(); ++i) {
        auto& sh = section_header(i);
        if (sh.sh_type == SHT_SYMTAB) {
            if (m_symbol_table_section_index && m_symbol_table_section_index != i)
                return m_valid = false;
            m_symbol_table_section_index = i;
        }
        if (sh.sh_type == SHT_STRTAB && i != header().e_shstrndx) {
            if (section_header_table_string(sh.sh_name) == ELF_STRTAB)
                m_string_table_section_index = i;
        }
    }

    // Then create a name-to-index map.
    for (unsigned i = 0; i < section_count(); ++i) {
        auto& section = this->section(i);
        m_sections.set(section.name(), move(i));
    }

    return m_valid;
}

StringView Image::table_string(unsigned table_index, unsigned offset) const
{
    ASSERT(m_valid);
    auto& sh = section_header(table_index);
    if (sh.sh_type != SHT_STRTAB)
        return nullptr;
    size_t computed_offset = sh.sh_offset + offset;
    if (computed_offset >= m_size) {
        if (m_verbose_logging)
            dbgprintf("SHENANIGANS! Image::table_string() computed offset outside image.\n");
        return {};
    }
    size_t max_length = m_size - computed_offset;
    size_t length = strnlen(raw_data(sh.sh_offset + offset), max_length);
    return { raw_data(sh.sh_offset + offset), length };
}

StringView Image::section_header_table_string(unsigned offset) const
{
    ASSERT(m_valid);
    return table_string(header().e_shstrndx, offset);
}

StringView Image::table_string(unsigned offset) const
{
    ASSERT(m_valid);
    return table_string(m_string_table_section_index, offset);
}

const char* Image::raw_data(unsigned offset) const
{
    ASSERT(offset < m_size); // Callers must check indices into raw_data()'s result are also in bounds.
    return reinterpret_cast<const char*>(m_buffer) + offset;
}

const Elf32_Ehdr& Image::header() const
{
    ASSERT(m_size >= sizeof(Elf32_Ehdr));
    return *reinterpret_cast<const Elf32_Ehdr*>(raw_data(0));
}

const Elf32_Phdr& Image::program_header_internal(unsigned index) const
{
    ASSERT(m_valid);
    ASSERT(index < header().e_phnum);
    return *reinterpret_cast<const Elf32_Phdr*>(raw_data(header().e_phoff + (index * sizeof(Elf32_Phdr))));
}

const Elf32_Shdr& Image::section_header(unsigned index) const
{
    ASSERT(m_valid);
    ASSERT(index < header().e_shnum);
    return *reinterpret_cast<const Elf32_Shdr*>(raw_data(header().e_shoff + (index * header().e_shentsize)));
}

const Image::Symbol Image::symbol(unsigned index) const
{
    ASSERT(m_valid);
    ASSERT(index < symbol_count());
    auto* raw_syms = reinterpret_cast<const Elf32_Sym*>(raw_data(section(m_symbol_table_section_index).offset()));
    return Symbol(*this, index, raw_syms[index]);
}

const Image::Section Image::section(unsigned index) const
{
    ASSERT(m_valid);
    ASSERT(index < section_count());
    return Section(*this, index);
}

const Image::ProgramHeader Image::program_header(unsigned index) const
{
    ASSERT(m_valid);
    ASSERT(index < program_header_count());
    return ProgramHeader(*this, index);
}

const Image::Relocation Image::RelocationSection::relocation(unsigned index) const
{
    ASSERT(index < relocation_count());
    auto* rels = reinterpret_cast<const Elf32_Rel*>(m_image.raw_data(offset()));
    return Relocation(m_image, rels[index]);
}

const Image::RelocationSection Image::Section::relocations() const
{
    StringBuilder builder;
    builder.append(".rel");
    builder.append(name());

    auto relocation_section = m_image.lookup_section(builder.to_string());
    if (relocation_section.type() != SHT_REL)
        return static_cast<const RelocationSection>(m_image.section(0));

#ifdef Image_DEBUG
    dbgprintf("Found relocations for %s in %s\n", name().to_string().characters(), relocation_section.name().to_string().characters());
#endif
    return static_cast<const RelocationSection>(relocation_section);
}

const Image::Section Image::lookup_section(const String& name) const
{
    ASSERT(m_valid);
    if (auto it = m_sections.find(name); it != m_sections.end())
        return section((*it).value);
    return section(0);
}

StringView Image::Symbol::raw_data() const
{
    auto& section = this->section();
    return { section.raw_data() + (value() - section.address()), size() };
}

} // end namespace ELF
