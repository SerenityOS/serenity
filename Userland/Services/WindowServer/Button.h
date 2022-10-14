/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Weakable.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <WindowServer/MultiScaleBitmaps.h>

namespace WindowServer {

class MouseEvent;
class Screen;
class WindowFrame;

class Button : public Weakable<Button> {
public:
    Button(WindowFrame&, Function<void(Button&)>&& on_click_handler);
    ~Button();

    Gfx::IntRect relative_rect() const { return m_relative_rect; }
    void set_relative_rect(Gfx::IntRect const& rect) { m_relative_rect = rect; }

    Gfx::IntRect rect() const { return { {}, m_relative_rect.size() }; }
    Gfx::IntRect screen_rect() const;

    void paint(Screen&, Gfx::Painter&);

    void on_mouse_event(MouseEvent const&);

    Function<void(Button&)> on_click;
    Function<void(Button&)> on_secondary_click;
    Function<void(Button&)> on_middle_click;

    bool is_visible() const { return m_visible; }

    struct Icon {
        RefPtr<MultiScaleBitmaps> bitmap { nullptr };
        RefPtr<MultiScaleBitmaps> hover_bitmap { nullptr };
    };

    void set_icon(Icon const& icon) { m_icon = icon; }

    enum class Style {
        Normal,
        IconOnly
    };

    void set_style(Style style) { m_style = style; }

private:
    WindowFrame& m_frame;
    Gfx::IntRect m_relative_rect;
    Icon m_icon;
    bool m_pressed { false };
    bool m_visible { true };
    bool m_hovered { false };
    Style m_style { Style::Normal };
};

}
