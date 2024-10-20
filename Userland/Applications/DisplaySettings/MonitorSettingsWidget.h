/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "MonitorWidget.h"
#include <AK/String.h>
#include <LibCore/Timer.h>
#include <LibEDID/EDID.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SettingsWindow.h>
#include <WindowServer/ScreenLayout.h>

namespace DisplaySettings {

enum class DidScreenIndexChange {
    No,
    Yes
};

class MonitorSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(MonitorSettingsWidget);

public:
    static ErrorOr<NonnullRefPtr<MonitorSettingsWidget>> try_create();
    ~MonitorSettingsWidget() override
    {
        if (m_showing_screen_numbers)
            show_screen_numbers(false);
    }

    virtual void apply_settings() override;
    void show_screen_numbers(bool);

protected:
    void show_event(GUI::ShowEvent& event) override;
    void hide_event(GUI::HideEvent& event) override;

private:
    MonitorSettingsWidget() = default;

    ErrorOr<void> create_frame();
    ErrorOr<void> create_resolution_list();
    ErrorOr<void> load_current_settings();
    ErrorOr<void> generate_resolution_strings();
    ErrorOr<void> selected_screen_index_or_resolution_changed(DidScreenIndexChange screen_index_changed);

    size_t m_selected_screen_index { 0 };

    WindowServer::ScreenLayout m_screen_layout;
    Vector<String> m_screens;
    Vector<Optional<EDID::Parser>> m_screen_edids;
    Vector<Gfx::IntSize> m_resolutions;
    Vector<String> m_resolution_strings;

    RefPtr<DisplaySettings::MonitorWidget> m_monitor_widget;
    RefPtr<GUI::ComboBox> m_screen_combo;
    RefPtr<GUI::ComboBox> m_resolution_combo;
    RefPtr<GUI::RadioButton> m_display_scale_radio_1x;
    RefPtr<GUI::RadioButton> m_display_scale_radio_2x;
    RefPtr<GUI::Label> m_dpi_label;

    bool m_showing_screen_numbers { false };
};

}
