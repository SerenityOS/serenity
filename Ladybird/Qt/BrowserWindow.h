/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tab.h"
#include <LibCore/Forward.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <QIcon>
#include <QLineEdit>
#include <QMainWindow>
#include <QMenuBar>
#include <QTabWidget>
#include <QToolBar>

namespace Browser {
class CookieJar;
}

namespace Ladybird {

class WebContentView;

class BrowserWindow : public QMainWindow {
    Q_OBJECT
public:
    explicit BrowserWindow(Optional<URL> const& initial_url, Browser::CookieJar&, StringView webdriver_content_ipc_path, WebView::EnableCallgrindProfiling, UseLagomNetworking);

    WebContentView& view() const { return m_current_tab->view(); }

    int tab_index(Tab*);

    QAction& go_back_action()
    {
        return *m_go_back_action;
    }

    QAction& go_forward_action()
    {
        return *m_go_forward_action;
    }

    QAction& reload_action()
    {
        return *m_reload_action;
    }

    QAction& copy_selection_action()
    {
        return *m_copy_selection_action;
    }

    QAction& select_all_action()
    {
        return *m_select_all_action;
    }

    QAction& view_source_action()
    {
        return *m_view_source_action;
    }

    QAction& inspect_dom_node_action()
    {
        return *m_inspect_dom_node_action;
    }

public slots:
    void tab_title_changed(int index, QString const&);
    void tab_favicon_changed(int index, QIcon icon);
    Tab& new_tab(QString const&, Web::HTML::ActivateTab);
    void activate_tab(int index);
    void close_tab(int index);
    void close_current_tab();
    void open_next_tab();
    void open_previous_tab();
    void open_file();
    void enable_auto_color_scheme();
    void enable_light_color_scheme();
    void enable_dark_color_scheme();
    void zoom_in();
    void zoom_out();
    void reset_zoom();
    void update_zoom_menu();
    void select_all();
    void copy_selected_text();

protected:
    bool eventFilter(QObject* obj, QEvent* event) override;

private:
    virtual void resizeEvent(QResizeEvent*) override;
    virtual void moveEvent(QMoveEvent*) override;
    virtual void wheelEvent(QWheelEvent*) override;

    void debug_request(DeprecatedString const& request, DeprecatedString const& argument = "");

    void set_current_tab(Tab* tab);
    void update_displayed_zoom_level();

    QTabWidget* m_tabs_container { nullptr };
    Vector<NonnullOwnPtr<Tab>> m_tabs;
    Tab* m_current_tab { nullptr };
    QMenu* m_zoom_menu { nullptr };

    OwnPtr<QAction> m_go_back_action {};
    OwnPtr<QAction> m_go_forward_action {};
    OwnPtr<QAction> m_reload_action {};
    OwnPtr<QAction> m_copy_selection_action {};
    OwnPtr<QAction> m_select_all_action {};
    OwnPtr<QAction> m_view_source_action {};
    OwnPtr<QAction> m_inspect_dom_node_action {};

    Browser::CookieJar& m_cookie_jar;

    StringView m_webdriver_content_ipc_path;
    WebView::EnableCallgrindProfiling m_enable_callgrind_profiling;
    UseLagomNetworking m_use_lagom_networking;
};

}
