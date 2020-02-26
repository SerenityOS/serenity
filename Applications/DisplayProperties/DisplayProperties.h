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

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Color.h>
#include <LibGfx/Size.h>

class DisplayPropertiesWidget final {
public:
    enum class ButtonOperations {
        Ok,
        Apply,
        Cancel,
    };

    enum TabIndices {
        Wallpaper,
        Settings
    };

public:
    DisplayPropertiesWidget();

    // Apply the settings to the Window Server
    void send_settings_to_window_server(int tabIndex);
    void create_frame();

    const GUI::Widget* root_widget() const { return m_root_widget; }
    GUI::Widget* root_widget() { return m_root_widget; }

private:
    void create_wallpaper_list();
    void create_resolution_list();
    void create_root_widget();

private:
    String m_wallpaper_path;
    RefPtr<Core::ConfigFile> m_wm_config;
    RefPtr<GUI::Widget> m_root_widget;
    Vector<Gfx::Size> m_resolutions;
    Vector<String> m_wallpapers;
    RefPtr<GUI::Label> m_wallpaper_preview;

    Gfx::Size m_selected_resolution;
    String m_selected_wallpaper;
};
