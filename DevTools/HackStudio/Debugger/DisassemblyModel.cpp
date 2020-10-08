/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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

#include "DisassemblyModel.h"
#include <AK/MappedFile.h>
#include <AK/StringBuilder.h>
#include <LibDebug/DebugSession.h>
#include <LibELF/Loader.h>
#include <LibX86/Disassembler.h>
#include <LibX86/ELFSymbolProvider.h>
#include <ctype.h>
#include <stdio.h>

namespace HackStudio {

DisassemblyModel::DisassemblyModel(const Debug::DebugSession& debug_session, const PtraceRegisters& regs)
{
    auto containing_function = debug_session.debug_info().get_containing_function(regs.eip);
    if (!containing_function.has_value()) {
        dbgln("Cannot disassemble as the containing function was not found.");
        return;
    }

    RefPtr<ELF::Loader> elf_loader;

    if (containing_function.value().address_low >= 0xc0000000) {
        auto kernel_file = make<MappedFile>("/boot/Kernel");
        if (!kernel_file->is_valid())
            return;
        elf_loader = ELF::Loader::create((const u8*)kernel_file->data(), kernel_file->size());
    } else {
        elf_loader = debug_session.elf();
    }

    auto symbol = elf_loader->find_symbol(containing_function.value().address_low);
    if (!symbol.has_value())
        return;
    ASSERT(symbol.has_value());

    auto view = symbol.value().raw_data();

    X86::ELFSymbolProvider symbol_provider(*elf_loader);
    X86::SimpleInstructionStream stream((const u8*)view.characters_without_null_termination(), view.length());
    X86::Disassembler disassembler(stream);

    size_t offset_into_symbol = 0;
    for (;;) {
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;
        FlatPtr address_in_profiled_program = symbol.value().value() + offset_into_symbol;
        auto disassembly = insn.value().to_string(address_in_profiled_program, &symbol_provider);
        StringView instruction_bytes = view.substring_view(offset_into_symbol, insn.value().length());
        m_instructions.append({ insn.value(), disassembly, instruction_bytes, address_in_profiled_program });

        offset_into_symbol += insn.value().length();
    }
}

DisassemblyModel::~DisassemblyModel()
{
}

int DisassemblyModel::row_count(const GUI::ModelIndex&) const
{
    return m_instructions.size();
}

String DisassemblyModel::column_name(int column) const
{
    switch (column) {
    case Column::Address:
        return "Address";
    case Column::InstructionBytes:
        return "Insn Bytes";
    case Column::Disassembly:
        return "Disassembly";
    default:
        ASSERT_NOT_REACHED();
        return {};
    }
}

GUI::Variant DisassemblyModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto& insn = m_instructions[index.row()];

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::Address)
            return String::formatted("{:p}", insn.address);
        if (index.column() == Column::InstructionBytes) {
            StringBuilder builder;
            for (auto ch : insn.bytes)
                builder.appendff("{:02x} ", static_cast<unsigned char>(ch));
            return builder.to_string();
        }
        if (index.column() == Column::Disassembly)
            return insn.disassembly;
        return {};
    }
    return {};
}

void DisassemblyModel::update()
{
    did_update();
}

}
