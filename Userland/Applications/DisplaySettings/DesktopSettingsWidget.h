/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/SpinBox.h>

namespace DisplaySettings {

class DesktopSettingsWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(DesktopSettingsWidget);

public:
    static ErrorOr<NonnullRefPtr<DesktopSettingsWidget>> try_create();
    virtual ~DesktopSettingsWidget() override = default;
    ErrorOr<void> initialize();

    virtual void apply_settings() override;

private:
    DesktopSettingsWidget() = default;

    ErrorOr<void> create_frame();
    void load_current_settings();

    RefPtr<GUI::SpinBox> m_workspace_rows_spinbox;
    RefPtr<GUI::SpinBox> m_workspace_columns_spinbox;
};

}
