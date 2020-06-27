/*
 * Copyright (c) 2020-2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include "LibGfx/Bitmap.h"
#include <LibGUI/Widget.h>

class MonitorWidget final : public GUI::Widget {
    C_OBJECT(MonitorWidget);

public:
    MonitorWidget();

    bool set_wallpaper(String path);
    String wallpaper();

    void set_wallpaper_mode(String mode);
    String wallpaper_mode();

    void set_desktop_resolution(Gfx::IntSize resolution);
    Gfx::IntSize desktop_resolution();

    void set_background_color(Gfx::Color background_color);
    Gfx::Color background_color();

private:
    virtual void paint_event(GUI::PaintEvent& event) override;

    Gfx::IntRect m_monitor_rect;
    RefPtr<Gfx::Bitmap> m_monitor_bitmap;

    String m_desktop_wallpaper_path;
    RefPtr<Gfx::Bitmap> m_desktop_wallpaper_bitmap;
    String m_desktop_wallpaper_mode;
    Gfx::IntSize m_desktop_resolution;
    Gfx::Color m_desktop_color;
};
