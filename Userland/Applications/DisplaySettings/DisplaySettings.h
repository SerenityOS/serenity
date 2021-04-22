/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MonitorWidget.h"
#include <LibCore/Timer.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/RadioButton.h>

class DisplaySettingsWidget : public GUI::Widget {
    C_OBJECT(DisplaySettingsWidget);

public:
    DisplaySettingsWidget();

private:
    void create_frame();
    void create_wallpaper_list();
    void create_resolution_list();
    void load_current_settings();
    void send_settings_to_window_server(); // Apply the settings to the Window Server

    Vector<String> m_wallpapers;
    Vector<String> m_modes;
    Vector<Gfx::IntSize> m_resolutions;

    RefPtr<DisplaySettings::MonitorWidget> m_monitor_widget;
    RefPtr<GUI::ComboBox> m_wallpaper_combo;
    RefPtr<GUI::ComboBox> m_mode_combo;
    RefPtr<GUI::ComboBox> m_resolution_combo;
    RefPtr<GUI::RadioButton> m_display_scale_radio_1x;
    RefPtr<GUI::RadioButton> m_display_scale_radio_2x;
    RefPtr<GUI::ColorInput> m_color_input;
};
