/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WindowActions.h"
#include <Applications/Browser/Browser.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

namespace Browser {

static WindowActions* s_the;

WindowActions& WindowActions::the()
{
    VERIFY(s_the);
    return *s_the;
}

WindowActions::WindowActions(GUI::Window& window)
{
    VERIFY(!s_the);
    s_the = this;
    m_create_new_tab_action = GUI::Action::create(
        "&New Tab", { Mod_Ctrl, Key_T }, g_icon_bag.new_tab, [this](auto&) {
            if (on_create_new_tab)
                on_create_new_tab();
        },
        &window);
    m_create_new_tab_action->set_status_tip("Open a new tab"_string);

    m_create_new_window_action = GUI::Action::create(
        "&New Window", { Mod_Ctrl, Key_N }, g_icon_bag.new_window, [this](auto&) {
            if (on_create_new_window) {
                on_create_new_window();
            }
        },
        &window);
    m_create_new_window_action->set_status_tip("Open a new browser window"_string);

    m_next_tab_action = GUI::Action::create(
        "&Next Tab", { Mod_Ctrl, Key_PageDown }, [this](auto&) {
            if (on_next_tab)
                on_next_tab();
        },
        &window);
    m_next_tab_action->set_status_tip("Switch to the next tab"_string);

    m_previous_tab_action = GUI::Action::create(
        "&Previous Tab", { Mod_Ctrl, Key_PageUp }, [this](auto&) {
            if (on_previous_tab)
                on_previous_tab();
        },
        &window);
    m_previous_tab_action->set_status_tip("Switch to the previous tab"_string);

    for (auto i = 0; i <= 7; ++i) {
        m_tab_actions.append(GUI::Action::create(
            ByteString::formatted("Tab {}", i + 1), { Mod_Ctrl, static_cast<KeyCode>(Key_1 + i) }, [this, i](auto&) {
                if (on_tabs[i])
                    on_tabs[i]();
            },
            &window));
        m_tab_actions.last()->set_status_tip(String::formatted("Switch to tab {}", i + 1).release_value_but_fixme_should_propagate_errors());
    }
    m_tab_actions.append(GUI::Action::create(
        "Last tab", { Mod_Ctrl, Key_9 }, [this](auto&) {
            if (on_tabs[8])
                on_tabs[8]();
        },
        &window));
    m_tab_actions.last()->set_status_tip("Switch to last tab"_string);

    m_about_action = GUI::CommonActions::make_about_action("Browser"_string, GUI::Icon::default_icon("app-browser"sv), &window);

    m_show_bookmarks_bar_action = GUI::Action::create_checkable(
        "&Bookmarks Bar", { Mod_Ctrl, Key_B },
        [this](auto& action) {
            if (on_show_bookmarks_bar)
                on_show_bookmarks_bar(action);
        },
        &window);
    m_show_bookmarks_bar_action->set_status_tip("Show/hide the bookmarks bar"_string);

    m_vertical_tabs_action = GUI::Action::create_checkable(
        "&Vertical Tabs", { Mod_Ctrl, Key_Comma },
        [this](auto& action) {
            if (on_vertical_tabs)
                on_vertical_tabs(action);
        },
        &window);
    m_vertical_tabs_action->set_status_tip("Enable/Disable vertical tabs"_string);
}

}
