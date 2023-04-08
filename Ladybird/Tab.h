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
#include <QListView>
#include <QToolBar>
#include <QToolButton>

#include <QAbstractListModel>

class BrowserWindow;

namespace Ladybird {
class ConsoleWidget;
class InspectorWidget;
}

class BookmarksModel : public QAbstractListModel {
    Q_OBJECT
public:
    explicit BookmarksModel(QObject* parent = nullptr);

    int rowCount(QModelIndex const&) const override;
    QVariant data(QModelIndex const& index, int role) const override;
    void add(QString url);
    void remove(int idx);
    QStringList get_urls() const;

private:
    QStringList m_urls;
};

class Tab final : public QWidget {
    Q_OBJECT
public:
    Tab(BrowserWindow* window, StringView webdriver_content_ipc_path, WebView::EnableCallgrindProfiling, BookmarksModel* bookmarks);
    virtual ~Tab() override;

    WebContentView& view()
    {
        return *m_view;
    }

    enum class LoadType {
        Normal,
        HistoryNavigation,
    };
    void navigate(QString, LoadType = LoadType::Normal);
    void back();
    void forward();
    void reload();

    void debug_request(DeprecatedString const& request, DeprecatedString const& argument);

    void update_reset_zoom_button();
    void update_bookmarks();

    enum class InspectorTarget {
        Document,
        HoveredElement
    };
    void show_inspector_window(InspectorTarget = InspectorTarget::Document);
    void show_console_window();

    Ladybird::ConsoleWidget* console() { return m_console_widget; };

public slots:
    void focus_location_editor();
    void location_edit_return_pressed();

signals:
    void title_changed(int id, QString);
    void favicon_changed(int id, QIcon);

private:
    virtual void resizeEvent(QResizeEvent*) override;
    virtual bool event(QEvent*) override;

    void rerender_toolbar_icons();
    void update_hover_label();

    void open_link(URL const&);
    void open_link_in_new_tab(URL const&);
    void copy_link_url(URL const&);

    void close_sub_widgets();

    QBoxLayout* m_layout;
    QToolBar* m_toolbar { nullptr };
    QListView* m_bookmarks_bar { nullptr };
    QToolButton* m_reset_zoom_button { nullptr };
    QAction* m_reset_zoom_button_action { nullptr };
    LocationEdit* m_location_edit { nullptr };
    WebContentView* m_view { nullptr };
    BrowserWindow* m_window { nullptr };
    Browser::History m_history;
    QString m_title;
    QLabel* m_hover_label { nullptr };

    OwnPtr<QMenu> m_page_context_menu;

    OwnPtr<QMenu> m_link_context_menu;
    URL m_link_context_menu_url;

    OwnPtr<QMenu> m_image_context_menu;
    Gfx::ShareableBitmap m_image_context_menu_bitmap;
    URL m_image_context_menu_url;

    OwnPtr<QMenu> m_video_context_menu;
    OwnPtr<QIcon> m_video_context_menu_play_icon;
    OwnPtr<QIcon> m_video_context_menu_pause_icon;
    OwnPtr<QAction> m_video_context_menu_play_pause_action;
    OwnPtr<QAction> m_video_context_menu_controls_action;
    OwnPtr<QAction> m_video_context_menu_loop_action;
    URL m_video_context_menu_url;
    OwnPtr<QAction> m_back_action;
    OwnPtr<QAction> m_forward_action;
    OwnPtr<QAction> m_reload_action;
    OwnPtr<QAction> m_add_bookmark_action;

    int tab_index();

    bool m_is_history_navigation { false };

    Ladybird::ConsoleWidget* m_console_widget { nullptr };
    Ladybird::InspectorWidget* m_inspector_widget { nullptr };

    void add_bookmark();
    void bookmark_clicked(QModelIndex const& index);
    void show_context_menu_bookmark(QPoint const& pos);
};
