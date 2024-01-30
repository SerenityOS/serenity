/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Matthew Costa <ucosty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "LocationEdit.h"
#include "WebContentView.h"
#include <LibWebView/History.h>
#include <QBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QMenu>
#include <QPointer>
#include <QToolBar>
#include <QToolButton>
#include <QWidget>

namespace Ladybird {

class BrowserWindow;
class InspectorWidget;

class Tab final : public QWidget {
    Q_OBJECT

public:
    Tab(BrowserWindow* window, WebContentOptions const&, StringView webdriver_content_ipc_path, RefPtr<WebView::WebContentClient> parent_client = nullptr, size_t page_index = 0);
    virtual ~Tab() override;

    WebContentView& view() { return *m_view; }

    void navigate(QString const&);
    void load_html(StringView);

    void back();
    void forward();
    void reload();

    void debug_request(ByteString const& request, ByteString const& argument);

    void open_file();
    void update_reset_zoom_button();

    enum class InspectorTarget {
        Document,
        HoveredElement
    };
    void show_inspector_window(InspectorTarget = InspectorTarget::Document);

public slots:
    void focus_location_editor();
    void location_edit_return_pressed();
    void select_dropdown_action();

signals:
    void title_changed(int id, QString);
    void favicon_changed(int id, QIcon);

private:
    void select_dropdown_add_item(QMenu* menu, Web::HTML::SelectItem const& item);

    virtual void resizeEvent(QResizeEvent*) override;
    virtual bool event(QEvent*) override;

    void recreate_toolbar_icons();
    void update_hover_label();

    void open_link(URL const&);
    void open_link_in_new_tab(URL const&);
    void copy_link_url(URL const&);

    void close_sub_widgets();

    QBoxLayout* m_layout { nullptr };
    QToolBar* m_toolbar { nullptr };
    QToolButton* m_reset_zoom_button { nullptr };
    QAction* m_reset_zoom_button_action { nullptr };
    LocationEdit* m_location_edit { nullptr };
    WebContentView* m_view { nullptr };
    BrowserWindow* m_window { nullptr };
    WebView::History m_history;
    QString m_title;
    QLabel* m_hover_label { nullptr };

    QMenu* m_page_context_menu { nullptr };
    Optional<String> m_page_context_menu_search_text;

    QMenu* m_link_context_menu { nullptr };
    QAction* m_link_context_menu_copy_url_action { nullptr };
    URL m_link_context_menu_url;

    QMenu* m_image_context_menu { nullptr };
    Gfx::ShareableBitmap m_image_context_menu_bitmap;
    URL m_image_context_menu_url;

    QMenu* m_audio_context_menu { nullptr };
    QMenu* m_video_context_menu { nullptr };
    QIcon m_media_context_menu_play_icon;
    QIcon m_media_context_menu_pause_icon;
    QIcon m_media_context_menu_mute_icon;
    QIcon m_media_context_menu_unmute_icon;
    QAction* m_media_context_menu_play_pause_action { nullptr };
    QAction* m_media_context_menu_mute_unmute_action { nullptr };
    QAction* m_media_context_menu_controls_action { nullptr };
    QAction* m_media_context_menu_loop_action { nullptr };
    URL m_media_context_menu_url;

    QMenu* m_select_dropdown { nullptr };

    int tab_index();

    bool m_is_history_navigation { false };

    Ladybird::InspectorWidget* m_inspector_widget { nullptr };

    QPointer<QDialog> m_dialog;
};

}
