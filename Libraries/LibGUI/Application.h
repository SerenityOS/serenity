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
#include <LibCore/Forward.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Shortcut.h>
#include <LibGfx/Forward.h>

namespace GUI {

class Application {
public:
    static Application& the();
    Application(int argc, char** argv);
    ~Application();

    int exec();
    void quit(int = 0);

    void set_menubar(OwnPtr<MenuBar>&&);
    Action* action_for_key_event(const KeyEvent&);

    void register_global_shortcut_action(Badge<Action>, Action&);
    void unregister_global_shortcut_action(Badge<Action>, Action&);

    void show_tooltip(const StringView&, const Gfx::Point& screen_location);
    void hide_tooltip();

    bool quit_when_last_window_deleted() const { return m_quit_when_last_window_deleted; }
    void set_quit_when_last_window_deleted(bool b) { m_quit_when_last_window_deleted = b; }

    void did_create_window(Badge<Window>);
    void did_delete_last_window(Badge<Window>);

    const String& invoked_as() const { return m_invoked_as; }
    const Vector<String>& args() const { return m_args; }

    Gfx::Palette palette() const;
    void set_palette(const Gfx::Palette&);

    void set_system_palette(SharedBuffer&);

private:
    OwnPtr<Core::EventLoop> m_event_loop;
    OwnPtr<MenuBar> m_menubar;
    RefPtr<Gfx::PaletteImpl> m_palette;
    RefPtr<Gfx::PaletteImpl> m_system_palette;
    HashMap<Shortcut, Action*> m_global_shortcut_actions;
    class TooltipWindow;
    RefPtr<TooltipWindow> m_tooltip_window;
    bool m_quit_when_last_window_deleted { true };
    String m_invoked_as;
    Vector<String> m_args;
};

}
