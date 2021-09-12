/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DesktopSettingsWidget.h"
#include <Applications/DisplaySettings/DesktopSettingsGML.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/WindowServerConnection.h>

namespace DisplaySettings {

DesktopSettingsWidget::DesktopSettingsWidget()
{
    create_frame();
    load_current_settings();
}

DesktopSettingsWidget::~DesktopSettingsWidget()
{
}

void DesktopSettingsWidget::create_frame()
{
    load_from_gml(desktop_settings_gml);

    auto& light_bulb_label = *find_descendant_of_type_named<GUI::Label>("light_bulb_label");
    light_bulb_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/32x32/app-welcome.png"));

    m_virtual_desktop_rows_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("virtual_desktop_rows_spinbox");
    m_virtual_desktop_columns_spinbox = *find_descendant_of_type_named<GUI::SpinBox>("virtual_desktop_columns_spinbox");
}

void DesktopSettingsWidget::load_current_settings()
{
    auto& desktop = GUI::Desktop::the();
    m_virtual_desktop_rows_spinbox->set_value(desktop.virtual_desktop_rows());
    m_virtual_desktop_columns_spinbox->set_value(desktop.virtual_desktop_columns());
}

void DesktopSettingsWidget::apply_settings()
{
    auto virtual_desktop_rows = (unsigned)m_virtual_desktop_rows_spinbox->value();
    auto virtual_desktop_columns = (unsigned)m_virtual_desktop_columns_spinbox->value();
    auto& desktop = GUI::Desktop::the();
    if (virtual_desktop_rows != desktop.virtual_desktop_rows() || virtual_desktop_columns != desktop.virtual_desktop_columns()) {
        if (!GUI::WindowServerConnection::the().apply_virtual_desktop_settings(virtual_desktop_rows, virtual_desktop_columns, true)) {
            GUI::MessageBox::show(window(), String::formatted("Error applying virtual desktop settings"),
                "Virtual desktop settings", GUI::MessageBox::Type::Error);
        }
    }
}

}
