/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Menu.h"
#include "Menubar.h"
#include "Window.h"
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>

namespace WindowServer {

class GlobalMenu final : public Core::EventReceiver {
    C_OBJECT(GlobalMenu);

public:
    ~GlobalMenu();

    static GlobalMenu& the();

    void set_enabled(bool enabled);
    bool enabled() const { return m_enabled; }

    void set_rect(Gfx::IntRect const&);
    Gfx::IntRect rect()
    {
        return m_window ? m_window->rect() : Gfx::IntRect(0, 0, 0, 0);
    }

    Window* window() { return m_window; }
    Window const* window() const { return m_window; }

    void handle_active_window_changed();
    void handle_active_window_closed();

    bool has_active_menu();
    void open_menubar_menu(Menu* menu);

    void invalidate();

protected:
    virtual void event(Core::Event&) override;

private:
    GlobalMenu();

    void invalidate(Gfx::IntRect rect);

    int paint_title(bool paint = true);

    void handle_mouse_event(MouseEvent const& event);
    void handle_menu_mouse_event(Menu* menu, MouseEvent const& event);

    void paint();

    RefPtr<Window> m_window { nullptr };
    WeakPtr<Window> m_active_window;
    OwnPtr<Gfx::Painter> m_painter;

    RefPtr<Gfx::Bitmap> m_ladyball;

    bool m_enabled { false };
    bool m_hovering { false };
    bool m_dirty { true };
};
}
