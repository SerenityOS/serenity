/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Function.h>
#include <AK/String.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Rect.h>

namespace GUI {

class Desktop {
public:
    static Desktop& the();
    Desktop();

    void set_background_color(const StringView& background_color);

    void set_wallpaper_mode(const StringView& mode);

    String wallpaper() const;
    bool set_wallpaper(const StringView& path, bool save_config = true);

    enum class RectAlignment {
        Center = 0,
        LeftOrTop,
        RightOrBottom
    };
    struct SideWithAlignment {
        Gfx::IntRect::Side side { Gfx::IntRect::Side::None };
        RectAlignment alignment { RectAlignment::Center };
    };
    Gfx::IntRect calculate_ideal_visible_rect(const Gfx::IntSize&, const Gfx::IntRect&, const Vector<SideWithAlignment>& = {}, bool = true) const;

    u32 primary_screen() const { return m_primary_screen; }
    const Vector<Gfx::IntRect, 1>& rects() const { return m_rects; }
    Gfx::IntRect rect() const { return !m_rects.is_empty() ? m_rects[m_primary_screen] : Gfx::IntRect {}; }

    int taskbar_height() const { return 28; }
    int menubar_height() const { return 19; }

    void did_receive_screen_rects(Badge<WindowServerConnection>, const Vector<Gfx::IntRect>&, u32);

    Function<void(const Vector<Gfx::IntRect>&, u32)> on_rects_change;

private:
    Gfx::IntRect get_screen_rect(size_t) const;
    int calculate_screen_area(size_t, const Gfx::IntRect&, bool) const;
    size_t find_best_screen(const Gfx::IntRect&, bool) const;

    Vector<Gfx::IntRect, 1> m_rects;
    u32 m_primary_screen { 0 };
};

}
