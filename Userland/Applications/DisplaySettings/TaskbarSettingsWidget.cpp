/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2022, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskbarSettingsWidget.h"
#include <Applications/DisplaySettings/TaskbarSettingsGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/Application.h>

namespace DisplaySettings {

TaskbarSettingsWidget::TaskbarSettingsWidget()
{
    load_from_gml(taskbar_settings_gml);

    m_classic = *find_descendant_of_type_named<GUI::RadioButton>("classic");
    m_modern = *find_descendant_of_type_named<GUI::RadioButton>("modern");

    bool dashboard_mode = Config::read_bool("Taskbar"sv, "Interface"sv, "Dashboard"sv);
    (!dashboard_mode ? m_classic : m_modern)->set_checked(true, GUI::AllowCallback::No);

    m_classic->on_checked = [this](bool checked) {
        if (checked) {
            set_modified(true);
        }
    };
    m_modern->on_checked = [this](bool checked) {
        if (checked) {
            set_modified(true);
        }
    };
}

void TaskbarSettingsWidget::apply_settings()
{
    if (m_classic->is_checked()) {
        Config::write_bool("Taskbar"sv, "Interface"sv, "Dashboard"sv, false);
    } else if (m_modern->is_checked()) {
        Config::write_bool("Taskbar"sv, "Interface"sv, "Dashboard"sv, true);
    } else {
        VERIFY_NOT_REACHED();
    }
}

}
