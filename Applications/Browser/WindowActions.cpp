/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "WindowActions.h"
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

namespace Browser {

static WindowActions* s_the;

WindowActions& WindowActions::the()
{
    ASSERT(s_the);
    return *s_the;
}

WindowActions::WindowActions(GUI::Window& window)
{
    ASSERT(!s_the);
    s_the = this;
    m_create_new_tab_action = GUI::Action::create(
        "New tab", { Mod_Ctrl, Key_T }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new-tab.png"), [this](auto&) {
            if (on_create_new_tab)
                on_create_new_tab();
        },
        &window);

    m_next_tab_action = GUI::Action::create(
        "Next tab", { Mod_Ctrl, Key_PageDown }, [this](auto&) {
            if (on_next_tab)
                on_next_tab();
        },
        &window);

    m_previous_tab_action = GUI::Action::create(
        "Previous tab", { Mod_Ctrl, Key_PageUp }, [this](auto&) {
            if (on_previous_tab)
                on_previous_tab();
        },
        &window);

    m_about_action = GUI::Action::create(
        "About", [this](const GUI::Action&) {
            if (on_about)
                on_about();
        },
        &window);
    m_show_bookmarks_bar_action = GUI::Action::create_checkable(
        "Show bookmarks bar",
        [this](auto& action) {
            if (on_show_bookmarks_bar)
                on_show_bookmarks_bar(action);
        },
        &window);
}

}
