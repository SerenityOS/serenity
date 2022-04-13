/*
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGUI/SettingsWindow.h>

class ClockSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(ClockSettingsWidget)

private:
    ClockSettingsWidget();

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

    void update_time_format_string();

    RefPtr<GUI::RadioButton> m_24_hour_radio;
    RefPtr<GUI::CheckBox> m_show_seconds_checkbox;
    RefPtr<GUI::TextBox> m_custom_format_input;

    String m_date_format;
};
