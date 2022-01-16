/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/OwnPtr.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibELF/Image.h>
#include <LibMain/Main.h>
#include <LibX86/Disassembler.h>
#include <LibX86/ELFSymbolProvider.h>
#include <string.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    char const* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Disassemble an executable, and show human-readable "
        "assembly code for each function.");
    args_parser.add_positional_argument(path, "Path to i386 binary file", "path");
    args_parser.parse(args);

    RefPtr<Core::MappedFile> file;
    u8 const* asm_data = nullptr;
    size_t asm_size = 0;
    if ((TRY(Core::System::stat(path))).st_size > 0) {
        file = TRY(Core::MappedFile::map(path));
        asm_data = static_cast<u8 const*>(file->data());
        asm_size = file->size();
    }

    struct Symbol {
        size_t value;
        size_t size;
        StringView name;

        size_t address() const { return value; }
        size_t address_end() const { return value + size; }

        bool contains(size_t virtual_address) { return address() <= virtual_address && virtual_address < address_end(); }
    };
    Vector<Symbol> symbols;

    size_t file_offset = 0;
    Vector<Symbol>::Iterator current_symbol = symbols.begin();
    OwnPtr<X86::ELFSymbolProvider> symbol_provider; // nullptr for non-ELF disassembly.
    OwnPtr<ELF::Image> elf;
    if (asm_size >= 4 && strncmp(reinterpret_cast<char const*>(asm_data), "\u007fELF", 4) == 0) {
        elf = make<ELF::Image>(asm_data, asm_size);
        if (elf->is_valid()) {
            symbol_provider = make<X86::ELFSymbolProvider>(*elf);
            elf->for_each_section_of_type(SHT_PROGBITS, [&](ELF::Image::Section const& section) {
                // FIXME: Disassemble all SHT_PROGBITS sections, not just .text.
                if (section.name() != ".text")
                    return IterationDecision::Continue;
                asm_data = reinterpret_cast<u8 const*>(section.raw_data());
                asm_size = section.size();
                file_offset = section.address();
                return IterationDecision::Break;
            });
            symbols.ensure_capacity(elf->symbol_count() + 1);
            symbols.append({ 0, 0, StringView() }); // Sentinel.
            elf->for_each_symbol([&](ELF::Image::Symbol const& symbol) {
                symbols.append({ symbol.value(), symbol.size(), symbol.name() });
                return IterationDecision::Continue;
            });
            quick_sort(symbols, [](auto& a, auto& b) {
                if (a.value != b.value)
                    return a.value < b.value;
                if (a.size != b.size)
                    return a.size < b.size;
                return a.name < b.name;
            });
            if constexpr (DISASM_DUMP_DEBUG) {
                for (size_t i = 0; i < symbols.size(); ++i)
                    dbgln("{}: {:p}, {}", symbols[i].name, symbols[i].value, symbols[i].size);
            }
        }
    }

    X86::SimpleInstructionStream stream(asm_data, asm_size);
    X86::Disassembler disassembler(stream);

    bool is_first_symbol = true;
    bool current_instruction_is_in_symbol = false;

    for (;;) {
        auto offset = stream.offset();
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;

        // Prefix regions of instructions belonging to a symbol with the symbol's name.
        // Separate regions of instructions belonging to distinct symbols with newlines,
        // and separate regions of instructions not belonging to symbols from regions belonging to symbols with newlines.
        // Interesting cases:
        // - More than 1 symbol covering a region of instructions (ICF, D1/D2)
        // - Symbols of size 0 that don't cover any instructions but are at an address (want to print them, separated from instructions both before and after)
        // Invariant: current_symbol is the largest instruction containing insn, or it is the largest instruction that has an address less than the instruction's address.
        size_t virtual_offset = file_offset + offset;
        if (current_symbol < symbols.end() && !current_symbol->contains(virtual_offset)) {
            if (!is_first_symbol && current_instruction_is_in_symbol) {
                // The previous instruction was part of a symbol that doesn't cover the current instruction, so separate it from the current instruction with a newline.
                outln();
                current_instruction_is_in_symbol = (current_symbol + 1 < symbols.end() && (current_symbol + 1)->contains(virtual_offset));
            }

            // Try to find symbol covering current instruction, if one exists.
            while (current_symbol + 1 < symbols.end() && !(current_symbol + 1)->contains(virtual_offset) && (current_symbol + 1)->address() <= virtual_offset) {
                ++current_symbol;
                if (!is_first_symbol)
                    outln("\n({} ({:p}-{:p}))\n", current_symbol->name, current_symbol->address(), current_symbol->address_end());
            }
            while (current_symbol + 1 < symbols.end() && (current_symbol + 1)->contains(virtual_offset)) {
                if (!is_first_symbol && !current_instruction_is_in_symbol)
                    outln();
                ++current_symbol;
                current_instruction_is_in_symbol = true;
                outln("{} ({:p}-{:p}):", current_symbol->name, current_symbol->address(), current_symbol->address_end());
            }

            is_first_symbol = false;
        }

        outln("{:p}  {}", virtual_offset, insn.value().to_string(virtual_offset, symbol_provider));
    }
    return 0;
}
