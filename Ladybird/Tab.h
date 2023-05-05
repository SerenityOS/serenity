/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "LocationEdit.h"
#include "WebContentView.h"
#include <Browser/History.h>
#include <QBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QToolBar>
#include <QToolButton>
#include <QWidget>

class BrowserWindow;

class Tab final : public QWidget {
    Q_OBJECT
public:
    Tab(BrowserWindow* window, StringView webdriver_content_ipc_path, WebView::EnableCallgrindProfiling);

    WebContentView& view() { return *m_view; }

    enum class LoadType {
        Normal,
        HistoryNavigation,
    };
    void navigate(QString, LoadType = LoadType::Normal);

    void debug_request(DeprecatedString const& request, DeprecatedString const& argument);

    void update_reset_zoom_button();

public slots:
    void focus_location_editor();
    void location_edit_return_pressed();
    void page_title_changed(QString);
    void page_favicon_changed(QIcon);
    void back();
    void forward();
    void reload();

signals:
    void title_changed(int id, QString);
    void favicon_changed(int id, QIcon);

private:
    virtual void resizeEvent(QResizeEvent*) override;

    void update_hover_label();

    QBoxLayout* m_layout;
    QToolBar* m_toolbar { nullptr };
    QToolButton* m_reset_zoom_button { nullptr };
    QAction* m_reset_zoom_button_action { nullptr };
    LocationEdit* m_location_edit { nullptr };
    WebContentView* m_view { nullptr };
    BrowserWindow* m_window { nullptr };
    Browser::History m_history;
    QString m_title;
    QLabel* m_hover_label { nullptr };

    OwnPtr<QAction> m_back_action;
    OwnPtr<QAction> m_forward_action;
    OwnPtr<QAction> m_reload_action;

    int tab_index();

    bool m_is_history_navigation { false };
};
