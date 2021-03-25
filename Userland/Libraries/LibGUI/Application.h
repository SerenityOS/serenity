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

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Shortcut.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Point.h>

namespace GUI {

class Application : public Core::Object {
    C_OBJECT(Application);

public:
    static Application* the();

    ~Application();

    int exec();
    void quit(int = 0);

    Action* action_for_key_event(const KeyEvent&);

    void register_global_shortcut_action(Badge<Action>, Action&);
    void unregister_global_shortcut_action(Badge<Action>, Action&);

    void show_tooltip(String, const Widget* tooltip_source_widget);
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

private:
    Application(int argc, char** argv);

    void tooltip_show_timer_did_fire();
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
    bool m_dnd_debugging_enabled { false };
    String m_invoked_as;
    Vector<String> m_args;
    WeakPtr<Widget> m_drag_hovered_widget;
    WeakPtr<Widget> m_pending_drop_widget;
};

}
