/*
 * Copyright (c) 2020, Andrew Kaster <andrewdkaster@gmail.com>
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

#include <AK/Assertions.h>
#include <AK/String.h>
#include <LibELF/Validation.h>
#include <LibELF/exec_elf.h>

namespace ELF {

bool validate_elf_header(const Elf32_Ehdr& elf_header, size_t file_size, bool verbose)
{
    if (!IS_ELF(elf_header)) {
        if (verbose)
            dbgln("File is not an ELF file.");
        return false;
    }

    if (ELFCLASS32 != elf_header.e_ident[EI_CLASS]) {
        if (verbose)
            dbgln("File is not a 32 bit ELF file.");
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

    if (EM_386 != elf_header.e_machine) {
        if (verbose)
            dbgln("File has unknown machine ({}), expected i386 (3)!", elf_header.e_machine);
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

    if (sizeof(Elf32_Ehdr) != elf_header.e_ehsize) {
        if (verbose)
            dbgln("File has incorrect ELF header size..? ({}), expected ({})!", elf_header.e_ehsize, sizeof(Elf32_Ehdr));
        return false;
    }

    if (elf_header.e_phoff < elf_header.e_ehsize || (elf_header.e_shnum != SHN_UNDEF && elf_header.e_shoff < elf_header.e_ehsize)) {
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

    if (0 != elf_header.e_phnum && sizeof(Elf32_Phdr) != elf_header.e_phentsize) {
        if (verbose)
            dbgln("File has incorrect program header size..? ({}), expected ({}).", elf_header.e_phentsize, sizeof(Elf32_Phdr));
        return false;
    }

    if (sizeof(Elf32_Shdr) != elf_header.e_shentsize) {
        if (verbose)
            dbgln("File has incorrect section header size..? ({}), expected ({}).", elf_header.e_shentsize, sizeof(Elf32_Shdr));
        return false;
    }

    size_t end_of_last_program_header = elf_header.e_phoff + (elf_header.e_phnum * elf_header.e_phentsize);
    if (end_of_last_program_header > file_size) {
        if (verbose)
            dbgln("SHENANIGANS! End of last program header ({}) is past the end of the file!", end_of_last_program_header);
        return false;
    }

    if (elf_header.e_shoff != SHN_UNDEF && elf_header.e_shoff < end_of_last_program_header) {
        if (verbose) {
            dbgln("SHENANIGANS! Section header table begins at file offset {}, which is within program headers [ {} - {} ]!",
                elf_header.e_shoff, elf_header.e_phoff, end_of_last_program_header);
        }
        return false;
    }

    size_t end_of_last_section_header = elf_header.e_shoff + (elf_header.e_shnum * elf_header.e_shentsize);
    if (end_of_last_section_header > file_size) {
        if (verbose)
            dbgln("SHENANIGANS! End of last section header ({}) is past the end of the file!", end_of_last_section_header);
        return false;
    }

    if (elf_header.e_shstrndx != SHN_UNDEF && elf_header.e_shstrndx >= elf_header.e_shnum) {
        if (verbose)
            dbgln("SHENANIGANS! Section header string table index ({}) is not a valid index given we have {} section headers!", elf_header.e_shstrndx, elf_header.e_shnum);
        return false;
    }

    return true;
}

bool validate_program_headers(const Elf32_Ehdr& elf_header, size_t file_size, const u8* buffer, size_t buffer_size, String* interpreter_path, bool verbose)
{
    // Can we actually parse all the program headers in the given buffer?
    size_t end_of_last_program_header = elf_header.e_phoff + (elf_header.e_phnum * elf_header.e_phentsize);
    if (end_of_last_program_header > buffer_size) {
        if (verbose)
            dbgln("Unable to parse program headers from buffer, buffer too small! Buffer size: {}, End of program headers {}",
                buffer_size, end_of_last_program_header);
        return false;
    }

    if (file_size < buffer_size) {
        dbgln("We somehow read more from a file than was in the file in the first place!");
        ASSERT_NOT_REACHED();
    }

    size_t num_program_headers = elf_header.e_phnum;
    auto program_header_begin = (const Elf32_Phdr*)&(buffer[elf_header.e_phoff]);

    for (size_t header_index = 0; header_index < num_program_headers; ++header_index) {
        auto& program_header = program_header_begin[header_index];

        if (program_header.p_filesz > program_header.p_memsz) {
            if (verbose)
                dbgln("Program header ({}) has p_filesz ({}) larger than p_memsz ({})", header_index, program_header.p_filesz, program_header.p_memsz);
            return false;
        }

        if (program_header.p_memsz <= 0 && (program_header.p_type == PT_TLS || program_header.p_type == PT_LOAD)) {
            if (verbose)
                dbgln("Program header ({}) has invalid size in memory ({})", header_index, program_header.p_memsz);
            return false;
        }

        if (program_header.p_type == PT_LOAD && program_header.p_align != PAGE_SIZE) {
            if (elf_header.e_type != ET_CORE) {
                if (verbose)
                    dbgln("Program header ({}) with p_type PT_LOAD has p_align ({}) not equal to page size ({})", header_index, program_header.p_align, PAGE_SIZE);
                return false;
            }
        }

        switch (program_header.p_type) {
        case PT_INTERP:
            // We checked above that file_size was >= buffer size. We only care about buffer size anyway, we're trying to read this!
            if (program_header.p_offset + program_header.p_filesz > buffer_size) {
                if (verbose)
                    dbgln("Found PT_INTERP header ({}), but the .interp section was not within the buffer :(", header_index);
                return false;
            }
            if (interpreter_path)
                *interpreter_path = String((const char*)&buffer[program_header.p_offset], program_header.p_filesz - 1);
            break;
        case PT_LOAD:
        case PT_DYNAMIC:
        case PT_NOTE:
        case PT_PHDR:
        case PT_TLS:
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
