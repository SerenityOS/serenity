/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BookmarksBarWidget.h"
#include "Tab.h"
#include "WindowActions.h"
#include <LibConfig/Listener.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Window.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWebView/Forward.h>

namespace Browser {

class Tab;

class BrowserWindow final : public GUI::Window
    , public Config::Listener {
    C_OBJECT(BrowserWindow);

public:
    virtual ~BrowserWindow() override = default;

    GUI::TabWidget& tab_widget();
    Tab& active_tab();
    Tab& create_new_tab(URL::URL const&, Web::HTML::ActivateTab activate);
    void create_new_window(URL::URL const&);

    GUI::Action& go_back_action() { return *m_go_back_action; }
    GUI::Action& go_forward_action() { return *m_go_forward_action; }
    GUI::Action& go_home_action() { return *m_go_home_action; }
    GUI::Action& reload_action() { return *m_reload_action; }
    GUI::Action& copy_selection_action() { return *m_copy_selection_action; }
    GUI::Action& paste_action() { return *m_paste_action; }
    GUI::Action& select_all_action() { return *m_select_all_action; }
    GUI::Action& view_source_action() { return *m_view_source_action; }
    GUI::Action& inspect_dom_tree_action() { return *m_inspect_dom_tree_action; }
    GUI::Action& inspect_dom_node_action() { return *m_inspect_dom_node_action; }

    void content_filters_changed();
    void autoplay_allowlist_changed();
    void proxy_mappings_changed();
    void update_zoom_menu();

    void broadcast_window_position(Gfx::IntPoint);
    void broadcast_window_size(Gfx::IntSize);

private:
    BrowserWindow(WebView::CookieJar&, Vector<URL::URL> const&, StringView const);

    void build_menus(StringView const);
    ErrorOr<void> load_search_engines(GUI::Menu& settings_menu);
    void set_window_title_for_tab(Tab const&);

    virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value) override;
    virtual void config_bool_did_change(StringView domain, StringView group, StringView key, bool value) override;

    virtual void event(Core::Event&) override;

    void update_displayed_zoom_level();

    void show_task_manager_window();
    void close_task_manager_window();

    RefPtr<GUI::Action> m_go_back_action;
    RefPtr<GUI::Action> m_go_forward_action;
    RefPtr<GUI::Action> m_go_home_action;
    RefPtr<GUI::Action> m_reload_action;
    RefPtr<GUI::Action> m_copy_selection_action;
    RefPtr<GUI::Action> m_paste_action;
    RefPtr<GUI::Action> m_select_all_action;
    RefPtr<GUI::Action> m_view_source_action;
    RefPtr<GUI::Action> m_inspect_dom_tree_action;
    RefPtr<GUI::Action> m_inspect_dom_node_action;
    RefPtr<GUI::Action> m_task_manager_action;

    RefPtr<GUI::Menu> m_zoom_menu;

    WebView::CookieJar& m_cookie_jar;
    WindowActions m_window_actions;
    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<BookmarksBarWidget> m_bookmarks_bar;

    // FIXME: This should be owned at a higher level in case we have multiple browser windows
    RefPtr<GUI::Window> m_task_manager_window;

    GUI::ActionGroup m_user_agent_spoof_actions;
    GUI::ActionGroup m_search_engine_actions;
    GUI::ActionGroup m_color_scheme_actions;
    RefPtr<GUI::Action> m_disable_user_agent_spoofing;
    RefPtr<GUI::Action> m_disable_search_engine_action;
    RefPtr<GUI::Action> m_change_homepage_action;
};

}
