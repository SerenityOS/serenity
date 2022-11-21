/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <AK/URL.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Widget.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/Page/Page.h>
#include <LibWebView/ViewImplementation.h>

namespace Messages::WebContentServer {
class WebdriverExecuteScriptResponse;
}

namespace WebView {

class WebContentClient;

class OutOfProcessWebView final
    : public GUI::AbstractScrollableWidget
    , public ViewImplementation {
    C_OBJECT(OutOfProcessWebView);

    using Super = GUI::AbstractScrollableWidget;

public:
    virtual ~OutOfProcessWebView() override;

    AK::URL url() const { return m_url; }
    void load(const AK::URL&);

    void load_html(StringView, const AK::URL&);
    void load_empty_document();

    void debug_request(String const& request, String const& argument = {});
    void get_source();

    void inspect_dom_tree();
    struct DOMNodeProperties {
        String specified_values_json;
        String computed_values_json;
        String custom_properties_json;
        String node_box_sizing_json;
    };
    Optional<DOMNodeProperties> inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement>);
    void clear_inspected_dom_node();
    i32 get_hovered_node_id();

    void js_console_input(String const& js_source);
    void js_console_request_messages(i32 start_index);

    void run_javascript(StringView);

    String selected_text();
    void select_all();

    String dump_layout_tree();

    OrderedHashMap<String, String> get_local_storage_entries();
    OrderedHashMap<String, String> get_session_storage_entries();

    void set_content_filters(Vector<String>);
    void set_proxy_mappings(Vector<String> proxies, HashMap<String, size_t> mappings);
    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);
    void connect_to_webdriver(String const& webdriver_ipc_path);

    void set_window_position(Gfx::IntPoint const&);
    void set_window_size(Gfx::IntSize const&);

    void set_system_visibility_state(bool visible);

    Gfx::ShareableBitmap take_screenshot() const;
    Gfx::ShareableBitmap take_document_screenshot();

    Function<void(Gfx::IntPoint const& screen_position)> on_context_menu_request;
    Function<void(const AK::URL&, String const& target, unsigned modifiers)> on_link_click;
    Function<void(const AK::URL&, Gfx::IntPoint const& screen_position)> on_link_context_menu_request;
    Function<void(const AK::URL&, Gfx::IntPoint const& screen_position, Gfx::ShareableBitmap const&)> on_image_context_menu_request;
    Function<void(const AK::URL&, String const& target, unsigned modifiers)> on_link_middle_click;
    Function<void(const AK::URL&)> on_link_hover;
    Function<void(String const&)> on_title_change;
    Function<void(const AK::URL&)> on_load_start;
    Function<void(const AK::URL&)> on_load_finish;
    Function<void()> on_navigate_back;
    Function<void()> on_navigate_forward;
    Function<void()> on_refresh;
    Function<void(Gfx::Bitmap const&)> on_favicon_change;
    Function<void(const AK::URL&)> on_url_drop;
    Function<void(Web::DOM::Document*)> on_set_document;
    Function<void(const AK::URL&, String const&)> on_get_source;
    Function<void(String const&)> on_get_dom_tree;
    Function<void(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties, String const& node_box_sizing)> on_get_dom_node_properties;
    Function<void(i32 message_id)> on_js_console_new_message;
    Function<void(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages)> on_get_js_console_messages;
    Function<Vector<Web::Cookie::Cookie>(AK::URL const& url)> on_get_all_cookies;
    Function<Optional<Web::Cookie::Cookie>(AK::URL const& url, String const& name)> on_get_named_cookie;
    Function<String(const AK::URL& url, Web::Cookie::Source source)> on_get_cookie;
    Function<void(const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)> on_set_cookie;
    Function<void(AK::URL const& url, Web::Cookie::Cookie const& cookie)> on_update_cookie;
    Function<void(i32 count_waiting)> on_resource_status_change;
    Function<void()> on_restore_window;
    Function<Gfx::IntPoint(Gfx::IntPoint const&)> on_reposition_window;
    Function<Gfx::IntSize(Gfx::IntSize const&)> on_resize_window;
    Function<Gfx::IntRect()> on_maximize_window;
    Function<Gfx::IntRect()> on_minimize_window;
    Function<Gfx::IntRect()> on_fullscreen_window;

