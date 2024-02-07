/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <AK/Assertions.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
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

namespace TerminalSettings {
ErrorOr<NonnullRefPtr<MainWidget>> MainWidget::create()
{
    auto widget = TRY(MainWidget::try_create());
    TRY(widget->setup());
    return widget;
}

ErrorOr<void> MainWidget::setup()
{
    auto& beep_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("beep_bell_radio");
    auto& visual_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("visual_bell_radio");
    auto& no_bell_radio = *find_descendant_of_type_named<GUI::RadioButton>("no_bell_radio");
    auto& automark_off_radio = *find_descendant_of_type_named<GUI::RadioButton>("automark_off");
    auto& automark_on_interactive_prompt_radio = *find_descendant_of_type_named<GUI::RadioButton>("automark_on_interactive_prompt");

    m_bell_mode = VT::TerminalWidget::parse_bell(Config::read_string("Terminal"sv, "Window"sv, "Bell"sv)).value_or(VT::TerminalWidget::BellMode::Visible);
    m_original_bell_mode = m_bell_mode;

    m_automark_mode = VT::TerminalWidget::parse_automark_mode(Config::read_string("Terminal"sv, "Terminal"sv, "AutoMark"sv)).value_or(VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt);
    m_original_automark_mode = m_automark_mode;

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
        Config::write_string("Terminal"sv, "Window"sv, "Bell"sv, VT::TerminalWidget::stringify_bell(m_bell_mode));
        set_modified(true);
    };
    visual_bell_radio.on_checked = [this](bool) {
        m_bell_mode = VT::TerminalWidget::BellMode::Visible;
        Config::write_string("Terminal"sv, "Window"sv, "Bell"sv, VT::TerminalWidget::stringify_bell(m_bell_mode));
        set_modified(true);
    };
    no_bell_radio.on_checked = [this](bool) {
        m_bell_mode = VT::TerminalWidget::BellMode::Disabled;
        Config::write_string("Terminal"sv, "Window"sv, "Bell"sv, VT::TerminalWidget::stringify_bell(m_bell_mode));
        set_modified(true);
    };

    switch (m_automark_mode) {
    case VT::TerminalWidget::AutoMarkMode::MarkNothing:
        automark_off_radio.set_checked(true, GUI::AllowCallback::No);
        break;
    case VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt:
        automark_on_interactive_prompt_radio.set_checked(true, GUI::AllowCallback::No);
        break;
    }

    automark_off_radio.on_checked = [this](bool) {
        m_automark_mode = VT::TerminalWidget::AutoMarkMode::MarkNothing;
        Config::write_string("Terminal"sv, "Terminal"sv, "AutoMark"sv, VT::TerminalWidget::stringify_automark_mode(m_automark_mode));
        set_modified(true);
    };
    automark_on_interactive_prompt_radio.on_checked = [this](bool) {
        m_automark_mode = VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt;
        Config::write_string("Terminal"sv, "Terminal"sv, "AutoMark"sv, VT::TerminalWidget::stringify_automark_mode(m_automark_mode));
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

void MainWidget::apply_settings()
{
    m_original_bell_mode = m_bell_mode;
    m_orignal_confirm_close = m_confirm_close;
    write_back_settings();
}
void MainWidget::write_back_settings() const
{
    Config::write_bool("Terminal"sv, "Terminal"sv, "ConfirmClose"sv, m_orignal_confirm_close);
    Config::write_string("Terminal"sv, "Window"sv, "Bell"sv, VT::TerminalWidget::stringify_bell(m_original_bell_mode));
    Config::write_string("Terminal"sv, "Terminal"sv, "AutoMark"sv, VT::TerminalWidget::stringify_automark_mode(m_automark_mode));
}

void MainWidget::cancel_settings()
{
    write_back_settings();
}
}
