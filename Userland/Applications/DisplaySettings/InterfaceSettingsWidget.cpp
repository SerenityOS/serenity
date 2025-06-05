/*
 * Copyright (c) 2023, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InterfaceSettingsWidget.h"
#include <Applications/DisplaySettings/InterfaceSettingsGML.h>
#include <LibConfig/Client.h>
#include <LibGUI/CheckBox.h>

namespace GUI {

namespace DisplaySettings {

InterfaceSettingsWidget::InterfaceSettingsWidget()
{
    load_from_gml(interface_settings_gml).release_value_but_fixme_should_propagate_errors();

    m_global_menu = *find_descendant_of_type_named<GUI::CheckBox>("global_menu_checkbox");
    load_settings();

    m_global_menu->on_checked = [this](bool) {
        set_modified(true);
    };
}

void InterfaceSettingsWidget::load_settings()
{
    m_global_menu->set_checked(Config::read_bool("Taskbar"sv, "GlobalMenu"sv, "Enabled"sv, false));
}

void InterfaceSettingsWidget::apply_settings()
{
    Config::write_bool("Taskbar"sv, "GlobalMenu"sv, "Enabled"sv, m_global_menu->is_checked());
}
}

}
