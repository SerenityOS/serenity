/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/SpinBox.h>

namespace DisplaySettings {

class DesktopSettingsWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT(DesktopSettingsWidget);

public:
    virtual ~DesktopSettingsWidget() override = default;

    virtual void apply_settings() override;

private:
    DesktopSettingsWidget();

    void create_frame();
    void load_current_settings();

    RefPtr<GUI::SpinBox> m_workspace_rows_spinbox;
    RefPtr<GUI::SpinBox> m_workspace_columns_spinbox;
};

}
