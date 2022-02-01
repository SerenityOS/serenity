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

namespace Browser {

class CookieJar;
class Tab;

class BrowserWindow final : public GUI::Window
    , public Config::Listener {
    C_OBJECT(BrowserWindow);

public:
    virtual ~BrowserWindow() override = default;

    GUI::TabWidget& tab_widget();
    Tab& active_tab();
    void create_new_tab(URL, bool activate);

    GUI::Action& go_back_action() { return *m_go_back_action; }
    GUI::Action& go_forward_action() { return *m_go_forward_action; }
    GUI::Action& go_home_action() { return *m_go_home_action; }
    GUI::Action& reload_action() { return *m_reload_action; }
    GUI::Action& copy_selection_action() { return *m_copy_selection_action; }
    GUI::Action& select_all_action() { return *m_select_all_action; }
    GUI::Action& view_source_action() { return *m_view_source_action; }
    GUI::Action& inspect_dom_tree_action() { return *m_inspect_dom_tree_action; }
    GUI::Action& inspect_dom_node_action() { return *m_inspect_dom_node_action; }

    void content_filters_changed();

private:
    explicit BrowserWindow(CookieJar&, URL);

    void build_menus();
    ErrorOr<void> load_search_engines(GUI::Menu& settings_menu);
    void set_window_title_for_tab(Tab const&);

    virtual void config_string_did_change(String const& domain, String const& group, String const& key, String const& value) override;
    virtual void config_bool_did_change(String const& domain, String const& group, String const& key, bool value) override;

    RefPtr<GUI::Action> m_go_back_action;
    RefPtr<GUI::Action> m_go_forward_action;
    RefPtr<GUI::Action> m_go_home_action;
    RefPtr<GUI::Action> m_reload_action;
    RefPtr<GUI::Action> m_copy_selection_action;
    RefPtr<GUI::Action> m_select_all_action;
    RefPtr<GUI::Action> m_view_source_action;
    RefPtr<GUI::Action> m_inspect_dom_tree_action;
    RefPtr<GUI::Action> m_inspect_dom_node_action;

    CookieJar& m_cookie_jar;
    WindowActions m_window_actions;
    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<BookmarksBarWidget> m_bookmarks_bar;

    GUI::ActionGroup m_user_agent_spoof_actions;
    GUI::ActionGroup m_search_engine_actions;
    GUI::ActionGroup m_color_scheme_actions;
    RefPtr<GUI::Action> m_disable_user_agent_spoofing;
    RefPtr<GUI::Action> m_disable_search_engine_action;
    RefPtr<GUI::Action> m_change_homepage_action;
};

}
