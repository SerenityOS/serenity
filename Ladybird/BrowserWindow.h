/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tab.h"
#include <AK/Vector.h>
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
    explicit BrowserWindow(Core::EventLoop&);

    WebView& view() const { return m_current_tab->view(); }

    int tab_index(Tab*);

    virtual void closeEvent(QCloseEvent*) override;

public slots:
    void tab_title_changed(int index, QString const&);
    void tab_favicon_changed(int index, QIcon icon);
    void new_tab();

private:
    QTabWidget* m_tabs_container { nullptr };
    Vector<NonnullOwnPtr<Tab>> m_tabs;
    Tab* m_current_tab { nullptr };

    Core::EventLoop& m_event_loop;
};
