/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MappedFile.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

static const char* object_file_type_to_string(ElfW(Half) type)
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

static const char* object_machine_type_to_string(ElfW(Half) type)
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
    case EM_X86_64:
        return "Advanced Micro Devices X86-64";
    default:
        return "(?)";
    }
}

static const char* object_program_header_type_to_string(ElfW(Word) type)
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

static const char* object_section_header_type_to_string(ElfW(Word) type)
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

static const char* object_symbol_type_to_string(ElfW(Word) type)
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

static const char* object_symbol_binding_to_string(ElfW(Word) type)
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

static const char* object_relocation_type_to_string(ElfW(Word) type)
{
    switch (type) {
#if ARCH(I386)
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
#else
    case R_X86_64_NONE:
        return "R_X86_64_NONE";
    case R_X86_64_64:
        return "R_X86_64";
    case R_X86_64_GLOB_DAT:
        return "R_x86_64_GLOB_DAT";
    case R_X86_64_JUMP_SLOT:
        return "R_X86_64_JUMP_SLOT";
    case R_X86_64_RELATIVE:
        return "R_X86_64_RELATIVE";
    case R_X86_64_TPOFF64:
        return "R_X86_64_TPOFF64";
#endif
    default:
        return "(?)";
    }
}

static const char* object_tag_to_string(ElfW(Sword) dt_tag)
{
    switch (dt_tag) {
    case DT_NULL:
        return "NULL"; /* marks end of _DYNAMIC array */
    case DT_NEEDED:
        return "NEEDED"; /* string table offset of needed lib */
    case DT_PLTRELSZ:
        return "PLTRELSZ"; /* size of relocation entries in PLT */
    case DT_PLTGOT:
        return "PLTGOT"; /* address PLT/GOT */
    case DT_HASH:
        return "HASH"; /* address of symbol hash table */
    case DT_STRTAB:
        return "STRTAB"; /* address of string table */
    case DT_SYMTAB:
        return "SYMTAB"; /* address of symbol table */
    case DT_RELA:
        return "RELA"; /* address of relocation table */
    case DT_RELASZ:
        return "RELASZ"; /* size of relocation table */
    case DT_RELAENT:
        return "RELAENT"; /* size of relocation entry */
    case DT_STRSZ:
        return "STRSZ"; /* size of string table */
    case DT_SYMENT:
        return "SYMENT"; /* size of symbol table entry */
    case DT_INIT:
        return "INIT"; /* address of initialization func. */
    case DT_FINI:
        return "FINI"; /* address of termination function */
    case DT_SONAME:
        return "SONAME"; /* string table offset of shared obj */
    case DT_RPATH:
        return "RPATH"; /* string table offset of library search path */
    case DT_SYMBOLIC:
        return "SYMBOLIC"; /* start sym search in shared obj. */
    case DT_REL:
        return "REL"; /* address of rel. tbl. w addends */
    case DT_RELSZ:
        return "RELSZ"; /* size of DT_REL relocation table */
    case DT_RELENT:
        return "RELENT"; /* size of DT_REL relocation entry */
    case DT_PLTREL:
        return "PLTREL"; /* PLT referenced relocation entry */
    case DT_DEBUG:
        return "DEBUG"; /* bugger */
    case DT_TEXTREL:
        return "TEXTREL"; /* Allow rel. mod. to unwritable seg */
    case DT_JMPREL:
        return "JMPREL"; /* add. of PLT's relocation entries */
    case DT_BIND_NOW:
        return "BIND_NOW"; /* Bind now regardless of env setting */
    case DT_INIT_ARRAY:
        return "INIT_ARRAY"; /* address of array of init func */
    case DT_FINI_ARRAY:
        return "FINI_ARRAY"; /* address of array of term func */
    case DT_INIT_ARRAYSZ:
        return "INIT_ARRAYSZ"; /* size of array of init func */
    case DT_FINI_ARRAYSZ:
        return "FINI_ARRAYSZ"; /* size of array of term func */
    case DT_RUNPATH:
        return "RUNPATH"; /* strtab offset of lib search path */
    case DT_FLAGS:
        return "FLAGS"; /* Set of DF_* flags */
    case DT_ENCODING:
        return "ENCODING"; /* further DT_* follow encoding rules */
    case DT_PREINIT_ARRAY:
        return "PREINIT_ARRAY"; /* address of array of preinit func */
    case DT_PREINIT_ARRAYSZ:
        return "PREINIT_ARRAYSZ"; /* size of array of preinit func */
    case DT_LOOS:
        return "LOOS"; /* reserved range for OS */
    case DT_HIOS:
        return "HIOS"; /*  specific dynamic array tags */
    case DT_LOPROC:
        return "LOPROC"; /* reserved range for processor */
    case DT_HIPROC:
        return "HIPROC"; /*  specific dynamic array tags */
    case DT_GNU_HASH:
        return "GNU_HASH"; /* address of GNU hash table */
    case DT_RELACOUNT:
        return "RELACOUNT"; /* if present, number of RELATIVE */
    case DT_RELCOUNT:
        return "RELCOUNT"; /* relocs, which must come first */
    case DT_FLAGS_1:
        return "FLAGS_1";
    default:
        return "??";
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
    static bool display_hardening = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(display_all, "Display all", "all", 'a');
    args_parser.add_option(display_elf_header, "Display ELF header", "file-header", 'h');
    args_parser.add_option(display_program_headers, "Display program headers", "program-headers", 'l');
    args_parser.add_option(display_section_headers, "Display section headers", "section-headers", 'S');
    args_parser.add_option(display_headers, "Equivalent to: -h -l -S -s -r -d -n -u -c", "headers", 'e');
    args_parser.add_option(display_symbol_table, "Display the symbol table", "syms", 's');
    args_parser.add_option(display_dynamic_symbol_table, "Display the dynamic symbol table", "dyn-syms", '\0');
    args_parser.add_option(display_dynamic_section, "Display the dynamic section", "dynamic", 'd');
    args_parser.add_option(display_core_notes, "Display core notes", "notes", 'n');
    args_parser.add_option(display_relocations, "Display relocations", "relocs", 'r');
    args_parser.add_option(display_unwind_info, "Display unwind info", "unwind", 'u');
    args_parser.add_option(display_hardening, "Display security hardening info", "checksec", 'c');
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
        display_hardening = true;
    }

    auto file_or_error = MappedFile::map(path);

    if (file_or_error.is_error()) {
        warnln("Unable to map file {}: {}", path, file_or_error.error());
        return -1;
    }

    auto elf_image_data = file_or_error.value()->bytes();
    ELF::Image elf_image(elf_image_data);

    if (!elf_image.is_valid()) {
        warnln("File is not a valid ELF object");
        return -1;
    }

    String interpreter_path;

    if (!ELF::validate_program_headers(*(const ElfW(Ehdr)*)elf_image_data.data(), elf_image_data.size(), (const u8*)elf_image_data.data(), elf_image_data.size(), &interpreter_path)) {
        warnln("Invalid ELF headers");
        return -1;
    }

    auto& header = *reinterpret_cast<const ElfW(Ehdr)*>(elf_image_data.data());

    RefPtr<ELF::DynamicObject> object = nullptr;

    if (elf_image.is_dynamic()) {
        if (interpreter_path.is_null()) {
            interpreter_path = "/usr/lib/Loader.so";
            warnln("Warning: Dynamic ELF object has no interpreter path. Using: {}", interpreter_path);
        }

        auto interpreter_file_or_error = MappedFile::map(interpreter_path);

        if (interpreter_file_or_error.is_error()) {
            warnln("Unable to map interpreter file {}: {}", interpreter_path, interpreter_file_or_error.error());
            return -1;
        }

        auto interpreter_image_data = interpreter_file_or_error.value()->bytes();

        ELF::Image interpreter_image(interpreter_image_data);

        if (!interpreter_image.is_valid()) {
            warnln("ELF interpreter image is invalid");
            return -1;
        }

        int fd = open(path, O_RDONLY);
        if (fd < 0) {
            outln("Unable to open file {}", path);
            return 1;
        }

        auto result = ELF::DynamicLoader::try_create(fd, path);
        if (result.is_error()) {
            outln("{}", result.error().text);
            return 1;
        }
        auto& loader = result.value();
        if (!loader->is_valid()) {
            outln("{} is not a valid ELF dynamic shared object!", path);
            return 1;
        }

        object = loader->map();
        if (!object) {
            outln("Failed to map dynamic ELF object {}", path);
            return 1;
        }
    }

    if (display_elf_header) {
        outln("ELF header:");

        out("  Magic:                             ");
        for (char i : StringView { header.e_ident, sizeof(header.e_ident) }) {
            if (isprint(i)) {
                out("{:c} ", i);
            } else {
                out("{:02x} ", i);
            }
        }
        outln();

        outln("  Type:                              {} ({})", header.e_type, object_file_type_to_string(header.e_type));
        outln("  Machine:                           {} ({})", header.e_machine, object_machine_type_to_string(header.e_machine));
        outln("  Version:                           {:#x}", header.e_version);
        outln("  Entry point address:               {:#x}", header.e_entry);
        outln("  Start of program headers:          {} (bytes into file)", header.e_phoff);
        outln("  Start of section headers:          {} (bytes into file)", header.e_shoff);
        outln("  Flags:                             {:#x}", header.e_flags);
        outln("  Size of this header:               {} (bytes)", header.e_ehsize);
        outln("  Size of program headers:           {} (bytes)", header.e_phentsize);
        outln("  Number of program headers:         {}", header.e_phnum);
        outln("  Size of section headers:           {} (bytes)", header.e_shentsize);
        outln("  Number of section headers:         {}", header.e_shnum);
        outln("  Section header string table index: {}", header.e_shstrndx);
        outln();
    }

    if (display_section_headers) {
        if (!display_all) {
            outln("There are {} section headers, starting at offset {:#x}:", header.e_shnum, header.e_shoff);
            outln();
        }

        if (!elf_image.section_count()) {
            outln("There are no sections in this file.");
        } else {
            outln("Section Headers:");
            outln("  Name                Type            Address    Offset     Size       Flags");

            elf_image.for_each_section([](const ELF::Image::Section& section) {
                out("  {:19} ", section.name());
                out("{:15} ", object_section_header_type_to_string(section.type()));
                out("{:p} ", section.address());
                out("{:p} ", section.offset());
                out("{:p} ", section.size());
                out("{}", section.flags());
                outln();
            });
        }
        outln();
    }

    if (display_program_headers) {
        if (!display_all) {
            outln("ELF file type is {} ({})", header.e_type, object_file_type_to_string(header.e_type));
            outln("Entry point {:#x}\n", header.e_entry);
            outln("There are {} program headers, starting at offset {}", header.e_phnum, header.e_phoff);
            outln();
        }

        if (!elf_image.program_header_count()) {
            outln("There are no program headers in this file.");
        } else {
            outln("Program Headers:");
            outln("  Type           Offset     VirtAddr   PhysAddr   FileSiz    MemSiz     Flg  Align");

            elf_image.for_each_program_header([](const ELF::Image::ProgramHeader& program_header) {
                out("  ");
                out("{:14} ", object_program_header_type_to_string(program_header.type()));
                out("{:#08x} ", program_header.offset());
                out("{:p} ", program_header.vaddr().as_ptr());
                out("{:p} ", program_header.vaddr().as_ptr()); // FIXME: assumes PhysAddr = VirtAddr
                out("{:#08x} ", program_header.size_in_image());
                out("{:#08x} ", program_header.size_in_memory());
                out("{:04x} ", program_header.flags());
                out("{:#08x}", program_header.alignment());
                outln();

                if (program_header.type() == PT_INTERP)
                    outln("      [Interpreter: {}]", program_header.raw_data());
            });
        }

        // TODO: Display section to segment mapping
        outln();
    }

    if (display_dynamic_section) {
        auto found_dynamic_section = false;
        if (elf_image.is_dynamic()) {
            elf_image.for_each_section([&found_dynamic_section](const ELF::Image::Section& section) {
                if (section.name() != ELF_DYNAMIC)
                    return IterationDecision::Continue;

                found_dynamic_section = true;

                if (section.entry_count()) {
                    outln("Dynamic section '{}' at offset {:#08x} contains {} entries.", section.name().to_string(), section.offset(), section.entry_count());
                } else {
                    outln("Dynamic section '{}' at offset {:#08x} contains zero entries.", section.name().to_string(), section.offset());
                }

                return IterationDecision::Break;
            });

            Vector<String> libraries;
            object->for_each_needed_library([&libraries](StringView entry) {
                libraries.append(String::formatted("{}", entry));
            });

            auto library_index = 0;
            outln("  Tag        Type              Name / Value");
            object->for_each_dynamic_entry([&library_index, &libraries, &object](const ELF::DynamicObject::DynamicEntry& entry) {
                out("  {:#08x} ", entry.tag());
                out("{:17} ", object_tag_to_string(entry.tag()));

                if (entry.tag() == DT_NEEDED) {
                    outln("Shared library: {}", libraries[library_index]);
                    library_index++;
                } else if (entry.tag() == DT_RPATH) {
                    outln("Library rpath: {}", object->rpath());
                } else if (entry.tag() == DT_RUNPATH) {
                    outln("Library runpath: {}", object->runpath());
                } else if (entry.tag() == DT_SONAME) {
                    outln("Library soname: {}", object->soname());
                } else {
                    outln("{:#08x}", entry.val());
                }
            });
        }

        if (!found_dynamic_section)
            outln("No dynamic section in this file.");

        outln();
    }

    if (display_relocations) {
        if (elf_image.is_dynamic()) {
            if (!object->relocation_section().entry_count()) {
                outln("Relocation section '{}' at offset {:#08x} contains zero entries:", object->relocation_section().name(), object->relocation_section().offset());
            } else {
                outln("Relocation section '{}' at offset {:#08x} contains {} entries:", object->relocation_section().name(), object->relocation_section().offset(), object->relocation_section().entry_count());
                outln("  Offset      Type               Sym Value   Sym Name");
                object->relocation_section().for_each_relocation([](const ELF::DynamicObject::Relocation& reloc) {
                    out("  {:#08x} ", reloc.offset());
                    out(" {:17} ", object_relocation_type_to_string(reloc.type()));
                    out(" {:#08x} ", reloc.symbol().value());
                    out(" {}", reloc.symbol().name());
                    outln();
                });
            }
            outln();

            if (!object->plt_relocation_section().entry_count()) {
                outln("Relocation section '{}' at offset {:#08x} contains zero entries:", object->plt_relocation_section().name(), object->plt_relocation_section().offset());
            } else {
                outln("Relocation section '{}' at offset {:#08x} contains {} entries:", object->plt_relocation_section().name(), object->plt_relocation_section().offset(), object->plt_relocation_section().entry_count());
                outln("  Offset      Type               Sym Value   Sym Name");
                object->plt_relocation_section().for_each_relocation([](const ELF::DynamicObject::Relocation& reloc) {
                    out("  {:#08x} ", reloc.offset());
                    out(" {:17} ", object_relocation_type_to_string(reloc.type()));
                    out(" {:#08x} ", reloc.symbol().value());
                    out(" {}", reloc.symbol().name());
                    outln();
                });
            }
        } else {
            outln("No relocations in this file.");
        }

        outln();
    }

    if (display_unwind_info) {
        // TODO: Unwind info
        outln("Decoding of unwind sections for machine type {} is not supported.", object_machine_type_to_string(header.e_machine));
        outln();
    }

    if (display_core_notes) {
        auto found_notes = false;
        elf_image.for_each_program_header([&found_notes](const ELF::Image::ProgramHeader& program_header) {
            if (program_header.type() != PT_NOTE)
                return;

            found_notes = true;

            outln("Displaying notes section '{}' at offset {:#08x} of length {:#08x}:", object_program_header_type_to_string(program_header.type()), program_header.offset(), program_header.size_in_image());

            // FIXME: Parse CORE notes. Notes are in JSON format on SerenityOS, but vary between systems.
            outln("{}", program_header.raw_data());
        });

        if (!found_notes)
            outln("No core notes in this file.");

        outln();
    }

    if (display_dynamic_symbol_table || display_symbol_table) {
        auto found_dynamic_symbol_table = false;

        if (elf_image.is_dynamic()) {
            elf_image.for_each_section([&found_dynamic_symbol_table](const ELF::Image::Section& section) {
                if (section.name() != ELF_DYNSYM)
                    return IterationDecision::Continue;

                found_dynamic_symbol_table = true;

                if (!section.entry_count()) {
                    outln("Symbol table '{}' contains zero entries.", ELF_DYNSYM);
                } else {
                    outln("Symbol table '{}' contains {} entries.", ELF_DYNSYM, section.entry_count());
                }

                return IterationDecision::Break;
            });

            if (object->symbol_count()) {
                // FIXME: Add support for init/fini/start/main sections
                outln("   Num: Value      Size       Type     Bind     Name");
                object->for_each_symbol([](const ELF::DynamicObject::Symbol& sym) {
                    out("  {:>4}: ", sym.index());
                    out("{:p} ", sym.value());
                    out("{:p} ", sym.size());
                    out("{:8} ", object_symbol_type_to_string(sym.type()));
                    out("{:8} ", object_symbol_binding_to_string(sym.bind()));
                    out("{}", sym.name());
                    outln();
                });
            }
        }

        if (!found_dynamic_symbol_table)
            outln("No dynamic symbol information for this file.");

        outln();
    }

    if (display_symbol_table) {
        if (elf_image.symbol_count()) {
            outln("Symbol table '{}' contains {} entries:", ELF_SYMTAB, elf_image.symbol_count());
            outln("   Num: Value      Size       Type     Bind     Name");

            elf_image.for_each_symbol([](const ELF::Image::Symbol& sym) {
                out("  {:>4}: ", sym.index());
                out("{:p} ", sym.value());
                out("{:p} ", sym.size());
                out("{:8} ", object_symbol_type_to_string(sym.type()));
                out("{:8} ", object_symbol_binding_to_string(sym.bind()));
                out("{}", sym.name());
                outln();
            });
        } else {
            outln("Symbol table '{}' contains zero entries.", ELF_SYMTAB);
        }
        outln();
    }

    if (display_hardening) {
        outln("Security Hardening:");
        outln("RELRO         Stack Canary NX           PIE          RPATH        RUNPATH      Symbols      ");

        bool relro = false;
        elf_image.for_each_program_header([&relro](const ELF::Image::ProgramHeader& program_header) {
            if (program_header.type() == PT_GNU_RELRO) {
                relro = true;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        bool full_relro = false;
        if (relro) {
            object->for_each_dynamic_entry([&full_relro](const ELF::DynamicObject::DynamicEntry& entry) {
                if (entry.tag() == DT_BIND_NOW) {
                    full_relro = true;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
            if (full_relro)
                out("\033[0;32m{:13}\033[0m ", "Full RELRO");
            else
                out("\033[0;33m{:13}\033[0m ", "Partial RELRO");
        } else {
            out("\033[0;31m{:13}\033[0m ", "No RELRO");
        }

        bool canary = false;
        elf_image.for_each_symbol([&canary](const ELF::Image::Symbol& sym) {
            if (sym.name() == "__stack_chk_fail" || sym.name() == "__intel_security_cookie") {
                canary = true;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        if (canary)
            out("\033[0;32m{:12}\033[0m ", "Canary found");
        else
            out("\033[0;31m{:12}\033[0m ", "No canary");

        bool nx = false;
        elf_image.for_each_program_header([&nx](const ELF::Image::ProgramHeader& program_header) {
            if (program_header.type() == PT_GNU_STACK) {
                if (program_header.flags() & PF_X)
                    nx = false;
                else
                    nx = true;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        if (nx)
            out("\033[0;32m{:12}\033[0m ", "NX enabled");
        else
            out("\033[0;31m{:12}\033[0m ", "NX disabled");

        bool pie = false;
        if (header.e_type == ET_REL || header.e_type == ET_DYN)
            pie = true;

        if (pie)
            out("\033[0;32m{:12}\033[0m ", "PIE enabled");
        else
            out("\033[0;31m{:12}\033[0m ", "No PIE");

        StringView rpath;
        if (elf_image.is_dynamic())
            rpath = object->rpath();

        if (rpath.is_empty())
            out("\033[0;32m{:12}\033[0m ", "No RPATH");
        else
            out("\033[0;31m{:12}\033[0m ", rpath);

        StringView runpath;
        if (elf_image.is_dynamic())
            runpath = object->runpath();

        if (runpath.is_empty())
            out("\033[0;32m{:12}\033[0m ", "No RUNPATH");
        else
            out("\033[0;31m{:12}\033[0m ", runpath);

        out("{} symbols", elf_image.symbol_count());
        outln();
    }

    return 0;
}
