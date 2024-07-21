/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/EFIPrekernel/Panic.h>
#include <Kernel/EFIPrekernel/Relocation.h>

#include <LibELF/Arch/GenericDynamicRelocationType.h>
#include <LibELF/ELFABI.h>
#include <LibELF/Image.h>

namespace Kernel {

// This function only works on ELF addresses that are not in a zero-padded area of a program header.
static ErrorOr<Bytes> get_data_at_kernel_elf_virtual_address(ELF::Image const& kernel_elf_image, Bytes kernel_elf_image_data, FlatPtr start_vaddr, size_t size)
{
    ErrorOr<Bytes> result = EINVAL;
    static Optional<ELF::Image::ProgramHeader> cached_program_header;

    static auto is_range_in_program_header = [](FlatPtr start_vaddr, size_t size, ELF::Image::ProgramHeader const& program_header) {
        return start_vaddr >= program_header.vaddr().get() && start_vaddr + size <= program_header.vaddr().get() + program_header.size_in_memory();
    };

    if (cached_program_header.has_value() && is_range_in_program_header(start_vaddr, size, cached_program_header.value()))
        return kernel_elf_image_data.slice(cached_program_header->offset() + start_vaddr - cached_program_header->vaddr().get(), size);

    kernel_elf_image.for_each_program_header([&result, kernel_elf_image_data, start_vaddr, size](ELF::Image::ProgramHeader const& program_header) {
        if (program_header.type() == PT_LOAD && is_range_in_program_header(start_vaddr, size, program_header)) {
            result = kernel_elf_image_data.slice(program_header.offset() + start_vaddr - program_header.vaddr().get(), size);
            cached_program_header = program_header;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    return result;
}

void perform_kernel_relocations(ELF::Image const& kernel_elf_image, Bytes kernel_elf_image_data, FlatPtr base_address)
{
    size_t dynamic_section_offset { 0 };
    kernel_elf_image.for_each_program_header([&dynamic_section_offset](ELF::Image::ProgramHeader const& program_header) {
        if (program_header.type() == PT_DYNAMIC) {
            dynamic_section_offset = program_header.offset();
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    if (dynamic_section_offset == 0)
        PANIC("Kernel image does not have a PT_DYNAMIC program header; can't perform relocations");

    FlatPtr relocation_table_vaddr = 0;
    size_t relocation_table_size = 0;
    size_t relocation_entry_size = 0;
    size_t number_of_relative_relocs = 0;

    FlatPtr relr_relocation_table_vaddr = 0;
    size_t relr_relocation_table_size = 0;

    auto const* dynamic_section_entries = bit_cast<Elf_Dyn const*>(bit_cast<FlatPtr>(kernel_elf_image_data.data()) + dynamic_section_offset);
    for (size_t i = 0;; i++) {
        auto const& entry = dynamic_section_entries[i];

        if (entry.d_tag == DT_NULL)
            break;

        switch (entry.d_tag) {
        case DT_REL:
        case DT_RELSZ:
        case DT_RELENT:
        case DT_RELCOUNT:
            PANIC("DT_REL relocation tables are not supported");

        case DT_RELA:
            relocation_table_vaddr = entry.d_un.d_ptr;
            break;
        case DT_RELASZ:
            relocation_table_size = entry.d_un.d_val;
            break;
        case DT_RELAENT:
            relocation_entry_size = entry.d_un.d_val;
            break;
        case DT_RELACOUNT:
            number_of_relative_relocs = entry.d_un.d_val;
            break;

        case DT_RELR:
            relr_relocation_table_vaddr = entry.d_un.d_ptr;
            break;
        case DT_RELRSZ:
            relr_relocation_table_size = entry.d_un.d_val;
            break;
        case DT_RELRENT:
            VERIFY(entry.d_un.d_val == sizeof(FlatPtr));
            break;
        }
    }

    VERIFY((relocation_table_vaddr != 0 && relocation_table_size != 0 && relocation_entry_size != 0 && number_of_relative_relocs != 0)
        || (relr_relocation_table_vaddr != 0 && relr_relocation_table_size != 0));

    if (relocation_table_vaddr != 0) {
        // We are still identity mapped, so translate the address of the relocation table.
        auto relocation_table_data = MUST(get_data_at_kernel_elf_virtual_address(kernel_elf_image, kernel_elf_image_data, relocation_table_vaddr, relocation_table_size));

        for (size_t i = 0; i < number_of_relative_relocs; i++) {
            auto const& raw_relocation = *bit_cast<Elf_Rela const*>(bit_cast<FlatPtr>(relocation_table_data.data()) + (i * relocation_entry_size));
            ELF::Image::Relocation relocation(kernel_elf_image, raw_relocation, true);

            VERIFY(relocation.type() == to_underlying(ELF::GenericDynamicRelocationType::RELATIVE));

            // Elf_Rela::r_offset is a virtual address for executables, so we need to translate it as well.
            auto* patch_ptr = bit_cast<FlatPtr*>(MUST(get_data_at_kernel_elf_virtual_address(kernel_elf_image, kernel_elf_image_data, relocation.offset(), sizeof(FlatPtr))).data());

            FlatPtr relocated_address = base_address + relocation.addend();
            __builtin_memcpy(patch_ptr, &relocated_address, sizeof(FlatPtr));
        }
    }

    if (relr_relocation_table_vaddr != 0) {
        auto patch_relr = [&kernel_elf_image, kernel_elf_image_data, base_address](FlatPtr patch_vaddr) {
            auto* patch_ptr = bit_cast<FlatPtr*>(MUST(get_data_at_kernel_elf_virtual_address(kernel_elf_image, kernel_elf_image_data, patch_vaddr, sizeof(FlatPtr))).data());

            FlatPtr relocated_address;
            __builtin_memcpy(&relocated_address, patch_ptr, sizeof(FlatPtr));
            relocated_address += base_address;
            __builtin_memcpy(patch_ptr, &relocated_address, sizeof(FlatPtr));
        };

        auto relr_relocation_table_data = MUST(get_data_at_kernel_elf_virtual_address(kernel_elf_image, kernel_elf_image_data, relr_relocation_table_vaddr, relr_relocation_table_size));
        auto* entries = bit_cast<Elf_Relr*>(relr_relocation_table_data.data());
        FlatPtr patch_vaddr = 0;

        for (size_t i = 0; i < relr_relocation_table_size / sizeof(FlatPtr); i++) {
            if ((entries[i] & 1) == 0) {
                patch_vaddr = entries[i];
                patch_relr(patch_vaddr);
                patch_vaddr += sizeof(FlatPtr);
            } else {
                auto bitmap = entries[i];
                for (size_t j = 0; (bitmap >>= 1) != 0; j++)
                    if ((bitmap & 1) != 0)
                        patch_relr(patch_vaddr + (j * sizeof(FlatPtr)));

                patch_vaddr += (8 * sizeof(FlatPtr) - 1) * sizeof(FlatPtr);
            }
        }
    }
}

}
