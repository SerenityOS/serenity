/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "History.h"
#include <AK/URL.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Widget.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibHTTP/HttpJob.h>
#include <LibWeb/Forward.h>

namespace Web {
class OutOfProcessWebView;
class WebViewHooks;
}

namespace Browser {

class BrowserWindow;
class InspectorWidget;
class ConsoleWidget;

class Tab final : public GUI::Widget {
    C_OBJECT(Tab);

    // FIXME: This should go away eventually.
    friend class BrowserWindow;

public:
    virtual ~Tab() override;

    URL url() const;

    enum class LoadType {
        Normal,
        HistoryNavigation,
    };

    void load(const URL&, LoadType = LoadType::Normal);
    void reload();
    void go_back(int steps = 1);
    void go_forward(int steps = 1);

    void did_become_active();
    void context_menu_requested(const Gfx::IntPoint& screen_position);

    void action_entered(GUI::Action&);
    void action_left(GUI::Action&);

    Function<void(const String&)> on_title_change;
    Function<void(const URL&)> on_tab_open_request;
    Function<void(Tab&)> on_tab_close_request;
    Function<void(Tab&)> on_tab_close_other_request;
    Function<void(const Gfx::Bitmap&)> on_favicon_change;
    Function<String(const URL&, Web::Cookie::Source source)> on_get_cookie;
    Function<void(const URL&, const Web::Cookie::ParsedCookie& cookie, Web::Cookie::Source source)> on_set_cookie;
    Function<void()> on_dump_cookies;

    enum class InspectorTarget {
        Document,
        HoveredElement
    };
    void show_inspector_window(InspectorTarget);

    void show_console_window();

    const String& title() const { return m_title; }
    const Gfx::Bitmap* icon() const { return m_icon; }

    GUI::AbstractScrollableWidget& view();

private:
    explicit Tab(BrowserWindow&);

    BrowserWindow const& window() const;
    BrowserWindow& window();

    Web::WebViewHooks& hooks();
    void update_actions();
    void bookmark_current_url();
    void update_bookmark_button(const String& url);
    void start_download(const URL& url);
    void view_source(const URL& url, const String& source);

    History m_history;

    RefPtr<Web::OutOfProcessWebView> m_web_content_view;

    RefPtr<GUI::UrlBox> m_location_box;
    RefPtr<GUI::Button> m_bookmark_button;
    RefPtr<InspectorWidget> m_dom_inspector_widget;
    RefPtr<ConsoleWidget> m_console_widget;
    RefPtr<GUI::Statusbar> m_statusbar;
    RefPtr<GUI::ToolbarContainer> m_toolbar_container;

    RefPtr<GUI::Menu> m_link_context_menu;
    RefPtr<GUI::Action> m_link_context_menu_default_action;
    URL m_link_context_menu_url;

    RefPtr<GUI::Menu> m_image_context_menu;
    Gfx::ShareableBitmap m_image_context_menu_bitmap;
    URL m_image_context_menu_url;

    RefPtr<GUI::Menu> m_tab_context_menu;
    RefPtr<GUI::Menu> m_page_context_menu;
    RefPtr<GUI::Menu> m_go_back_context_menu;
    RefPtr<GUI::Menu> m_go_forward_context_menu;
    String m_title;
    RefPtr<const Gfx::Bitmap> m_icon;

    bool m_is_history_navigation { false };
};

URL url_from_user_input(const String& input);

}
