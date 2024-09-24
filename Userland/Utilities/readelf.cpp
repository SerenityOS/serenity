/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibELF/DynamicLoader.h>
#include <LibELF/DynamicObject.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

static char const* object_program_header_type_to_string(Elf_Word type)
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

static char const* object_section_header_type_to_string(Elf_Word type)
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
    case SHT_RELR:
        return "RELR";
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

static char const* object_symbol_type_to_string(Elf_Word type)
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
    case STT_GNU_IFUNC:
        return "IFUNC";
    case STT_LOPROC:
        return "LOPROC";
    case STT_HIPROC:
        return "HIPROC";
    default:
        return "(?)";
    }
}

static char const* object_symbol_binding_to_string(Elf_Word type)
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

static char const* object_relocation_type_to_string(Elf_Half machine, Elf_Word type)
{
#define ENUMERATE_RELOCATION(name) \
    case name:                     \
        return #name;
    if (machine == EM_X86_64) {
        switch (type) {
            __ENUMERATE_X86_64_DYNAMIC_RELOCS(ENUMERATE_RELOCATION)
        }
    } else if (machine == EM_AARCH64) {
        switch (type) {
            __ENUMERATE_AARCH64_DYNAMIC_RELOCS(ENUMERATE_RELOCATION)
        }
    } else if (machine == EM_RISCV) {
        switch (type) {
            __ENUMERATE_RISCV_DYNAMIC_RELOCS(ENUMERATE_RELOCATION)
        }
    }
#undef ENUMERATE_RELOCATION
    return "(?)";
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath map_fixed"));

    ByteString path {};
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
    StringView string_dump_section {};

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
    args_parser.add_option(string_dump_section, "Display the contents of a section as strings", "string-dump", 'p', "section-name");
    args_parser.add_positional_argument(path, "ELF path", "path");
    args_parser.parse(arguments);

    if (arguments.argc < 3) {
        args_parser.print_usage(stderr, arguments.strings[0]);
        return Error::from_errno(EINVAL);
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
        display_dynamic_symbol_table = true;
        display_dynamic_section = true;
        display_core_notes = true;
        display_relocations = true;
        display_unwind_info = true;
        display_symbol_table = true;
        display_hardening = true;
    }

    path = LexicalPath::absolute_path(TRY(Core::System::getcwd()), path);

    auto file_or_error = Core::MappedFile::map(path);

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

    Optional<Elf_Phdr> interpreter_path_program_header {};
    if (!ELF::validate_program_headers(*bit_cast<Elf_Ehdr const*>(elf_image_data.data()), elf_image_data.size(), elf_image_data, interpreter_path_program_header)) {
        warnln("Invalid ELF headers");
        return -1;
    }

    StringBuilder interpreter_path_builder;
    if (interpreter_path_program_header.has_value())
        TRY(interpreter_path_builder.try_append({ elf_image_data.offset(interpreter_path_program_header.value().p_offset), static_cast<size_t>(interpreter_path_program_header.value().p_filesz) - 1 }));
    auto interpreter_path = interpreter_path_builder.string_view();

    auto& header = *reinterpret_cast<Elf_Ehdr const*>(elf_image_data.data());

    RefPtr<ELF::DynamicObject> object = nullptr;

    if (elf_image.is_dynamic()) {
        if (interpreter_path.is_empty()) {
            interpreter_path = "/usr/lib/Loader.so"sv;
            warnln("Warning: Dynamic ELF object has no interpreter path. Using: {}", interpreter_path);
        }

        auto interpreter_file_or_error = Core::MappedFile::map(interpreter_path);

        if (interpreter_file_or_error.is_error()) {
            warnln("Unable to map interpreter file {}: {}", interpreter_path, interpreter_file_or_error.error());
        } else {
            auto interpreter_image_data = interpreter_file_or_error.value()->bytes();

            ELF::Image interpreter_image(interpreter_image_data);

            if (!interpreter_image.is_valid()) {
                warnln("ELF interpreter image is invalid");
            }
        }

        int fd = TRY(Core::System::open(path, O_RDONLY));
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
            if (is_ascii_printable(i)) {
                out("{:c} ", i);
            } else {
                out("{:02x} ", i);
            }
        }
        outln();

        outln("  Type:                              {} ({})", header.e_type, ELF::Image::object_file_type_to_string(header.e_type).value_or("(?)"sv));
        outln("  Machine:                           {} ({})", header.e_machine, ELF::Image::object_machine_type_to_string(header.e_machine).value_or("(?)"sv));
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

    auto addr_padding = "        ";

    if (display_section_headers) {
        if (!display_all) {
            outln("There are {} section headers, starting at offset {:#x}:", header.e_shnum, header.e_shoff);
            outln();
        }

        if (!elf_image.section_count()) {
            outln("There are no sections in this file.");
        } else {
            outln("Section Headers:");
            outln("  Name                Type            Address{}    Offset{}     Size{}       Flags", addr_padding, addr_padding, addr_padding);

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
            outln("ELF file type is {} ({})", header.e_type, ELF::Image::object_file_type_to_string(header.e_type).value_or("(?)"sv));
            outln("Entry point {:#x}\n", header.e_entry);
            outln("There are {} program headers, starting at offset {}", header.e_phnum, header.e_phoff);
            outln();
        }

        if (!elf_image.program_header_count()) {
            outln("There are no program headers in this file.");
        } else {
            outln("Program Headers:");
            outln("  Type           Offset{}     VirtAddr{}   PhysAddr{}   FileSiz{}    MemSiz{}     Flg  Align",
                addr_padding, addr_padding, addr_padding, addr_padding, addr_padding);

            elf_image.for_each_program_header([](const ELF::Image::ProgramHeader& program_header) {
                out("  ");
                out("{:14} ", object_program_header_type_to_string(program_header.type()));
                out("{:p} ", program_header.offset());
                out("{:p} ", program_header.vaddr().as_ptr());
                out("{:p} ", program_header.vaddr().as_ptr()); // FIXME: assumes PhysAddr = VirtAddr
                out("{:p} ", program_header.size_in_image());
                out("{:p} ", program_header.size_in_memory());
                out("{:04x} ", program_header.flags());
                out("{:p}", program_header.alignment());
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
                    outln("Dynamic section '{}' at offset {:#08x} contains {} entries.", section.name().to_byte_string(), section.offset(), section.entry_count());
                } else {
                    outln("Dynamic section '{}' at offset {:#08x} contains zero entries.", section.name().to_byte_string(), section.offset());
                }

                return IterationDecision::Break;
            });

            Vector<ByteString> libraries;
            object->for_each_needed_library([&libraries](StringView entry) {
                libraries.append(ByteString::formatted("{}", entry));
            });

            auto library_index = 0;
            outln("  Tag        Type              Name / Value");
            object->for_each_dynamic_entry([&library_index, &libraries, &object](const ELF::DynamicObject::DynamicEntry& entry) {
                out("  {:#08x} ", entry.tag());
                out("{:17} ", ELF::DynamicObject::name_for_dtag(entry.tag()));

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
                outln("  Offset{}      Type                Sym Value{}   Sym Name", addr_padding, addr_padding);
                object->relocation_section().for_each_relocation([&](const ELF::DynamicObject::Relocation& reloc) {
                    out("  {:p} ", reloc.offset());
                    out(" {:18} ", object_relocation_type_to_string(header.e_machine, reloc.type()));
                    out(" {:p} ", reloc.symbol().value());
                    out(" {}", reloc.symbol().name());
                    outln();
                });
            }
            outln();

            if (!object->plt_relocation_section().entry_count()) {
                outln("Relocation section '{}' at offset {:#08x} contains zero entries:", object->plt_relocation_section().name(), object->plt_relocation_section().offset());
            } else {
                outln("Relocation section '{}' at offset {:#08x} contains {} entries:", object->plt_relocation_section().name(), object->plt_relocation_section().offset(), object->plt_relocation_section().entry_count());
                outln("  Offset{}      Type                Sym Value{}   Sym Name", addr_padding, addr_padding);
                object->plt_relocation_section().for_each_relocation([&](const ELF::DynamicObject::Relocation& reloc) {
                    out("  {:p} ", reloc.offset());
                    out(" {:18} ", object_relocation_type_to_string(header.e_machine, reloc.type()));
                    out(" {:p} ", reloc.symbol().value());
                    out(" {}", reloc.symbol().name());
                    outln();
                });
            }

            outln();

            size_t relr_count = 0;
            object->for_each_relr_relocation([&relr_count](auto) { ++relr_count; });
            if (relr_count != 0) {
                outln("Relocation section '.relr.dyn' at offset {:#08x} contains {} entries:", object->relr_relocation_section().offset(), object->relr_relocation_section().entry_count());
                outln("{:>8x} offsets", relr_count);
                object->for_each_relr_relocation([](auto offset) { outln("{:p}", offset); });
            }
        } else {
            outln("No relocations in this file.");
        }

        outln();
    }

    if (display_unwind_info) {
        // TODO: Unwind info
        outln("Decoding of unwind sections for machine type {} is not supported.", ELF::Image::object_machine_type_to_string(header.e_machine).value_or("?"sv));
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
                outln("   Num: Value{}      Size{}       Type     Bind     Name", addr_padding, addr_padding);
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
            outln("   Num: Value{}      Size{}       Type     Bind     Name", addr_padding, addr_padding);

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

    if (!string_dump_section.is_null()) {
        auto maybe_section = elf_image.lookup_section(string_dump_section);
        if (maybe_section.has_value()) {
            outln("String dump of section \'{}\':", string_dump_section);
            StringView data(maybe_section->raw_data(), maybe_section->size());
            data.for_each_split_view('\0', SplitBehavior::Nothing, [&data](auto string) {
                auto offset = string.characters_without_null_termination() - data.characters_without_null_termination();
                outln("[{:6x}] {}", offset, string);
            });
        } else {
            warnln("Could not find section \'{}\'", string_dump_section);
            return 1;
        }
    }
    return 0;
}
