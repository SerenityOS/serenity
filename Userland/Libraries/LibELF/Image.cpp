/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/Debug.h>
#include <AK/Demangle.h>
#include <AK/Memory.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>
#include <limits.h>

namespace ELF {

Image::Image(ReadonlyBytes bytes, bool verbose_logging)
    : m_buffer(bytes.data())
    , m_size(bytes.size())
    , m_verbose_logging(verbose_logging)
{
    parse();
}

Image::Image(const u8* buffer, size_t size, bool verbose_logging)
    : Image(ReadonlyBytes { buffer, size }, verbose_logging)
{
}

Image::~Image()
{
}

StringView Image::section_index_to_string(unsigned index) const
{
    VERIFY(m_valid);
    if (index == SHN_UNDEF)
        return "Undefined"sv;
    if (index >= SHN_LORESERVE)
        return "Reserved"sv;
    return section(index).name();
}

unsigned Image::symbol_count() const
{
    VERIFY(m_valid);
    if (!section_count())
        return 0;
    return section(m_symbol_table_section_index).entry_count();
}

void Image::dump() const
{
#if ELF_IMAGE_DEBUG
    dbgln("ELF::Image({:p}) {{", this);
    dbgln("    is_valid: {}", is_valid());

    if (!is_valid()) {
        dbgln("}}");
        return;
    }

    dbgln("    type:    {}", ELF::Image::object_file_type_to_string(header().e_type).value_or("(?)"));
    dbgln("    machine: {}", header().e_machine);
    dbgln("    entry:   {:x}", header().e_entry);
    dbgln("    shoff:   {}", header().e_shoff);
    dbgln("    shnum:   {}", header().e_shnum);
    dbgln("    phoff:   {}", header().e_phoff);
    dbgln("    phnum:   {}", header().e_phnum);
    dbgln(" shstrndx:   {}", header().e_shstrndx);

    for_each_program_header([&](const ProgramHeader& program_header) {
        dbgln("    Program Header {}: {{", program_header.index());
        dbgln("        type: {:x}", program_header.type());
        dbgln("      offset: {:x}", program_header.offset());
        dbgln("       flags: {:x}", program_header.flags());
        dbgln("    }}");
    });

    for (unsigned i = 0; i < header().e_shnum; ++i) {
        const auto& section = this->section(i);
        dbgln("    Section {}: {{", i);
        dbgln("        name: {}", section.name());
        dbgln("        type: {:x}", section.type());
        dbgln("      offset: {:x}", section.offset());
        dbgln("        size: {}", section.size());
        dbgln("        ");
        dbgln("    }}");
    }

    dbgln("Symbol count: {} (table is {})", symbol_count(), m_symbol_table_section_index);
    for (unsigned i = 1; i < symbol_count(); ++i) {
        const auto& sym = symbol(i);
        dbgln("Symbol @{}:", i);
        dbgln("    Name: {}", sym.name());
        dbgln("    In section: {}", section_index_to_string(sym.section_index()));
        dbgln("    Value: {}", sym.value());
        dbgln("    Size: {}", sym.size());
    }

    dbgln("}}");
#endif
}

unsigned Image::section_count() const
{
    VERIFY(m_valid);
    return header().e_shnum;
}

unsigned Image::program_header_count() const
{
    VERIFY(m_valid);
    return header().e_phnum;
}

bool Image::parse()
{
    if (m_size < sizeof(ElfW(Ehdr)) || !validate_elf_header(header(), m_size, m_verbose_logging)) {
        if (m_verbose_logging)
            dbgln("ELF::Image::parse(): ELF Header not valid");
        m_valid = false;
        return false;
    }

    auto result_or_error = validate_program_headers(header(), m_size, { m_buffer, m_size }, nullptr, m_verbose_logging);
    if (result_or_error.is_error()) {
        if (m_verbose_logging)
            dbgln("ELF::Image::parse(): Failed validating ELF Program Headers");
        m_valid = false;
        return false;
    }
    if (!result_or_error.value()) {
        if (m_verbose_logging)
            dbgln("ELF::Image::parse(): ELF Program Headers not valid");
        m_valid = false;
        return false;
    }

    m_valid = true;

    // First locate the string tables.
    for (unsigned i = 0; i < section_count(); ++i) {
        auto& sh = section_header(i);
        if (sh.sh_type == SHT_SYMTAB) {
            if (m_symbol_table_section_index && m_symbol_table_section_index != i) {
                m_valid = false;
                return false;
            }
            m_symbol_table_section_index = i;
        }
        if (sh.sh_type == SHT_STRTAB && i != header().e_shstrndx) {
            if (section_header_table_string(sh.sh_name) == ELF_STRTAB)
                m_string_table_section_index = i;
        }
    }

    return m_valid;
}

StringView Image::table_string(unsigned table_index, unsigned offset) const
{
    VERIFY(m_valid);
    auto& sh = section_header(table_index);
    if (sh.sh_type != SHT_STRTAB)
        return nullptr;
    size_t computed_offset = sh.sh_offset + offset;
    if (computed_offset >= m_size) {
        if (m_verbose_logging)
            dbgln("SHENANIGANS! Image::table_string() computed offset outside image.");
        return {};
    }
    size_t max_length = min(m_size - computed_offset, (size_t)PAGE_SIZE);
    size_t length = strnlen(raw_data(sh.sh_offset + offset), max_length);
    return { raw_data(sh.sh_offset + offset), length };
}

StringView Image::section_header_table_string(unsigned offset) const
{
    VERIFY(m_valid);
    return table_string(header().e_shstrndx, offset);
}

StringView Image::table_string(unsigned offset) const
{
    VERIFY(m_valid);
    return table_string(m_string_table_section_index, offset);
}

const char* Image::raw_data(unsigned offset) const
{
    VERIFY(offset < m_size); // Callers must check indices into raw_data()'s result are also in bounds.
    return reinterpret_cast<const char*>(m_buffer) + offset;
}

const ElfW(Ehdr) & Image::header() const
{
    VERIFY(m_size >= sizeof(ElfW(Ehdr)));
    return *reinterpret_cast<const ElfW(Ehdr)*>(raw_data(0));
}

const ElfW(Phdr) & Image::program_header_internal(unsigned index) const
{
    VERIFY(m_valid);
    VERIFY(index < header().e_phnum);
    return *reinterpret_cast<const ElfW(Phdr)*>(raw_data(header().e_phoff + (index * sizeof(ElfW(Phdr)))));
}

const ElfW(Shdr) & Image::section_header(unsigned index) const
{
    VERIFY(m_valid);
    VERIFY(index < header().e_shnum);
    return *reinterpret_cast<const ElfW(Shdr)*>(raw_data(header().e_shoff + (index * header().e_shentsize)));
}

Image::Symbol Image::symbol(unsigned index) const
{
    VERIFY(m_valid);
    VERIFY(index < symbol_count());
    auto* raw_syms = reinterpret_cast<const ElfW(Sym)*>(raw_data(section(m_symbol_table_section_index).offset()));
    return Symbol(*this, index, raw_syms[index]);
}

Image::Section Image::section(unsigned index) const
{
    VERIFY(m_valid);
    VERIFY(index < section_count());
    return Section(*this, index);
}

Image::ProgramHeader Image::program_header(unsigned index) const
{
    VERIFY(m_valid);
    VERIFY(index < program_header_count());
    return ProgramHeader(*this, index);
}

Image::Relocation Image::RelocationSection::relocation(unsigned index) const
{
    VERIFY(index < relocation_count());
    auto* rels = reinterpret_cast<const ElfW(Rel)*>(m_image.raw_data(offset()));
    return Relocation(m_image, rels[index]);
}

Optional<Image::RelocationSection> Image::Section::relocations() const
{
    StringBuilder builder;
    builder.append(".rel"sv);
    builder.append(name());

    auto relocation_section = m_image.lookup_section(builder.string_view());
    if (!relocation_section.has_value())
        return {};

    dbgln_if(ELF_IMAGE_DEBUG, "Found relocations for {} in {}", name(), relocation_section.value().name());
    return static_cast<RelocationSection>(relocation_section.value());
}

Optional<Image::Section> Image::lookup_section(StringView name) const
{
    VERIFY(m_valid);
    for (unsigned i = 0; i < section_count(); ++i) {
        auto section = this->section(i);
        if (section.name() == name)
            return section;
    }
    return {};
}

Optional<StringView> Image::object_file_type_to_string(ElfW(Half) type)
{
    switch (type) {
    case ET_NONE:
        return "None"sv;
    case ET_REL:
        return "Relocatable"sv;
    case ET_EXEC:
        return "Executable"sv;
    case ET_DYN:
        return "Shared object"sv;
    case ET_CORE:
        return "Core"sv;
    default:
        return {};
    }
}

Optional<StringView> Image::object_machine_type_to_string(ElfW(Half) type)
{
    switch (type) {
    case ET_NONE:
        return "None"sv;
    case EM_M32:
        return "AT&T WE 32100"sv;
    case EM_SPARC:
        return "SPARC"sv;
    case EM_386:
        return "Intel 80386"sv;
    case EM_68K:
        return "Motorola 68000"sv;
    case EM_88K:
        return "Motorola 88000"sv;
    case EM_486:
        return "Intel 80486"sv;
    case EM_860:
        return "Intel 80860"sv;
    case EM_MIPS:
        return "MIPS R3000 Big-Endian only"sv;
    case EM_X86_64:
        return "x86_64"sv;
    default:
        return {};
    }
}

Optional<StringView> Image::object_abi_type_to_string(Elf_Byte type)
{
    switch (type) {
    case ELFOSABI_SYSV:
        return "SYSV"sv;
    case ELFOSABI_HPUX:
        return "HP-UX"sv;
    case ELFOSABI_NETBSD:
        return "NetBSD"sv;
    case ELFOSABI_LINUX:
        return "Linux"sv;
    case ELFOSABI_HURD:
        return "GNU Hurd"sv;
    case ELFOSABI_86OPEN:
        return "86Open"sv;
    case ELFOSABI_SOLARIS:
        return "Solaris"sv;
    case ELFOSABI_MONTEREY:
        return "AIX"sv;
    case ELFOSABI_IRIX:
        return "IRIX"sv;
    case ELFOSABI_FREEBSD:
        return "FreeBSD"sv;
    case ELFOSABI_TRU64:
        return "Tru64"sv;
    case ELFOSABI_MODESTO:
        return "Novell Modesto"sv;
    case ELFOSABI_OPENBSD:
        return "OpenBSD"sv;
    case ELFOSABI_ARM:
        return "ARM"sv;
    case ELFOSABI_STANDALONE:
        return "Standalone"sv;
    default:
        return {};
    }
}

StringView Image::Symbol::raw_data() const
{
    auto section = this->section();
    return { section.raw_data() + (value() - section.address()), size() };
}

#ifndef KERNEL
Optional<Image::Symbol> Image::find_demangled_function(StringView name) const
{
    Optional<Image::Symbol> found;
    for_each_symbol([&](const Image::Symbol& symbol) {
        if (symbol.type() != STT_FUNC)
            return IterationDecision::Continue;
        if (symbol.is_undefined())
            return IterationDecision::Continue;
        auto demangled = demangle(symbol.name());
        auto index_of_paren = demangled.find('(');
        if (index_of_paren.has_value()) {
            demangled = demangled.substring(0, index_of_paren.value());
        }
        if (demangled != name)
            return IterationDecision::Continue;
        found = symbol;
        return IterationDecision::Break;
    });
    return found;
}

Image::SortedSymbol* Image::find_sorted_symbol(FlatPtr address) const
{
    if (m_sorted_symbols.is_empty())
        sort_symbols();

    size_t index = 0;
    binary_search(m_sorted_symbols, nullptr, &index, [&address](auto, auto& candidate) {
        if (address < candidate.address)
            return -1;
        else if (address > candidate.address)
            return 1;
        else
            return 0;
    });
    // FIXME: The error path here feels strange, index == 0 means error but what about symbol #0?
    if (index == 0)
        return nullptr;
    return &m_sorted_symbols[index];
}

Optional<Image::Symbol> Image::find_symbol(FlatPtr address, u32* out_offset) const
{
    auto symbol_count = this->symbol_count();
    if (!symbol_count)
        return {};

    auto* symbol = find_sorted_symbol(address);
    if (!symbol)
        return {};
    if (out_offset)
        *out_offset = address - symbol->address;
    return symbol->symbol;
}

NEVER_INLINE void Image::sort_symbols() const
{
    m_sorted_symbols.ensure_capacity(symbol_count());
    for_each_symbol([this](const auto& symbol) {
        m_sorted_symbols.append({ symbol.value(), symbol.name(), {}, symbol });
    });
    quick_sort(m_sorted_symbols, [](auto& a, auto& b) {
        return a.address < b.address;
    });
}

String Image::symbolicate(FlatPtr address, u32* out_offset) const
{
    auto symbol_count = this->symbol_count();
    if (!symbol_count) {
        if (out_offset)
            *out_offset = 0;
        return "??";
    }

    auto* symbol = find_sorted_symbol(address);
    if (!symbol) {
        if (out_offset)
            *out_offset = 0;
        return "??";
    }

    auto& demangled_name = symbol->demangled_name;
    if (demangled_name.is_null())
        demangled_name = demangle(symbol->name);

    if (out_offset) {
        *out_offset = address - symbol->address;
        return demangled_name;
    }
    return String::formatted("{} +{:#x}", demangled_name, address - symbol->address);
}
#endif

} // end namespace ELF
