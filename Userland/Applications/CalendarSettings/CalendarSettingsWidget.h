/*
 * Copyright (c) 2022-2022, Olivier De Cannière <olivier.decanniere96@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ComboBox.h>
#include <LibGUI/SettingsWindow.h>

class CalendarSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(CalendarSettingsWidget)

public:
    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    CalendarSettingsWidget();

    RefPtr<GUI::ComboBox> m_first_day_of_week_combobox;
};
