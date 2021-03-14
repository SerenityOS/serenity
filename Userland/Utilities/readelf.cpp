/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/MappedFile.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>

static const char* object_file_type_to_string(Elf32_Half type)
{
    switch (type) {
    case ET_NONE:
        return "None";
    case ET_REL:
        return "Relocatable";
    case ET_EXEC:
        return "Executable";
    case ET_DYN:
        return "Shared object";
    case ET_CORE:
        return "Core";
    default:
        return "(?)";
    }
}

static const char* object_machine_type_to_string(Elf32_Half type)
{
    switch (type) {
    case ET_NONE:
        return "None";
    case EM_M32:
        return "AT&T WE 32100";
    case EM_SPARC:
        return "SPARC";
    case EM_386:
        return "Intel 80386";
    case EM_68K:
        return "Motorola 68000";
    case EM_88K:
        return "Motorola 88000";
    case EM_486:
        return "Intel 80486";
    case EM_860:
        return "Intel 80860";
    case EM_MIPS:
        return "MIPS R3000 Big-Endian only";
    default:
        return "(?)";
    }
}

static const char* object_program_header_type_to_string(Elf32_Word type)
{
    switch (type) {
    case PT_NULL:
        return "NULL";
    case PT_LOAD:
        return "LOAD";
    case PT_DYNAMIC:
        return "DYNAMIC";
    case PT_INTERP:
        return "INTERP";
    case PT_NOTE:
        return "NOTE";
    case PT_SHLIB:
        return "SHLIB";
    case PT_PHDR:
        return "PHDR";
    case PT_TLS:
        return "TLS";
    case PT_LOOS:
        return "LOOS";
    case PT_HIOS:
        return "HIOS";
    case PT_LOPROC:
        return "LOPROC";
    case PT_HIPROC:
        return "HIPROC";
    case PT_GNU_EH_FRAME:
        return "GNU_EH_FRAME";
    case PT_GNU_RELRO:
        return "GNU_RELRO";
    case PT_GNU_STACK:
        return "GNU_STACK";
    case PT_OPENBSD_RANDOMIZE:
        return "OPENBSD_RANDOMIZE";
    case PT_OPENBSD_WXNEEDED:
        return "OPENBSD_WXNEEDED";
    case PT_OPENBSD_BOOTDATA:
        return "OPENBSD_BOOTDATA";
    default:
        return "(?)";
    }
}

static const char* object_section_header_type_to_string(Elf32_Word type)
{
    switch (type) {
    case SHT_NULL:
        return "NULL";
    case SHT_PROGBITS:
        return "PROGBITS";
    case SHT_SYMTAB:
        return "SYMTAB";
    case SHT_STRTAB:
        return "STRTAB";
    case SHT_RELA:
        return "RELA";
    case SHT_HASH:
        return "HASH";
    case SHT_DYNAMIC:
        return "DYNAMIC";
    case SHT_NOTE:
        return "NOTE";
    case SHT_NOBITS:
        return "NOBITS";
    case SHT_REL:
        return "REL";
    case SHT_SHLIB:
        return "SHLIB";
    case SHT_DYNSYM:
        return "DYNSYM";
    case SHT_NUM:
        return "NUM";
    case SHT_INIT_ARRAY:
        return "INIT_ARRAY";
    case SHT_FINI_ARRAY:
        return "FINI_ARRAY";
    case SHT_PREINIT_ARRAY:
        return "PREINIT_ARRAY";
    case SHT_GROUP:
        return "GROUP";
    case SHT_SYMTAB_SHNDX:
        return "SYMTAB_SHNDX";
    case SHT_LOOS:
        return "SOOS";
    case SHT_SUNW_dof:
        return "SUNW_dof";
    case SHT_GNU_LIBLIST:
        return "GNU_LIBLIST";
    case SHT_SUNW_move:
        return "SUNW_move";
    case SHT_SUNW_syminfo:
        return "SUNW_syminfo";
    case SHT_SUNW_verdef:
        return "SUNW_verdef";
    case SHT_SUNW_verneed:
        return "SUNW_verneed";
    case SHT_SUNW_versym: // or SHT_HIOS
        return "SUNW_versym";
    case SHT_LOPROC:
        return "LOPROC";
    case SHT_HIPROC:
        return "HIPROC";
    case SHT_LOUSER:
        return "LOUSER";
    case SHT_HIUSER:
        return "HIUSER";
    case SHT_GNU_HASH:
        return "GNU_HASH";
    default:
        return "(?)";
    }
}

