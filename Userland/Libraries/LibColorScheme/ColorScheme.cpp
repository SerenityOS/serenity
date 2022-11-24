/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ColorScheme.h"
#include <AK/QuickSort.h>
#include <LibConfig/Client.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>

ErrorOr<void> ColorScheme::ColorScheme::set_color_scheme_from_string(StringView name)
{
    if (name.contains('/'))
        return Error::from_string_literal("Shenanigans! Color scheme names can't contain slashes.");

    constexpr StringView color_names[] = {
        "Black"sv,
        "Red"sv,
        "Green"sv,
        "Yellow"sv,
        "Blue"sv,
        "Magenta"sv,
        "Cyan"sv,
        "White"sv
    };

    auto path = String::formatted("/res/terminal-colors/{}.ini", name);
    auto color_config = TRY(Core::ConfigFile::open(path));

    m_show_bold_text_as_bright = color_config->read_bool_entry("Options", "ShowBoldTextAsBright", true);

    m_background_color = Gfx::Color::from_string(color_config->read_entry("Primary", "Background"));
    m_foreground_color = Gfx::Color::from_string(color_config->read_entry("Primary", "Foreground"));

    m_colors.resize(8);
    for (u8 color_idx = 0; color_idx < 8; ++color_idx)
        m_colors[color_idx] = Gfx::Color::from_string(color_config->read_entry("Normal", color_names[color_idx])).release_value();

    m_bright_colors.resize(8);
    for (u8 color_idx = 0; color_idx < 8; ++color_idx)
        m_bright_colors[color_idx] = Gfx::Color::from_string(color_config->read_entry("Bright", color_names[color_idx])).release_value();

    return {};
}

Vector<String> ColorScheme::get_color_scheme_names()
{
    Vector<String> color_scheme_names;
    color_scheme_names.clear();
    Core::DirIterator iterator("/res/terminal-colors", Core::DirIterator::SkipParentAndBaseDir);
    while (iterator.has_next()) {
        auto path = iterator.next_path();
        color_scheme_names.append(path.replace(".ini"sv, ""sv, ReplaceMode::FirstOnly));
    }
    quick_sort(color_scheme_names);
    return color_scheme_names;
}
