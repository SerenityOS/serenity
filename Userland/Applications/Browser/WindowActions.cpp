/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
    m_create_new_tab_action->set_status_tip("Open a new tab");

    m_next_tab_action = GUI::Action::create(
        "&Next Tab", { Mod_Ctrl, Key_PageDown }, [this](auto&) {
            if (on_next_tab)
                on_next_tab();
        },
        &window);
    m_next_tab_action->set_status_tip("Switch to the next tab");

    m_previous_tab_action = GUI::Action::create(
        "&Previous Tab", { Mod_Ctrl, Key_PageUp }, [this](auto&) {
            if (on_previous_tab)
                on_previous_tab();
        },
        &window);
    m_previous_tab_action->set_status_tip("Switch to the previous tab");

    for (auto i = 0; i <= 7; ++i) {
        m_tab_actions.append(GUI::Action::create(
            String::formatted("Tab {}", i + 1), { Mod_Ctrl, static_cast<KeyCode>(Key_1 + i) }, [this, i](auto&) {
                if (on_tabs[i])
                    on_tabs[i]();
            },
            &window));
        m_tab_actions.last().set_status_tip(String::formatted("Switch to tab {}", i + 1));
    }
    m_tab_actions.append(GUI::Action::create(
        "Last tab", { Mod_Ctrl, Key_9 }, [this](auto&) {
            if (on_tabs[8])
                on_tabs[8]();
        },
        &window));
    m_tab_actions.last().set_status_tip("Switch to last tab");

    m_about_action = GUI::Action::create(
        "&About Browser", GUI::Icon::default_icon("app-browser").bitmap_for_size(16), [this](const GUI::Action&) {
            if (on_about)
                on_about();
        },
        &window);
    m_about_action->set_status_tip("Show application about box");

    m_show_bookmarks_bar_action = GUI::Action::create_checkable(
        "&Bookmarks Bar", { Mod_Ctrl, Key_B },
        [this](auto& action) {
            if (on_show_bookmarks_bar)
                on_show_bookmarks_bar(action);
        },
        &window);
    m_show_bookmarks_bar_action->set_status_tip("Show/hide the bookmarks bar");
}

}
