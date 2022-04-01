/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    Function<void(String const& url, unsigned modifiers)> on_bookmark_click;
    Function<void(String const&, String const&)> on_bookmark_hover;

    bool contains_bookmark(String const& url);
    bool remove_bookmark(String const& url);
    bool add_bookmark(String const& url, String const& title);
    bool edit_bookmark(String const& url);

private:
    BookmarksBarWidget(String const&, bool enabled);

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
