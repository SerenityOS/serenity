/*
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
#include "List.h"
#include "Syscalls.h"
#include "Utils.h"
#include <LibELF/AuxiliaryData.h>
#include <LibELF/exec_elf.h>

class DynamicObject {
public:
    explicit DynamicObject(const ELF::AuxiliaryData&);
    ~DynamicObject() = default;

    List<const char*>& needed_libraries() { return m_needed_libraries; }
    const char* object_name() const
    {
        return m_object_name ? m_object_name : "[UNNAMED]";
    };

    Elf32_Addr base_address() const { return m_base_adderss; }
    size_t tls_size() const { return m_tls_size; }
    bool has_tls() const { return m_tls_size != 0; }
    void set_tls_end_offset(size_t offset) { m_tls_end_offset = offset; }
    size_t tls_end_offset() const { return m_tls_end_offset; }
    bool has_text_relocations() const { return m_flags & DF_TEXTREL; }
    bool has_static_tls() const { return m_flags & DF_STATIC_TLS; }
    Elf32_Addr text_segment_load_address() const { return m_base_adderss; }
    size_t text_segment_size() const { return m_text_segment_size; }

    typedef void (*InitializationFunction)();
    bool has_init_section() const
    {
        return m_init_section != 0
            && m_init_section != m_base_adderss;
    }
    InitializationFunction init_section_function() const
    {
        ASSERT(has_init_section());
        return (InitializationFunction)m_init_section;
    }

    class Symbol;
    class Relocation;

    Symbol symbol(size_t index) const;
    Relocation relocation(size_t index) const;
    Relocation plt_relocation(size_t index) const;

    class Symbol {
    public:
        Symbol(const DynamicObject& dynamic, size_t index, const Elf32_Sym& sym)
            : m_dynamic(dynamic)
            , m_sym(sym)
            , m_index(index)
        {
        }

        static Symbol create_undefined(const DynamicObject& dynamic)
        {
            auto s = Symbol(dynamic, 0, {});
            s.m_is_undefined = true;
            return s;
        }

        ~Symbol() {}

        const char* name() const { return m_dynamic.symbol_string_table_string(m_sym.st_name); }
        unsigned section_index() const { return m_sym.st_shndx; }
        unsigned value() const { return m_sym.st_value; }
        unsigned size() const { return m_sym.st_size; }
        unsigned index() const { return m_index; }
        unsigned type() const { return ELF32_ST_TYPE(m_sym.st_info); }
        unsigned bind() const { return ELF32_ST_BIND(m_sym.st_info); }
        bool is_undefined() const { return (section_index() == 0) || m_is_undefined; }
        Elf32_Addr address() const { return value() + m_dynamic.base_address(); }
        const DynamicObject& object() const { return m_dynamic; }

    private:
        const DynamicObject& m_dynamic;
        const Elf32_Sym& m_sym;
        const unsigned m_index;
        bool m_is_undefined { false };
    };

    class Relocation {
    public:
        Relocation(const DynamicObject& dynamic, const Elf32_Rel& rel, unsigned offset_in_section)
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
        Elf32_Addr address() const { return m_dynamic.base_address() + offset(); }

    private:
        const DynamicObject& m_dynamic;
        const Elf32_Rel& m_rel;
        const unsigned m_offset_in_section;
    };

    template<typename Func>
    void for_each_symbol(Func f);
    template<typename Func>
    void for_each_relocation(Func f);
    template<typename Func>
    void for_each_plt_relocation(Func f);
    template<typename Func>
    void for_each_initialization_array_function(Func f);

    Symbol lookup_symbol(const char*) const;

private:
    static Elf32_Addr find_dynamic_section_address(const ELF::AuxiliaryData&);
    void iterate_entries();

    const char* symbol_string_table_string(Elf32_Word index) const;

    Elf32_Addr m_base_adderss { 0 };
    const Elf32_Dyn* m_dynamic_section_entries { nullptr };

    Elf32_Addr m_string_table { 0 };
    Elf32_Addr m_hash_section { 0 };
    Elf32_Sym* m_dyn_sym_table { nullptr };
    size_t m_symbol_count { 0 };

    Elf32_Rel* m_relocations_table { nullptr };
    Elf32_Word m_relocation_entry_size { 0 };
    Elf32_Word m_relocations_table_size { 0 };
    size_t m_relocations_count { 0 };

    Elf32_Addr m_plt_got_address { 0 };
    Elf32_Rel* m_plt_relocations_table { nullptr };
    Elf32_Word m_plt_relocations_table_size { 0 };
    size_t m_plt_relocations_count { 0 };

    Elf32_Addr m_init_section { 0 };
    Elf32_Addr m_init_array { 0 };
    Elf32_Word m_init_array_size { 0 };

    size_t m_tls_size { 0 };
    size_t m_text_segment_size { 0 };

    Elf32_Word m_flags { 0 };

    // Offset of the end of the allocated tls block for this object,
    // from the global tls pool.
    size_t m_tls_end_offset { 0 };

    List<const char*>
        m_needed_libraries;
    const char* m_object_name { nullptr };
};

template<typename Func>
void DynamicObject::for_each_symbol(Func f)
{
    if (!m_dyn_sym_table)
        return;
    for (size_t i = 0; i < m_symbol_count; ++i) {
        f(symbol(i));
    }
}

template<typename Func>
void DynamicObject::for_each_relocation(Func f)
{
    if (!m_relocations_table)
        return;
    for (size_t i = 0; i < m_relocations_count; ++i) {
        f(relocation(i));
    }
}

template<typename Func>
void DynamicObject::for_each_plt_relocation(Func f)
{
    if (!m_plt_relocations_table)
        return;
    for (size_t i = 0; i < m_plt_relocations_count; ++i) {
        f(plt_relocation(i));
    }
}

template<typename Func>
void DynamicObject::for_each_initialization_array_function(Func f)
{
    if (!m_init_array)
        return;
    for (size_t i = 0; i < (m_init_array_size / sizeof(void*)); ++i) {
        InitializationFunction current = ((InitializationFunction*)(m_init_array))[i];
        f(current);
    }
}
