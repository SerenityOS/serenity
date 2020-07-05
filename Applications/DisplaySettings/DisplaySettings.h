/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
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

#include "MonitorWidget.h"
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>

class DisplaySettingsWidget : public GUI::Widget {
    C_OBJECT(DisplaySettingsWidget);

public:
    DisplaySettingsWidget();

    GUI::Widget* root_widget() { return m_root_widget; }

private:
    void create_frame();
    void create_wallpaper_list();
    void create_resolution_list();
    void load_current_settings();
    void send_settings_to_window_server(); // Apply the settings to the Window Server

    Vector<String> m_wallpapers;
    Vector<String> m_modes;
    Vector<Gfx::IntSize> m_resolutions;

    RefPtr<GUI::Widget> m_root_widget;
    RefPtr<MonitorWidget> m_monitor_widget;
    RefPtr<GUI::ComboBox> m_wallpaper_combo;
    RefPtr<GUI::ComboBox> m_mode_combo;
    RefPtr<GUI::ComboBox> m_resolution_combo;
    RefPtr<GUI::ColorInput> m_color_input;
};
