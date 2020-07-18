
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

DynamicObject::DynamicObject(Elf32_Addr base_adderss, Elf32_Addr dynamic_section_address)
    : m_base_adderss(base_adderss)
    , m_entries(reinterpret_cast<const Elf32_Dyn*>(dynamic_section_address))
{
    iterate_entries();
}

void DynamicObject::iterate_entries()
{
    List<uint32_t> m_needed_libraries_offsets;
    int32_t object_name_string_table_offset = -1;
    for (const Elf32_Dyn* current = m_entries; current->d_tag != DT_NULL; ++current) {
        dbgprintf("DT tag: %x\n", current->d_tag);
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
        }
    }
    dbgprintf("string table: %p\n", m_string_table);

    if (object_name_string_table_offset > 0) {
        m_object_name = reinterpret_cast<const char*>(m_string_table + object_name_string_table_offset);
    }

    for (auto offset : m_needed_libraries_offsets) {
        m_needed_libraries.append(reinterpret_cast<const char*>(m_string_table + offset));
    }

    for (const auto lib_name : m_needed_libraries) {
        dbgprintf("library: %s\n", lib_name);
    }
}
