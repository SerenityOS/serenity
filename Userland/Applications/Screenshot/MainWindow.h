/*
 * Copyright (c) 2024, circl <circl.lastname@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace Screenshot {

class MainWindow final : public GUI::Window {
    C_OBJECT(MainWindow)
public:
    virtual ~MainWindow() override = default;

private:
    MainWindow();

    void take_screenshot();

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_browse;
    RefPtr<GUI::RadioButton> m_selected_area;
    RefPtr<GUI::RadioButton> m_output_radio_clipboard;
    RefPtr<GUI::RadioButton> m_output_radio_pixel_paint;
    RefPtr<GUI::RadioButton> m_output_radio_file;
    RefPtr<GUI::TextBox> m_destination;
};

}
