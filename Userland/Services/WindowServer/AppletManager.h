/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <WindowServer/Window.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

class AppletManager : public Core::Object {
    C_OBJECT(AppletManager)
public:
    ~AppletManager();

    static AppletManager& the();

    virtual void event(Core::Event&) override;

    void add_applet(Window& applet);
    void remove_applet(Window& applet);
    void draw();
    void invalidate_applet(const Window& applet, const Gfx::IntRect& rect);
    void relayout();

    void set_position(const Gfx::IntPoint&);

    Window* window() { return m_window; }
    const Window* window() const { return m_window; }

    void did_change_theme();

private:
    AppletManager();

    void repaint();
    void draw_applet(const Window& applet);
    void set_hovered_applet(Window*);

    Vector<WeakPtr<Window>> m_applets;
    RefPtr<Window> m_window;
    WeakPtr<Window> m_hovered_applet;
};

}
