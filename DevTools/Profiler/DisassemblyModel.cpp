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
#include <LibELF/Loader.h>
#include <LibGUI/Painter.h>
#include <LibX86/Disassembler.h>
#include <LibX86/ELFSymbolProvider.h>
#include <ctype.h>
#include <stdio.h>

static const Gfx::Bitmap& heat_gradient()
{
    static RefPtr<Gfx::Bitmap> bitmap;
    if (!bitmap) {
        bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { 100, 1 });
        GUI::Painter painter(*bitmap);
        painter.fill_rect_with_gradient(Orientation::Horizontal, bitmap->rect(), Color::from_rgb(0xffc080), Color::from_rgb(0xff3000));
    }
    return *bitmap;
}

static Color color_for_percent(int percent)
{
    ASSERT(percent >= 0 && percent <= 100);
    return heat_gradient().get_pixel(percent, 0);
}

DisassemblyModel::DisassemblyModel(Profile& profile, ProfileNode& node)
    : m_profile(profile)
    , m_node(node)
{
    String path;
    if (m_node.address() >= 0xc0000000)
        path = "/boot/Kernel";
    else
        path = profile.executable_path();
    m_file = make<MappedFile>(path);

    if (!m_file->is_valid())
        return;

    auto elf_loader = ELF::Loader::create((const u8*)m_file->data(), m_file->size());

    auto symbol = elf_loader->find_symbol(node.address());
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
        size_t samples_at_this_instruction = m_node.events_per_address().get(address_in_profiled_program).value_or(0);
        float percent = ((float)samples_at_this_instruction / (float)m_node.event_count()) * 100.0f;

        m_instructions.append({ insn.value(), disassembly, instruction_bytes, address_in_profiled_program, samples_at_this_instruction, percent });

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

struct ColorPair {
    Color background;
    Color foreground;
};

static Optional<ColorPair> color_pair_for(const InstructionData& insn)
{
    if (insn.percent == 0)
        return {};

    Color background = color_for_percent(insn.percent);
    Color foreground;
    if (insn.percent > 50)
        foreground = Color::White;
    else
        foreground = Color::Black;
    return ColorPair { background, foreground };
}

GUI::Variant DisassemblyModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto& insn = m_instructions[index.row()];

    if (role == GUI::ModelRole::BackgroundColor) {
        auto colors = color_pair_for(insn);
        if (!colors.has_value())
            return {};
        return colors.value().background;
    }

    if (role == GUI::ModelRole::ForegroundColor) {
        auto colors = color_pair_for(insn);
        if (!colors.has_value())
            return {};
        return colors.value().foreground;
    }

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SampleCount) {
            if (m_profile.show_percentages())
                return ((float)insn.event_count / (float)m_node.event_count()) * 100.0f;
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
