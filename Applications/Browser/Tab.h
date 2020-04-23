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

#include "History.h"
#include <AK/URL.h>
#include <LibGUI/Widget.h>
#include <LibWeb/Forward.h>

class BookmarksBarWidget;

namespace Browser {

class Tab final : public GUI::Widget {
    C_OBJECT(Tab);

public:
    virtual ~Tab() override;

    void load(const URL&);

    void did_become_active();

    Function<void(String)> on_title_change;
    Function<void(Tab&)> on_tab_close_request;

    const String& title() const { return m_title; }

private:
    Tab();

    void update_actions();
    void update_bookmark_button(const String& url);

    History<URL> m_history;
    RefPtr<Web::HtmlView> m_html_widget;
    RefPtr<GUI::Action> m_go_back_action;
    RefPtr<GUI::Action> m_go_forward_action;
    RefPtr<GUI::TextBox> m_location_box;
    RefPtr<BookmarksBarWidget> m_bookmarks_bar;
    RefPtr<GUI::Button> m_bookmark_button;
    RefPtr<GUI::Window> m_dom_inspector_window;
    RefPtr<GUI::StatusBar> m_statusbar;
    RefPtr<GUI::MenuBar> m_menubar;

    String m_title;
    bool m_should_push_loads_to_history { true };
};

}
