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

#pragma once

#include <LibELF/exec_elf.h>

#include <Kernel/VM/VirtualAddress.h>

class ELFDynamicObject {
public:
    explicit ELFDynamicObject(VirtualAddress base_address, VirtualAddress dynamic_section_address);
    ~ELFDynamicObject();
    void dump() const;

    class DynamicEntry;
    class Section;
    class RelocationSection;
    class Symbol;
    class Relocation;
    class HashSection;

    class DynamicEntry {
    public:
        DynamicEntry(const Elf32_Dyn& dyn)
            : m_dyn(dyn)
        {
        }

        ~DynamicEntry() {}

        Elf32_Sword tag() const { return m_dyn.d_tag; }
        Elf32_Addr ptr() const { return m_dyn.d_un.d_ptr; }
        Elf32_Word val() const { return m_dyn.d_un.d_val; }

    private:
        const Elf32_Dyn& m_dyn;
    };

    class Symbol {
    public:
        Symbol(const ELFDynamicObject& dynamic, unsigned index, const Elf32_Sym& sym)
            : m_dynamic(dynamic)
            , m_sym(sym)
            , m_index(index)
        {
        }

        ~Symbol() {}

        const char* name() const { return m_dynamic.symbol_string_table_string(m_sym.st_name); }
        unsigned section_index() const { return m_sym.st_shndx; }
        unsigned value() const { return m_sym.st_value; }
        unsigned size() const { return m_sym.st_size; }
        unsigned index() const { return m_index; }
        unsigned type() const { return ELF32_ST_TYPE(m_sym.st_info); }
        unsigned bind() const { return ELF32_ST_BIND(m_sym.st_info); }
        bool is_undefined() const { return this == &m_dynamic.the_undefined_symbol(); }
        VirtualAddress address() const { return m_dynamic.base_address().offset(value()); }

    private:
        const ELFDynamicObject& m_dynamic;
        const Elf32_Sym& m_sym;
        const unsigned m_index;
    };

    class Section {
    public:
        Section(const ELFDynamicObject& dynamic, unsigned section_offset, unsigned section_size_bytes, unsigned entry_size, const char* name)
            : m_dynamic(dynamic)
            , m_section_offset(section_offset)
            , m_section_size_bytes(section_size_bytes)
            , m_entry_size(entry_size)
            , m_name(name)
        {
        }
        ~Section() {}

        const char* name() const { return m_name; }
        unsigned offset() const { return m_section_offset; }
        unsigned size() const { return m_section_size_bytes; }
        unsigned entry_size() const { return m_entry_size; }
        unsigned entry_count() const { return !entry_size() ? 0 : size() / entry_size(); }
        VirtualAddress address() const { return m_dynamic.base_address().offset(m_section_offset); }

    protected:
        friend class RelocationSection;
        friend class HashSection;
        const ELFDynamicObject& m_dynamic;
        unsigned m_section_offset;
        unsigned m_section_size_bytes;
        unsigned m_entry_size;
        const char* m_name { nullptr };
    };

    class RelocationSection : public Section {
    public:
        RelocationSection(const Section& section)
            : Section(section.m_dynamic, section.m_section_offset, section.m_section_size_bytes, section.m_entry_size, section.m_name)
        {
        }
        unsigned relocation_count() const { return entry_count(); }
        const Relocation relocation(unsigned index) const;
        const Relocation relocation_at_offset(unsigned offset) const;
        template<typename F>
        void for_each_relocation(F) const;
    };

    class Relocation {
    public:
        Relocation(const ELFDynamicObject& dynamic, const Elf32_Rel& rel, unsigned offset_in_section)
            : m_dynamic(dynamic)
            , m_rel(rel)
            , m_offset_in_section(offset_in_section)
        {
        }

        ~Relocation() {}

        unsigned offset_in_section() const { return m_offset_in_section; }
        unsigned offset() const { return m_rel.r_offset; }
        unsigned type() const { return ELF32_R_TYPE(m_rel.r_info); }
        unsigned symbol_index() const { return ELF32_R_SYM(m_rel.r_info); }
        const Symbol symbol() const { return m_dynamic.symbol(symbol_index()); }
        VirtualAddress address() const { return m_dynamic.base_address().offset(offset()); }

    private:
        const ELFDynamicObject& m_dynamic;
        const Elf32_Rel& m_rel;
        const unsigned m_offset_in_section;
    };

    enum class HashType {
        SYSV,
        GNU
    };

