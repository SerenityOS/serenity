/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalSettingsWidget.h"
#include <AK/Assertions.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <Applications/TerminalSettings/TerminalSettingsMainGML.h>
#include <Applications/TerminalSettings/TerminalSettingsViewGML.h>
#include <LibConfig/Client.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibKeyboard/CharacterMap.h>
#include <LibVT/TerminalWidget.h>
#include <spawn.h>

TerminalSettingsMainWidget::TerminalSettingsMainWidget()
{
    load_from_gml(terminal_settings_main_gml);

    auto& beep_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("beep_bell_radio");
    auto& visual_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("visual_bell_radio");
    auto& no_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("no_bell_radio");

    m_bell_mode = parse_bell(Config::read_string("Terminal", "Window", "Bell"));
    m_original_bell_mode = m_bell_mode;

    switch (m_bell_mode) {
    case VT::TerminalWidget::BellMode::Visible:
        visual_bell_radio.set_checked(true);
        break;
    case VT::TerminalWidget::BellMode::AudibleBeep:
        beep_bell_radio.set_checked(true);
        break;
    case VT::TerminalWidget::BellMode::Disabled:
        no_bell_radio.set_checked(true);
        break;
    }

    beep_bell_radio.on_checked = [this](bool) {
        m_bell_mode = VT::TerminalWidget::BellMode::AudibleBeep;
        Config::write_string("Terminal", "Window", "Bell", stringify_bell(m_bell_mode));
    };
    visual_bell_radio.on_checked = [this](bool) {
        m_bell_mode = VT::TerminalWidget::BellMode::Visible;
        Config::write_string("Terminal", "Window", "Bell", stringify_bell(m_bell_mode));
    };
    no_bell_radio.on_checked = [this](bool) {
        m_bell_mode = VT::TerminalWidget::BellMode::Disabled;
        Config::write_string("Terminal", "Window", "Bell", stringify_bell(m_bell_mode));
    };

    m_max_history_size = Config::read_i32("Terminal", "Terminal", "MaxHistorySize");
    m_original_max_history_size = m_max_history_size;
    auto& history_size_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("history_size_spinbox");
    history_size_spinbox.set_value(m_max_history_size);
    history_size_spinbox.on_change = [this](int value) {
        m_max_history_size = value;
        Config::write_i32("Terminal", "Terminal", "MaxHistorySize", static_cast<i32>(m_max_history_size));
    };
}

