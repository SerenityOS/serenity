/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_DONT_REPLACE_STD

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StandardCursor.h>
#include <LibWebView/ViewImplementation.h>

#include <LibWeb/Forward.h>
#include <QAbstractScrollArea>
#include <QPointer>

class QTextEdit;
class QLineEdit;

namespace Ladybird {
class ConsoleWidget;
}

namespace WebView {
class WebContentClient;
}

using WebView::WebContentClient;

enum class ColorScheme {
    Auto,
    Light,
    Dark,
};

class Tab;

class WebContentView final
    : public QAbstractScrollArea
    , public WebView::ViewImplementation {
    Q_OBJECT
public:
    WebContentView();
    virtual ~WebContentView() override;

    void load(AK::URL const&);
    void load_html(StringView html, AK::URL const&);
    void reload();

    Function<void(Gfx::IntPoint const& screen_position)> on_context_menu_request;
    Function<void(const AK::URL&, String const& target, unsigned modifiers)> on_link_click;
    Function<void(const AK::URL&, Gfx::IntPoint const& screen_position)> on_link_context_menu_request;
    Function<void(const AK::URL&, Gfx::IntPoint const& screen_position, Gfx::ShareableBitmap const&)> on_image_context_menu_request;
    Function<void(const AK::URL&, String const& target, unsigned modifiers)> on_link_middle_click;
    Function<void(const AK::URL&)> on_link_hover;
    Function<void(String const&)> on_title_change;
    Function<void(const AK::URL&)> on_load_start;
    Function<void(const AK::URL&)> on_load_finish;
    Function<void(Gfx::Bitmap const&)> on_favicon_change;
    Function<void(const AK::URL&)> on_url_drop;
    Function<void(Web::DOM::Document*)> on_set_document;
    Function<void(const AK::URL&, String const&)> on_get_source;
    Function<void(String const&)> on_get_dom_tree;
    Function<void(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties, String const& node_box_sizing)> on_get_dom_node_properties;
    Function<void(i32 message_id)> on_js_console_new_message;
    Function<void(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages)> on_get_js_console_messages;
    Function<String(const AK::URL& url, Web::Cookie::Source source)> on_get_cookie;
    Function<void(const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)> on_set_cookie;
    Function<void(i32 count_waiting)> on_resource_status_change;

    virtual void paintEvent(QPaintEvent*) override;
    virtual void resizeEvent(QResizeEvent*) override;
    virtual void mouseMoveEvent(QMouseEvent*) override;
    virtual void mousePressEvent(QMouseEvent*) override;
    virtual void mouseReleaseEvent(QMouseEvent*) override;
    virtual void keyPressEvent(QKeyEvent* event) override;
    virtual void keyReleaseEvent(QKeyEvent* event) override;
    virtual void showEvent(QShowEvent*) override;
    virtual void hideEvent(QHideEvent*) override;
    virtual void focusInEvent(QFocusEvent*) override;
    virtual void focusOutEvent(QFocusEvent*) override;
    virtual bool event(QEvent*) override;

    void debug_request(String const& request, String const& argument);

    void get_source();

    void run_javascript(String const& js_source);

    void did_output_js_console_message(i32 message_index);
    void did_get_js_console_messages(i32 start_index, Vector<String> message_types, Vector<String> messages);

    void show_js_console();
    void show_inspector();

    Gfx::IntPoint to_content(Gfx::IntPoint) const;
    Gfx::IntPoint to_widget(Gfx::IntPoint) const;

    void set_color_scheme(ColorScheme);

    virtual void notify_server_did_layout(Badge<WebContentClient>, Gfx::IntSize const& content_size) override;
    virtual void notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id) override;
    virtual void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, Gfx::IntRect const&) override;
    virtual void notify_server_did_change_selection(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor) override;
    virtual void notify_server_did_change_title(Badge<WebContentClient>, String const&) override;
    virtual void notify_server_did_request_scroll(Badge<WebContentClient>, i32, i32) override;
    virtual void notify_server_did_request_scroll_to(Badge<WebContentClient>, Gfx::IntPoint const&) override;
    virtual void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, Gfx::IntRect const&) override;
    virtual void notify_server_did_enter_tooltip_area(Badge<WebContentClient>, Gfx::IntPoint const&, String const&) override;
    virtual void notify_server_did_leave_tooltip_area(Badge<WebContentClient>) override;
    virtual void notify_server_did_hover_link(Badge<WebContentClient>, const AK::URL&) override;
    virtual void notify_server_did_unhover_link(Badge<WebContentClient>) override;
    virtual void notify_server_did_click_link(Badge<WebContentClient>, const AK::URL&, String const& target, unsigned modifiers) override;
    virtual void notify_server_did_middle_click_link(Badge<WebContentClient>, const AK::URL&, String const& target, unsigned modifiers) override;
    virtual void notify_server_did_start_loading(Badge<WebContentClient>, const AK::URL&) override;
    virtual void notify_server_did_finish_loading(Badge<WebContentClient>, const AK::URL&) override;
    virtual void notify_server_did_request_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&) override;
    virtual void notify_server_did_request_link_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&, const AK::URL&, String const& target, unsigned modifiers) override;
    virtual void notify_server_did_request_image_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&, const AK::URL&, String const& target, unsigned modifiers, Gfx::ShareableBitmap const&) override;
    virtual void notify_server_did_request_alert(Badge<WebContentClient>, String const& message) override;
    virtual bool notify_server_did_request_confirm(Badge<WebContentClient>, String const& message) override;
    virtual String notify_server_did_request_prompt(Badge<WebContentClient>, String const& message, String const& default_) override;
    virtual void notify_server_did_get_source(const AK::URL& url, String const& source) override;
    virtual void notify_server_did_get_dom_tree(String const& dom_tree) override;
    virtual void notify_server_did_get_dom_node_properties(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties, String const& node_box_sizing) override;
    virtual void notify_server_did_output_js_console_message(i32 message_index) override;
    virtual void notify_server_did_get_js_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages) override;
    virtual void notify_server_did_change_favicon(Gfx::Bitmap const& favicon) override;
    virtual String notify_server_did_request_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::Source source) override;
    virtual void notify_server_did_set_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source) override;
    virtual void notify_server_did_update_resource_count(i32 count_waiting) override;
    virtual void notify_server_did_request_file(Badge<WebContentClient>, String const& path, i32) override;

