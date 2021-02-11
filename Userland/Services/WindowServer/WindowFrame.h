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

#include <AK/Forward.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/RefPtr.h>
#include <LibCore/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/WindowTheme.h>

namespace WindowServer {

class Button;
class MouseEvent;
class Window;

class WindowFrame {
public:
    static void reload_config();

    WindowFrame(Window&);
    ~WindowFrame();

    Gfx::IntRect rect() const;
    Gfx::IntRect render_rect() const;
    void paint(Gfx::Painter&, const Gfx::IntRect&);
    void render(Gfx::Painter&);
    void render_to_cache();
    void on_mouse_event(const MouseEvent&);
    void notify_window_rect_changed(const Gfx::IntRect& old_rect, const Gfx::IntRect& new_rect);
    void invalidate_title_bar();
    void invalidate(Gfx::IntRect relative_rect);

    Gfx::IntRect title_bar_rect() const;
    Gfx::IntRect title_bar_icon_rect() const;
    Gfx::IntRect title_bar_text_rect() const;

    void did_set_maximized(Badge<Window>, bool);

    void layout_buttons();
    void set_button_icons();

    void start_flash_animation();

    bool has_alpha_channel() const { return m_has_alpha_channel || frame_has_alpha(); }
    void set_has_alpha_channel(bool value) { m_has_alpha_channel = value; }

    void set_opacity(float);
    float opacity() const { return m_opacity; }

    bool is_opaque() const
    {
        if (opacity() < 1.0f)
            return false;
        if (has_alpha_channel())
            return false;
        return true;
    }

    void set_dirty(bool re_render_shadow = false)
    {
        m_dirty = true;
        m_shadow_dirty |= re_render_shadow;
    }

    void theme_changed()
    {
        m_dirty = m_shadow_dirty = true;
        m_top_bottom = nullptr;
        m_left_right = nullptr;
        m_bottom_y = m_right_x = 0;

        layout_buttons();
        set_button_icons();
    }

private:
    void paint_simple_rect_shadow(Gfx::Painter&, const Gfx::IntRect&, const Gfx::Bitmap&) const;
    void paint_notification_frame(Gfx::Painter&);
    void paint_normal_frame(Gfx::Painter&);
    Gfx::Bitmap* window_shadow() const;
    bool frame_has_alpha() const;
    Gfx::IntRect inflated_for_shadow(const Gfx::IntRect&) const;
    Gfx::Bitmap* inflate_for_shadow(Gfx::IntRect&, Gfx::IntPoint&) const;

    Gfx::WindowTheme::WindowState window_state_for_theme() const;

    Window& m_window;
    NonnullOwnPtrVector<Button> m_buttons;
    Button* m_close_button { nullptr };
    Button* m_maximize_button { nullptr };
    Button* m_minimize_button { nullptr };

    Gfx::IntPoint m_shadow_offset {};

    RefPtr<Gfx::Bitmap> m_top_bottom;
    RefPtr<Gfx::Bitmap> m_left_right;
    int m_bottom_y { 0 }; // y-offset in m_top_bottom for the bottom half
    int m_right_x { 0 };  // x-offset in m_left_right for the right half

    RefPtr<Core::Timer> m_flash_timer;
    size_t m_flash_counter { 0 };
    float m_opacity { 1 };
    bool m_has_alpha_channel { false };
    bool m_shadow_dirty { false };
    bool m_dirty { false };
};

}
