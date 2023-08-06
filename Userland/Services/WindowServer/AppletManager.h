/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <WindowServer/Window.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

class AppletManager : public Core::EventReceiver {
    C_OBJECT(AppletManager)
public:
    ~AppletManager() = default;

    static AppletManager& the();

    virtual void event(Core::Event&) override;

    void add_applet(Window& applet);
    void remove_applet(Window& applet);
    void draw();
    void invalidate_applet(Window const& applet, Gfx::IntRect const& rect);
    void relayout();

    void set_position(Gfx::IntPoint);

    Window* window() { return m_window; }
    Window const* window() const { return m_window; }

    void did_change_theme();

private:
    AppletManager();

    void repaint();
    void draw_applet(Window const& applet);
    void set_hovered_applet(Window*);

    Vector<WeakPtr<Window>> m_applets;
    RefPtr<Window> m_window;
    WeakPtr<Window> m_hovered_applet;
};

}
