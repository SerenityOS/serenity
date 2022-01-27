/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Shortcut.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Point.h>
#include <LibMain/Main.h>

namespace GUI {

class Application : public Core::Object {
    C_OBJECT(Application);

public:
    static Application* the();

    ~Application();

    static bool in_teardown();

    int exec();
    void quit(int = 0);

    Action* action_for_key_event(const KeyEvent&);

    void register_global_shortcut_action(Badge<Action>, Action&);
    void unregister_global_shortcut_action(Badge<Action>, Action&);

    void show_tooltip(String, const Widget* tooltip_source_widget);
    void show_tooltip_immediately(String, const Widget* tooltip_source_widget);
    void hide_tooltip();
    Widget* tooltip_source_widget() { return m_tooltip_source_widget; };

    bool quit_when_last_window_deleted() const { return m_quit_when_last_window_deleted; }
    void set_quit_when_last_window_deleted(bool b) { m_quit_when_last_window_deleted = b; }

    void did_create_window(Badge<Window>);
    void did_delete_last_window(Badge<Window>);

    const String& invoked_as() const { return m_invoked_as; }
    const Vector<String>& args() const { return m_args; }

    Gfx::Palette palette() const;
    void set_palette(const Gfx::Palette&);

    void set_system_palette(Core::AnonymousBuffer&);

    bool focus_debugging_enabled() const { return m_focus_debugging_enabled; }
    bool hover_debugging_enabled() const { return m_hover_debugging_enabled; }
    bool dnd_debugging_enabled() const { return m_dnd_debugging_enabled; }

    Core::EventLoop& event_loop() { return *m_event_loop; }

    Window* active_window() { return m_active_window; }
    const Window* active_window() const { return m_active_window; }

    void window_did_become_active(Badge<Window>, Window&);
    void window_did_become_inactive(Badge<Window>, Window&);

    Widget* drag_hovered_widget() { return m_drag_hovered_widget.ptr(); }
    const Widget* drag_hovered_widget() const { return m_drag_hovered_widget.ptr(); }

    Widget* pending_drop_widget() { return m_pending_drop_widget.ptr(); }
    const Widget* pending_drop_widget() const { return m_pending_drop_widget.ptr(); }

    void set_drag_hovered_widget(Badge<Window>, Widget* widget, const Gfx::IntPoint& position = {}, Vector<String> mime_types = {})
    {
        set_drag_hovered_widget_impl(widget, position, move(mime_types));
    }
    void notify_drag_cancelled(Badge<WindowServerConnection>);

    Function<void(Action&)> on_action_enter;
    Function<void(Action&)> on_action_leave;

    auto const& global_shortcut_actions(Badge<GUI::CommandPalette>) const { return m_global_shortcut_actions; }

private:
    Application(int argc, char** argv, Core::EventLoop::MakeInspectable = Core::EventLoop::MakeInspectable::No);
    Application(Main::Arguments const& arguments, Core::EventLoop::MakeInspectable inspectable = Core::EventLoop::MakeInspectable::No)
        : Application(arguments.argc, arguments.argv, inspectable)
    {
    }

    virtual void event(Core::Event&) override;

    void request_tooltip_show();
    void tooltip_hide_timer_did_fire();

    void set_drag_hovered_widget_impl(Widget*, const Gfx::IntPoint& = {}, Vector<String> = {});
    void set_pending_drop_widget(Widget*);

    OwnPtr<Core::EventLoop> m_event_loop;
    RefPtr<Gfx::PaletteImpl> m_palette;
    RefPtr<Gfx::PaletteImpl> m_system_palette;
    HashMap<Shortcut, Action*> m_global_shortcut_actions;
    class TooltipWindow;
    RefPtr<Core::Timer> m_tooltip_show_timer;
    RefPtr<Core::Timer> m_tooltip_hide_timer;
    RefPtr<TooltipWindow> m_tooltip_window;
    RefPtr<Widget> m_tooltip_source_widget;
    WeakPtr<Window> m_active_window;
    bool m_quit_when_last_window_deleted { true };
    bool m_focus_debugging_enabled { false };
    bool m_hover_debugging_enabled { false };
    bool m_dnd_debugging_enabled { false };
    String m_invoked_as;
    Vector<String> m_args;
    WeakPtr<Widget> m_drag_hovered_widget;
    WeakPtr<Widget> m_pending_drop_widget;
};

}
