/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ClockWidget.h"
#include "WindowList.h"
#include <AK/NonnullRefPtr.h>
#include <LibConfig/Listener.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/ShareableBitmap.h>
#include <Services/Taskbar/QuickLaunchWidget.h>
#include <Services/WindowServer/ScreenLayout.h>

class TaskbarWindow final : public GUI::Window
    , public Config::Listener {
    C_OBJECT(TaskbarWindow);

public:
    static ErrorOr<NonnullRefPtr<TaskbarWindow>> create();
    virtual ~TaskbarWindow() override = default;

    static int taskbar_height() { return 27; }
    static int taskbar_icon_size() { return 16; }

    virtual void config_string_did_change(StringView, StringView, StringView, StringView) override;
    virtual void add_system_menu(NonnullRefPtr<GUI::Menu> system_menu);

private:
    explicit TaskbarWindow();
    static void show_desktop_button_clicked(unsigned);
    static void toggle_show_desktop();
    void on_screen_rects_change(Vector<Gfx::IntRect, 4> const&, size_t);
    NonnullRefPtr<GUI::Button> create_button(WindowIdentifier const&);
    void add_window_button(::Window&, WindowIdentifier const&);
    void remove_window_button(::Window&, bool);
    void update_window_button(::Window&, bool);

    ErrorOr<void> populate_taskbar();
    ErrorOr<void> load_assistant();

    virtual void event(Core::Event&) override;
    virtual void wm_event(GUI::WMEvent&) override;
    virtual void screen_rects_change_event(GUI::ScreenRectsChangeEvent&) override;

    void update_applet_area();

    bool is_window_on_current_workspace(::Window&) const;
    void workspace_change_event(unsigned, unsigned);

    void set_start_button_font(Gfx::Font const&);

    RefPtr<GUI::Menu> m_system_menu;
    RefPtr<GUI::Widget> m_task_button_container;
    RefPtr<Gfx::Bitmap> m_default_icon;

    Gfx::IntSize m_applet_area_size;
    RefPtr<GUI::Frame> m_applet_area_container;
    RefPtr<GUI::Button> m_start_button;
    RefPtr<GUI::Button> m_show_desktop_button;
    RefPtr<Taskbar::QuickLaunchWidget> m_quick_launch;
    RefPtr<Taskbar::ClockWidget> m_clock_widget;

    RefPtr<Desktop::AppFile> m_assistant_app_file;

    unsigned m_current_workspace_row { 0 };
    unsigned m_current_workspace_column { 0 };
};
