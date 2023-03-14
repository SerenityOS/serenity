/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tab.h"
#include <LibCore/Forward.h>
#include <QIcon>
#include <QLineEdit>
#include <QMainWindow>
#include <QMenuBar>
#include <QTabWidget>
#include <QToolBar>

class WebContentView;

namespace Browser {
class CookieJar;
}

class BrowserWindow : public QMainWindow {
    Q_OBJECT
public:
    explicit BrowserWindow(Browser::CookieJar&, StringView webdriver_content_ipc_path);

    WebContentView& view() const { return m_current_tab->view(); }

    int tab_index(Tab*);

    enum class Activate {
        Yes,
        No,
    };

public slots:
    void tab_title_changed(int index, QString const&);
    void tab_favicon_changed(int index, QIcon icon);
    Tab& new_tab(QString const&, Activate);
    void close_tab(int index);
    void close_current_tab();
    void open_next_tab();
    void open_previous_tab();
    void enable_auto_color_scheme();
    void enable_light_color_scheme();
    void enable_dark_color_scheme();
    void zoom_in();
    void zoom_out();
    void reset_zoom();
    void select_all();
    void copy_selected_text();

protected:
    bool eventFilter(QObject* obj, QEvent* event) override;

private:
    virtual void resizeEvent(QResizeEvent*) override;
    virtual void moveEvent(QMoveEvent*) override;

    void debug_request(DeprecatedString const& request, DeprecatedString const& argument = "");

    QTabWidget* m_tabs_container { nullptr };
    Vector<NonnullOwnPtr<Tab>> m_tabs;
    Tab* m_current_tab { nullptr };

    Browser::CookieJar& m_cookie_jar;

    StringView m_webdriver_content_ipc_path;
};
