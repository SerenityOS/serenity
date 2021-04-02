/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@gmail.com>
 * Copyright (c) 2021, the SerenityOS developers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Window.h>

class MouseSettingsWindow final : public GUI::Window {
    C_OBJECT(MouseSettingsWindow)
public:
    virtual ~MouseSettingsWindow() override;

private:
    MouseSettingsWindow();

    void update_window_server();
    void reset_default_values();

    RefPtr<GUI::HorizontalSlider> m_speed_slider;
    RefPtr<GUI::Label> m_speed_label;
    RefPtr<GUI::SpinBox> m_scroll_length_spinbox;
    RefPtr<GUI::HorizontalSlider> m_double_click_speed_slider;
    RefPtr<GUI::Label> m_double_click_speed_label;
    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_apply_button;
    RefPtr<GUI::Button> m_reset_button;
};
