/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "History.h"
#include "ManualModel.h"
#include <LibGUI/FilteringProxyModel.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Help {

class MainWidget final : public GUI::Widget {
    C_OBJECT(MainWidget);

public:
    virtual ~MainWidget() override = default;

    static ErrorOr<NonnullRefPtr<MainWidget>> try_create();

    ErrorOr<void> initialize(GUI::Window&);
    ErrorOr<void> set_start_page(Vector<StringView, 2> query_parameters);

private:
    MainWidget();

    void open_url(URL::URL const&);
    void open_page(Optional<String> const& path);
    void open_external(URL::URL const&);

    History m_history;
    RefPtr<GUI::Menu> m_context_menu;
    RefPtr<ManualModel> m_manual_model;
    RefPtr<GUI::FilteringProxyModel> m_filter_model;

    RefPtr<GUI::Action> m_go_back_action;
    RefPtr<GUI::Action> m_go_forward_action;
    RefPtr<GUI::Action> m_go_home_action;
    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_select_all_action;

    RefPtr<GUI::TabWidget> m_tab_widget;
    RefPtr<GUI::Widget> m_search_container;
    RefPtr<GUI::TextBox> m_search_box;
    RefPtr<GUI::ListView> m_search_view;
    RefPtr<GUI::TreeView> m_browse_view;
    RefPtr<WebView::OutOfProcessWebView> m_web_view;

    RefPtr<GUI::Toolbar> m_toolbar;
    RefPtr<GUI::Statusbar> m_statusbar;
};

}
