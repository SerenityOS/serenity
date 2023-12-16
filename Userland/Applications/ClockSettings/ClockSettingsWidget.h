/*
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGUI/SettingsWindow.h>

class ClockSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(ClockSettingsWidget)

public:
    static ErrorOr<NonnullRefPtr<ClockSettingsWidget>> try_create();

private:
    ClockSettingsWidget() = default;
    ErrorOr<void> setup();

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

    void update_time_format_string();
    void update_clock_preview();

    RefPtr<GUI::RadioButton> m_24_hour_radio;
    RefPtr<GUI::CheckBox> m_show_seconds_checkbox;
    RefPtr<GUI::TextBox> m_custom_format_input;
    RefPtr<GUI::Label> m_clock_preview;

    RefPtr<Core::Timer> m_clock_preview_update_timer;

    ByteString m_time_format;
};
