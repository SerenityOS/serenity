/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>

class ClockSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(ClockSettingsWidget)

private:
    ClockSettingsWidget();

    virtual void apply_settings() override;
    virtual void reset_default_values() override;
    void set_time_zone() const;

    String m_time_zone;
    RefPtr<GUI::ComboBox> m_time_zone_combo_box;
};
