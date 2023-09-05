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
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibKeyboard/CharacterMap.h>
#include <LibVT/TerminalWidget.h>
#include <spawn.h>

ErrorOr<NonnullRefPtr<TerminalSettingsMainWidget>> TerminalSettingsMainWidget::try_create()
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) TerminalSettingsMainWidget()));
    TRY(widget->setup());
    return widget;
}

ErrorOr<void> TerminalSettingsMainWidget::setup()
{
    TRY(load_from_gml(terminal_settings_main_gml));

    auto& beep_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("beep_bell_radio");
    auto& visual_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("visual_bell_radio");
    auto& no_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("no_bell_radio");

    m_bell_mode = parse_bell(Config::read_string("Terminal"sv, "Window"sv, "Bell"sv));
    m_original_bell_mode = m_bell_mode;

    switch (m_bell_mode) {
    case VT::TerminalWidget::BellMode::Visible:
        visual_bell_radio.set_checked(true, GUI::AllowCallback::No);
        break;
    case VT::TerminalWidget::BellMode::AudibleBeep:
        beep_bell_radio.set_checked(true, GUI::AllowCallback::No);
        break;
    case VT::TerminalWidget::BellMode::Disabled:
        no_bell_radio.set_checked(true, GUI::AllowCallback::No);
        break;
    }

    beep_bell_radio.on_checked = [this](bool) {
        m_bell_mode = VT::TerminalWidget::BellMode::AudibleBeep;
        Config::write_string("Terminal"sv, "Window"sv, "Bell"sv, stringify_bell(m_bell_mode));
        set_modified(true);
    };
    visual_bell_radio.on_checked = [this](bool) {
        m_bell_mode = VT::TerminalWidget::BellMode::Visible;
        Config::write_string("Terminal"sv, "Window"sv, "Bell"sv, stringify_bell(m_bell_mode));
        set_modified(true);
    };
    no_bell_radio.on_checked = [this](bool) {
        m_bell_mode = VT::TerminalWidget::BellMode::Disabled;
        Config::write_string("Terminal"sv, "Window"sv, "Bell"sv, stringify_bell(m_bell_mode));
        set_modified(true);
    };

    m_confirm_close = Config::read_bool("Terminal"sv, "Terminal"sv, "ConfirmClose"sv, true);
    m_orignal_confirm_close = m_confirm_close;
    auto& confirm_close_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("terminal_confirm_close");
    confirm_close_checkbox.on_checked = [&](bool confirm_close) {
        m_confirm_close = confirm_close;
        Config::write_bool("Terminal"sv, "Terminal"sv, "ConfirmClose"sv, confirm_close);
        set_modified(true);
    };
    confirm_close_checkbox.set_checked(m_confirm_close, GUI::AllowCallback::No);
    return {};
}

ErrorOr<NonnullRefPtr<TerminalSettingsViewWidget>> TerminalSettingsViewWidget::try_create()
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) TerminalSettingsViewWidget()));
    TRY(widget->setup());
    return widget;
}

