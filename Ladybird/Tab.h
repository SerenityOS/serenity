/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_DONT_REPLACE_STD

#include "History.h"
#include "WebView.h"
#include <QBoxLayout>
#include <QLineEdit>
#include <QToolBar>
#include <QWidget>

class Tab final : public QWidget {
    Q_OBJECT
public:
    explicit Tab(QMainWindow* window);

    WebView& view() { return *m_view; }

    void navigate(QString const&);

    void debug_request(String const& request, String const& argument);

public slots:
    void location_edit_return_pressed();
    void page_title_changed(QString);
    void page_favicon_changed(QIcon);
    void back();
    void forward();
    void home();
    void reload();

signals:
    void title_changed(int id, QString);
    void favicon_changed(int id, QIcon);

private:
    QBoxLayout* m_layout;
    QToolBar* m_toolbar { nullptr };
    QLineEdit* m_location_edit { nullptr };
    WebView* m_view { nullptr };
    QMainWindow* m_window { nullptr };
    Browser::History m_history;
    QString m_title;

    OwnPtr<QAction> m_back_action;
    OwnPtr<QAction> m_forward_action;
    OwnPtr<QAction> m_home_action;
    OwnPtr<QAction> m_reload_action;

    int tab_index();
};
