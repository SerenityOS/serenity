/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include <LibGUI/Forward.h>
#include <LibGUI/Model.h>
#include <LibGUI/Widget.h>

namespace Browser {

class BookmarksBarWidget final
    : public GUI::Widget
    , private GUI::ModelClient {
    C_OBJECT(BookmarksBarWidget);

public:
    static BookmarksBarWidget& the();

    virtual ~BookmarksBarWidget() override;

    void set_model(RefPtr<GUI::Model>);
    GUI::Model* model() { return m_model.ptr(); }
    const GUI::Model* model() const { return m_model.ptr(); }

    Function<void(const String& url, unsigned modifiers)> on_bookmark_click;
    Function<void(const String&, const String&)> on_bookmark_hover;

    bool contains_bookmark(const String& url);
    bool remove_bookmark(const String& url);
    bool add_bookmark(const String& url, const String& title);

private:
    BookmarksBarWidget(const String&, bool enabled);

    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    // ^GUI::Widget
    virtual void resize_event(GUI::ResizeEvent&) override;

    void update_content_size();

    RefPtr<GUI::Model> m_model;
    RefPtr<GUI::Button> m_additional;
    RefPtr<GUI::Widget> m_separator;
    RefPtr<GUI::Menu> m_additional_menu;

    RefPtr<GUI::Menu> m_context_menu;
    RefPtr<GUI::Action> m_context_menu_default_action;
    String m_context_menu_url;

    NonnullRefPtrVector<GUI::Button> m_bookmarks;

    int m_last_visible_index { -1 };
};

}
