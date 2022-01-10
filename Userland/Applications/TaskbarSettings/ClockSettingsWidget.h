/*
 * Copyright (c) 2022, Simon Holm <simholm@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>

class ClockSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(ClockSettingsWidget)

public:
    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    ClockSettingsWidget();

    String m_format;
    Vector<String> m_available_formats;

    RefPtr<GUI::ComboBox> m_format_combobox;
};
