/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Forward.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class WindowTheme {
public:
    enum class WindowType {
        Normal,
        Notification,
        Other,
    };

    enum class WindowState {
        Active,
        Inactive,
        Highlighted,
        Moving,
    };

    virtual ~WindowTheme();

    static WindowTheme& current();

    virtual void paint_normal_frame(Painter&, WindowState, const IntRect& window_rect, const StringView& title, const Bitmap& icon, const Palette&, const IntRect& leftmost_button_rect) const = 0;
    virtual void paint_notification_frame(Painter&, const IntRect& window_rect, const Palette&, const IntRect& close_button_rect) const = 0;

    virtual IntRect title_bar_rect(WindowType, const IntRect& window_rect, const Palette&) const = 0;
    virtual IntRect title_bar_icon_rect(WindowType, const IntRect& window_rect, const Palette&) const = 0;
    virtual IntRect title_bar_text_rect(WindowType, const IntRect& window_rect, const Palette&) const = 0;

    virtual IntRect frame_rect_for_window(WindowType, const IntRect& window_rect, const Palette&) const = 0;

    virtual Vector<IntRect> layout_buttons(WindowType, const IntRect& window_rect, const Palette&, size_t buttons) const = 0;

protected:
    WindowTheme() { }
};

}
