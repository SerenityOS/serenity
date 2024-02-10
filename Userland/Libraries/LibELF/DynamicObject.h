/*
 * Copyright (c) 2019-2020, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/ByteString.h>
#include <AK/Concepts.h>
#include <AK/RefCounted.h>
#include <Kernel/Memory/VirtualAddress.h>
#include <LibELF/Arch/GenericDynamicRelocationType.h>
#include <LibELF/ELFABI.h>
#include <link.h>

namespace ELF {

class DynamicObject : public RefCounted<DynamicObject> {
public:
    static NonnullRefPtr<DynamicObject> create(ByteString const& filepath, VirtualAddress base_address, VirtualAddress dynamic_section_address);
    static char const* name_for_dtag(Elf_Sword d_tag);

    ~DynamicObject();
    void dump() const;

    class DynamicEntry;
    class Section;
    class RelocationSection;
    class Symbol;
    class Relocation;
    class HashSection;

    class DynamicEntry {
    public:
        explicit DynamicEntry(Elf_Dyn const& dyn)
            : m_dyn(dyn)
        {
        }

        ~DynamicEntry() = default;

        Elf_Sword tag() const { return m_dyn.d_tag; }
        Elf_Addr ptr() const { return m_dyn.d_un.d_ptr; }
        Elf_Word val() const { return m_dyn.d_un.d_val; }

    private:
        Elf_Dyn const& m_dyn;
    };

    class Symbol {
    public:
        Symbol(DynamicObject const& dynamic, unsigned index, Elf_Sym const& sym)
            : m_dynamic(dynamic)
            , m_sym(sym)
            , m_index(index)
        {
        }

        StringView name() const { return m_dynamic.symbol_string_table_string(m_sym.st_name); }
        char const* raw_name() const { return m_dynamic.raw_symbol_string_table_string(m_sym.st_name); }
        unsigned section_index() const { return m_sym.st_shndx; }
        FlatPtr value() const { return m_sym.st_value; }
        size_t size() const { return m_sym.st_size; }
        unsigned index() const { return m_index; }
        unsigned type() const
        {
            return ELF64_ST_TYPE(m_sym.st_info);
        }
        unsigned bind() const { return ELF64_ST_BIND(m_sym.st_info); }

        bool is_undefined() const
        {
            return section_index() == 0;
        }

        VirtualAddress address() const
        {
            if (m_dynamic.elf_is_dynamic())
                return m_dynamic.base_address().offset(value());
            return VirtualAddress { value() };
        }
        DynamicObject const& object() const { return m_dynamic; }

        // This might return false even if the two Symbol objects resolve to the same thing.
        bool definitely_equals(Symbol const& other) const
        {
            return &m_dynamic == &other.m_dynamic && &m_sym == &other.m_sym && m_index == other.m_index;
        }

    private:
        DynamicObject const& m_dynamic;
        Elf_Sym const& m_sym;
        unsigned const m_index;
    };

    class Section {
    public:
        Section(DynamicObject const& dynamic, unsigned section_offset, unsigned section_size_bytes, unsigned entry_size, StringView name)
            : m_dynamic(dynamic)
            , m_section_offset(section_offset)
            , m_section_size_bytes(section_size_bytes)
            , m_entry_size(entry_size)
            , m_name(name)
        {
        }
        ~Section() = default;

        StringView name() const { return m_name; }
        unsigned offset() const { return m_section_offset; }
        unsigned size() const { return m_section_size_bytes; }
        unsigned entry_size() const { return m_entry_size; }
        unsigned entry_count() const
        {
            return !entry_size() ? 0 : size() / entry_size();
        }
        VirtualAddress address() const
        {
            return m_dynamic.base_address().offset(m_section_offset);
        }

    protected:
        friend class RelocationSection;
        friend class HashSection;
        DynamicObject const& m_dynamic;
        unsigned m_section_offset;
        unsigned m_section_size_bytes;
        unsigned m_entry_size;
        StringView m_name;
    };

    class RelocationSection : public Section {
    public:
        explicit RelocationSection(Section const& section, bool addend_used)
            : Section(section.m_dynamic, section.m_section_offset, section.m_section_size_bytes, section.m_entry_size, section.m_name)
            , m_addend_used(addend_used)
        {
        }
        unsigned relocation_count() const { return entry_count(); }
        Relocation relocation(unsigned index) const;
        Relocation relocation_at_offset(unsigned offset) const;

        template<IteratorFunction<DynamicObject::Relocation&> F>
        void for_each_relocation(F) const;
        template<VoidFunction<DynamicObject::Relocation&> F>
        void for_each_relocation(F func) const;

    private:
        bool const m_addend_used;
    };

    class Relocation {
    public:
        Relocation(DynamicObject const& dynamic, Elf_Rela const& rel, unsigned offset_in_section, bool addend_used)
            : m_dynamic(dynamic)
            , m_rel(rel)
            , m_offset_in_section(offset_in_section)
            , m_addend_used(addend_used)
        {
        }

        ~Relocation() = default;

        unsigned offset_in_section() const { return m_offset_in_section; }
        unsigned offset() const { return m_rel.r_offset; }
        unsigned type() const
        {
            return ELF64_R_TYPE(m_rel.r_info);
        }
        unsigned symbol_index() const { return ELF64_R_SYM(m_rel.r_info); }
        unsigned addend() const
        {
            VERIFY(m_addend_used);
            return m_rel.r_addend;
        }
        bool addend_used() const { return m_addend_used; }

        Symbol symbol() const
        {
            return m_dynamic.symbol(symbol_index());
        }
        VirtualAddress address() const
        {
            if (m_dynamic.elf_is_dynamic())
                return m_dynamic.base_address().offset(offset());
            return VirtualAddress { offset() };
        }
        [[nodiscard]] DynamicObject const& dynamic_object() const { return m_dynamic; }

    private:
        DynamicObject const& m_dynamic;
        Elf_Rela const& m_rel;
        unsigned const m_offset_in_section;
        bool const m_addend_used;
    };

    enum class HashType {
        SYSV,
        GNU
    };

    class HashSymbol {
    public:
        HashSymbol(StringView name)
            : m_name(name)
        {
        }

        StringView name() const { return m_name; }
        u32 gnu_hash() const;
        u32 sysv_hash() const;

    private:
        StringView m_name;
        mutable Optional<u32> m_gnu_hash;
        mutable Optional<u32> m_sysv_hash;
    };

    class HashSection : public Section {
    public:
        HashSection(Section const& section, HashType hash_type)
            : Section(section.m_dynamic, section.m_section_offset, section.m_section_size_bytes, section.m_entry_size, section.m_name)
            , m_hash_type(hash_type)
        {
        }

        Optional<Symbol> lookup_symbol(HashSymbol const& symbol) const
        {
            if (m_hash_type == HashType::SYSV)
                return lookup_sysv_symbol(symbol.name(), symbol.sysv_hash());
            return lookup_gnu_symbol(symbol.name(), symbol.gnu_hash());
        }

    private:
        Optional<Symbol> lookup_sysv_symbol(StringView name, u32 hash_value) const;
        Optional<Symbol> lookup_gnu_symbol(StringView name, u32 hash) const;

        HashType m_hash_type {};
    };

    unsigned symbol_count() const { return m_symbol_count; }

    Symbol symbol(unsigned) const;

    typedef void (*InitializationFunction)();
    typedef void (*FinalizationFunction)();
    typedef Elf_Addr (*IfuncResolver)();

    bool has_init_section() const { return m_init_offset != 0; }
    bool has_init_array_section() const { return m_init_array_offset != 0; }
    Section init_section() const;
    InitializationFunction init_section_function() const;
    Section init_array_section() const;

    bool is_pie() const { return m_is_pie; }

    bool has_fini_section() const { return m_fini_offset != 0; }
    bool has_fini_array_section() const { return m_fini_array_offset != 0; }
    Section fini_section() const;
    FinalizationFunction fini_section_function() const;
    Section fini_array_section() const;

    HashSection hash_section() const
    {
        auto section_name = m_hash_type == HashType::SYSV ? "DT_HASH"sv : "DT_GNU_HASH"sv;

        return HashSection(Section(*this, m_hash_table_offset, 0, 0, section_name), m_hash_type);
    }

    RelocationSection relocation_section() const;
    RelocationSection plt_relocation_section() const;
    Section relr_relocation_section() const;

    bool should_process_origin() const { return m_dt_flags & DF_ORIGIN; }
    bool requires_symbolic_symbol_resolution() const { return m_dt_flags & DF_SYMBOLIC; }
    // Text relocations meaning: we need to edit the .text section which is normally mapped PROT_READ
    bool has_text_relocations() const { return m_dt_flags & DF_TEXTREL; }
    bool must_bind_now() const { return m_dt_flags & DF_BIND_NOW; }
    bool has_static_thread_local_storage() const { return m_dt_flags & DF_STATIC_TLS; }

    bool has_plt() const { return m_procedure_linkage_table_offset.has_value(); }
    VirtualAddress plt_got_base_address() const { return m_base_address.offset(m_procedure_linkage_table_offset.value()); }
    VirtualAddress base_address() const { return m_base_address; }

    ByteString const& filepath() const { return m_filepath; }

    StringView rpath() const { return m_has_rpath ? symbol_string_table_string(m_rpath_index) : StringView {}; }
    StringView runpath() const { return m_has_runpath ? symbol_string_table_string(m_runpath_index) : StringView {}; }
    StringView soname() const { return m_has_soname ? symbol_string_table_string(m_soname_index) : StringView {}; }

    Optional<FlatPtr> tls_offset() const { return m_tls_offset; }
    Optional<FlatPtr> tls_size() const { return m_tls_size; }
    void set_tls_offset(FlatPtr offset) { m_tls_offset = offset; }
    void set_tls_size(FlatPtr size) { m_tls_size = size; }

    Elf_Half program_header_count() const;
    Elf_Phdr const* program_headers() const;

    template<VoidFunction<StringView> F>
    void for_each_needed_library(F) const;

    template<VoidFunction<InitializationFunction&> F>
    void for_each_initialization_array_function(F f) const;

    template<IteratorFunction<DynamicEntry&> F>
    void for_each_dynamic_entry(F) const;
    template<VoidFunction<DynamicEntry&> F>
    void for_each_dynamic_entry(F func) const;

    template<VoidFunction<Symbol&> F>
    void for_each_symbol(F) const;

    template<typename F>
    void for_each_relr_relocation(F) const;

    struct SymbolLookupResult {
        FlatPtr value { 0 };
        size_t size { 0 };
        VirtualAddress address;
        unsigned bind { STB_LOCAL };
        unsigned type { STT_FUNC };
        const ELF::DynamicObject* dynamic_object { nullptr }; // The object in which the symbol is defined
    };

    Optional<SymbolLookupResult> lookup_symbol(StringView name) const;
    Optional<SymbolLookupResult> lookup_symbol(HashSymbol const& symbol) const;

    bool elf_is_dynamic() const { return m_is_elf_dynamic; }

    void* symbol_for_name(StringView name);

private:
    explicit DynamicObject(ByteString const& filepath, VirtualAddress base_address, VirtualAddress dynamic_section_address);

    StringView symbol_string_table_string(Elf_Word) const;
    char const* raw_symbol_string_table_string(Elf_Word) const;
    void parse();

    ByteString m_filepath;

    VirtualAddress m_base_address;
    VirtualAddress m_dynamic_address;
    VirtualAddress m_elf_base_address;

    unsigned m_symbol_count { 0 };

    // Begin Section information collected from DT_* entries
    FlatPtr m_init_offset { 0 };
    FlatPtr m_fini_offset { 0 };

    FlatPtr m_init_array_offset { 0 };
    size_t m_init_array_size { 0 };
    FlatPtr m_fini_array_offset { 0 };
    size_t m_fini_array_size { 0 };

    FlatPtr m_hash_table_offset { 0 };
    HashType m_hash_type { HashType::SYSV };

    FlatPtr m_string_table_offset { 0 };
    size_t m_size_of_string_table { 0 };
    FlatPtr m_symbol_table_offset { 0 };
    size_t m_size_of_symbol_table_entry { 0 };

    Elf_Sword m_procedure_linkage_table_relocation_type { -1 };
    FlatPtr m_plt_relocation_offset_location { 0 }; // offset of PLT relocations, at end of relocations
    size_t m_size_of_plt_relocation_entry_list { 0 };
    Optional<FlatPtr> m_procedure_linkage_table_offset;

    // NOTE: We'll only ever either RELA or REL entries, not both (thank god)
    // NOTE: The x86 ABI will only ever genrerate REL entries.
    size_t m_number_of_relocations { 0 };
    size_t m_size_of_relocation_entry { 0 };
    size_t m_size_of_relocation_table { 0 };
    bool m_addend_used { false };
    FlatPtr m_relocation_table_offset { 0 };
    size_t m_size_of_relr_relocations_entry { 0 };
    size_t m_size_of_relr_relocation_table { 0 };
    FlatPtr m_relr_relocation_table_offset { 0 };
    bool m_is_elf_dynamic { false };

    bool m_is_pie { false };

    // DT_FLAGS
    Elf_Word m_dt_flags { 0 };

    bool m_has_soname { false };
    Elf_Word m_soname_index { 0 }; // Index into dynstr table for SONAME
    bool m_has_rpath { false };
    Elf_Word m_rpath_index { 0 }; // Index into dynstr table for RPATH
    bool m_has_runpath { false };
    Elf_Word m_runpath_index { 0 }; // Index into dynstr table for RUNPATH

    Optional<FlatPtr> m_tls_offset;
    Optional<FlatPtr> m_tls_size;
    // End Section information from DT_* entries
};

template<IteratorFunction<DynamicObject::Relocation&> F>
inline void DynamicObject::RelocationSection::for_each_relocation(F func) const
{
    for (unsigned i = 0; i < relocation_count(); ++i) {
        auto const reloc = relocation(i);
        if (static_cast<GenericDynamicRelocationType>(reloc.type()) == GenericDynamicRelocationType::NONE)
            continue;
        if (func(reloc) == IterationDecision::Break)
            break;
    }
}

template<VoidFunction<DynamicObject::Relocation&> F>
inline void DynamicObject::RelocationSection::for_each_relocation(F func) const
{
    for_each_relocation([&](auto& reloc) {
        func(reloc);
        return IterationDecision::Continue;
    });
}

template<typename F>
inline void DynamicObject::for_each_relr_relocation(F f) const
{
    auto section = relr_relocation_section();
    if (section.entry_count() == 0)
        return;

    VERIFY(section.entry_size() == sizeof(FlatPtr));
    VERIFY(section.size() >= section.entry_size() * section.entry_count());

    auto* entries = reinterpret_cast<Elf_Relr*>(section.address().get());
    auto base = base_address().get();
    FlatPtr patch_addr = 0;
    for (unsigned i = 0; i < section.entry_count(); ++i) {
        if ((entries[i] & 1u) == 0) {
            patch_addr = base + entries[i];
            f(patch_addr);
            patch_addr += sizeof(FlatPtr);
        } else {
            unsigned j = 0;
            for (auto bitmap = entries[i]; (bitmap >>= 1u) != 0; ++j)
                if (bitmap & 1u)
                    f(patch_addr + j * sizeof(FlatPtr));

            patch_addr += (8 * sizeof(FlatPtr) - 1) * sizeof(FlatPtr);
        }
    }
}

template<VoidFunction<DynamicObject::Symbol&> F>
inline void DynamicObject::for_each_symbol(F func) const
{
    for (unsigned i = 0; i < symbol_count(); ++i) {
        func(symbol(i));
    }
}

template<IteratorFunction<DynamicObject::DynamicEntry&> F>
inline void DynamicObject::for_each_dynamic_entry(F func) const
{
    auto* dyns = reinterpret_cast<Elf_Dyn const*>(m_dynamic_address.as_ptr());
    for (unsigned i = 0;; ++i) {
        auto&& dyn = DynamicEntry(dyns[i]);
        if (dyn.tag() == DT_NULL)
            break;
        if (func(dyn) == IterationDecision::Break)
            break;
    }
}

template<VoidFunction<DynamicObject::DynamicEntry&> F>
inline void DynamicObject::for_each_dynamic_entry(F func) const
{
    for_each_dynamic_entry([&](auto& dyn) {
        func(dyn);
        return IterationDecision::Continue;
    });
}

template<VoidFunction<StringView> F>
inline void DynamicObject::for_each_needed_library(F func) const
{
    for_each_dynamic_entry([func, this](auto entry) {
        if (entry.tag() != DT_NEEDED)
            return;
        Elf_Word offset = entry.val();
        func(symbol_string_table_string(offset));
    });
}

template<VoidFunction<DynamicObject::InitializationFunction&> F>
void DynamicObject::for_each_initialization_array_function(F f) const
{
    if (!has_init_array_section())
        return;
    FlatPtr init_array = (FlatPtr)init_array_section().address().as_ptr();
    for (size_t i = 0; i < (m_init_array_size / sizeof(void*)); ++i) {
        InitializationFunction current = ((InitializationFunction*)(init_array))[i];
        f(current);
    }
}

} // end namespace ELF
