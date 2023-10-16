/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Demangle.h>
#include <AK/IterationDecision.h>
#include <AK/OwnPtr.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibDisassembly/Architecture.h>
#include <LibDisassembly/Disassembler.h>
#include <LibDisassembly/ELFSymbolProvider.h>
#include <LibELF/Image.h>
#include <LibMain/Main.h>
#include <string.h>

struct Symbol {
    size_t value { 0 };
    size_t size { 0 };
    StringView name;

    size_t address() const { return value; }
    size_t address_end() const { return value + size; }

    bool contains(size_t virtual_address) { return (address() <= virtual_address && virtual_address < address_end()) || (size == 0 && address() == virtual_address); }

    String format_symbol_address() const
    {
        if (size > 0)
            return MUST(String::formatted("{:p}-{:p}", address(), address_end()));
        return MUST(String::formatted("{:p}", address()));
    }
};

ErrorOr<int> serenity_main(Main::Arguments args)
{
    StringView path {};
    StringView target_symbol;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Disassemble an executable, and show human-readable "
        "assembly code for each function.");
    args_parser.add_positional_argument(path, "Path to binary file", "path");
    args_parser.add_option(target_symbol, "Show disassembly only for a specific symbol", "symbol", 's', "symbol");
    args_parser.parse(args);

    OwnPtr<Core::MappedFile> file;
    u8 const* asm_data = nullptr;
    size_t asm_size = 0;
    if ((TRY(Core::System::stat(path))).st_size > 0) {
        file = TRY(Core::MappedFile::map(path));
        asm_data = static_cast<u8 const*>(file->data());
        asm_size = MUST(file->size());
    }

    // Functions and similar symbols.
    Vector<Symbol> ranged_symbols;
    // Jump labels, relocation targets, etc.
    Vector<Symbol> zero_size_symbols;

    size_t file_offset = 0;
    OwnPtr<Disassembly::ELFSymbolProvider> symbol_provider; // nullptr for non-ELF disassembly.
    OwnPtr<ELF::Image> elf;
    auto architecture = Disassembly::host_architecture();
    if (asm_size >= 4 && strncmp(reinterpret_cast<char const*>(asm_data), "\u007fELF", 4) == 0) {
        elf = make<ELF::Image>(asm_data, asm_size);
        if (elf->is_valid()) {
            if (auto elf_architecture = Disassembly::architecture_from_elf_machine(elf->machine()); elf_architecture.has_value())
                architecture = elf_architecture.release_value();

            symbol_provider = make<Disassembly::ELFSymbolProvider>(*elf);
            elf->for_each_section_of_type(SHT_PROGBITS, [&](ELF::Image::Section const& section) {
                // FIXME: Disassemble all SHT_PROGBITS sections, not just .text.
                if (section.name() != ".text")
                    return IterationDecision::Continue;
                asm_data = reinterpret_cast<u8 const*>(section.raw_data());
                asm_size = section.size();
                file_offset = section.address();
                return IterationDecision::Break;
            });
            ranged_symbols.ensure_capacity(elf->symbol_count() + 1);
            zero_size_symbols.ensure_capacity(elf->symbol_count() + 1);
            // Sentinels:
            ranged_symbols.append({ 0, 0, StringView() });
            zero_size_symbols.append({ 0, 0, StringView() });

            elf->for_each_symbol([&](ELF::Image::Symbol const& symbol) {
                if (symbol.name().is_empty())
                    return IterationDecision::Continue;

                if (symbol.size() == 0)
                    zero_size_symbols.append({ symbol.value(), symbol.size(), symbol.name() });
                else
                    ranged_symbols.append({ symbol.value(), symbol.size(), symbol.name() });
                return IterationDecision::Continue;
            });
            auto symbol_order = [](auto& a, auto& b) {
                if (a.value != b.value)
                    return a.value < b.value;
                if (a.size != b.size)
                    return a.size < b.size;
                return a.name < b.name;
            };
            quick_sort(ranged_symbols, symbol_order);
            quick_sort(zero_size_symbols, symbol_order);
            if constexpr (DISASM_DUMP_DEBUG) {
                for (size_t i = 0; i < ranged_symbols.size(); ++i)
                    dbgln("{}: {:p}, {}", ranged_symbols[i].name, ranged_symbols[i].value, ranged_symbols[i].size);
                for (size_t i = 0; i < zero_size_symbols.size(); ++i)
                    dbgln("{}: {:p}", zero_size_symbols[i].name, zero_size_symbols[i].value);
            }
        }
    }

    Disassembly::SimpleInstructionStream stream(asm_data, asm_size);
    Disassembly::Disassembler disassembler(stream, architecture);

    Vector<Symbol>::Iterator current_ranged_symbol = ranged_symbols.begin();
    Vector<Symbol>::Iterator current_zero_size_symbol = zero_size_symbols.begin();
    bool is_first_symbol = true;
    bool current_instruction_is_in_symbol = false;
    bool found_symbol = false;

    for (;;) {
        auto offset = stream.offset();
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;

        size_t virtual_offset = file_offset + offset;
        // Prefix regions of instructions belonging to a symbol with the symbol's name.
        // Separate regions of instructions belonging to distinct symbols with newlines,
        // and separate regions of instructions not belonging to symbols from regions belonging to symbols with newlines.
        // Interesting cases:
        // - More than 1 symbol covering a region of instructions (ICF, D1/D2)
        // - Symbols of size 0 that don't cover any instructions but are at an address (want to print them, separated from instructions both before and after)
        // Invariant: current_ranged_symbol is the largest instruction containing insn, or it is the largest instruction that has an address less than the instruction's address.
        StringBuilder dangling_symbols;
        StringBuilder instruction_symbols;
        bool needs_separator = false;
        if (current_zero_size_symbol < zero_size_symbols.end()) {
            // Print "dangling" symbols preceding the current instruction.
            while (current_zero_size_symbol + 1 < zero_size_symbols.end() && !(current_zero_size_symbol + 1)->contains(virtual_offset) && (current_zero_size_symbol + 1)->address() <= virtual_offset) {
                ++current_zero_size_symbol;
                if (!is_first_symbol)
                    dangling_symbols.appendff("\n({} ({}))\n", demangle(current_zero_size_symbol->name), current_zero_size_symbol->format_symbol_address());
            }
            // Find and print all symbols covering the current instruction.
            while (current_zero_size_symbol + 1 < zero_size_symbols.end() && (current_zero_size_symbol + 1)->contains(virtual_offset)) {
                if (!is_first_symbol && !current_instruction_is_in_symbol)
                    needs_separator = true;
                ++current_zero_size_symbol;
                current_instruction_is_in_symbol = true;

                instruction_symbols.appendff("{} ({}):\n", demangle(current_zero_size_symbol->name), current_zero_size_symbol->format_symbol_address());
            }
        }
        // Handle ranged symbols separately.
        if (current_ranged_symbol < ranged_symbols.end() && !current_ranged_symbol->contains(virtual_offset)) {
            if (!is_first_symbol && current_instruction_is_in_symbol) {
                // The previous instruction was part of a symbol that doesn't cover the current instruction, so separate it from the current instruction with a newline.
                needs_separator = true;
                current_instruction_is_in_symbol = (current_ranged_symbol + 1 < ranged_symbols.end() && (current_ranged_symbol + 1)->contains(virtual_offset));
            }

            // Print "dangling" symbols preceding the current instruction.
            while (current_ranged_symbol + 1 < ranged_symbols.end() && !(current_ranged_symbol + 1)->contains(virtual_offset) && (current_ranged_symbol + 1)->address() <= virtual_offset) {
                ++current_ranged_symbol;
                if (!is_first_symbol)
                    dangling_symbols.appendff("\n({} ({}))\n", demangle(current_ranged_symbol->name), current_ranged_symbol->format_symbol_address());
            }
            // Find and print all symbols covering the current instruction.
            while (current_ranged_symbol + 1 < ranged_symbols.end() && (current_ranged_symbol + 1)->contains(virtual_offset)) {
                if (!is_first_symbol && !current_instruction_is_in_symbol)
                    needs_separator = true;
                ++current_ranged_symbol;
                current_instruction_is_in_symbol = true;

                instruction_symbols.appendff("{} ({}):\n", demangle(current_ranged_symbol->name), current_ranged_symbol->format_symbol_address());
            }

            is_first_symbol = false;
        }

        // Past the target symbol now; no need to disassemble more.
        if (found_symbol && current_ranged_symbol->name != target_symbol)
            break;

        found_symbol = !target_symbol.is_empty() && current_ranged_symbol->name == target_symbol;

        // We have not found the target symbol yet; don't print anything.
        if (!target_symbol.is_empty() && current_ranged_symbol->name != target_symbol)
            continue;

        // Insert extra newline after the "dangling" symbols.
        if (needs_separator)
            outln();
        if (auto dangling_symbols_text = TRY(dangling_symbols.to_string()); !dangling_symbols_text.is_empty())
            outln("{}", dangling_symbols_text);
        if (auto instruction_symbols_text = TRY(instruction_symbols.to_string()); !instruction_symbols_text.is_empty())
            out("{}", instruction_symbols_text);

        size_t length = insn.value()->length();
        StringBuilder builder;
        builder.appendff("{:p}  ", virtual_offset);
        for (size_t i = 0; i < 7; i++) {
            if (i < length)
                builder.appendff("{:02x} ", asm_data[offset + i]);
            else
                builder.append("   "sv);
        }
        builder.append(" "sv);
        builder.append(insn.value()->to_byte_string(virtual_offset, *symbol_provider));
        outln("{}", builder.string_view());

        for (size_t bytes_printed = 7; bytes_printed < length; bytes_printed += 7) {
            builder.clear();
            builder.appendff("{:p} ", virtual_offset + bytes_printed);
            for (size_t i = bytes_printed; i < bytes_printed + 7 && i < length; i++)
                builder.appendff(" {:02x}", asm_data[offset + i]);
            outln("{}", builder.string_view());
        }
    }
    return 0;
}
