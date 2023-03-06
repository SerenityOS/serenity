/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Action.h>

namespace Browser {

class WindowActions {
public:
    static WindowActions& the();

    WindowActions(GUI::Window&);

    Function<void()> on_create_new_tab;
    Function<void()> on_create_new_window;
    Function<void()> on_next_tab;
    Function<void()> on_previous_tab;
    Vector<Function<void()>> on_tabs;
    Function<void(GUI::Action&)> on_show_bookmarks_bar;
    Function<void(GUI::Action&)> on_vertical_tabs;

    GUI::Action& create_new_tab_action() { return *m_create_new_tab_action; }
    GUI::Action& create_new_window_action() { return *m_create_new_window_action; }
    GUI::Action& next_tab_action() { return *m_next_tab_action; }
    GUI::Action& previous_tab_action() { return *m_previous_tab_action; }
    GUI::Action& about_action() { return *m_about_action; }
    GUI::Action& show_bookmarks_bar_action() { return *m_show_bookmarks_bar_action; }
    GUI::Action& vertical_tabs_action() { return *m_vertical_tabs_action; }

private:
    RefPtr<GUI::Action> m_create_new_tab_action;
    RefPtr<GUI::Action> m_create_new_window_action;
    RefPtr<GUI::Action> m_next_tab_action;
    RefPtr<GUI::Action> m_previous_tab_action;
    Vector<NonnullRefPtr<GUI::Action>> m_tab_actions;
    RefPtr<GUI::Action> m_about_action;
    RefPtr<GUI::Action> m_show_bookmarks_bar_action;
    RefPtr<GUI::Action> m_vertical_tabs_action;
};

}
