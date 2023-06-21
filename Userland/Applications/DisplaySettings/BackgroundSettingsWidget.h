/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MonitorWidget.h"
#include <AK/String.h>
#include <LibCore/Timer.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SettingsWindow.h>

namespace DisplaySettings {

class BackgroundSettingsWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(BackgroundSettingsWidget);

public:
    static ErrorOr<NonnullRefPtr<BackgroundSettingsWidget>> try_create(bool& background_settings_changed);
    virtual ~BackgroundSettingsWidget() override = default;

    virtual void apply_settings() override;

private:
    BackgroundSettingsWidget(bool& background_settings_changed);

    ErrorOr<void> create_frame();
    ErrorOr<void> load_current_settings();

    Vector<String> m_modes;

    bool& m_background_settings_changed;

    RefPtr<DisplaySettings::MonitorWidget> m_monitor_widget;
    RefPtr<GUI::IconView> m_wallpaper_view;
    RefPtr<GUI::ComboBox> m_mode_combo;
    RefPtr<GUI::ColorInput> m_color_input;
    RefPtr<GUI::Menu> m_context_menu;
    RefPtr<GUI::Action> m_show_in_file_manager_action;
    RefPtr<GUI::Action> m_copy_action;
};

}
