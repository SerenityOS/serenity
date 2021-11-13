/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/SpinBox.h>

namespace DisplaySettings {

class DesktopSettingsWidget : public GUI::Widget {
    C_OBJECT(DesktopSettingsWidget);

public:
    virtual ~DesktopSettingsWidget() override;

    void apply_settings();

private:
    DesktopSettingsWidget();

    void create_frame();
    void load_current_settings();

    RefPtr<GUI::SpinBox> m_workspace_rows_spinbox;
    RefPtr<GUI::SpinBox> m_workspace_columns_spinbox;
};

}
