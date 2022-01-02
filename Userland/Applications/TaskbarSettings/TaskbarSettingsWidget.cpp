/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskbarSettingsWidget.h"
#include <AK/Assertions.h>
#include <Applications/TaskbarSettings/TaskbarSettingsMainGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Widget.h>
#include <spawn.h>

TaskbarSettingsMainWidget::TaskbarSettingsMainWidget()
{
    load_from_gml(taskbar_settings_main_gml);

    auto& taskbar_bottom_radio = *find_descendant_of_type_named<GUI::RadioButton>("taskbar_bottom_radio");
    auto& taskbar_left_radio = *find_descendant_of_type_named<GUI::RadioButton>("taskbar_left_radio");
    auto& taskbar_top_radio = *find_descendant_of_type_named<GUI::RadioButton>("taskbar_top_radio");
    auto& taskbar_right_radio = *find_descendant_of_type_named<GUI::RadioButton>("taskbar_right_radio");

    m_taskbar_location = string_to_location(Config::read_string("Taskbar", "Appearance", "Location", "Bottom"));
    m_original_taskbar_location = m_taskbar_location;

    switch (m_taskbar_location) {
    case Gfx::Alignment::Bottom:
        taskbar_bottom_radio.set_checked(true);
        break;
    case Gfx::Alignment::Left:
        taskbar_left_radio.set_checked(true);
        break;
    case Gfx::Alignment::Top:
        taskbar_top_radio.set_checked(true);
        break;
    case Gfx::Alignment::Right:
        taskbar_right_radio.set_checked(true);
        break;
    case Gfx::Alignment::TopCenter:
    case Gfx::Alignment::BottomCenter:
    case Gfx::Alignment::Center:
        dbgln("Taskbar location can't be center!");
        VERIFY_NOT_REACHED();
    }

    taskbar_bottom_radio.on_checked = [this](bool) {
        m_taskbar_location = Gfx::Alignment::Bottom;
        Config::write_string("Taskbar", "Appearance", "Location", location_to_string(m_taskbar_location));
    };
    taskbar_left_radio.on_checked = [this](bool) {
        m_taskbar_location = Gfx::Alignment::Left;
        Config::write_string("Taskbar", "Appearance", "Location", location_to_string(m_taskbar_location));
    };
    taskbar_top_radio.on_checked = [this](bool) {
        m_taskbar_location = Gfx::Alignment::Top;
        Config::write_string("Taskbar", "Appearance", "Location", location_to_string(m_taskbar_location));
    };
    taskbar_right_radio.on_checked = [this](bool) {
        m_taskbar_location = Gfx::Alignment::Right;
        Config::write_string("Taskbar", "Appearance", "Location", location_to_string(m_taskbar_location));
    };

    auto& preview_desktop_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("preview_desktop_checkbox");
    m_preview_desktop = Config::read_bool("Taskbar", "Appearance", "PreviewDesktop", true);

    preview_desktop_checkbox.on_checked = [&](bool checked) {
        Config::write_bool("Taskbar", "Appearance", "PreviewDesktop", checked);
    };

    preview_desktop_checkbox.set_checked(m_preview_desktop);
}

Gfx::Alignment TaskbarSettingsMainWidget::string_to_location(StringView location)
{
    if (location == "Bottom")
        return Gfx::Alignment::Bottom;
    if (location == "Left")
        return Gfx::Alignment::Left;
    if (location == "Top")
        return Gfx::Alignment::Top;
    if (location == "Right")
        return Gfx::Alignment::Right;
    VERIFY_NOT_REACHED();
}

String TaskbarSettingsMainWidget::location_to_string(Gfx::Alignment location)
{
    if (location == Gfx::Alignment::Bottom)
        return "Bottom";
    if (location == Gfx::Alignment::Left)
        return "Left";
    if (location == Gfx::Alignment::Top)
        return "Top";
    if (location == Gfx::Alignment::Right)
        return "Right";
    VERIFY_NOT_REACHED();
}

void TaskbarSettingsMainWidget::apply_settings()
{
    m_original_taskbar_location = m_taskbar_location;
    m_original_preview_desktop = m_preview_desktop;
    write_back_settings();
}
void TaskbarSettingsMainWidget::write_back_settings() const
{
    Config::write_string("Taskbar", "Appearance", "Location", location_to_string(m_original_taskbar_location));
    Config::write_bool("Taskbar", "Appearance", "PreviewDesktop", m_original_preview_desktop);
}

void TaskbarSettingsMainWidget::cancel_settings()
{
    write_back_settings();
}
