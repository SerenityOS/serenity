/*
 * Copyright (c) 2022, Simon Holm <simholm@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClockSettingsWidget.h"
#include <Applications/TaskbarSettings/TaskbarSettingsClockViewGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>

void ClockSettingsWidget::reset_default_values()
{
    m_format_combobox->set_text("HH:MM:SS");
}

void ClockSettingsWidget::apply_settings()
{
    m_format = m_format_combobox->text();

    Config::write_string("Taskbar", "Clock", "Format", m_format);
}

ClockSettingsWidget::ClockSettingsWidget()
{
    m_available_formats.append("HH:MM:SS");
    m_available_formats.append("HH:MM");

    load_from_gml(taskbar_settings_clock_view_gml);

    m_format_combobox = *find_descendant_of_type_named<GUI::ComboBox>("clock_format_input");
    m_format_combobox->set_text(Config::read_string("Taskbar", "Clock", "Format", "HH:MM:SS"));
    m_format_combobox->set_only_allow_values_from_model(false);
    m_format_combobox->set_model(*GUI::ItemListModel<String>::create(m_available_formats));
}