ErrorOr<void> TerminalSettingsViewWidget::setup()
{
    TRY(load_from_gml(terminal_settings_view_gml));

    auto& slider = *find_descendant_of_type_named<GUI::HorizontalOpacitySlider>("background_opacity_slider");
    m_opacity = Config::read_i32("Terminal"sv, "Window"sv, "Opacity"sv);
    m_original_opacity = m_opacity;
    slider.set_value(m_opacity);
    slider.on_change = [this](int value) {
        m_opacity = value;
        Config::write_i32("Terminal"sv, "Window"sv, "Opacity"sv, static_cast<i32>(m_opacity));
        set_modified(true);
    };

    auto& font_button = *find_descendant_of_type_named<GUI::Button>("terminal_font_button");
    auto& font_text = *find_descendant_of_type_named<GUI::Label>("terminal_font_label");
    auto font_name = Config::read_string("Terminal"sv, "Text"sv, "Font"sv);
    if (font_name.is_empty())
        m_font = Gfx::FontDatabase::the().default_fixed_width_font();
    else
        m_font = Gfx::FontDatabase::the().get_by_name(font_name);
    m_original_font = m_font;
    font_text.set_text(m_font->human_readable_name());
    font_text.set_font(m_font);
    font_button.on_click = [&](auto) {
        auto picker = GUI::FontPicker::construct(window(), m_font.ptr(), true);
        if (picker->exec() == GUI::Dialog::ExecResult::OK) {
            m_font = picker->font();
            font_text.set_text(m_font->human_readable_name());
            font_text.set_font(m_font);
            Config::write_string("Terminal"sv, "Text"sv, "Font"sv, m_font->qualified_name());
            set_modified(true);
        }
    };

    auto& font_selection = *find_descendant_of_type_named<GUI::Widget>("terminal_font_selection");
    auto& use_default_font_button = *find_descendant_of_type_named<GUI::CheckBox>("terminal_font_defaulted");
    use_default_font_button.on_checked = [&, font_name](bool use_default_font) {
        if (use_default_font) {
            font_selection.set_enabled(false);
            m_font = Gfx::FontDatabase::the().default_fixed_width_font();
            font_text.set_text(m_font->human_readable_name());
            font_text.set_font(m_font);
            Config::write_string("Terminal"sv, "Text"sv, "Font"sv, m_font->qualified_name());
        } else {
            font_selection.set_enabled(true);
            m_font = font_name.is_empty()
                ? Gfx::FontDatabase::the().default_fixed_width_font()
                : Gfx::FontDatabase::the().get_by_name(font_name);
            Config::write_string("Terminal"sv, "Text"sv, "Font"sv, m_font->qualified_name());
        }
        set_modified(true);
    };
    // The "use default font" setting is not stored itself - we automatically set it if the actually present font is the default,
    // whether that was filled in by the above defaulting code or by the user.
    use_default_font_button.set_checked(m_font == Gfx::FontDatabase::the().default_fixed_width_font(), GUI::AllowCallback::No);
    font_selection.set_enabled(!use_default_font_button.is_checked());

    auto& terminal_cursor_block = *find_descendant_of_type_named<GUI::RadioButton>("terminal_cursor_block");
    auto& terminal_cursor_underline = *find_descendant_of_type_named<GUI::RadioButton>("terminal_cursor_underline");
    auto& terminal_cursor_bar = *find_descendant_of_type_named<GUI::RadioButton>("terminal_cursor_bar");

    auto& terminal_cursor_blinking = *find_descendant_of_type_named<GUI::CheckBox>("terminal_cursor_blinking");

    m_cursor_shape = VT::TerminalWidget::parse_cursor_shape(Config::read_string("Terminal"sv, "Cursor"sv, "Shape"sv)).value_or(VT::CursorShape::Block);
    m_original_cursor_shape = m_cursor_shape;

    m_cursor_is_blinking_set = Config::read_bool("Terminal"sv, "Cursor"sv, "Blinking"sv, true);
    m_original_cursor_is_blinking_set = m_cursor_is_blinking_set;

    switch (m_cursor_shape) {
    case VT::CursorShape::Underline:
        terminal_cursor_underline.set_checked(true);
        break;
    case VT::CursorShape::Bar:
        terminal_cursor_bar.set_checked(true);
        break;
    default:
        terminal_cursor_block.set_checked(true);
    }

    terminal_cursor_blinking.on_checked = [&](bool is_checked) {
        set_modified(true);
        m_cursor_is_blinking_set = is_checked;
        Config::write_bool("Terminal"sv, "Cursor"sv, "Blinking"sv, is_checked);
    };
    terminal_cursor_blinking.set_checked(Config::read_bool("Terminal"sv, "Cursor"sv, "Blinking"sv, true));

    terminal_cursor_block.on_checked = [&](bool) {
        set_modified(true);
        m_cursor_shape = VT::CursorShape::Block;
        Config::write_string("Terminal"sv, "Cursor"sv, "Shape"sv, "Block"sv);
    };
    terminal_cursor_block.set_checked(Config::read_string("Terminal"sv, "Cursor"sv, "Shape"sv) == "Block"sv);

    terminal_cursor_underline.on_checked = [&](bool) {
        set_modified(true);
        m_cursor_shape = VT::CursorShape::Underline;
        Config::write_string("Terminal"sv, "Cursor"sv, "Shape"sv, "Underline"sv);
    };
    terminal_cursor_underline.set_checked(Config::read_string("Terminal"sv, "Cursor"sv, "Shape"sv) == "Underline"sv);

    terminal_cursor_bar.on_checked = [&](bool) {
        set_modified(true);
        m_cursor_shape = VT::CursorShape::Bar;
        Config::write_string("Terminal"sv, "Cursor"sv, "Shape"sv, "Bar"sv);
    };
    terminal_cursor_bar.set_checked(Config::read_string("Terminal"sv, "Cursor"sv, "Shape"sv) == "Bar"sv);

    m_max_history_size = Config::read_i32("Terminal"sv, "Terminal"sv, "MaxHistorySize"sv);
    m_original_max_history_size = m_max_history_size;
    auto& history_size_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("history_size_spinbox");
    history_size_spinbox.set_value(m_max_history_size, GUI::AllowCallback::No);
    history_size_spinbox.on_change = [this](int value) {
        m_max_history_size = value;
        Config::write_i32("Terminal"sv, "Terminal"sv, "MaxHistorySize"sv, static_cast<i32>(m_max_history_size));
        set_modified(true);
    };

    m_show_scrollbar = Config::read_bool("Terminal"sv, "Terminal"sv, "ShowScrollBar"sv, true);
    m_original_show_scrollbar = m_show_scrollbar;
    auto& show_scrollbar_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("terminal_show_scrollbar");
    show_scrollbar_checkbox.on_checked = [&](bool show_scrollbar) {
        m_show_scrollbar = show_scrollbar;
        Config::write_bool("Terminal"sv, "Terminal"sv, "ShowScrollBar"sv, show_scrollbar);
        set_modified(true);
    };
    show_scrollbar_checkbox.set_checked(m_show_scrollbar, GUI::AllowCallback::No);
    return {};
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

DeprecatedString TerminalSettingsMainWidget::stringify_bell(VT::TerminalWidget::BellMode bell_mode)
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
    m_original_bell_mode = m_bell_mode;
    m_orignal_confirm_close = m_confirm_close;
    write_back_settings();
}
void TerminalSettingsMainWidget::write_back_settings() const
{
    Config::write_bool("Terminal"sv, "Terminal"sv, "ConfirmClose"sv, m_orignal_confirm_close);
    Config::write_string("Terminal"sv, "Window"sv, "Bell"sv, stringify_bell(m_original_bell_mode));
}

