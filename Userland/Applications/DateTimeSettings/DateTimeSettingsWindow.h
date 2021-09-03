/*
 * Copyright (c) 2021, The SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Window.h>

class DateTimeSettingsWindow final : public GUI::Window {
    C_OBJECT(DateTimeSettingsWindow)
public:
    virtual ~DateTimeSettingsWindow() override;

private:
    DateTimeSettingsWindow();

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_apply_button;
    RefPtr<GUI::ComboBox> m_time_format_box;
    Vector<String> m_time_format_model;
};
