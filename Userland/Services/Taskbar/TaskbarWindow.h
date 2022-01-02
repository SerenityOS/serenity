/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WindowList.h"
#include <LibConfig/Listener.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Alignment.h>
#include <LibGfx/ShareableBitmap.h>
#include <WindowServer/ScreenLayout.h>

class TaskbarWindow final : public GUI::Window
    , public Config::Listener {
    C_OBJECT(TaskbarWindow);

public:
    virtual ~TaskbarWindow() override;

    static int taskbar_height() { return 27; }
    static int taskbar_width() { return 40; }
    static int taskbar_icon_size() { return 16; }

private:
    explicit TaskbarWindow(Gfx::Alignment location, NonnullRefPtr<GUI::Menu> start_menu);
    static void show_desktop_button_clicked(unsigned);
    void set_quick_launch_button_data(GUI::Button&, String const&, NonnullRefPtr<Desktop::AppFile>);
    void on_screen_rects_change(const Vector<Gfx::IntRect, 4>&, size_t);
    NonnullRefPtr<GUI::Button> create_button(const WindowIdentifier&);
    void add_window_button(::Window&, const WindowIdentifier&);
    void remove_window_button(::Window&, bool);
    void update_window_button(::Window&, bool);
    ::Window* find_window_owner(::Window&) const;

    virtual void event(Core::Event&) override;
    virtual void wm_event(GUI::WMEvent&) override;
    virtual void screen_rects_change_event(GUI::ScreenRectsChangeEvent&) override;
    virtual void config_string_did_change(String const&, String const&, String const&, String const&) override;

    void update_applet_area();

    bool is_window_on_current_workspace(::Window&) const;
    void workspace_change_event(unsigned, unsigned);

    void set_start_button_font(Gfx::Font const&);

    NonnullRefPtr<GUI::Menu> m_start_menu;
    RefPtr<GUI::Widget> m_task_button_container;
    RefPtr<Gfx::Bitmap> m_default_icon;

    Gfx::IntSize m_applet_area_size;
    RefPtr<GUI::Frame> m_applet_area_container;
    RefPtr<GUI::Button> m_start_button;
    RefPtr<GUI::Button> m_show_desktop_button;

    RefPtr<Desktop::AppFile> m_assistant_app_file;

    Gfx::Orientation m_orientation {};
    Gfx::Alignment m_location {};
    unsigned m_current_workspace_row { 0 };
    unsigned m_current_workspace_column { 0 };
};
