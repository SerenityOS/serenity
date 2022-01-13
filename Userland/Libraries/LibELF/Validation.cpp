/*
 * Copyright (c) 2020, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Checked.h>
#include <AK/String.h>
#include <LibC/elf.h>
#include <LibELF/Validation.h>
#include <limits.h>

namespace ELF {

bool validate_elf_header(ElfW(Ehdr) const& elf_header, size_t file_size, bool verbose)
{
    if (!IS_ELF(elf_header)) {
        if (verbose)
            dbgln("File is not an ELF file.");
        return false;
    }

#if ARCH(I386)
    auto expected_class = ELFCLASS32;
    auto expected_bitness = 32;
#else
    auto expected_class = ELFCLASS64;
    auto expected_bitness = 64;
#endif
    if (expected_class != elf_header.e_ident[EI_CLASS]) {
        if (verbose)
            dbgln("File is not a {}-bit ELF file.", expected_bitness);
        return false;
    }

    if (ELFDATA2LSB != elf_header.e_ident[EI_DATA]) {
        if (verbose)
            dbgln("File is not a little endian ELF file.");
        return false;
    }

    if (EV_CURRENT != elf_header.e_ident[EI_VERSION]) {
        if (verbose)
            dbgln("File has unrecognized ELF version ({}), expected ({})!", elf_header.e_ident[EI_VERSION], EV_CURRENT);
        return false;
    }

    if (ELFOSABI_SYSV != elf_header.e_ident[EI_OSABI]) {
        if (verbose)
            dbgln("File has unknown OS ABI ({}), expected SYSV(0)!", elf_header.e_ident[EI_OSABI]);
        return false;
    }

    if (0 != elf_header.e_ident[EI_ABIVERSION]) {
        if (verbose)
            dbgln("File has unknown SYSV ABI version ({})!", elf_header.e_ident[EI_ABIVERSION]);
        return false;
    }

#if ARCH(I386)
    auto expected_machine = EM_386;
    auto expected_machine_name = "i386";
#else
    auto expected_machine = EM_X86_64;
    auto expected_machine_name = "x86-64";
#endif

    if (expected_machine != elf_header.e_machine) {
        if (verbose)
            dbgln("File has unknown machine ({}), expected {} ({})!", elf_header.e_machine, expected_machine_name, expected_machine);
        return false;
    }

    if (ET_EXEC != elf_header.e_type && ET_DYN != elf_header.e_type && ET_REL != elf_header.e_type && ET_CORE != elf_header.e_type) {
        if (verbose)
            dbgln("File has unloadable ELF type ({}), expected REL (1), EXEC (2), DYN (3) or CORE(4)!", elf_header.e_type);
        return false;
    }

    if (EV_CURRENT != elf_header.e_version) {
        if (verbose)
            dbgln("File has unrecognized ELF version ({}), expected ({})!", elf_header.e_version, EV_CURRENT);
        return false;
    }

    if (sizeof(ElfW(Ehdr)) != elf_header.e_ehsize) {
        if (verbose)
            dbgln("File has incorrect ELF header size..? ({}), expected ({})!", elf_header.e_ehsize, sizeof(ElfW(Ehdr)));
        return false;
    }

    if ((elf_header.e_phnum != 0 && elf_header.e_phoff < elf_header.e_ehsize) || (elf_header.e_shnum != SHN_UNDEF && elf_header.e_shoff < elf_header.e_ehsize)) {
        if (verbose) {
            dbgln("SHENANIGANS! program header offset ({}) or section header offset ({}) overlap with ELF header!",
                elf_header.e_phoff, elf_header.e_shoff);
        }
        return false;
    }

    if (elf_header.e_phoff > file_size || elf_header.e_shoff > file_size) {
        if (verbose) {
            dbgln("SHENANIGANS! program header offset ({}) or section header offset ({}) are past the end of the file!",
                elf_header.e_phoff, elf_header.e_shoff);
        }
        return false;
    }

    if (elf_header.e_phnum == 0 && elf_header.e_phoff != 0) {
        if (verbose)
            dbgln("SHENANIGANS! File has no program headers, but it does have a program header offset ({})!", elf_header.e_phoff);
        return false;
    }

    if (elf_header.e_phnum != 0 && elf_header.e_phoff != elf_header.e_ehsize) {
        if (verbose) {
            dbgln("File does not have program headers directly after the ELF header? program header offset ({}), expected ({}).",
                elf_header.e_phoff, elf_header.e_ehsize);
        }
        return false;
    }

    if (0 != elf_header.e_flags) {
        if (verbose)
            dbgln("File has incorrect ELF header flags...? ({}), expected ({}).", elf_header.e_flags, 0);
        return false;
    }

    if (0 != elf_header.e_phnum && sizeof(ElfW(Phdr)) != elf_header.e_phentsize) {
        if (verbose)
            dbgln("File has incorrect program header size..? ({}), expected ({}).", elf_header.e_phentsize, sizeof(ElfW(Phdr)));
        return false;
    }

    if (sizeof(ElfW(Shdr)) != elf_header.e_shentsize) {
        if (verbose)
            dbgln("File has incorrect section header size..? ({}), expected ({}).", elf_header.e_shentsize, sizeof(ElfW(Shdr)));
        return false;
    }

    Checked<size_t> total_size_of_program_headers = elf_header.e_phnum;
    total_size_of_program_headers *= elf_header.e_phentsize;

    Checked<size_t> end_of_last_program_header = elf_header.e_phoff;
    end_of_last_program_header += total_size_of_program_headers;

    if (end_of_last_program_header.has_overflow()) {
        if (verbose)
            dbgln("SHENANIGANS! Integer overflow in program header validation");
        return false;
    }

    if (end_of_last_program_header > file_size) {
        if (verbose)
            dbgln("SHENANIGANS! End of last program header ({}) is past the end of the file!", end_of_last_program_header.value());
        return false;
    }

    if (elf_header.e_shoff != SHN_UNDEF && elf_header.e_shoff < end_of_last_program_header.value()) {
        if (verbose) {
            dbgln("SHENANIGANS! Section header table begins at file offset {}, which is within program headers [ {} - {} ]!",
                elf_header.e_shoff, elf_header.e_phoff, end_of_last_program_header.value());
        }
        return false;
    }

    Checked<size_t> total_size_of_section_headers = elf_header.e_shnum;
    total_size_of_section_headers *= elf_header.e_shentsize;

    Checked<size_t> end_of_last_section_header = elf_header.e_shoff;
    end_of_last_section_header += total_size_of_section_headers;

    if (end_of_last_section_header.has_overflow()) {
        if (verbose)
            dbgln("SHENANIGANS! Integer overflow in section header validation");
        return false;
    }

    if (end_of_last_section_header > file_size) {
        if (verbose)
            dbgln("SHENANIGANS! End of last section header ({}) is past the end of the file!", end_of_last_section_header.value());
        return false;
    }

    if (elf_header.e_shstrndx != SHN_UNDEF && elf_header.e_shstrndx >= elf_header.e_shnum) {
        if (verbose)
            dbgln("SHENANIGANS! Section header string table index ({}) is not a valid index given we have {} section headers!", elf_header.e_shstrndx, elf_header.e_shnum);
        return false;
    }

    return true;
}

ErrorOr<bool> validate_program_headers(ElfW(Ehdr) const& elf_header, size_t file_size, ReadonlyBytes buffer, StringBuilder* interpreter_path_builder, bool verbose)
{
    Checked<size_t> total_size_of_program_headers = elf_header.e_phnum;
    total_size_of_program_headers *= elf_header.e_phentsize;

    Checked<size_t> end_of_last_program_header = elf_header.e_phoff;
    end_of_last_program_header += total_size_of_program_headers;

    if (end_of_last_program_header.has_overflow()) {
        if (verbose)
            dbgln("SHENANIGANS! Integer overflow in program header validation");
        return false;
    }

    // Can we actually parse all the program headers in the given buffer?
    if (end_of_last_program_header > buffer.size()) {
        if (verbose)
            dbgln("Unable to parse program headers from buffer, buffer too small! Buffer size: {}, End of program headers {}", buffer.size(), end_of_last_program_header.value());
        return false;
    }

    if (file_size < buffer.size()) {
        dbgln("We somehow read more from a file than was in the file in the first place!");
        VERIFY_NOT_REACHED();
    }

    size_t num_program_headers = elf_header.e_phnum;
    auto program_header_begin = (const ElfW(Phdr)*)buffer.offset(elf_header.e_phoff);

    for (size_t header_index = 0; header_index < num_program_headers; ++header_index) {
        auto& program_header = program_header_begin[header_index];

        if (program_header.p_filesz > program_header.p_memsz) {
            if (verbose)
                dbgln("Program header ({}) has p_filesz ({}) larger than p_memsz ({})", header_index, program_header.p_filesz, program_header.p_memsz);
            return false;
        }

        if (elf_header.e_type != ET_CORE) {
            if (program_header.p_type == PT_LOAD && program_header.p_align == 0) {
                if (verbose)
                    dbgln("Program header ({}) with p_type PT_LOAD missing p_align (p_align == 0)", header_index);
                return false;
            }

            if (program_header.p_type == PT_LOAD && program_header.p_align % (size_t)PAGE_SIZE != 0) {
                if (verbose)
                    dbgln("Program header ({}) with p_type PT_LOAD has p_align ({}) not divisible by page size ({})", header_index, program_header.p_align, PAGE_SIZE);
                return false;
            }

            if (program_header.p_type == PT_LOAD && program_header.p_vaddr % program_header.p_align != program_header.p_offset % program_header.p_align) {
                if (verbose)
                    dbgln("Program header ({}) with p_type PT_LOAD has mis-aligned p_vaddr ({:x})", header_index, program_header.p_vaddr);
                return false;
            }
        }

        switch (program_header.p_type) {
        case PT_INTERP:
            // We checked above that file_size was >= buffer size. We only care about buffer size anyway, we're trying to read this!
            if (Checked<size_t>::addition_would_overflow(program_header.p_offset, program_header.p_filesz)) {
                if (verbose)
                    dbgln("Integer overflow while validating PT_INTERP header");
                return false;
            }
            if (program_header.p_offset + program_header.p_filesz > buffer.size()) {
                if (verbose)
                    dbgln("Found PT_INTERP header ({}), but the .interp section was not within the buffer :(", header_index);
                return false;
            }
            if (program_header.p_filesz <= 1) {
                if (verbose)
                    dbgln("Found PT_INTERP header ({}), but p_filesz is invalid ({})", header_index, program_header.p_filesz);
                return false;
            }
            if (interpreter_path_builder)
                TRY(interpreter_path_builder->try_append({ buffer.offset(program_header.p_offset), program_header.p_filesz - 1 }));
            break;
        case PT_LOAD:
        case PT_DYNAMIC:
        case PT_GNU_EH_FRAME:
        case PT_NOTE:
        case PT_PHDR:
        case PT_TLS:
            if (Checked<size_t>::addition_would_overflow(program_header.p_offset, program_header.p_filesz)) {
                if (verbose)
                    dbgln("Integer overflow while validating a program header");
                return false;
            }
            if (program_header.p_offset + program_header.p_filesz > file_size) {
                if (verbose)
                    dbgln("SHENANIGANS! Program header {} segment leaks beyond end of file!", header_index);
                return false;
            }
            if ((program_header.p_flags & PF_X) && (program_header.p_flags & PF_W)) {
                if (verbose)
                    dbgln("SHENANIGANS! Program header {} segment is marked write and execute", header_index);
                return false;
            }
            break;
        case PT_GNU_STACK:
            if (program_header.p_flags & PF_X) {
                if (verbose)
                    dbgln("Possible shenanigans! Validating an ELF with executable stack.");
            }
            break;
        case PT_GNU_RELRO:
            if ((program_header.p_flags & PF_X) && (program_header.p_flags & PF_W)) {
                if (verbose)
                    dbgln("SHENANIGANS! Program header {} segment is marked write and execute", header_index);
                return false;
            }
            break;
        default:
            // Not handling other program header types in other code so... let's not surprise them
            if (verbose)
                dbgln("Found program header ({}) of unrecognized type {}!", header_index, program_header.p_type);
            return false;
        }
    }
    return true;
}

} // end namespace ELF
