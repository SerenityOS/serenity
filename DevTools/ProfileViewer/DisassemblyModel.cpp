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

#include "DisassemblyModel.h"
#include "Profile.h"
#include <AK/MappedFile.h>
#include <LibELF/ELFLoader.h>
#include <LibX86/Disassembler.h>
#include <ctype.h>
#include <stdio.h>

DisassemblyModel::DisassemblyModel(Profile& profile, ProfileNode& node)
    : m_profile(profile)
    , m_node(node)
{
    m_file = make<MappedFile>(profile.executable_path());
    auto elf_loader = make<ELFLoader>((const u8*)m_file->data(), m_file->size());

    auto symbol = elf_loader->find_symbol(node.address());
    ASSERT(symbol.has_value());

    auto view = symbol.value().raw_data();
    X86::SimpleInstructionStream stream((const u8*)view.characters_without_null_termination(), view.length());
    X86::Disassembler disassembler(stream);

    dbg() << "Disassembly for " << node.symbol() << " @ " << (const void*)node.address();

    size_t offset_into_symbol = 0;
    for (;;) {
        auto insn = disassembler.next();
        if (!insn.has_value())
            break;
        FlatPtr address_in_profiled_program = symbol.value().value() + offset_into_symbol;

        auto disassembly = insn.value().to_string(address_in_profiled_program);
        dbg() << disassembly;

        StringView instruction_bytes = view.substring_view(offset_into_symbol, insn.value().length());
        size_t samples_at_this_instruction = m_node.events_per_address().get(address_in_profiled_program).value_or(0);

        m_instructions.append({ insn.value(), disassembly, instruction_bytes, address_in_profiled_program, samples_at_this_instruction });

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
    case Column::SampleCount:
        return m_profile.show_percentages() ? "% Samples" : "# Samples";
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

GUI::Model::ColumnMetadata DisassemblyModel::column_metadata(int column) const
{
    if (column == Column::SampleCount)
        return ColumnMetadata { 0, Gfx::TextAlignment::CenterRight };
    return {};
}

GUI::Variant DisassemblyModel::data(const GUI::ModelIndex& index, Role role) const
{
    auto& insn = m_instructions[index.row()];

    if (role == Role::BackgroundColor) {
        if (insn.event_count > 0)
            return Color(Color::Yellow);
        return {};
    }

    if (role == Role::Display) {
        if (index.column() == Column::SampleCount) {
            if (m_profile.show_percentages())
                return ((float)insn.event_count / (float)m_profile.filtered_event_count()) * 100.0f;
            return insn.event_count;
        }
        if (index.column() == Column::Address)
            return String::format("%#08x", insn.address);
        if (index.column() == Column::InstructionBytes) {
            StringBuilder builder;
            for (auto ch : insn.bytes) {
                builder.appendf("%02x ", (u8)ch);
            }
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