    class HashSection : public Section {
    public:
        HashSection(const Section& section, HashType hash_type = HashType::SYSV)
            : Section(section.m_dynamic, section.m_section_offset, section.m_section_size_bytes, section.m_entry_size, section.m_name)
        {
            switch (hash_type) {
            case HashType::SYSV:
                m_hash_function = &HashSection::calculate_elf_hash;
                break;
            case HashType::GNU:
                m_hash_function = &HashSection::calculate_gnu_hash;
                break;
            default:
                ASSERT_NOT_REACHED();
                break;
            }
        }

        const Symbol lookup_symbol(const char*) const;

    private:
        u32 calculate_elf_hash(const char* name) const;
        u32 calculate_gnu_hash(const char* name) const;

        typedef u32 (HashSection::*HashFunction)(const char*) const;
        HashFunction m_hash_function;
    };

    unsigned symbol_count() const { return m_symbol_count; }

    const Symbol symbol(unsigned) const;
    const Symbol& the_undefined_symbol() const { return m_the_undefined_symbol; }

    const Section init_section() const;
    const Section fini_section() const;
    const Section init_array_section() const;
    const Section fini_array_section() const;

    const HashSection hash_section() const;

    const RelocationSection relocation_section() const;
    const RelocationSection plt_relocation_section() const;

    bool should_process_origin() const { return m_dt_flags & DF_ORIGIN; }
    bool requires_symbolic_symbol_resolution() const { return m_dt_flags & DF_SYMBOLIC; }
    // Text relocations meaning: we need to edit the .text section which is normally mapped PROT_READ
    bool has_text_relocations() const { return m_dt_flags & DF_TEXTREL; }
    bool must_bind_now() const { return m_dt_flags & DF_BIND_NOW; }
    bool has_static_thread_local_storage() const { return m_dt_flags & DF_STATIC_TLS; }

    VirtualAddress plt_got_base_address() const { return m_base_address.offset(m_procedure_linkage_table_offset); }
    VirtualAddress base_address() const { return m_base_address; }

private:
    const char* symbol_string_table_string(Elf32_Word) const;
    void parse();

    template<typename F>
    void for_each_symbol(F) const;

    template<typename F>
    void for_each_dynamic_entry(F) const;

    VirtualAddress m_base_address;
    VirtualAddress m_dynamic_address;
    Symbol m_the_undefined_symbol { *this, 0, {} };

    unsigned m_symbol_count { 0 };

    // Begin Section information collected from DT_* entries
    uintptr_t m_init_offset { 0 };
    uintptr_t m_fini_offset { 0 };

    uintptr_t m_init_array_offset { 0 };
    size_t m_init_array_size { 0 };
    uintptr_t m_fini_array_offset { 0 };
    size_t m_fini_array_size { 0 };

    uintptr_t m_hash_table_offset { 0 };

    uintptr_t m_string_table_offset { 0 };
    size_t m_size_of_string_table { 0 };
    uintptr_t m_symbol_table_offset { 0 };
    size_t m_size_of_symbol_table_entry { 0 };

    Elf32_Sword m_procedure_linkage_table_relocation_type { -1 };
    uintptr_t m_plt_relocation_offset_location { 0 }; // offset of PLT relocations, at end of relocations
    size_t m_size_of_plt_relocation_entry_list { 0 };
    uintptr_t m_procedure_linkage_table_offset { 0 };

    // NOTE: We'll only ever either RELA or REL entries, not both (thank god)
    // NOTE: The x86 ABI will only ever genrerate REL entries.
    size_t m_number_of_relocations { 0 };
    size_t m_size_of_relocation_entry { 0 };
    size_t m_size_of_relocation_table { 0 };
    uintptr_t m_relocation_table_offset { 0 };

    // DT_FLAGS
    Elf32_Word m_dt_flags { 0 };
    // End Section information from DT_* entries
};

template<typename F>
inline void ELFDynamicObject::RelocationSection::for_each_relocation(F func) const
{
    for (unsigned i = 0; i < relocation_count(); ++i) {
        if (func(relocation(i)) == IterationDecision::Break)
            break;
    }
}

template<typename F>
inline void ELFDynamicObject::for_each_symbol(F func) const
{
    for (unsigned i = 0; i < symbol_count(); ++i) {
        if (func(symbol(i)) == IterationDecision::Break)
            break;
    }
}

template<typename F>
inline void ELFDynamicObject::for_each_dynamic_entry(F func) const
{
    auto* dyns = reinterpret_cast<const Elf32_Dyn*>(m_dynamic_address.as_ptr());
    for (unsigned i = 0;; ++i) {
        auto&& dyn = DynamicEntry(dyns[i]);
        if (dyn.tag() == DT_NULL)
            break;
        if (func(dyn) == IterationDecision::Break)
            break;
    }
}
