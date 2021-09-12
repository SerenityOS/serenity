/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MonitorWidget.h"
#include <LibGUI/ComboBox.h>
#include <LibGUI/RadioButton.h>
#include <WindowServer/ScreenLayout.h>

namespace DisplaySettings {

class MonitorSettingsWidget : public GUI::Widget {
    C_OBJECT(MonitorSettingsWidget);

public:
    ~MonitorSettingsWidget() override
    {
        if (m_showing_screen_numbers)
            show_screen_numbers(false);
    }

    void apply_settings();
    void show_screen_numbers(bool);

private:
    MonitorSettingsWidget();

    void create_frame();
    void create_resolution_list();
    void load_current_settings();
    void selected_screen_index_changed();

    size_t m_selected_screen_index { 0 };

    WindowServer::ScreenLayout m_screen_layout;
    Vector<String> m_screens;
    Vector<Gfx::IntSize> m_resolutions;

    RefPtr<DisplaySettings::MonitorWidget> m_monitor_widget;
    RefPtr<GUI::ComboBox> m_screen_combo;
    RefPtr<GUI::ComboBox> m_resolution_combo;
    RefPtr<GUI::RadioButton> m_display_scale_radio_1x;
    RefPtr<GUI::RadioButton> m_display_scale_radio_2x;

    bool m_showing_screen_numbers { false };
};

}
