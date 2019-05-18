#pragma once

#include <AK/OwnPtr.h>
#include <AK/HashMap.h>
#include <AK/AKString.h>
#include <Kernel/ELF/exec_elf.h>

class ELFImage {
public:
    explicit ELFImage(const byte*);
    ~ELFImage();
    void dump();
    bool is_valid() const { return m_valid; }
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

        const char* name() const { return m_image.table_string(m_sym.st_name); }
        unsigned section_index() const { return m_sym.st_shndx; }
        unsigned value() const { return m_sym.st_value; }
        unsigned size() const { return m_sym.st_size; }
        unsigned index() const { return m_index; }
        unsigned type() const { return ELF32_ST_TYPE(m_sym.st_info); }
        const Section section() const { return m_image.section(section_index()); }

    private:
        const ELFImage& m_image;
        const Elf32_Sym& m_sym;
        const unsigned m_index;
    };

    class ProgramHeader {
    public:
        ProgramHeader(const ELFImage& image, unsigned program_header_index)
            : m_image(image)
            , m_program_header(image.program_header_internal(program_header_index))
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
        const char* raw_data() const { return m_image.raw_data(m_program_header.p_offset); }
    private:
        const ELFImage& m_image;
        const Elf32_Phdr& m_program_header;
        unsigned m_program_header_index { 0 };
    };

    class Section {
    public:
        Section(const ELFImage& image, unsigned sectionIndex)
            : m_image(image)
            , m_section_header(image.section_header(sectionIndex))
            , m_section_index(sectionIndex)
        {
        }
        ~Section() { }

        const char* name() const { return m_image.section_header_table_string(m_section_header.sh_name); }
        unsigned type() const { return m_section_header.sh_type; }
        unsigned offset() const { return m_section_header.sh_offset; }
        unsigned size() const { return m_section_header.sh_size; }
        unsigned entry_size() const { return m_section_header.sh_entsize; }
        unsigned entry_count() const { return !entry_size() ? 0 : size() / entry_size(); }
        dword address() const { return m_section_header.sh_addr; }
        const char* raw_data() const { return m_image.raw_data(m_section_header.sh_offset); }
        bool is_undefined() const { return m_section_index == SHN_UNDEF; }
        dword flags() const { return m_section_header.sh_flags; }
        bool is_writable() const { return flags() & SHF_WRITE; }
        bool is_executable() const { return flags() & PF_X; }

    protected:
        friend class RelocationSection;
        const ELFImage& m_image;
        const Elf32_Shdr& m_section_header;
        unsigned m_section_index;
    };

    unsigned symbol_count() const;
    unsigned section_count() const;
    unsigned program_header_count() const;

    const Symbol symbol(unsigned) const;
    const Section section(unsigned) const;
    const ProgramHeader program_header(unsigned const) const;

    template<typename F> void for_each_section(F) const;
    template<typename F> void for_each_section_of_type(unsigned, F) const;
    template<typename F> void for_each_symbol(F) const;
    template<typename F> void for_each_program_header(F) const;

    bool is_executable() const { return header().e_type == ET_EXEC; }
    bool is_relocatable() const { return header().e_type == ET_REL; }

    LinearAddress entry() const { return LinearAddress(header().e_entry); }

private:
    bool parse_header();
    const char* raw_data(unsigned offset) const;
    const Elf32_Ehdr& header() const;
    const Elf32_Shdr& section_header(unsigned) const;
    const Elf32_Phdr& program_header_internal(unsigned) const;
    const char* table_string(unsigned offset) const;
    const char* section_header_table_string(unsigned offset) const;
    const char* section_index_to_string(unsigned index);

    const byte* m_buffer { nullptr };
    bool m_valid { false };
    unsigned m_symbol_table_section_index { 0 };
    unsigned m_string_table_section_index { 0 };
};

template<typename F>
inline void ELFImage::for_each_section(F func) const
{
    for (unsigned i = 0; i < section_count(); ++i)
        func(section(i));
}

template<typename F>
inline void ELFImage::for_each_section_of_type(unsigned type, F func) const
{
    for (unsigned i = 0; i < section_count(); ++i) {
        auto& section = this->section(i);
        if (section.type() == type) {
            if (!func(section))
                break;
        }
    }
}

template<typename F>
inline void ELFImage::for_each_symbol(F func) const
{
    for (unsigned i = 0; i < symbol_count(); ++i) {
        if (func(symbol(i)) == IterationDecision::Abort)
            break;
    }
}

template<typename F>
inline void ELFImage::for_each_program_header(F func) const
{
    for (unsigned i = 0; i < program_header_count(); ++i)
        func(program_header(i));
}
