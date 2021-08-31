/*
 * Copyright (c) 2021, the SerenityOS Developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

#include "SelectableOverlay.h"

class ScreenshotWidget final : public GUI::Widget {
    C_OBJECT(ScreenshotWidget);

public:
    virtual ~ScreenshotWidget() override;

    void set_path(String const& path);

private:
    ScreenshotWidget();
    virtual void timer_event(Core::TimerEvent& event) override;

    void save_screenshot(bool from_callback);

    RefPtr<GUI::RadioButton> m_whole_button;
    RefPtr<GUI::RadioButton> m_custom_button;
    RefPtr<GUI::CheckBox> m_copy_checkbox;
    RefPtr<GUI::SpinBox> m_delay_spinbox;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_ok_button;

    RefPtr<GUI::Window> m_window_sel;
    RefPtr<SelectableOverlay> m_overlay;

    String m_output_path = "";
};