signals:
    void link_hovered(QString, int timeout = 0);
    void link_unhovered();
    void load_started(const URL&);
    void title_changed(QString);
    void favicon_changed(QIcon);
    void got_source(URL, QString);

private:
    void request_repaint();
    void update_viewport_rect();
    void handle_resize();

    void ensure_js_console_widget();
    void ensure_inspector_widget();

    qreal m_inverse_pixel_scaling_ratio { 1.0 };
    bool m_should_show_line_box_borders { false };

    QPointer<QWidget> m_inspector_widget;

    Ladybird::ConsoleWidget* m_console_widget { nullptr };

    Gfx::IntRect m_viewport_rect;

    void create_client();
    WebContentClient& client();

    void handle_web_content_process_crash();

    AK::URL m_url;

    struct SharedBitmap {
        i32 id { -1 };
        i32 pending_paints { 0 };
        RefPtr<Gfx::Bitmap> bitmap;
    };

    struct ClientState {
        RefPtr<WebContentClient> client;
        SharedBitmap front_bitmap;
        SharedBitmap back_bitmap;
        i32 next_bitmap_id { 0 };
        bool has_usable_bitmap { false };
        bool got_repaint_requests_while_painting { false };
    } m_client_state;

    RefPtr<Gfx::Bitmap> m_backup_bitmap;
};
