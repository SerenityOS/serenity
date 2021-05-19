/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

class BackgroundSettingsWidget : public GUI::Widget {
    C_OBJECT(BackgroundSettingsWidget);

public:
    virtual ~BackgroundSettingsWidget() override;

    void apply_settings();

private:
    BackgroundSettingsWidget();

    void create_frame();
    void load_current_settings();

    Vector<String> m_modes;

    RefPtr<DisplaySettings::MonitorWidget> m_monitor_widget;
    RefPtr<GUI::IconView> m_wallpaper_view;
    RefPtr<GUI::ComboBox> m_mode_combo;
    RefPtr<GUI::ColorInput> m_color_input;
};

}
