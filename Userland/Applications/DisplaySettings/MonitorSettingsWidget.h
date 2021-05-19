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

namespace DisplaySettings {

class MonitorSettingsWidget : public GUI::Widget {
    C_OBJECT(MonitorSettingsWidget);

public:
    void apply_settings();

private:
    MonitorSettingsWidget();

    void create_frame();
    void create_resolution_list();
    void load_current_settings();

    Vector<Gfx::IntSize> m_resolutions;

    RefPtr<DisplaySettings::MonitorWidget> m_monitor_widget;
    RefPtr<GUI::ComboBox> m_resolution_combo;
    RefPtr<GUI::RadioButton> m_display_scale_radio_1x;
    RefPtr<GUI::RadioButton> m_display_scale_radio_2x;
};

}
