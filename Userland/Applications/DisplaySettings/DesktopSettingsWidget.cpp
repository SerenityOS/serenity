/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopSettingsWidget.h"
#include <Applications/DisplaySettings/DesktopSettingsGML.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SpinBox.h>

namespace DisplaySettings {

DesktopSettingsWidget::DesktopSettingsWidget()
{
    create_frame();
    load_current_settings();
}

void DesktopSettingsWidget::create_frame()
{
    load_from_gml(desktop_settings_gml);

    m_workspace_rows_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("workspace_rows_spinbox");
    m_workspace_rows_spinbox->on_change = [&](auto) {
        set_modified(true);
    };
    m_workspace_columns_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("workspace_columns_spinbox");
    m_workspace_columns_spinbox->on_change = [&](auto) {
        set_modified(true);
    };
}

void DesktopSettingsWidget::load_current_settings()
{
    auto& desktop = GUI::Desktop::the();
    m_workspace_rows_spinbox->set_value(desktop.workspace_rows(), GUI::AllowCallback::No);
    m_workspace_columns_spinbox->set_value(desktop.workspace_columns(), GUI::AllowCallback::No);
}

void DesktopSettingsWidget::apply_settings()
{
    auto workspace_rows = (unsigned)m_workspace_rows_spinbox->value();
    auto workspace_columns = (unsigned)m_workspace_columns_spinbox->value();
    auto& desktop = GUI::Desktop::the();
    if (workspace_rows != desktop.workspace_rows() || workspace_columns != desktop.workspace_columns()) {
        if (!GUI::ConnectionToWindowServer::the().apply_workspace_settings(workspace_rows, workspace_columns, true)) {
            GUI::MessageBox::show(window(), String::formatted("Error applying workspace settings"),
                "Workspace settings", GUI::MessageBox::Type::Error);
        }
    }
}

}
