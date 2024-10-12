/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibELF/Arch/GenericDynamicRelocationType.h>
#include <LibELF/ELFABI.h>
#include <LibELF/Relocation.h>

namespace ELF {

[[gnu::no_stack_protector]] bool perform_relative_relocations(FlatPtr base_address, FlatPtr runtime_base_address, FlatPtr dynamic_section_addr)
{
    FlatPtr relocation_section_addr = 0;
    size_t relocation_table_size = 0;
    size_t relocation_count = 0;
    size_t relocation_entry_size = 0;
    FlatPtr relr_relocation_section_addr = 0;
    size_t relr_relocation_table_size = 0;
    bool use_addend = false;
    auto* dyns = reinterpret_cast<Elf_Dyn const*>(dynamic_section_addr);
    for (unsigned i = 0;; ++i) {
        auto& dyn = dyns[i];
        if (dyn.d_tag == DT_NULL)
            break;
        if (dyn.d_tag == DT_RELA)
            use_addend = true;
        if (dyn.d_tag == DT_REL || dyn.d_tag == DT_RELA)
            relocation_section_addr = base_address + dyn.d_un.d_ptr;
        else if (dyn.d_tag == DT_RELCOUNT || dyn.d_tag == DT_RELACOUNT)
            relocation_count = dyn.d_un.d_val;
        else if (dyn.d_tag == DT_RELSZ || dyn.d_tag == DT_RELASZ)
            relocation_table_size = dyn.d_un.d_val;
        else if (dyn.d_tag == DT_RELENT || dyn.d_tag == DT_RELAENT)
            relocation_entry_size = dyn.d_un.d_val;
        else if (dyn.d_tag == DT_RELR)
            relr_relocation_section_addr = base_address + dyn.d_un.d_ptr;
        else if (dyn.d_tag == DT_RELRSZ)
            relr_relocation_table_size = dyn.d_un.d_val;
        else if (dyn.d_tag == DT_RELRENT)
            VERIFY(dyn.d_un.d_val == sizeof(FlatPtr));
    }

    if ((!relocation_section_addr || !relocation_table_size || !relocation_count) && (!relr_relocation_section_addr || !relr_relocation_table_size))
        return false;

    for (unsigned i = 0; i < relocation_count; ++i) {
        size_t offset_in_section = i * relocation_entry_size;
        auto* relocation = (Elf_Rela*)(relocation_section_addr + offset_in_section);
        VERIFY(static_cast<GenericDynamicRelocationType>(ELF64_R_TYPE(relocation->r_info)) == GenericDynamicRelocationType::RELATIVE);
        auto* patch_address = (FlatPtr*)(base_address + relocation->r_offset);
        FlatPtr relocated_address;
        if (use_addend) {
            relocated_address = runtime_base_address + relocation->r_addend;
        } else {
            __builtin_memcpy(&relocated_address, patch_address, sizeof(relocated_address));
            relocated_address += runtime_base_address;
        }
        __builtin_memcpy(patch_address, &relocated_address, sizeof(relocated_address));
    }

    auto patch_relr = [runtime_base_address](FlatPtr* patch_ptr) {
        FlatPtr relocated_address;
        __builtin_memcpy(&relocated_address, patch_ptr, sizeof(FlatPtr));
        relocated_address += runtime_base_address;
        __builtin_memcpy(patch_ptr, &relocated_address, sizeof(FlatPtr));
    };

    auto* entries = reinterpret_cast<Elf_Relr*>(relr_relocation_section_addr);
    FlatPtr* patch_ptr = nullptr;

    for (unsigned i = 0; i < relr_relocation_table_size / sizeof(FlatPtr); ++i) {
        if ((entries[i] & 1u) == 0) {
            patch_ptr = reinterpret_cast<FlatPtr*>(base_address + entries[i]);
            patch_relr(patch_ptr);
            ++patch_ptr;
        } else {
            unsigned j = 0;
            for (auto bitmap = entries[i]; (bitmap >>= 1u) != 0; ++j)
                if (bitmap & 1u)
                    patch_relr(patch_ptr + j);

            patch_ptr += 8 * sizeof(FlatPtr) - 1;
        }
    }
    return true;
}

[[gnu::no_stack_protector]] bool perform_relative_relocations(FlatPtr base_address)
{
    Elf_Ehdr* header = (Elf_Ehdr*)(base_address);
    Elf_Phdr* pheader = (Elf_Phdr*)(base_address + header->e_phoff);
    FlatPtr dynamic_section_addr = 0;
    for (size_t i = 0; i < (size_t)header->e_phnum; ++i, ++pheader) {
        if (pheader->p_type != PT_DYNAMIC)
            continue;
        dynamic_section_addr = pheader->p_vaddr + base_address;
    }
    if (!dynamic_section_addr)
        return false;

    return perform_relative_relocations(base_address, base_address, dynamic_section_addr);
}

}
