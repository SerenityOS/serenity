/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/URL.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StandardCursor.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWebView/ViewImplementation.h>
#include <QAbstractScrollArea>
#include <QPointer>

class QTextEdit;
class QLineEdit;

namespace Ladybird {
class ConsoleWidget;
class InspectorWidget;
}

namespace WebView {
class WebContentClient;
}

using WebView::WebContentClient;

class Tab;

class WebContentView final
    : public QAbstractScrollArea
    , public WebView::ViewImplementation {
    Q_OBJECT
public:
    explicit WebContentView(StringView webdriver_content_ipc_path, WebView::EnableCallgrindProfiling);
    virtual ~WebContentView() override;

    Function<String(Web::HTML::ActivateTab)> on_new_tab;
    Function<String(const AK::URL&, Web::HTML::ActivateTab)> on_tab_open_request;
    Function<void()> on_close;
    Function<void(Gfx::IntPoint screen_position)> on_context_menu_request;
    Function<void(const AK::URL&, DeprecatedString const& target, unsigned modifiers)> on_link_click;
    Function<void(const AK::URL&, Gfx::IntPoint screen_position)> on_link_context_menu_request;
    Function<void(const AK::URL&, Gfx::IntPoint screen_position, Gfx::ShareableBitmap const&)> on_image_context_menu_request;
    Function<void(const AK::URL&, DeprecatedString const& target, unsigned modifiers)> on_link_middle_click;
    Function<void(const AK::URL&)> on_link_hover;
    Function<void(DeprecatedString const&)> on_title_change;
    Function<void(const AK::URL&)> on_load_start;
    Function<void(const AK::URL&)> on_load_finish;
    Function<void(Gfx::Bitmap const&)> on_favicon_change;
    Function<void(const AK::URL&)> on_url_drop;
    Function<void(Web::DOM::Document*)> on_set_document;
    Function<void(const AK::URL&, DeprecatedString const&)> on_get_source;
    Function<void(DeprecatedString const&)> on_get_dom_tree;
    Function<void(i32 node_id, DeprecatedString const& specified_style, DeprecatedString const& computed_style, DeprecatedString const& custom_properties, DeprecatedString const& node_box_sizing)> on_get_dom_node_properties;
    Function<void(i32 message_id)> on_js_console_new_message;
    Function<void(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages)> on_get_js_console_messages;
    Function<Vector<Web::Cookie::Cookie>(AK::URL const& url)> on_get_all_cookies;
    Function<Optional<Web::Cookie::Cookie>(AK::URL const& url, DeprecatedString const& name)> on_get_named_cookie;
    Function<DeprecatedString(const AK::URL& url, Web::Cookie::Source source)> on_get_cookie;
    Function<void(const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)> on_set_cookie;
    Function<void(Web::Cookie::Cookie const& cookie)> on_update_cookie;
    Function<void(i32 count_waiting)> on_resource_status_change;

    virtual void paintEvent(QPaintEvent*) override;
    virtual void resizeEvent(QResizeEvent*) override;
    virtual void mouseMoveEvent(QMouseEvent*) override;
    virtual void mousePressEvent(QMouseEvent*) override;
    virtual void mouseReleaseEvent(QMouseEvent*) override;
    virtual void mouseDoubleClickEvent(QMouseEvent*) override;
    virtual void dragEnterEvent(QDragEnterEvent*) override;
    virtual void dropEvent(QDropEvent*) override;
    virtual void keyPressEvent(QKeyEvent* event) override;
    virtual void keyReleaseEvent(QKeyEvent* event) override;
    virtual void showEvent(QShowEvent*) override;
    virtual void hideEvent(QHideEvent*) override;
    virtual void focusInEvent(QFocusEvent*) override;
    virtual void focusOutEvent(QFocusEvent*) override;
    virtual bool event(QEvent*) override;

    void did_output_js_console_message(i32 message_index);
    void did_get_js_console_messages(i32 start_index, Vector<DeprecatedString> message_types, Vector<DeprecatedString> messages);

    void show_js_console();

    enum class InspectorTarget {
        Document,
        HoveredElement
    };
    void show_inspector(InspectorTarget = InspectorTarget::Document);

    Ladybird::ConsoleWidget* console() { return m_console_widget; };

    ErrorOr<String> dump_layout_tree();

    void set_viewport_rect(Gfx::IntRect);
    void set_window_size(Gfx::IntSize);
    void set_window_position(Gfx::IntPoint);

    Gfx::IntPoint to_content(Gfx::IntPoint) const;
    Gfx::IntPoint to_widget(Gfx::IntPoint) const;

    enum class PaletteMode {
        Default,
        Dark,
    };
    void update_palette(PaletteMode = PaletteMode::Default);

    virtual void notify_server_did_layout(Badge<WebContentClient>, Gfx::IntSize content_size) override;
    virtual void notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id, Gfx::IntSize) override;
    virtual void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, Gfx::IntRect const&) override;
    virtual void notify_server_did_change_selection(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor) override;
    virtual void notify_server_did_change_title(Badge<WebContentClient>, DeprecatedString const&) override;
    virtual void notify_server_did_request_scroll(Badge<WebContentClient>, i32, i32) override;
    virtual void notify_server_did_request_scroll_to(Badge<WebContentClient>, Gfx::IntPoint) override;
    virtual void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, Gfx::IntRect const&) override;
    virtual void notify_server_did_enter_tooltip_area(Badge<WebContentClient>, Gfx::IntPoint, DeprecatedString const&) override;
    virtual void notify_server_did_leave_tooltip_area(Badge<WebContentClient>) override;
    virtual void notify_server_did_hover_link(Badge<WebContentClient>, const AK::URL&) override;
    virtual void notify_server_did_unhover_link(Badge<WebContentClient>) override;
    virtual void notify_server_did_click_link(Badge<WebContentClient>, const AK::URL&, DeprecatedString const& target, unsigned modifiers) override;
    virtual void notify_server_did_middle_click_link(Badge<WebContentClient>, const AK::URL&, DeprecatedString const& target, unsigned modifiers) override;
    virtual void notify_server_did_start_loading(Badge<WebContentClient>, const AK::URL&, bool) override;
    virtual void notify_server_did_finish_loading(Badge<WebContentClient>, const AK::URL&) override;
    virtual void notify_server_did_request_navigate_back(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_navigate_forward(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_refresh(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_context_menu(Badge<WebContentClient>, Gfx::IntPoint) override;
    virtual void notify_server_did_request_link_context_menu(Badge<WebContentClient>, Gfx::IntPoint, const AK::URL&, DeprecatedString const& target, unsigned modifiers) override;
    virtual void notify_server_did_request_image_context_menu(Badge<WebContentClient>, Gfx::IntPoint, const AK::URL&, DeprecatedString const& target, unsigned modifiers, Gfx::ShareableBitmap const&) override;
    virtual void notify_server_did_request_alert(Badge<WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_confirm(Badge<WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_prompt(Badge<WebContentClient>, String const& message, String const& default_) override;
    virtual void notify_server_did_request_set_prompt_text(Badge<WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_accept_dialog(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_dismiss_dialog(Badge<WebContentClient>) override;
    virtual void notify_server_did_get_source(const AK::URL& url, DeprecatedString const& source) override;
    virtual void notify_server_did_get_dom_tree(DeprecatedString const& dom_tree) override;
    virtual void notify_server_did_get_dom_node_properties(i32 node_id, DeprecatedString const& specified_style, DeprecatedString const& computed_style, DeprecatedString const& custom_properties, DeprecatedString const& node_box_sizing) override;
    virtual void notify_server_did_get_accessibility_tree(DeprecatedString const& accessibility_tree) override;
    virtual void notify_server_did_output_js_console_message(i32 message_index) override;
    virtual void notify_server_did_get_js_console_messages(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages) override;
    virtual void notify_server_did_change_favicon(Gfx::Bitmap const& favicon) override;
    virtual Vector<Web::Cookie::Cookie> notify_server_did_request_all_cookies(Badge<WebContentClient>, AK::URL const& url) override;
    virtual Optional<Web::Cookie::Cookie> notify_server_did_request_named_cookie(Badge<WebContentClient>, AK::URL const& url, DeprecatedString const& name) override;
    virtual DeprecatedString notify_server_did_request_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::Source source) override;
    virtual void notify_server_did_set_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source) override;
    virtual void notify_server_did_update_cookie(Badge<WebContentClient>, Web::Cookie::Cookie const& cookie) override;
    virtual String notify_server_did_request_new_tab(Badge<WebContentClient>, Web::HTML::ActivateTab activate_tab) override;
    virtual void notify_server_did_request_activate_tab(Badge<WebContentClient>) override;
    virtual void notify_server_did_close_browsing_context(Badge<WebContentClient>) override;
    virtual void notify_server_did_update_resource_count(i32 count_waiting) override;
    virtual void notify_server_did_request_restore_window() override;
    virtual Gfx::IntPoint notify_server_did_request_reposition_window(Gfx::IntPoint) override;
    virtual Gfx::IntSize notify_server_did_request_resize_window(Gfx::IntSize) override;
    virtual Gfx::IntRect notify_server_did_request_maximize_window() override;
    virtual Gfx::IntRect notify_server_did_request_minimize_window() override;
    virtual Gfx::IntRect notify_server_did_request_fullscreen_window() override;
    virtual void notify_server_did_request_file(Badge<WebContentClient>, DeprecatedString const& path, i32) override;
    virtual void notify_server_did_finish_handling_input_event(bool event_was_accepted) override;

signals:
    void activate_tab();
    void close();
    void link_hovered(QString, int timeout = 0);
    void link_unhovered();
    void back_mouse_button();
    void forward_mouse_button();
    void load_started(const URL&, bool);
    void title_changed(QString);
    void favicon_changed(QIcon);
    void got_source(URL, QString);
    void navigate_back();
    void navigate_forward();
    void refresh();
    void restore_window();
    void urls_dropped(QList<QUrl> const&);
    Gfx::IntPoint reposition_window(Gfx::IntPoint);
    Gfx::IntSize resize_window(Gfx::IntSize);
    Gfx::IntRect maximize_window();
    Gfx::IntRect minimize_window();
    Gfx::IntRect fullscreen_window();

private:
    // ^WebView::ViewImplementation
    virtual void create_client(WebView::EnableCallgrindProfiling = WebView::EnableCallgrindProfiling::No) override;
    virtual void update_zoom() override;

    void request_repaint();
    void update_viewport_rect();
    void handle_resize();

    void ensure_js_console_widget();
    void ensure_inspector_widget();

    bool is_inspector_open() const;
    void close_sub_widgets();

    qreal m_inverse_pixel_scaling_ratio { 1.0 };
    bool m_should_show_line_box_borders { false };

    QPointer<QDialog> m_dialog;

    Ladybird::ConsoleWidget* m_console_widget { nullptr };
    Ladybird::InspectorWidget* m_inspector_widget { nullptr };

    Gfx::IntRect m_viewport_rect;

    void handle_web_content_process_crash();

    RefPtr<Gfx::Bitmap> m_backup_bitmap;

    StringView m_webdriver_content_ipc_path;
};
