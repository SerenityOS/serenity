/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Vector.h>
#include <Kernel/Memory/VirtualAddress.h>
#include <LibELF/ELFABI.h>

#ifndef KERNEL
#    include <AK/ByteString.h>
#endif

namespace ELF {

class Image {
public:
    explicit Image(ReadonlyBytes, bool verbose_logging = true);
    explicit Image(u8 const*, size_t, bool verbose_logging = true);

    ~Image() = default;
    void dump() const;
    bool is_valid() const { return m_valid; }
    bool parse();

    bool is_within_image(void const* address, size_t size) const
    {
        if (address < m_buffer)
            return false;
        if (((u8 const*)address + size) > m_buffer + m_size)
            return false;
        return true;
    }

    class Section;
    class RelocationSection;
    class Symbol;
    class Relocation;

    class Symbol {
    public:
        Symbol(Image const& image, unsigned index, Elf_Sym const& sym)
            : m_image(image)
            , m_sym(sym)
            , m_index(index)
        {
        }

        ~Symbol() = default;

        StringView name() const { return m_image.table_string(m_sym.st_name); }
        unsigned section_index() const { return m_sym.st_shndx; }
        FlatPtr value() const { return m_sym.st_value; }
        size_t size() const { return m_sym.st_size; }
        unsigned index() const { return m_index; }
        unsigned type() const
        {
            return ELF64_ST_TYPE(m_sym.st_info);
        }
        unsigned bind() const { return ELF64_ST_BIND(m_sym.st_info); }
        Section section() const
        {
            return m_image.section(section_index());
        }
        bool is_undefined() const { return section_index() == 0; }
        StringView raw_data() const;

    private:
        Image const& m_image;
        Elf_Sym const& m_sym;
        unsigned const m_index;
    };

    class ProgramHeader {
    public:
        ProgramHeader(Image const& image, unsigned program_header_index)
            : m_image(image)
            , m_program_header(image.program_header_internal(program_header_index))
            , m_program_header_index(program_header_index)
        {
        }
        ~ProgramHeader() = default;

        unsigned index() const { return m_program_header_index; }
        u32 type() const { return m_program_header.p_type; }
        u32 flags() const { return m_program_header.p_flags; }
        size_t offset() const { return m_program_header.p_offset; }
        VirtualAddress vaddr() const { return VirtualAddress(m_program_header.p_vaddr); }
        size_t size_in_memory() const { return m_program_header.p_memsz; }
        size_t size_in_image() const { return m_program_header.p_filesz; }
        size_t alignment() const { return m_program_header.p_align; }
        bool is_readable() const { return flags() & PF_R; }
        bool is_writable() const { return flags() & PF_W; }
        bool is_executable() const { return flags() & PF_X; }
        char const* raw_data() const { return m_image.raw_data(m_program_header.p_offset); }
        Elf_Phdr raw_header() const { return m_program_header; }

    private:
        Image const& m_image;
        Elf_Phdr const& m_program_header;
        unsigned m_program_header_index { 0 };
    };

    class Section {
    public:
        Section(Image const& image, unsigned sectionIndex)
            : m_image(image)
            , m_section_header(image.section_header(sectionIndex))
            , m_section_index(sectionIndex)
        {
        }
        ~Section() = default;

        StringView name() const { return m_image.section_header_table_string(m_section_header.sh_name); }
        u32 type() const { return m_section_header.sh_type; }
        size_t offset() const { return m_section_header.sh_offset; }
        size_t size() const { return m_section_header.sh_size; }
        size_t entry_size() const { return m_section_header.sh_entsize; }
        size_t entry_count() const { return !entry_size() ? 0 : size() / entry_size(); }
        FlatPtr address() const { return m_section_header.sh_addr; }
        char const* raw_data() const { return m_image.raw_data(m_section_header.sh_offset); }
        ReadonlyBytes bytes() const { return { raw_data(), size() }; }
        auto flags() const { return m_section_header.sh_flags; }
        bool is_writable() const { return flags() & SHF_WRITE; }
        bool is_executable() const { return flags() & PF_X; }

    protected:
        friend class RelocationSection;
        Image const& m_image;
        Elf_Shdr const& m_section_header;
        unsigned m_section_index;
    };

    class RelocationSection : public Section {
    public:
        explicit RelocationSection(Section const& section)
            : Section(section.m_image, section.m_section_index)
        {
        }
        size_t relocation_count() const { return entry_count(); }
        Relocation relocation(unsigned index) const;

        template<VoidFunction<Image::Relocation&> F>
        void for_each_relocation(F) const;

        bool addend_used() const { return type() == SHT_RELA; }
    };

    class Relocation {
    public:
        Relocation(Image const& image, Elf_Rela const& rel, bool addend_used)
            : m_image(image)
            , m_rel(rel)
            , m_addend_used(addend_used)
        {
        }

        ~Relocation() = default;

        size_t offset() const { return m_rel.r_offset; }
        unsigned type() const
        {
            return ELF64_R_TYPE(m_rel.r_info);
        }
        unsigned symbol_index() const { return ELF64_R_SYM(m_rel.r_info); }
        Symbol symbol() const
        {
            return m_image.symbol(symbol_index());
        }

        bool addend_used() const { return m_addend_used; }
        unsigned addend() const
        {
            VERIFY(m_addend_used);
            return m_rel.r_addend;
        }

    private:
        Image const& m_image;
        Elf_Rela const& m_rel;
        bool m_addend_used;
    };

    unsigned symbol_count() const;
    unsigned section_count() const;
    unsigned program_header_count() const;

