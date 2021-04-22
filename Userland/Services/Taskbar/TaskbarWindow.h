/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WindowList.h"
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class TaskbarWindow final : public GUI::Window {
    C_OBJECT(TaskbarWindow);

public:
    virtual ~TaskbarWindow() override;

    static int taskbar_height() { return 28; }

private:
    explicit TaskbarWindow(NonnullRefPtr<GUI::Menu> start_menu);
    void create_quick_launch_bar();
    void on_screen_rect_change(const Gfx::IntRect&);
    NonnullRefPtr<GUI::Button> create_button(const WindowIdentifier&);
    void add_window_button(::Window&, const WindowIdentifier&);
    void remove_window_button(::Window&, bool);
    void update_window_button(::Window&, bool);
    ::Window* find_window_owner(::Window&) const;

    virtual void wm_event(GUI::WMEvent&) override;
    virtual void screen_rect_change_event(GUI::ScreenRectChangeEvent&) override;

    void update_applet_area();

    NonnullRefPtr<GUI::Menu> m_start_menu;
    RefPtr<GUI::Widget> m_task_button_container;
    RefPtr<Gfx::Bitmap> m_default_icon;

    Gfx::IntSize m_applet_area_size;
    RefPtr<GUI::Frame> m_applet_area_container;
    RefPtr<GUI::Button> m_start_button;
};
