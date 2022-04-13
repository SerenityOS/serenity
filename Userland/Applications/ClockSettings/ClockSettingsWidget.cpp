/*
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClockSettingsWidget.h"
#include <Applications/ClockSettings/ClockSettingsWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>

ClockSettingsWidget::ClockSettingsWidget()
{
    load_from_gml(clock_settings_widget_gml);

    m_24_hour_radio = *find_descendant_of_type_named<GUI::RadioButton>("24hour_radio");
    auto& twelve_hour_radio = *find_descendant_of_type_named<GUI::RadioButton>("12hour_radio");
    m_show_seconds_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("seconds_checkbox");
    auto& custom_radio = *find_descendant_of_type_named<GUI::RadioButton>("custom_radio");
    custom_radio.set_checked(true);

    m_custom_format_input = *find_descendant_of_type_named<GUI::TextBox>("custom_format_input");
    m_custom_format_input->set_text(Config::read_string("Taskbar", "Clock", "TimeFormat"));

    m_24_hour_radio->on_checked = [&](bool) {
        m_show_seconds_checkbox->set_enabled(true);
        m_custom_format_input->set_enabled(false);
        update_time_format_string();
    };

    twelve_hour_radio.on_checked = [&](bool) {
        m_show_seconds_checkbox->set_enabled(true);
        m_custom_format_input->set_enabled(false);
        update_time_format_string();
    };

    m_show_seconds_checkbox->on_checked = [&](bool) {
        update_time_format_string();
    };

    custom_radio.on_checked = [&](bool) {
        m_show_seconds_checkbox->set_enabled(false);
        m_custom_format_input->set_enabled(true);
    };
}

void ClockSettingsWidget::apply_settings()
{
    Config::write_string("Taskbar", "Clock", "TimeFormat", m_custom_format_input->text());
}

void ClockSettingsWidget::reset_default_values()
{
    m_24_hour_radio->set_checked(true);
    m_show_seconds_checkbox->set_checked(true);
    Config::write_string("Taskbar", "Clock", "TimeFormat", "%T");
}

void ClockSettingsWidget::update_time_format_string()
{
    bool show_seconds = m_show_seconds_checkbox->is_checked();
    if (m_24_hour_radio->is_checked())
        m_custom_format_input->set_text(show_seconds ? "%T" : "%R");
    else
        m_custom_format_input->set_text(show_seconds ? "%r" : "%I:%M %p");
}
