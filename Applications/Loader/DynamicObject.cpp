
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
#include "DynamicObject.h"
#include "Utils.h"

DynamicObject::DynamicObject(const ELF::AuxiliaryData& aux_data)
    : m_base_adderss(aux_data.base_address)
    , m_dynamic_section_entries(reinterpret_cast<const Elf32_Dyn*>(find_dynamic_section_address(aux_data)))
    , m_tls_size(aux_data.tls_section_size)
    , m_text_segment_size(aux_data.text_segment_size)
{
    iterate_entries();
}

Elf32_Addr DynamicObject::find_dynamic_section_address(const ELF::AuxiliaryData& aux_data)
{
    Elf32_Addr dynamic_section_addr = 0;
    for (size_t i = 0; i < aux_data.num_program_headers; ++i) {
        Elf32_Phdr* phdr = &((Elf32_Phdr*)aux_data.program_headers)[i];
        VERBOSE("phdr: %p\n", phdr);
        VERBOSE("phdr type: %d\n", phdr->p_type);
        if (phdr->p_type == PT_DYNAMIC) {
            dynamic_section_addr = aux_data.base_address + phdr->p_offset;
        }
    }
    ASSERT(dynamic_section_addr);
    return dynamic_section_addr;
}

void DynamicObject::iterate_entries()
{
    List<uint32_t> m_needed_libraries_offsets;
    int32_t object_name_string_table_offset = -1;
    for (const Elf32_Dyn* current = m_dynamic_section_entries; current->d_tag != DT_NULL; ++current) {
        VERBOSE("DT tag: %x\n", current->d_tag);
        switch (current->d_tag) {
        case DT_NEEDED:
            m_needed_libraries_offsets.append(current->d_un.d_val);
            break;
        case DT_STRTAB:
            m_string_table = m_base_adderss + current->d_un.d_ptr;
            break;
        case DT_SONAME:
            object_name_string_table_offset = current->d_un.d_val;
            break;
        case DT_SYMTAB:
            m_dyn_sym_table = (Elf32_Sym*)(m_base_adderss + current->d_un.d_val);
            break;
        case DT_HASH:
            m_hash_section = m_base_adderss + current->d_un.d_ptr;
            break;
        case DT_REL:
            m_relocations_table = (Elf32_Rel*)(m_base_adderss + current->d_un.d_ptr);
            break;
        case DT_RELSZ:
            m_relocations_table_size = current->d_un.d_val;
            break;
        case DT_RELENT:
            m_relocation_entry_size = current->d_un.d_val;
            ASSERT(m_relocation_entry_size = sizeof(Elf32_Rel));
            break;
        case DT_PLTREL:
            // TODO: Do we need to support RELA here?
            ASSERT(current->d_un.d_val == DT_REL);
            break;
        case DT_PLTGOT:
            m_plt_got_address = m_base_adderss + current->d_un.d_ptr;
            break;
        case DT_PLTRELSZ:
            m_plt_relocations_table_size = current->d_un.d_val;
            break;
        case DT_JMPREL:
            m_plt_relocations_table = (Elf32_Rel*)(m_base_adderss + current->d_un.d_ptr);
            break;
        case DT_INIT:
            m_init_section = m_base_adderss + current->d_un.d_ptr;
            break;
        case DT_INIT_ARRAY:
            m_init_array = m_base_adderss + current->d_un.d_ptr;
            break;
        case DT_INIT_ARRAYSZ:
            m_init_array_size = current->d_un.d_val;
            break;
        case DT_FLAGS:
            m_flags = current->d_un.d_val;
            break;
        }
    }

    ASSERT(m_string_table);
    ASSERT(m_dyn_sym_table);
    ASSERT(m_hash_section);

    if (object_name_string_table_offset > 0) {
        m_object_name = reinterpret_cast<const char*>(m_string_table + object_name_string_table_offset);
    }

    for (auto offset : m_needed_libraries_offsets) {
        m_needed_libraries.append(reinterpret_cast<const char*>(m_string_table + offset));
    }

    for (const auto lib_name : m_needed_libraries) {
        dbgprintf("needed library: %s\n", lib_name);
    }

    m_symbol_count = ((Elf32_Word*)m_hash_section)[1];
    if (m_relocations_table) {
        ASSERT(m_relocations_table_size);
        m_relocations_count = m_relocations_table_size / sizeof(Elf32_Rel);
    }
    if (m_plt_relocations_table) {
        ASSERT(m_plt_relocations_table_size);
        m_plt_relocations_count = m_plt_relocations_table_size / sizeof(Elf32_Rel);
    }
}

DynamicObject::Symbol DynamicObject::symbol(size_t index) const
{
    return Symbol(*this, index, m_dyn_sym_table[index]);
}

DynamicObject::Relocation DynamicObject::relocation(size_t index) const
{
    return Relocation(*this, m_relocations_table[index], index * m_relocation_entry_size);
}

DynamicObject::Relocation DynamicObject::plt_relocation(size_t index) const
{
    return Relocation(*this, m_plt_relocations_table[index], index * m_relocation_entry_size);
}

const char* DynamicObject::symbol_string_table_string(Elf32_Word index) const
{
    return (const char*)(m_string_table + index);
}

unsigned long
elf_Hash(const unsigned char* name)
{
    unsigned long h = 0, g;

    while (*name) {
        h = (h << 4) + *name++;
        if ((g = (h & 0xf0000000)))
            h ^= g >> 24;
        h &= ~g;
    }
    return h;
}

DynamicObject::Symbol DynamicObject::lookup_symbol(const char* name) const
{
    // Elf32_Word n_chains = m_symbol_count;
    Elf32_Word n_buckets = ((Elf32_Word*)m_hash_section)[0];
    Elf32_Word* buckets = &((Elf32_Word*)m_hash_section)[2];
    Elf32_Word* chains = &((Elf32_Word*)m_hash_section)[n_buckets + 2];

    unsigned long hash_value = elf_Hash((const unsigned char*)name);
    Elf32_Word current_index = buckets[hash_value % n_buckets];
    while (current_index != STN_UNDEF) {
        Symbol sym = symbol(current_index);
        if (!strcmp(sym.name(), name)) {
            return sym;
        }
        current_index = chains[current_index];
    }
    return Symbol::create_undefined(*this);
}