    Symbol symbol(unsigned) const;
    Section section(unsigned) const;
    ProgramHeader program_header(unsigned) const;

    template<IteratorFunction<Image::Section> F>
    void for_each_section(F) const;
    template<VoidFunction<Section> F>
    void for_each_section(F) const;

    template<IteratorFunction<Section&> F>
    void for_each_section_of_type(unsigned, F) const;
    template<VoidFunction<Section&> F>
    void for_each_section_of_type(unsigned, F) const;

    template<IteratorFunction<Symbol> F>
    void for_each_symbol(F) const;
    template<VoidFunction<Symbol> F>
    void for_each_symbol(F) const;

    template<IteratorFunction<ProgramHeader> F>
    void for_each_program_header(F func) const;
    template<VoidFunction<ProgramHeader> F>
    void for_each_program_header(F) const;

    Optional<Section> lookup_section(StringView name) const;

    bool is_executable() const { return header().e_type == ET_EXEC; }
    bool is_relocatable() const { return header().e_type == ET_REL; }
    bool is_dynamic() const { return header().e_type == ET_DYN; }

    VirtualAddress entry() const { return VirtualAddress(header().e_entry); }
    Elf64_Quarter machine() const { return header().e_machine; }
    FlatPtr base_address() const { return (FlatPtr)m_buffer; }
    size_t size() const { return m_size; }

    unsigned char elf_class() const { return header().e_ident[EI_CLASS]; }
    unsigned char byte_order() const { return header().e_ident[EI_DATA]; }

    static Optional<StringView> object_file_type_to_string(Elf_Half type);
    static Optional<StringView> object_machine_type_to_string(Elf_Half type);
    static Optional<StringView> object_abi_type_to_string(Elf_Byte type);

    bool has_symbols() const { return symbol_count(); }
#ifndef KERNEL
    Optional<Symbol> find_demangled_function(StringView name) const;
    ByteString symbolicate(FlatPtr address, u32* offset = nullptr) const;
#endif
    Optional<Image::Symbol> find_symbol(FlatPtr address, u32* offset = nullptr) const;

private:
    char const* raw_data(unsigned offset) const;
    Elf_Ehdr const& header() const;
    Elf_Shdr const& section_header(unsigned) const;
    Elf_Phdr const& program_header_internal(unsigned) const;
    StringView table_string(unsigned offset) const;
    StringView section_header_table_string(unsigned offset) const;
    StringView section_index_to_string(unsigned index) const;
    StringView table_string(unsigned table_index, unsigned offset) const;

    u8 const* m_buffer { nullptr };
    size_t m_size { 0 };
    bool m_verbose_logging { true };
    bool m_valid { false };
    unsigned m_symbol_table_section_index { 0 };
    unsigned m_string_table_section_index { 0 };

#ifndef KERNEL
    struct SortedSymbol {
        FlatPtr address;
        StringView name;
        ByteString demangled_name;
        Optional<Image::Symbol> symbol;
    };

    void sort_symbols() const;
    SortedSymbol* find_sorted_symbol(FlatPtr) const;

    mutable Vector<SortedSymbol> m_sorted_symbols;
#endif
};

template<IteratorFunction<Image::Section> F>
inline void Image::for_each_section(F func) const
{
    auto section_count = this->section_count();
    for (unsigned i = 0; i < section_count; ++i) {
        if (func(section(i)) == IterationDecision::Break)
            break;
    }
}

template<VoidFunction<Image::Section> F>
inline void Image::for_each_section(F func) const
{
    for_each_section([&](auto section) {
        func(move(section));
        return IterationDecision::Continue;
    });
}

template<IteratorFunction<Image::Section&> F>
inline void Image::for_each_section_of_type(unsigned type, F func) const
{
    auto section_count = this->section_count();
    for (unsigned i = 0; i < section_count; ++i) {
        auto section = this->section(i);
        if (section.type() == type) {
            if (func(section) == IterationDecision::Break)
                break;
        }
    }
}

template<VoidFunction<Image::Section&> F>
inline void Image::for_each_section_of_type(unsigned type, F func) const
{
    for_each_section_of_type(type, [&](auto& section) {
        func(section);
        return IterationDecision::Continue;
    });
}

template<VoidFunction<Image::Relocation&> F>
inline void Image::RelocationSection::for_each_relocation(F func) const
{
    auto relocation_count = this->relocation_count();
    for (unsigned i = 0; i < relocation_count; ++i) {
        func(relocation(i));
    }
}

template<IteratorFunction<Image::Symbol> F>
inline void Image::for_each_symbol(F func) const
{
    auto symbol_count = this->symbol_count();
    for (unsigned i = 0; i < symbol_count; ++i) {
        if (func(symbol(i)) == IterationDecision::Break)
            break;
    }
}

template<VoidFunction<Image::Symbol> F>
inline void Image::for_each_symbol(F func) const
{
    for_each_symbol([&](auto symbol) {
        func(move(symbol));
        return IterationDecision::Continue;
    });
}

template<IteratorFunction<Image::ProgramHeader> F>
inline void Image::for_each_program_header(F func) const
{
    auto program_header_count = this->program_header_count();
    for (unsigned i = 0; i < program_header_count; ++i) {
        if (func(program_header(i)) == IterationDecision::Break)
            break;
    }
}

template<VoidFunction<Image::ProgramHeader> F>
inline void Image::for_each_program_header(F func) const
{
    for_each_program_header([&](auto header) {
        func(move(header));
        return IterationDecision::Continue;
    });
}

} // end namespace ELF
