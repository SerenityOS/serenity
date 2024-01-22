/*
 * Copyright (c) 2022-2022, Olivier De Cannière <olivier.decanniere96@gmail.com>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>

namespace CalendarSettings {

class CalendarSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(CalendarSettingsWidget)

public:
    static ErrorOr<NonnullRefPtr<CalendarSettingsWidget>> try_create();
    ErrorOr<void> initialize();

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    CalendarSettingsWidget() = default;

    static constexpr Array<StringView, 2> const m_view_modes = { "Month"sv, "Year"sv };

    RefPtr<GUI::ComboBox> m_first_day_of_week_combobox;
    RefPtr<GUI::ComboBox> m_first_day_of_weekend_combobox;
    RefPtr<GUI::SpinBox> m_weekend_length_spinbox;
    RefPtr<GUI::ComboBox> m_default_view_combobox;
};

}