TerminalSettingsViewWidget::TerminalSettingsViewWidget()
{
    load_from_gml(terminal_settings_view_gml);

    auto& slider = *find_descendant_of_type_named<GUI::OpacitySlider>("background_opacity_slider");
    m_opacity = Config::read_i32("Terminal", "Window", "Opacity");
    m_original_opacity = m_opacity;
    slider.set_value(m_opacity);
    slider.on_change = [this](int value) {
        m_opacity = value;
        Config::write_i32("Terminal", "Window", "Opacity", static_cast<i32>(m_opacity));
    };

    m_color_scheme = Config::read_string("Terminal", "Window", "ColorScheme");
    m_original_color_scheme = m_color_scheme;
    // The settings window takes a reference to this vector, so it needs to outlive this scope.
    // As long as we ensure that only one settings window may be open at a time (which we do),
    // this should cause no problems.
    static Vector<String> color_scheme_names;
    color_scheme_names.clear();
    Core::DirIterator iterator("/res/terminal-colors", Core::DirIterator::SkipParentAndBaseDir);
    while (iterator.has_next()) {
        auto path = iterator.next_path();
        color_scheme_names.append(path.replace(".ini", ""));
    }
    quick_sort(color_scheme_names);
    auto& color_scheme_combo = *find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combo");
    color_scheme_combo.set_only_allow_values_from_model(true);
    color_scheme_combo.set_model(*GUI::ItemListModel<String>::create(color_scheme_names));
    color_scheme_combo.set_selected_index(color_scheme_names.find_first_index(m_color_scheme).value());
    color_scheme_combo.set_enabled(color_scheme_names.size() > 1);
    color_scheme_combo.on_change = [&](auto&, const GUI::ModelIndex& index) {
        m_color_scheme = index.data().as_string();
        Config::write_string("Terminal", "Window", "ColorScheme", m_color_scheme);
    };

    auto& font_button = *find_descendant_of_type_named<GUI::Button>("terminal_font_button");
    auto& font_text = *find_descendant_of_type_named<GUI::Label>("terminal_font_label");
    auto font_name = Config::read_string("Terminal", "Text", "Font");
    if (font_name.is_empty())
        m_font = Gfx::FontDatabase::the().default_fixed_width_font();
    else
        m_font = Gfx::FontDatabase::the().get_by_name(font_name);
    m_original_font = m_font;
    font_text.set_text(m_font->qualified_name());
    font_text.set_font(m_font);
    font_button.on_click = [&](auto) {
        auto picker = GUI::FontPicker::construct(window(), m_font.ptr(), true);
        if (picker->exec() == GUI::Dialog::ExecOK) {
            m_font = picker->font();
            font_text.set_text(m_font->qualified_name());
            font_text.set_font(m_font);
            Config::write_string("Terminal", "Text", "Font", m_font->qualified_name());
        }
    };

    auto& font_selection = *find_descendant_of_type_named<GUI::Widget>("terminal_font_selection");
    auto& use_default_font_button = *find_descendant_of_type_named<GUI::CheckBox>("terminal_font_defaulted");
    use_default_font_button.on_checked = [&](bool use_default_font) {
        if (use_default_font) {
            font_selection.set_enabled(false);
            m_font = Gfx::FontDatabase::the().default_fixed_width_font();
            Config::write_string("Terminal", "Text", "Font", m_font->qualified_name());
        } else {
            font_selection.set_enabled(true);
            m_font = Gfx::FontDatabase::the().get_by_name(font_text.text());
            Config::write_string("Terminal", "Text", "Font", m_font->qualified_name());
        }
    };
    // The "use default font" setting is not stored itself - we automatically set it if the actually present font is the default,
    // whether that was filled in by the above defaulting code or by the user.
    use_default_font_button.set_checked(m_font == Gfx::FontDatabase::the().default_fixed_width_font());
}

VT::TerminalWidget::BellMode TerminalSettingsMainWidget::parse_bell(StringView bell_string)
{
    if (bell_string == "AudibleBeep")
        return VT::TerminalWidget::BellMode::AudibleBeep;
    if (bell_string == "Visible")
        return VT::TerminalWidget::BellMode::Visible;
    if (bell_string == "Disabled")
        return VT::TerminalWidget::BellMode::Disabled;
    VERIFY_NOT_REACHED();
}

String TerminalSettingsMainWidget::stringify_bell(VT::TerminalWidget::BellMode bell_mode)
{
    if (bell_mode == VT::TerminalWidget::BellMode::AudibleBeep)
        return "AudibleBeep";
    if (bell_mode == VT::TerminalWidget::BellMode::Disabled)
        return "Disabled";
    if (bell_mode == VT::TerminalWidget::BellMode::Visible)
        return "Visible";
    VERIFY_NOT_REACHED();
}

void TerminalSettingsMainWidget::apply_settings()
{
    m_original_max_history_size = m_max_history_size;
    m_original_bell_mode = m_bell_mode;
    write_back_settings();
}
void TerminalSettingsMainWidget::write_back_settings() const
{
    Config::write_i32("Terminal", "Terminal", "MaxHistorySize", static_cast<i32>(m_original_max_history_size));
    Config::write_string("Terminal", "Window", "Bell", stringify_bell(m_original_bell_mode));
}

void TerminalSettingsMainWidget::cancel_settings()
{
    write_back_settings();
}

void TerminalSettingsViewWidget::apply_settings()
{
    m_original_opacity = m_opacity;
    m_original_font = m_font;
    m_original_color_scheme = m_color_scheme;
    write_back_settings();
}

void TerminalSettingsViewWidget::write_back_settings() const
{
    Config::write_i32("Terminal", "Window", "Opacity", static_cast<i32>(m_original_opacity));
    Config::write_string("Terminal", "Text", "Font", m_original_font->qualified_name());
    Config::write_string("Terminal", "Window", "ColorScheme", m_original_color_scheme);
}

void TerminalSettingsViewWidget::cancel_settings()
{
    write_back_settings();
}