static const char* object_symbol_type_to_string(Elf32_Word type)
{
    switch (type) {
    case STT_NOTYPE:
        return "NOTYPE";
    case STT_OBJECT:
        return "OBJECT";
    case STT_FUNC:
        return "FUNC";
    case STT_SECTION:
        return "SECTION";
    case STT_FILE:
        return "FILE";
    case STT_TLS:
        return "TLS";
    case STT_LOPROC:
        return "LOPROC";
    case STT_HIPROC:
        return "HIPROC";
    default:
        return "(?)";
    }
}

static const char* object_symbol_binding_to_string(Elf32_Word type)
{
    switch (type) {
    case STB_LOCAL:
        return "LOCAL";
    case STB_GLOBAL:
        return "GLOBAL";
    case STB_WEAK:
        return "WEAK";
    case STB_NUM:
        return "NUM";
    case STB_LOPROC:
        return "LOPROC";
    case STB_HIPROC:
        return "HIPROC";
    default:
        return "(?)";
    }
}

static const char* object_relocation_type_to_string(Elf32_Word type)
{
    switch (type) {
    case R_386_NONE:
        return "R_386_NONE";
    case R_386_32:
        return "R_386_32";
    case R_386_PC32:
        return "R_386_PC32";
    case R_386_GOT32:
        return "R_386_GOT32";
    case R_386_PLT32:
        return "R_386_PLT32";
    case R_386_COPY:
        return "R_386_COPY";
    case R_386_GLOB_DAT:
        return "R_386_GLOB_DAT";
    case R_386_JMP_SLOT:
        return "R_386_JMP_SLOT";
    case R_386_RELATIVE:
        return "R_386_RELATIVE";
    case R_386_TLS_TPOFF:
        return "R_386_TLS_TPOFF";
    case R_386_TLS_TPOFF32:
        return "R_386_TLS_TPOFF32";
    default:
        return "(?)";
    }
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path;
    static bool display_all = false;
    static bool display_elf_header = false;
    static bool display_program_headers = false;
    static bool display_section_headers = false;
    static bool display_headers = false;
    static bool display_symbol_table = false;
    static bool display_dynamic_symbol_table = false;
    static bool display_core_notes = false;
    static bool display_relocations = false;
    static bool display_unwind_info = false;
    static bool display_dynamic_section = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(display_all, "Display all", "all", 'a');
    args_parser.add_option(display_elf_header, "Display ELF header", "file-header", 'h');
    args_parser.add_option(display_program_headers, "Display program headers", "program-headers", 'l');
    args_parser.add_option(display_section_headers, "Display section headers", "section-headers", 'S');
    args_parser.add_option(display_headers, "Equivalent to: -h -l -S -s -r -d -n -u", "headers", 'e');
    args_parser.add_option(display_symbol_table, "Display the symbol table", "syms", 's');
    args_parser.add_option(display_dynamic_section, "Display the dynamic section", "dynamic", 'd');
    args_parser.add_option(display_core_notes, "Display core notes", "notes", 'n');
    args_parser.add_option(display_relocations, "Display relocations", "relocs", 'r');
    args_parser.add_option(display_unwind_info, "Display unwind info", "unwind", 'u');
    args_parser.add_positional_argument(path, "ELF path", "path");
    args_parser.parse(argc, argv);

    if (argc < 3) {
        args_parser.print_usage(stderr, argv[0]);
        return -1;
    }

    if (display_headers) {
        display_elf_header = true;
        display_program_headers = true;
        display_section_headers = true;
    }

    if (display_all) {
        display_elf_header = true;
        display_program_headers = true;
        display_section_headers = true;
        display_core_notes = true;
        display_relocations = true;
        display_unwind_info = true;
        display_symbol_table = true;
    }

    auto file_or_error = MappedFile::map(path);

    if (file_or_error.is_error()) {
        warnln("Unable to map file {}: {}", path, file_or_error.error());
        return -1;
    }

    auto elf_image_data = file_or_error.value()->bytes();
    ELF::Image executable_elf(elf_image_data);

    if (!executable_elf.is_valid()) {
        fprintf(stderr, "File is not a valid ELF object\n");
        return -1;
    }

    String interpreter_path;

    if (!ELF::validate_program_headers(*(const Elf32_Ehdr*)elf_image_data.data(), elf_image_data.size(), (const u8*)elf_image_data.data(), elf_image_data.size(), &interpreter_path)) {
        fprintf(stderr, "Invalid ELF headers\n");
        return -1;
    }

    if (executable_elf.is_dynamic() && interpreter_path.is_null()) {
        fprintf(stderr, "Warning: Dynamic ELF object has no interpreter path\n");
    }

    ELF::Image interpreter_image(elf_image_data);

    if (!interpreter_image.is_valid()) {
        fprintf(stderr, "ELF image is invalid\n");
        return -1;
    }

    auto& header = *reinterpret_cast<const Elf32_Ehdr*>(elf_image_data.data());

    if (display_elf_header) {
        printf("ELF header:\n");

        String magic = String::format("%s", header.e_ident);
        printf("  Magic:                             ");
        for (size_t i = 0; i < magic.length(); i++) {
            if (isprint(magic[i])) {
                printf("%c ", magic[i]);
            } else {
                printf("%02x ", magic[i]);
            }
        }
        printf("\n");

        printf("  Type:                              %d (%s)\n", header.e_type, object_file_type_to_string(header.e_type));
        printf("  Machine:                           %u (%s)\n", header.e_machine, object_machine_type_to_string(header.e_machine));
        printf("  Version:                           0x%x\n", header.e_version);
        printf("  Entry point address:               0x%x\n", header.e_entry);
        printf("  Start of program headers:          %u (bytes into file)\n", header.e_phoff);
        printf("  Start of section headers:          %u (bytes into file)\n", header.e_shoff);
        printf("  Flags:                             0x%x\n", header.e_flags);
        printf("  Size of this header:               %u (bytes)\n", header.e_ehsize);
        printf("  Size of program headers:           %u (bytes)\n", header.e_phentsize);
        printf("  Number of program headers:         %u\n", header.e_phnum);
        printf("  Size of section headers:           %u (bytes)\n", header.e_shentsize);
        printf("  Number of section headers:         %u\n", header.e_shnum);
        printf("  Section header string table index: %u\n", header.e_shstrndx);
        printf("\n");
    }

    if (display_section_headers) {
        if (!display_all) {
            printf("There are %u section headers, starting at offset 0x%x:\n", header.e_shnum, header.e_shoff);
            printf("\n");
        }

        if (!interpreter_image.section_count()) {
            printf("There are no sections in this file.\n");
        } else {
            printf("Section Headers:\n");
            printf("  Name            Type            Address  Offset   Size     Flags\n");

            interpreter_image.for_each_section([](const ELF::Image::Section& section) {
                printf("  %-15s ", StringView(section.name()).to_string().characters());
                printf("%-15s ", object_section_header_type_to_string(section.type()));
                printf("%08x ", section.address());
                printf("%08x ", section.offset());
                printf("%08x ", section.size());
                printf("%u", section.flags());
                printf("\n");
                return IterationDecision::Continue;
            });
        }
        printf("\n");
    }

    if (display_program_headers) {
        if (!display_all) {
            printf("Elf file type is %d (%s)\n", header.e_type, object_file_type_to_string(header.e_type));
            printf("Entry point 0x%x\n", header.e_entry);
            printf("There are %u program headers, starting at offset %u\n", header.e_phnum, header.e_phoff);
            printf("\n");
        }

        printf("Program Headers:\n");
        printf("  Type           Offset     VirtAddr   PhysAddr   FileSiz    MemSiz     Flg  Align\n");

        interpreter_image.for_each_program_header([](const ELF::Image::ProgramHeader& program_header) {
            printf("  %-14s ", object_program_header_type_to_string(program_header.type()));
            printf("0x%08x ", program_header.offset());
            printf("%p ", program_header.vaddr().as_ptr());
            printf("%p ", program_header.vaddr().as_ptr()); // FIXME: assumes PhysAddr = VirtAddr
            printf("0x%08x ", program_header.size_in_image());
            printf("0x%08x ", program_header.size_in_memory());
            printf("%04x ", program_header.flags());
            printf("0x%08x", program_header.alignment());
            printf("\n");

            if (program_header.type() == PT_INTERP)
                printf("      [Interpreter: %s]\n", program_header.raw_data());

            return IterationDecision::Continue;
        });

        // TODO: Display section to segment mapping
        printf("\n");
    }

    if (display_dynamic_section) {
        auto found_dynamic_section = false;
        interpreter_image.for_each_section([&found_dynamic_section](const ELF::Image::Section& section) {
            if (section.name() != ELF_DYNAMIC)
                return IterationDecision::Continue;

            found_dynamic_section = true;

            if (!section.entry_count()) {
                printf("Dynamic section '%s' at offset 0x%08x contains zero entries.\n", section.name().to_string().characters(), section.offset());
                return IterationDecision::Continue;
            }

            printf("Dynamic section '%s' at offset 0x%08x contains %u entries.\n", section.name().to_string().characters(), section.offset(), section.entry_count());

            // TODO: Display dynamic section

            return IterationDecision::Continue;
        });

        if (!found_dynamic_section)
            printf("No dynamic section in this file.\n");

        printf("\n");
    }

    if (display_relocations) {
        auto found_relocations = false;
        interpreter_image.for_each_section([&found_relocations](const ELF::Image::Section& section) {
            // FIXME: support dynamic relocations (.rel.dyn)
            if (!section.relocations().relocation_count())
                return IterationDecision::Continue;

            found_relocations = true;

            if (!section.relocations().entry_count()) {
                printf("Relocation section '%s' at offset 0x%08x contains zero entries.\n", section.name().to_string().characters(), section.offset());
                return IterationDecision::Continue;
            }

            printf("Relocation section '%s' at offset 0x%08x contains %u entries:\n", section.relocations().name().to_string().characters(), section.relocations().offset(), section.relocations().entry_count());
            printf("  Offset      Type               Sym Value   Sym Name\n");
            section.relocations().for_each_relocation([](const ELF::Image::Relocation& reloc) {
                printf("  0x%08x ", reloc.offset());
                printf(" %-17s ", object_relocation_type_to_string(reloc.type()));
                printf(" 0x%08x ", reloc.symbol().value());
                printf(" %s", reloc.symbol().name().to_string().characters());
                printf("\n");
                return IterationDecision::Continue;
            });

            printf("\n");
            return IterationDecision::Continue;
        });

        if (!found_relocations)
            printf("No relocations in this file.\n");

        printf("\n");
    }

    if (display_unwind_info) {
        // TODO: Unwind info
        printf("Decoding of unwind sections for machine type %s is not supported.\n", object_machine_type_to_string(header.e_machine));
        printf("\n");
    }

    if (display_core_notes) {
        auto found_notes = false;
        interpreter_image.for_each_program_header([&found_notes](const ELF::Image::ProgramHeader& program_header) {
            if (program_header.type() != PT_NOTE)
                return IterationDecision::Continue;

            found_notes = true;

            printf("Displaying notes section '%s' at offset 0x%08x of length 0x%08x:\n", object_program_header_type_to_string(program_header.type()), program_header.offset(), program_header.size_in_image());

            // FIXME: Parse CORE notes. Notes are in JSON format on SerenityOS, but vary between systems.
            printf("%s\n", program_header.raw_data());

            return IterationDecision::Continue;
        });

        if (!found_notes)
            printf("No core notes in this file.\n");

        printf("\n");
    }

    if (display_dynamic_symbol_table || display_symbol_table) {
        auto found_dynamic_symbol_table = false;
        interpreter_image.for_each_section([&found_dynamic_symbol_table](const ELF::Image::Section& section) {
            if (section.name() != ELF_DYNSYM)
                return IterationDecision::Continue;

            found_dynamic_symbol_table = true;

            if (!section.entry_count()) {
                printf("Symbol table '%s' contains zero entries.\n", ELF_DYNSYM);
                return IterationDecision::Continue;
            }

            printf("Symbol table '%s' contains %u entries.\n", ELF_DYNSYM, section.entry_count());

            // TODO: display dynamic symbol table

            return IterationDecision::Continue;
        });

        if (!found_dynamic_symbol_table)
            printf("No dynamic symbol information for this file.\n");

        printf("\n");
    }

    if (display_symbol_table) {
        if (interpreter_image.symbol_count()) {
            printf("Symbol table '%s' contains %u entries:\n", ELF_SYMTAB, interpreter_image.symbol_count());
            printf("   Num: Value    Size     Type     Bind     Name\n");

            interpreter_image.for_each_symbol([](const ELF::Image::Symbol& sym) {
                printf("  %4u: ", sym.index());
                printf("%08x ", sym.value());
                printf("%08x ", sym.size());
                printf("%-8s ", object_symbol_type_to_string(sym.type()));
                printf("%-8s ", object_symbol_binding_to_string(sym.bind()));
                printf("%s", StringView(sym.name()).to_string().characters());
                printf("\n");
                return IterationDecision::Continue;
            });
        }
        printf("\n");
    }

    return 0;
}
