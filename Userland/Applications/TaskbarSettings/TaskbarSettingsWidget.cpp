/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskbarSettingsWidget.h"
#include <Applications/TaskbarSettings/TaskbarSettingsWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/CheckBox.h>

TaskbarSettingsWidget::TaskbarSettingsWidget()
{
    load_from_gml(taskbar_settings_widget_gml);

    m_close_on_middle_click_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("close_on_middle_click");
    m_close_on_middle_click_checkbox->set_checked(Config::read_bool("Taskbar", "WindowList", "CloseOnMiddleClick"));
}

void TaskbarSettingsWidget::apply_settings()
{
    m_close_on_middle_click = m_close_on_middle_click_checkbox->is_checked();

    Config::write_bool("Taskbar", "WindowList", "CloseOnMiddleClick", m_close_on_middle_click);
}

void TaskbarSettingsWidget::reset_default_values()
{
    m_close_on_middle_click_checkbox->set_checked(false);
}