private:
    OutOfProcessWebView();

    // ^Widget
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void theme_change_event(GUI::ThemeChangeEvent&) override;
    virtual void screen_rects_change_event(GUI::ScreenRectsChangeEvent&) override;
    virtual void focusin_event(GUI::FocusEvent&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;
    virtual void show_event(GUI::ShowEvent&) override;
    virtual void hide_event(GUI::HideEvent&) override;

    // ^AbstractScrollableWidget
    virtual void did_scroll() override;

    // ^WebView::ViewImplementation
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
    virtual void notify_server_did_request_navigate_back(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_navigate_forward(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_refresh(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&) override;
    virtual void notify_server_did_request_link_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&, const AK::URL&, String const& target, unsigned modifiers) override;
    virtual void notify_server_did_request_image_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&, const AK::URL&, String const& target, unsigned modifiers, Gfx::ShareableBitmap const&) override;
    virtual void notify_server_did_request_alert(Badge<WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_confirm(Badge<WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_prompt(Badge<WebContentClient>, String const& message, String const& default_) override;
    virtual void notify_server_did_request_set_prompt_text(Badge<WebContentClient>, String const& message) override;
    virtual void notify_server_did_request_accept_dialog(Badge<WebContentClient>) override;
    virtual void notify_server_did_request_dismiss_dialog(Badge<WebContentClient>) override;
    virtual void notify_server_did_get_source(const AK::URL& url, String const& source) override;
    virtual void notify_server_did_get_dom_tree(String const& dom_tree) override;
    virtual void notify_server_did_get_dom_node_properties(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties, String const& node_box_sizing) override;
    virtual void notify_server_did_output_js_console_message(i32 message_index) override;
    virtual void notify_server_did_get_js_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages) override;
    virtual void notify_server_did_change_favicon(Gfx::Bitmap const& favicon) override;
    virtual Vector<Web::Cookie::Cookie> notify_server_did_request_all_cookies(Badge<WebContentClient>, AK::URL const& url) override;
    virtual Optional<Web::Cookie::Cookie> notify_server_did_request_named_cookie(Badge<WebContentClient>, AK::URL const& url, String const& name) override;
    virtual String notify_server_did_request_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::Source source) override;
    virtual void notify_server_did_set_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source) override;
    virtual void notify_server_did_update_cookie(Badge<WebContentClient>, AK::URL const& url, Web::Cookie::Cookie const& cookie) override;
    virtual void notify_server_did_update_resource_count(i32 count_waiting) override;
    virtual void notify_server_did_request_restore_window() override;
    virtual Gfx::IntPoint notify_server_did_request_reposition_window(Gfx::IntPoint const&) override;
    virtual Gfx::IntSize notify_server_did_request_resize_window(Gfx::IntSize const&) override;
    virtual Gfx::IntRect notify_server_did_request_maximize_window() override;
    virtual Gfx::IntRect notify_server_did_request_minimize_window() override;
    virtual Gfx::IntRect notify_server_did_request_fullscreen_window() override;
    virtual void notify_server_did_request_file(Badge<WebContentClient>, String const& path, i32) override;
    virtual void notify_server_did_finish_handling_input_event(bool event_was_accepted) override;

    void request_repaint();
    void handle_resize();

    void create_client();
    WebContentClient& client();

    void handle_web_content_process_crash();

    using InputEvent = Variant<GUI::KeyEvent, GUI::MouseEvent>;
    void enqueue_input_event(InputEvent const&);
    void process_next_input_event();

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
    RefPtr<GUI::Dialog> m_dialog;

    bool m_is_awaiting_response_for_input_event { false };
    Queue<InputEvent> m_pending_input_events;
};

}
