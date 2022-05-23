/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Widget.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/Page/Page.h>

namespace WebView {

class WebContentClient;

class OutOfProcessWebView final : public GUI::AbstractScrollableWidget {
    C_OBJECT(OutOfProcessWebView);

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

    void set_content_filters(Vector<String>);
    void set_proxy_mappings(Vector<String> proxies, HashMap<String, size_t> mappings);
    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);

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

    void notify_server_did_layout(Badge<WebContentClient>, Gfx::IntSize const& content_size);
    void notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id);
    void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, Gfx::IntRect const&);
    void notify_server_did_change_selection(Badge<WebContentClient>);
    void notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor);
    void notify_server_did_change_title(Badge<WebContentClient>, String const&);
    void notify_server_did_request_scroll(Badge<WebContentClient>, i32, i32);
    void notify_server_did_request_scroll_to(Badge<WebContentClient>, Gfx::IntPoint const&);
    void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, Gfx::IntRect const&);
    void notify_server_did_enter_tooltip_area(Badge<WebContentClient>, Gfx::IntPoint const&, String const&);
    void notify_server_did_leave_tooltip_area(Badge<WebContentClient>);
    void notify_server_did_hover_link(Badge<WebContentClient>, const AK::URL&);
    void notify_server_did_unhover_link(Badge<WebContentClient>);
    void notify_server_did_click_link(Badge<WebContentClient>, const AK::URL&, String const& target, unsigned modifiers);
    void notify_server_did_middle_click_link(Badge<WebContentClient>, const AK::URL&, String const& target, unsigned modifiers);
    void notify_server_did_start_loading(Badge<WebContentClient>, const AK::URL&);
    void notify_server_did_finish_loading(Badge<WebContentClient>, const AK::URL&);
    void notify_server_did_request_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&);
    void notify_server_did_request_link_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&, const AK::URL&, String const& target, unsigned modifiers);
    void notify_server_did_request_image_context_menu(Badge<WebContentClient>, Gfx::IntPoint const&, const AK::URL&, String const& target, unsigned modifiers, Gfx::ShareableBitmap const&);
    void notify_server_did_request_alert(Badge<WebContentClient>, String const& message);
    bool notify_server_did_request_confirm(Badge<WebContentClient>, String const& message);
    String notify_server_did_request_prompt(Badge<WebContentClient>, String const& message, String const& default_);
    void notify_server_did_get_source(const AK::URL& url, String const& source);
    void notify_server_did_get_dom_tree(String const& dom_tree);
    void notify_server_did_get_dom_node_properties(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties, String const& node_box_sizing);
    void notify_server_did_output_js_console_message(i32 message_index);
    void notify_server_did_get_js_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages);
    void notify_server_did_change_favicon(Gfx::Bitmap const& favicon);
    String notify_server_did_request_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::Source source);
    void notify_server_did_set_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source);
    void notify_server_did_update_resource_count(i32 count_waiting);

private:
    OutOfProcessWebView();

    // ^Widget
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void theme_change_event(GUI::ThemeChangeEvent&) override;
    virtual void screen_rects_change_event(GUI::ScreenRectsChangeEvent&) override;
    virtual void focusin_event(GUI::FocusEvent&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;

    // ^AbstractScrollableWidget
    virtual void did_scroll() override;

    void request_repaint();
    void handle_resize();

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

}