void TerminalSettingsMainWidget::cancel_settings()
{
    write_back_settings();
}

void TerminalSettingsViewWidget::apply_settings()
{
    m_original_opacity = m_opacity;
    m_original_font = m_font;
    m_original_cursor_shape = m_cursor_shape;
    m_original_cursor_is_blinking_set = m_cursor_is_blinking_set;
    m_original_max_history_size = m_max_history_size;
    m_original_show_scrollbar = m_show_scrollbar;
    write_back_settings();
}

void TerminalSettingsViewWidget::write_back_settings() const
{
    Config::write_i32("Terminal"sv, "Window"sv, "Opacity"sv, static_cast<i32>(m_original_opacity));
    Config::write_string("Terminal"sv, "Text"sv, "Font"sv, m_original_font->qualified_name());
    Config::write_string("Terminal"sv, "Cursor"sv, "Shape"sv, VT::TerminalWidget::stringify_cursor_shape(m_original_cursor_shape));
    Config::write_bool("Terminal"sv, "Cursor"sv, "Blinking"sv, m_original_cursor_is_blinking_set);
    Config::write_i32("Terminal"sv, "Terminal"sv, "MaxHistorySize"sv, static_cast<i32>(m_original_max_history_size));
    Config::write_bool("Terminal"sv, "Terminal"sv, "ShowScrollBar"sv, m_original_show_scrollbar);
}

void TerminalSettingsViewWidget::cancel_settings()
{
    write_back_settings();
}
