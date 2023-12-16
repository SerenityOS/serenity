/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SourceModel.h"
#include "Gradient.h"
#include "Profile.h"
#include <LibDebug/DebugInfo.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibSymbolication/Symbolication.h>
#include <stdio.h>

namespace Profiler {

class SourceFile final {
public:
    struct Line {
        ByteString content;
        size_t num_samples { 0 };
    };

    static constexpr StringView source_root_path = "/usr/src/serenity/"sv;

    SourceFile(StringView filename)
    {
        ByteString source_file_name = filename.replace("../../"sv, source_root_path, ReplaceMode::FirstOnly);

        auto try_read_lines = [&]() -> ErrorOr<void> {
            auto unbuffered_file = TRY(Core::File::open(source_file_name, Core::File::OpenMode::Read));
            auto file = TRY(Core::InputBufferedFile::create(move(unbuffered_file)));

            Array<u8, 1024> buffer;
            while (!file->is_eof())
                m_lines.append({ TRY(file->read_line(buffer)), 0 });

            return {};
        };

        auto maybe_error = try_read_lines();
        if (maybe_error.is_error()) {
            dbgln("Could not map source file \"{}\". Tried {}. {} (errno={})", filename, source_file_name, maybe_error.error().string_literal(), maybe_error.error().code());
            return;
        }
    }

    void try_add_samples(size_t line, size_t samples)
    {
        if (line < 1 || line - 1 >= m_lines.size())
            return;

        m_lines[line - 1].num_samples += samples;
    }

    Vector<Line> const& lines() const { return m_lines; }

private:
    Vector<Line> m_lines;
};

SourceModel::SourceModel(Profile& profile, ProfileNode& node)
    : m_profile(profile)
    , m_node(node)
{
    FlatPtr base_address = 0;
    Debug::DebugInfo const* debug_info;
    if (auto maybe_kernel_base = Symbolication::kernel_base(); maybe_kernel_base.has_value() && m_node.address() >= *maybe_kernel_base) {
        if (!g_kernel_debuginfo_object.has_value())
            return;
        base_address = maybe_kernel_base.release_value();
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
        debug_info = &library_data->load_debug_info(base_address);
    }

    VERIFY(debug_info != nullptr);

    // Try to read all source files contributing to the selected function and aggregate the samples by line.
    HashMap<ByteString, SourceFile> source_files;
    for (auto const& pair : node.events_per_address()) {
        auto position = debug_info->get_source_position(pair.key - base_address);
        if (position.has_value()) {
            auto it = source_files.find(position.value().file_path);
            if (it == source_files.end()) {
                source_files.set(position.value().file_path, SourceFile(position.value().file_path));
                it = source_files.find(position.value().file_path);
            }

            it->value.try_add_samples(position.value().line_number, pair.value);
        }
    }

    // Process source file map and turn content into view model
    for (auto const& file_iterator : source_files) {
        u32 line_number = 0;
        for (auto const& line_iterator : file_iterator.value.lines()) {
            line_number++;

            m_source_lines.append({
                (u32)line_iterator.num_samples,
                line_iterator.num_samples * 100.0f / node.event_count(),
                file_iterator.key,
                line_number,
                line_iterator.content,
            });
        }
    }
}

int SourceModel::row_count(GUI::ModelIndex const&) const
{
    return m_source_lines.size();
}

ErrorOr<String> SourceModel::column_name(int column) const
{
    switch (column) {
    case Column::SampleCount:
        return m_profile.show_percentages() ? "% Samples"_string : "# Samples"_string;
    case Column::SourceCode:
        return "Source Code"_string;
    case Column::Location:
        return "Location"_string;
    case Column::LineNumber:
        return "Line"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

struct ColorPair {
    Color background;
    Color foreground;
};

static Optional<ColorPair> color_pair_for(SourceLineData const& line)
{
    if (line.percent == 0)
        return {};

    Color background = color_for_percent(line.percent);
    Color foreground;
    if (line.percent > 50)
        foreground = Color::White;
    else
        foreground = Color::Black;
    return ColorPair { background, foreground };
}

GUI::Variant SourceModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto const& line = m_source_lines[index.row()];

    if (role == GUI::ModelRole::BackgroundColor) {
        auto colors = color_pair_for(line);
        if (!colors.has_value())
            return {};
        return colors.value().background;
    }

    if (role == GUI::ModelRole::ForegroundColor) {
        auto colors = color_pair_for(line);
        if (!colors.has_value())
            return {};
        return colors.value().foreground;
    }

    if (role == GUI::ModelRole::Font) {
        if (index.column() == Column::SourceCode)
            return Gfx::FontDatabase::default_fixed_width_font();
        return {};
    }

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::SampleCount) {
            if (m_profile.show_percentages())
                return ((float)line.event_count / (float)m_node.event_count()) * 100.0f;
            return line.event_count;
        }

        if (index.column() == Column::Location)
            return line.location;

        if (index.column() == Column::LineNumber)
            return line.line_number;

        if (index.column() == Column::SourceCode)
            return line.source_code;

        return {};
    }
    return {};
}

}
