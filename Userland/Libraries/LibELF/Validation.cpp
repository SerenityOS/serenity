/*
 * Copyright (c) 2020, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Checked.h>
#include <Kernel/API/serenity_limits.h>
#include <LibELF/ELFABI.h>
#include <LibELF/Validation.h>

#ifndef KERNEL
#    include <limits.h>
#    include <pthread.h>
#endif

namespace ELF {

bool validate_elf_header(Elf_Ehdr const& elf_header, size_t file_size, bool verbose)
{
    if (!IS_ELF(elf_header)) {
        if (verbose)
            dbgln("File is not an ELF file.");
        return false;
    }

    auto expected_class = ELFCLASS64;
    auto expected_bitness = 64;
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

    // NOTE: With Clang, -fprofile-instr-generate -fcoverage-mapping sets our ELF ABI Version to 3 b/c of SHF_GNU_RETAIN
    if (ELFOSABI_SYSV != elf_header.e_ident[EI_OSABI] && ELFOSABI_LINUX != elf_header.e_ident[EI_OSABI]) {
        if (verbose)
            dbgln("File has unknown OS ABI ({}), expected SYSV(0) or GNU/Linux(3)!", elf_header.e_ident[EI_OSABI]);
        return false;
    }

    if (0 != elf_header.e_ident[EI_ABIVERSION]) {
        if (verbose)
            dbgln("File has unknown SYSV ABI version ({})!", elf_header.e_ident[EI_ABIVERSION]);
        return false;
    }

    auto expected_machines = Array { EM_X86_64, EM_AARCH64, EM_RISCV };
    auto expected_machine_names = Array { "x86-64"sv, "aarch64"sv, "riscv64"sv };

    if (!expected_machines.span().contains_slow(elf_header.e_machine)) {
        if (verbose)
            dbgln("File has unknown machine ({}), expected {} ({})!", elf_header.e_machine, expected_machine_names.span(), expected_machines.span());
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

    if (sizeof(Elf_Ehdr) != elf_header.e_ehsize) {
        if (verbose)
            dbgln("File has incorrect ELF header size..? ({}), expected ({})!", elf_header.e_ehsize, sizeof(Elf_Ehdr));
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
        // TODO: Refuse to run C ABI binaries on system without the C extension.
        // TODO: Refuse to run TSO ABI binaries on system without the Ztso extension.
        if (elf_header.e_machine == EM_RISCV) {
            auto float_abi = elf_header.e_flags & EF_RISCV_FLOAT_ABI;
            // TODO: Support 32-bit hardware float ABI somehow?
            if (float_abi != EF_RISCV_FLOAT_ABI_DOUBLE) {
                if (verbose)
                    dbgln("File has unsupported float ABI ({}), only double ({}) is supported.", float_abi, EF_RISCV_FLOAT_ABI_DOUBLE);
                return false;
            }
        } else {
            if (verbose)
                dbgln("File has incorrect ELF header flags...? ({}), expected ({}).", elf_header.e_flags, 0);
            return false;
        }
    }

    if (0 != elf_header.e_phnum && sizeof(Elf_Phdr) != elf_header.e_phentsize) {
        if (verbose)
            dbgln("File has incorrect program header size..? ({}), expected ({}).", elf_header.e_phentsize, sizeof(Elf_Phdr));
        return false;
    }

    if (sizeof(Elf_Shdr) != elf_header.e_shentsize) {
        if (verbose)
            dbgln("File has incorrect section header size..? ({}), expected ({}).", elf_header.e_shentsize, sizeof(Elf_Shdr));
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

bool validate_program_headers(Elf_Ehdr const& elf_header, size_t file_size, ReadonlyBytes buffer, Optional<Elf_Phdr>& interpreter_path_program_header, Optional<size_t>* requested_stack_size, bool verbose)
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
    auto program_header_begin = (Elf_Phdr const*)buffer.offset(elf_header.e_phoff);

    for (size_t header_index = 0; header_index < num_program_headers; ++header_index) {
        auto& program_header = program_header_begin[header_index];

        if (elf_header.e_machine == EM_RISCV && program_header.p_type == PT_RISCV_ATTRIBUTES) {
            // TODO: Handle RISC-V attribute section.
            //       We have to continue here, as `p_memsz` is 0 when using the GNU toolchain
            continue;
        }

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

            if (program_header.p_type == PT_LOAD && program_header.p_align % (size_t)SERENITY_PAGE_SIZE != 0) {
                if (verbose)
                    dbgln("Program header ({}) with p_type PT_LOAD has p_align ({}) not divisible by page size ({})", header_index, program_header.p_align, SERENITY_PAGE_SIZE);
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
            if (program_header.p_offset + program_header.p_filesz > file_size) {
                if (verbose)
                    dbgln("SHENANIGANS! PT_INTERP header segment leaks beyond end of file!");
                return false;
            }
            if (program_header.p_filesz <= 1) {
                if (verbose)
                    dbgln("Found PT_INTERP header ({}), but p_filesz is invalid ({})", header_index, program_header.p_filesz);
                return false;
            }
            interpreter_path_program_header = program_header;
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

            if (program_header.p_memsz != 0) {
                if (
#ifdef PTHREAD_STACK_MIN
                    program_header.p_memsz < static_cast<unsigned>(PTHREAD_STACK_MIN) ||
#endif
                    program_header.p_memsz > static_cast<unsigned>(PTHREAD_STACK_MAX)) {
                    if (verbose)
                        dbgln("PT_GNU_STACK defines an unacceptable stack size.");
                    return false;
                }

                if (program_header.p_memsz % SERENITY_PAGE_SIZE != 0) {
                    if (verbose)
                        dbgln("PT_GNU_STACK size is not page-aligned.");
                    return false;
                }

                if (requested_stack_size)
                    *requested_stack_size = program_header.p_memsz;
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
