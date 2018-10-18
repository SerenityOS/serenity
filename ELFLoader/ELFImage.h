#pragma once

#ifndef SERENITY_KERNEL
#include <AK/MappedFile.h>
#endif

#include <AK/OwnPtr.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include "elf.h"

class ELFImage {
public:
#ifdef SERENITY_KERNEL
    explicit ELFImage(const byte* data);
#else
    explicit ELFImage(MappedFile&&);
#endif
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

    const Symbol symbol(unsigned) const;
    const Section section(unsigned) const;

    template<typename F> void forEachSectionOfType(unsigned, F) const;
    template<typename F> void forEachSymbol(F) const;

    // NOTE: Returns section(0) if section with name is not found.
    // FIXME: I don't love this API.
    const Section lookupSection(const char* name) const;

private:
    bool parseHeader();
    const char* rawData(unsigned offset) const;
    const Elf32_Ehdr& header() const;
    const Elf32_Shdr& sectionHeader(unsigned) const;
    const char* tableString(unsigned offset) const;
    const char* sectionHeaderTableString(unsigned offset) const;
    const char* sectionIndexToString(unsigned index);

#ifdef SERENITY_KERNEL
    const byte* m_data;
#else
    MappedFile m_file;
#endif
    HashMap<String, unsigned> m_sections;
    bool m_isValid { false };
    unsigned m_symbolTableSectionIndex { 0 };
    unsigned m_stringTableSectionIndex { 0 };
};

template<typename F>
inline void ELFImage::forEachSectionOfType(unsigned type, F func) const
{
    for (unsigned i = 0; i < sectionCount(); ++i) {
        auto& section = this->section(i);
        if (section.type() == type)
            func(section);
    }
}

template<typename F>
inline void ELFImage::RelocationSection::forEachRelocation(F func) const
{
    for (unsigned i = 0; i < relocationCount(); ++i)
        func(relocation(i));
}

template<typename F>
inline void ELFImage::forEachSymbol(F func) const
{
    for (unsigned i = 0; i < symbolCount(); ++i)
        func(symbol(i));
}

