/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tab.h"
#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/Forward.h>
#include <QIcon>
#include <QLineEdit>
#include <QMainWindow>
#include <QMenuBar>
#include <QTabWidget>
#include <QToolBar>

#pragma once

class WebView;

class BrowserWindow : public QMainWindow {
    Q_OBJECT
public:
    explicit BrowserWindow();

    WebView& view() const { return m_current_tab->view(); }

    int tab_index(Tab*);

public slots:
    void tab_title_changed(int index, QString const&);
    void tab_favicon_changed(int index, QIcon icon);
    void new_tab();
    void close_tab(int index);
    void close_current_tab();
    void open_next_tab();
    void open_previous_tab();
    void enable_auto_color_scheme();
    void enable_light_color_scheme();
    void enable_dark_color_scheme();

private:
    void debug_request(String const& request, String const& argument = "");

    QTabWidget* m_tabs_container { nullptr };
    NonnullOwnPtrVector<Tab> m_tabs;
    Tab* m_current_tab { nullptr };
};
