/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/LogStream.h>
#include <AK/MappedFile.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibELF/Loader.h>
#include <LibX86/Disassembler.h>
#include <LibX86/ELFSymbolProvider.h>
#include <stdio.h>
#include <string.h>

//#define DISASM_DUMP

int main(int argc, char** argv)
{
    const char* path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to i386 binary file", "path");
    args_parser.parse(argc, argv);

    MappedFile file(path);
    if (!file.is_valid()) {
        // Already printed some error message.
        return 1;
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

    const u8* asm_data = (const u8*)file.data();
    size_t asm_size = file.size();
    size_t file_offset = 0;
    Vector<Symbol>::Iterator current_symbol = symbols.begin();
    RefPtr<ELF::Loader> elf;
    OwnPtr<X86::ELFSymbolProvider> symbol_provider; // nullptr for non-ELF disassembly.
    if (asm_size >= 4 && strncmp((const char*)asm_data, "\u007fELF", 4) == 0) {
        NonnullRefPtr<ELF::Loader> elf_loader = ELF::Loader::create(asm_data, asm_size);
        if (elf_loader->image().is_valid()) {
            elf = elf_loader;
            symbol_provider = make<X86::ELFSymbolProvider>(*elf);
            elf->image().for_each_section_of_type(SHT_PROGBITS, [&](const ELF::Image::Section& section) {
                // FIXME: Disassemble all SHT_PROGBITS sections, not just .text.
                if (section.name() != ".text")
                    return IterationDecision::Continue;
                asm_data = (const u8*)section.raw_data();
                asm_size = section.size();
                file_offset = section.address();
                return IterationDecision::Break;
            });
            symbols.ensure_capacity(elf->image().symbol_count() + 1);
            symbols.append({ 0, 0, StringView() }); // Sentinel.
            elf->image().for_each_symbol([&](const ELF::Image::Symbol& symbol) {
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
#ifdef DISASM_DUMP
            for (size_t i = 0; i < symbols.size(); ++i)
                dbg() << symbols[i].name << ": " << (void*)(uintptr_t)symbols[i].value << ", " << symbols[i].size;
#endif
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
                out();
                current_instruction_is_in_symbol = (current_symbol + 1 < symbols.end() && (current_symbol + 1)->contains(virtual_offset));
            }

            // Try to find symbol covering current instruction, if one exists.
            while (current_symbol + 1 < symbols.end() && !(current_symbol + 1)->contains(virtual_offset) && (current_symbol + 1)->address() <= virtual_offset) {
                ++current_symbol;
                if (!is_first_symbol)
                    out() << "\n(" << current_symbol->name << " (" << String::format("%08x-%08x", current_symbol->address(), current_symbol->address_end()) << "))\n";
            }
            while (current_symbol + 1 < symbols.end() && (current_symbol + 1)->contains(virtual_offset)) {
                if (!is_first_symbol && !current_instruction_is_in_symbol)
                    out();
                ++current_symbol;
                current_instruction_is_in_symbol = true;
                out() << current_symbol->name << " (" << String::format("%08x-%08x", current_symbol->address(), current_symbol->address_end()) << "):";
            }

            is_first_symbol = false;
        }

        out() << String::format("%08x", virtual_offset) << "  " << insn.value().to_string(virtual_offset, symbol_provider);
    }

    return 0;
}
