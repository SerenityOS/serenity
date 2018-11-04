#pragma once

#include <AK/OwnPtr.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include "elf.h"
#include "types.h"

class ELFImage {
public:
    explicit ELFImage(ByteBuffer&&);
    ~ELFImage();
    void dump();
    bool isValid() const { return m_isValid; }
    bool parse();

    class Section;
    class RelocationSection;
    class Symbol;
    class Relocation;

    class Symbol {
    public:
        Symbol(const ELFImage& image, unsigned index, const Elf32_Sym& sym)
            : m_image(image)
            , m_sym(sym)
            , m_index(index)
        {
        }

        ~Symbol() { }

        const char* name() const { return m_image.tableString(m_sym.st_name); }
        unsigned sectionIndex() const { return m_sym.st_shndx; }
        unsigned value() const { return m_sym.st_value; }
        unsigned size() const { return m_sym.st_size; }
        unsigned index() const { return m_index; }
        unsigned type() const { return ELF32_ST_TYPE(m_sym.st_info); }
        const Section section() const { return m_image.section(sectionIndex()); }

    private:
        const ELFImage& m_image;
        const Elf32_Sym& m_sym;
        const unsigned m_index;
    };

    class ProgramHeader {
    public:
        ProgramHeader(const ELFImage& image, unsigned program_header_index)
            : m_program_header(image.program_header_internal(program_header_index))
            , m_program_header_index(program_header_index)
        {
        }
        ~ProgramHeader() { }

        unsigned index() const { return m_program_header_index; }
        dword type() const { return m_program_header.p_type; }
        dword flags() const { return m_program_header.p_flags; }
        dword offset() const { return m_program_header.p_offset; }
        LinearAddress laddr() const { return LinearAddress(m_program_header.p_vaddr); }
        dword size_in_memory() const { return m_program_header.p_memsz; }
        dword size_in_image() const { return m_program_header.p_filesz; }
        dword alignment() const { return m_program_header.p_align; }
        bool is_readable() const { return flags() & PF_R; }
        bool is_writable() const { return flags() & PF_W; }
        bool is_executable() const { return flags() & PF_X; }
    private:
        const Elf32_Phdr& m_program_header;
        unsigned m_program_header_index { 0 };
    };

    class Section {
    public:
        Section(const ELFImage& image, unsigned sectionIndex)
            : m_image(image)
            , m_sectionHeader(image.sectionHeader(sectionIndex))
            , m_sectionIndex(sectionIndex)
        {
        }
        ~Section() { }

        const char* name() const { return m_image.sectionHeaderTableString(m_sectionHeader.sh_name); }
        unsigned type() const { return m_sectionHeader.sh_type; }
        unsigned offset() const { return m_sectionHeader.sh_offset; }
        unsigned size() const { return m_sectionHeader.sh_size; }
        unsigned entrySize() const { return m_sectionHeader.sh_entsize; }
        unsigned entryCount() const { return size() / entrySize(); }
        dword address() const { return m_sectionHeader.sh_addr; }
        const char* rawData() const { return m_image.rawData(m_sectionHeader.sh_offset); }
        bool isUndefined() const { return m_sectionIndex == SHN_UNDEF; }
        const RelocationSection relocations() const;

    protected:
        friend class RelocationSection;
        const ELFImage& m_image;
        const Elf32_Shdr& m_sectionHeader;
        unsigned m_sectionIndex;
    };

    class RelocationSection : public Section {
    public:
        RelocationSection(const Section& section)
            : Section(section.m_image, section.m_sectionIndex)
        {
        }
        unsigned relocationCount() const { return entryCount(); }
        const Relocation relocation(unsigned index) const;
        template<typename F> void forEachRelocation(F) const;
    };

    class Relocation {
    public:
        Relocation(const ELFImage& image, const Elf32_Rel& rel)
            : m_image(image)
            , m_rel(rel)
        {
        }

        ~Relocation() { }

        unsigned offset() const { return m_rel.r_offset; }
        unsigned type() const { return ELF32_R_TYPE(m_rel.r_info); }
        unsigned symbolIndex() const { return ELF32_R_SYM(m_rel.r_info); }
        const Symbol symbol() const { return m_image.symbol(symbolIndex()); }

    private:
        const ELFImage& m_image;
        const Elf32_Rel& m_rel;
    };

    unsigned symbolCount() const;
    unsigned sectionCount() const;
    unsigned program_header_count() const;

    const Symbol symbol(unsigned) const;
    const Section section(unsigned) const;
    const ProgramHeader program_header(unsigned const) const;

    template<typename F> void forEachSection(F) const;
    template<typename F> void forEachSectionOfType(unsigned, F) const;
    template<typename F> void forEachSymbol(F) const;
    template<typename F> void for_each_program_header(F) const;

    // NOTE: Returns section(0) if section with name is not found.
    // FIXME: I don't love this API.
    const Section lookupSection(const char* name) const;

    bool isExecutable() const { return header().e_type == ET_EXEC; }
    bool isRelocatable() const { return header().e_type == ET_REL; }

private:
    bool parseHeader();
    const char* rawData(unsigned offset) const;
    const Elf32_Ehdr& header() const;
    const Elf32_Shdr& sectionHeader(unsigned) const;
    const Elf32_Phdr& program_header_internal(unsigned) const;
    const char* tableString(unsigned offset) const;
    const char* sectionHeaderTableString(unsigned offset) const;
    const char* sectionIndexToString(unsigned index);

#ifdef SERENITY
    ByteBuffer m_buffer;
#else
    MappedFile m_file;
#endif
    HashMap<String, unsigned> m_sections;
    bool m_isValid { false };
    unsigned m_symbolTableSectionIndex { 0 };
    unsigned m_stringTableSectionIndex { 0 };
};

template<typename F>
inline void ELFImage::forEachSection(F func) const
{
    for (unsigned i = 0; i < sectionCount(); ++i)
        func(section(i));
}

template<typename F>
inline void ELFImage::forEachSectionOfType(unsigned type, F func) const
{
    for (unsigned i = 0; i < sectionCount(); ++i) {
        auto& section = this->section(i);
        if (section.type() == type) {
            if (!func(section))
                break;
        }
    }
}

template<typename F>
inline void ELFImage::RelocationSection::forEachRelocation(F func) const
{
    for (unsigned i = 0; i < relocationCount(); ++i) {
        if (!func(relocation(i)))
            break;
    }
}

template<typename F>
inline void ELFImage::forEachSymbol(F func) const
{
    for (unsigned i = 0; i < symbolCount(); ++i) {
        if (!func(symbol(i)))
            break;
    }
}

template<typename F>
inline void ELFImage::for_each_program_header(F func) const
{
    for (unsigned i = 0; i < program_header_count(); ++i) {
        func(program_header(i));
    }
}
