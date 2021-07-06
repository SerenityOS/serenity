/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/VirtualAddress.h>
#include <LibC/elf.h>

namespace ELF {

class Image {
public:
    explicit Image(ReadonlyBytes, bool verbose_logging = true);
    explicit Image(const u8*, size_t, bool verbose_logging = true);

    ~Image();
    void dump() const;
    bool is_valid() const { return m_valid; }
    bool parse();

    bool is_within_image(const void* address, size_t size) const
    {
        if (address < m_buffer)
            return false;
        if (((const u8*)address + size) > m_buffer + m_size)
            return false;
        return true;
    }

    class Section;
    class RelocationSection;
    class Symbol;
    class Relocation;

    class Symbol {
    public:
        Symbol(const Image& image, unsigned index, const ElfW(Sym) & sym)
            : m_image(image)
            , m_sym(sym)
            , m_index(index)
        {
        }

        ~Symbol() { }

        StringView name() const { return m_image.table_string(m_sym.st_name); }
        unsigned section_index() const { return m_sym.st_shndx; }
        unsigned value() const { return m_sym.st_value; }
        unsigned size() const { return m_sym.st_size; }
        unsigned index() const { return m_index; }
#if ARCH(I386)
        unsigned type() const
        {
            return ELF32_ST_TYPE(m_sym.st_info);
        }
        unsigned bind() const { return ELF32_ST_BIND(m_sym.st_info); }
#else
        unsigned type() const
        {
            return ELF64_ST_TYPE(m_sym.st_info);
        }
        unsigned bind() const { return ELF64_ST_BIND(m_sym.st_info); }
#endif
        Section section() const
        {
            return m_image.section(section_index());
        }
        bool is_undefined() const { return section_index() == 0; }
        StringView raw_data() const;

    private:
        const Image& m_image;
        const ElfW(Sym) & m_sym;
        const unsigned m_index;
    };

    class ProgramHeader {
    public:
        ProgramHeader(const Image& image, unsigned program_header_index)
            : m_image(image)
            , m_program_header(image.program_header_internal(program_header_index))
            , m_program_header_index(program_header_index)
        {
        }
        ~ProgramHeader() { }

        unsigned index() const { return m_program_header_index; }
        u32 type() const { return m_program_header.p_type; }
        u32 flags() const { return m_program_header.p_flags; }
        u32 offset() const { return m_program_header.p_offset; }
        VirtualAddress vaddr() const { return VirtualAddress(m_program_header.p_vaddr); }
        u32 size_in_memory() const { return m_program_header.p_memsz; }
        u32 size_in_image() const { return m_program_header.p_filesz; }
        u32 alignment() const { return m_program_header.p_align; }
        bool is_readable() const { return flags() & PF_R; }
        bool is_writable() const { return flags() & PF_W; }
        bool is_executable() const { return flags() & PF_X; }
        const char* raw_data() const { return m_image.raw_data(m_program_header.p_offset); }
        ElfW(Phdr) raw_header() const { return m_program_header; }

    private:
        const Image& m_image;
        const ElfW(Phdr) & m_program_header;
        unsigned m_program_header_index { 0 };
    };

    class Section {
    public:
        Section(const Image& image, unsigned sectionIndex)
            : m_image(image)
            , m_section_header(image.section_header(sectionIndex))
            , m_section_index(sectionIndex)
        {
        }
        ~Section() { }

        StringView name() const { return m_image.section_header_table_string(m_section_header.sh_name); }
        unsigned type() const { return m_section_header.sh_type; }
        unsigned offset() const { return m_section_header.sh_offset; }
        unsigned size() const { return m_section_header.sh_size; }
        unsigned entry_size() const { return m_section_header.sh_entsize; }
        unsigned entry_count() const { return !entry_size() ? 0 : size() / entry_size(); }
        u32 address() const { return m_section_header.sh_addr; }
        const char* raw_data() const { return m_image.raw_data(m_section_header.sh_offset); }
        ReadonlyBytes bytes() const { return { raw_data(), size() }; }
        Optional<RelocationSection> relocations() const;
        u32 flags() const { return m_section_header.sh_flags; }
        bool is_writable() const { return flags() & SHF_WRITE; }
        bool is_executable() const { return flags() & PF_X; }

    protected:
        friend class RelocationSection;
        const Image& m_image;
        const ElfW(Shdr) & m_section_header;
        unsigned m_section_index;
    };

    class RelocationSection : public Section {
    public:
        explicit RelocationSection(const Section& section)
            : Section(section.m_image, section.m_section_index)
        {
        }
        unsigned relocation_count() const { return entry_count(); }
        Relocation relocation(unsigned index) const;

        template<VoidFunction<Image::Relocation&> F>
        void for_each_relocation(F) const;
    };

    class Relocation {
    public:
        Relocation(const Image& image, const ElfW(Rel) & rel)
            : m_image(image)
            , m_rel(rel)
        {
        }

        ~Relocation() { }

        unsigned offset() const { return m_rel.r_offset; }
#if ARCH(I386)
        unsigned type() const
        {
            return ELF32_R_TYPE(m_rel.r_info);
        }
        unsigned symbol_index() const { return ELF32_R_SYM(m_rel.r_info); }
#else
        unsigned type() const
        {
            return ELF64_R_TYPE(m_rel.r_info);
        }
        unsigned symbol_index() const { return ELF64_R_SYM(m_rel.r_info); }
#endif
        Symbol symbol() const
        {
            return m_image.symbol(symbol_index());
        }

    private:
        const Image& m_image;
        const ElfW(Rel) & m_rel;
    };

    unsigned symbol_count() const;
    unsigned section_count() const;
    unsigned program_header_count() const;

    Symbol symbol(unsigned) const;
    Section section(unsigned) const;
    ProgramHeader program_header(unsigned) const;
    FlatPtr program_header_table_offset() const;

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

    Optional<Section> lookup_section(StringView const& name) const;

    bool is_executable() const { return header().e_type == ET_EXEC; }
    bool is_relocatable() const { return header().e_type == ET_REL; }
    bool is_dynamic() const { return header().e_type == ET_DYN; }

    VirtualAddress entry() const { return VirtualAddress(header().e_entry); }
    FlatPtr base_address() const { return (FlatPtr)m_buffer; }
    size_t size() const { return m_size; }

    bool has_symbols() const { return symbol_count(); }
#ifndef KERNEL
    Optional<Symbol> find_demangled_function(const StringView& name) const;
    String symbolicate(u32 address, u32* offset = nullptr) const;
#endif
    Optional<Image::Symbol> find_symbol(u32 address, u32* offset = nullptr) const;

private:
    const char* raw_data(unsigned offset) const;
    const ElfW(Ehdr) & header() const;
    const ElfW(Shdr) & section_header(unsigned) const;
    const ElfW(Phdr) & program_header_internal(unsigned) const;
    StringView table_string(unsigned offset) const;
    StringView section_header_table_string(unsigned offset) const;
    StringView section_index_to_string(unsigned index) const;
    StringView table_string(unsigned table_index, unsigned offset) const;

    const u8* m_buffer { nullptr };
    size_t m_size { 0 };
    bool m_verbose_logging { true };
    bool m_valid { false };
    unsigned m_symbol_table_section_index { 0 };
    unsigned m_string_table_section_index { 0 };

    struct SortedSymbol {
        u32 address;
        StringView name;
        String demangled_name;
        Optional<Image::Symbol> symbol;
    };

    void sort_symbols() const;
    SortedSymbol* find_sorted_symbol(FlatPtr) const;

    mutable Vector<SortedSymbol> m_sorted_symbols;
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
