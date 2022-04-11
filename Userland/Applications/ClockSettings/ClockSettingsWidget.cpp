/*
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClockSettingsWidget.h"
#include <Applications/ClockSettings/ClockSettingsWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>

ClockSettingsWidget::ClockSettingsWidget()
{
    load_from_gml(clock_settings_widget_gml);

    m_24_hour_radio = *find_descendant_of_type_named<GUI::RadioButton>("24hour_radio");
    auto& twelve_hour_radio = *find_descendant_of_type_named<GUI::RadioButton>("12hour_radio");
    auto& custom_radio = *find_descendant_of_type_named<GUI::RadioButton>("custom_radio");
    custom_radio.set_checked(true);

    m_custom_format_input = *find_descendant_of_type_named<GUI::TextBox>("custom_format_input");
    m_custom_format_input->set_text(Config::read_string("Taskbar", "Clock", "TimeFormat"));

    m_24_hour_radio->on_checked = [&](bool) {
        m_custom_format_input->set_enabled(false);
        m_custom_format_input->set_text("%T");
    };

    twelve_hour_radio.on_checked = [&](bool) {
        m_custom_format_input->set_enabled(false);
        m_custom_format_input->set_text("%I:%M %p");
    };

    custom_radio.on_checked = [&](bool) {
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
    Config::write_string("Taskbar", "Clock", "TimeFormat", "%T");
}
