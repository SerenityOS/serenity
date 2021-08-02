/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/Window.h>

class MouseSettingsWindow final : public GUI::Window {
    C_OBJECT(MouseSettingsWindow)
public:
    virtual ~MouseSettingsWindow() override;

private:
    MouseSettingsWindow();

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_apply_button;
    RefPtr<GUI::Button> m_reset_button;
};
