/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DisassemblyModel.h"
#include "Gradient.h"
#include "PercentageFormatting.h"
#include "Profile.h"
#include <LibCore/MappedFile.h>
#include <LibDebug/DebugInfo.h>
#include <LibDisassembly/Architecture.h>
#include <LibDisassembly/Disassembler.h>
#include <LibDisassembly/ELFSymbolProvider.h>
#include <LibELF/Image.h>
#include <LibSymbolication/Symbolication.h>
#include <stdio.h>

namespace Profiler {

static Optional<MappedObject> s_kernel_binary;

static ELF::Image* try_load_kernel_binary()
{
    if (s_kernel_binary.has_value())
        return &s_kernel_binary->elf;
    auto kernel_binary_or_error = Core::MappedFile::map("/boot/Kernel"sv);
    if (!kernel_binary_or_error.is_error()) {
        auto kernel_binary = kernel_binary_or_error.release_value();
        auto image = ELF::Image(kernel_binary->bytes());
        s_kernel_binary = { { move(kernel_binary), image } };
        return &s_kernel_binary->elf;
    }
    return nullptr;
}

DisassemblyModel::DisassemblyModel(Profile& profile, ProfileNode& node)
    : m_profile(profile)
    , m_node(node)
{
    FlatPtr base_address = 0;
    Debug::DebugInfo const* debug_info;
    ELF::Image const* elf;
    if (auto maybe_kernel_base = Symbolication::kernel_base(); maybe_kernel_base.has_value() && m_node.address() >= *maybe_kernel_base) {
        if (!g_kernel_debuginfo_object.has_value())
            return;
        base_address = maybe_kernel_base.release_value();
        elf = try_load_kernel_binary();
        if (elf == nullptr)
            return;
        if (g_kernel_debug_info == nullptr)
            g_kernel_debug_info = make<Debug::DebugInfo>(g_kernel_debuginfo_object->elf, ByteString::empty(), base_address);
        debug_info = g_kernel_debug_info.ptr();
    } else {
        auto const& process = node.process();
        auto const* library_data = process.library_metadata.library_containing(node.address());
        if (!library_data) {
            dbgln("no library data for address {:p}", node.address());
            return;
        }
        base_address = library_data->base;
        elf = &library_data->object->elf;
        debug_info = &library_data->load_debug_info(base_address);
    }

    VERIFY(elf != nullptr);
    VERIFY(debug_info != nullptr);

    FlatPtr function_address = node.address() - base_address;
    auto is_function_address = false;
    auto function = debug_info->get_containing_function(function_address);
    if (function.has_value()) {
        if (function_address == function->address_low)
            is_function_address = true;
        function_address = function->address_low;
    } else {
        dbgln("DisassemblyModel: Function containing {:p} ({}) not found", node.address() - base_address, node.symbol());
    }

    auto symbol = elf->find_symbol(function_address);
    if (!symbol.has_value()) {
        dbgln("DisassemblyModel: symbol not found");
        return;
    }
    if (!symbol.value().raw_data().length()) {
        dbgln("DisassemblyModel: Found symbol without code");
        return;
    }
    VERIFY(symbol.has_value());

    auto symbol_offset_from_function_start = node.address() - base_address - symbol->value();
    auto view = symbol.value().raw_data().substring_view(symbol_offset_from_function_start);

    Disassembly::ELFSymbolProvider symbol_provider(*elf, base_address);
    Disassembly::SimpleInstructionStream stream((u8 const*)view.characters_without_null_termination(), view.length());
    Disassembly::Disassembler disassembler(stream, Disassembly::architecture_from_elf_machine(elf->machine()).value_or(Disassembly::host_architecture()));

    size_t offset_into_symbol = 0;
    FlatPtr last_instruction_offset = 0;
    if (!is_function_address) {
        FlatPtr last_instruction_address = 0;
        for (auto const& event : node.events_per_address())
            last_instruction_address = max(event.key, last_instruction_address);
        last_instruction_offset = last_instruction_address - node.address();
    }
    for (;;) {
        if (!is_function_address && offset_into_symbol > last_instruction_offset)
            break;

        auto insn = disassembler.next();
        if (!insn.has_value())
            break;
        FlatPtr address_in_profiled_program = node.address() + offset_into_symbol;

        auto disassembly = insn.value()->to_byte_string(address_in_profiled_program, symbol_provider);

        auto length = insn.value()->length();
        StringView instruction_bytes = view.substring_view(offset_into_symbol, length);
        u32 samples_at_this_instruction = m_node.events_per_address().get(address_in_profiled_program).value_or(0);
        float percent = ((float)samples_at_this_instruction / (float)m_node.event_count()) * 100.0f;
        auto source_position = debug_info->get_source_position_with_inlines(address_in_profiled_program - base_address).release_value_but_fixme_should_propagate_errors();

        m_instructions.append({ insn.release_value(), disassembly, instruction_bytes, address_in_profiled_program, samples_at_this_instruction, percent, source_position });

        offset_into_symbol += length;
    }
}

int DisassemblyModel::row_count(GUI::ModelIndex const&) const
{
    return m_instructions.size();
}

ErrorOr<String> DisassemblyModel::column_name(int column) const
{
    switch (column) {
    case Column::SampleCount:
        return m_profile.show_percentages() ? "% Samples"_string : "# Samples"_string;
    case Column::Address:
        return "Address"_string;
    case Column::InstructionBytes:
        return "Insn Bytes"_string;
    case Column::Disassembly:
        return "Disassembly"_string;
    case Column::SourceLocation:
        return "Source Location"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

struct ColorPair {
    Color background;
    Color foreground;
};

static Optional<ColorPair> color_pair_for(InstructionData const& insn)
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

GUI::Variant DisassemblyModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto const& insn = m_instructions[index.row()];

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

    if (role == GUI::ModelRole::TextAlignment) {
        if (index.column() == Column::SampleCount)
            return Gfx::TextAlignment::CenterRight;
    }

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SampleCount) {
            if (m_profile.show_percentages())
                return format_percentage(insn.event_count, m_node.event_count());
            return insn.event_count;
        }

        if (index.column() == Column::Address)
            return ByteString::formatted("{:p}", insn.address);

        if (index.column() == Column::InstructionBytes) {
            StringBuilder builder;
            for (auto ch : insn.bytes) {
                builder.appendff("{:02x} ", (u8)ch);
            }
            return builder.to_byte_string();
        }

        if (index.column() == Column::Disassembly)
            return insn.disassembly;

        if (index.column() == Column::SourceLocation) {
            StringBuilder builder;
            auto first = true;
            for (auto const& entry : insn.source_position_with_inlines.inline_chain) {
                if (first)
                    first = false;
                else
                    builder.append(" => "sv);
                builder.appendff("{}:{}", entry.file_path, entry.line_number);
            }
            if (insn.source_position_with_inlines.source_position.has_value()) {
                if (!first)
                    builder.append(" => "sv);
                auto const& entry = insn.source_position_with_inlines.source_position.value();
                builder.appendff("{}:{}", entry.file_path, entry.line_number);
            }
            return builder.to_byte_string();
        }

        return {};
    }
    return {};
}

}
