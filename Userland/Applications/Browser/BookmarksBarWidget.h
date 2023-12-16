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

    enum class Open {
        InNewTab,
        InSameTab,
        InNewWindow
    };

    Function<void(ByteString const& url, Open)> on_bookmark_click;
    Function<void(ByteString const&, ByteString const&)> on_bookmark_hover;
    Function<void()> on_bookmark_change;

    bool contains_bookmark(StringView url);
    ErrorOr<void> remove_bookmark(StringView url);
    ErrorOr<void> add_bookmark(StringView url, StringView title);
    ErrorOr<void> edit_bookmark(StringView url);

    virtual Optional<GUI::UISize> calculated_min_size() const override
    {
        // Large enough to fit the `m_additional` button.
        return GUI::UISize(20, 20);
    }

private:
    BookmarksBarWidget(ByteString const&, bool enabled);

    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    // ^GUI::Widget
    virtual void resize_event(GUI::ResizeEvent&) override;

    void update_content_size();

    ErrorOr<void> update_model(Vector<JsonValue>& values, Function<ErrorOr<void>(GUI::JsonArrayModel& model, Vector<JsonValue>&& values)> perform_model_change);

    RefPtr<GUI::Model> m_model;
    RefPtr<GUI::Button> m_additional;
    RefPtr<GUI::Widget> m_separator;
    RefPtr<GUI::Menu> m_additional_menu;

    RefPtr<GUI::Menu> m_context_menu;
    RefPtr<GUI::Action> m_context_menu_default_action;
    ByteString m_context_menu_url;

    Vector<NonnullRefPtr<GUI::Button>> m_bookmarks;

    int m_last_visible_index { -1 };
};

}
