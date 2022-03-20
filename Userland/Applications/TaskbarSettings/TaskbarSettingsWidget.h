/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>

class TaskbarSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(TaskbarSettingsWidget)

public:
    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    TaskbarSettingsWidget();

    RefPtr<GUI::CheckBox> m_close_on_middle_click_checkbox;
    bool m_close_on_middle_click { false };
};
