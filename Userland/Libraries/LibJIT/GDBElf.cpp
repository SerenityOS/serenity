/*
 * Copyright (c) 2023, Jesús Lapastora <cyber.gsuscode@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibELF/ELFBuild.h>
#include <LibJIT/GDB.h>

namespace JIT::GDB {

Optional<FixedArray<u8>> build_gdb_image(ReadonlyBytes code, StringView file_symbol_name, StringView code_symbol_name)
{
    Vector<Elf64_Sym> symbols;
    ELF::StringTable section_names;
    ELF::StringTable symbol_names;
    ELF::SectionTable sections;

    auto const null_section = sections.build_null();

    // empty name so that its name in the dump isn't confused with '.text'.
    sections.header_at(null_section).sh_name = section_names.insert(""sv);

    // Build .text as a NOBITS section since the code isn't loaded inside the
    // image. The image just holds the addresses for the executable region.
    auto const text = sections.build_nobits([&](Elf64_Shdr& text) {
        text.sh_name = section_names.insert(".text"sv);
        text.sh_flags = SHF_EXECINSTR | SHF_ALLOC;
        text.sh_addr = bit_cast<uintptr_t>(code.offset(0));
        text.sh_size = code.size();
        text.sh_link = 0;
        text.sh_info = 0;
        text.sh_addralign = 1;
        text.sh_entsize = 0;
    });

    // Without this, GDB won't show the symbol names for our code.
    Elf64_Sym file;
    {
        file.st_name = symbol_names.insert(file_symbol_name);
        file.st_info = ELF64_ST_INFO(STB_GLOBAL, STT_FILE);
        file.st_other = STV_DEFAULT;
        file.st_shndx = SHN_ABS;
        file.st_value = 0;
        file.st_size = code.size();
    }
    symbols.append(file);

    // The index of the first symbol that does not have a `STB_LOCAL` binding.
    // Note that all non-local bindings must come before all local bindings.
    auto const first_non_local_symbol_index = symbols.size();
    Elf64_Sym code_sym;
    {
        code_sym.st_name = symbol_names.insert(code_symbol_name);
        code_sym.st_info = ELF64_ST_INFO(STB_GLOBAL, STT_FUNC);
        code_sym.st_other = STV_DEFAULT;
        code_sym.st_shndx = text.index,
        code_sym.st_value = 0; // 0 bytes relative to .text
        code_sym.st_size = code.size();
    }
    symbols.append(code_sym);

    auto const strtab = symbol_names.emit_into_builder(
        section_names.insert(".strtab"sv), sections);

    sections.build<Elf64_Sym>(symbols.span(),
        [&section_names, first_non_local_symbol_index, strtab](Elf64_Shdr& symtab) {
            symtab.sh_name = section_names.insert(".symtab"sv);
            symtab.sh_type = SHT_SYMTAB;
            symtab.sh_flags = 0;
            symtab.sh_addr = 0;
            symtab.sh_info = first_non_local_symbol_index;
            symtab.sh_link = strtab.raw_index();
            symtab.sh_addralign = 0;
        });

    // Make sure we find where the name for .shstrtab resides before we insert
    // it into the image.
    auto const shstrtab_name_index = section_names.insert(".shstrtab"sv);
    auto const shstrtab = section_names.emit_into_builder(
        shstrtab_name_index, sections);

    // Set the type to an "object" file, as GDB seems to request it:
    // https://sourceware.org/gdb/current/onlinedocs/gdb.html/Registering-Code.html#Registering-Code
    return ELF::build_elf_image(shstrtab.raw_index(), ET_REL, sections.span());
}
}
