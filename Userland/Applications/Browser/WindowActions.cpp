/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WindowActions.h"
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
        "&New Tab", { Mod_Ctrl, Key_T }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new-tab.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
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
