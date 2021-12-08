/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Widget.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/WebViewHooks.h>

namespace Web {

class WebContentClient;

class OutOfProcessWebView final
    : public GUI::AbstractScrollableWidget
    , public Web::WebViewHooks {
    C_OBJECT(OutOfProcessWebView);

public:
    virtual ~OutOfProcessWebView() override;

    AK::URL url() const { return m_url; }
    void load(const AK::URL&);

    void load_html(StringView, const AK::URL&);
    void load_empty_document();

    void debug_request(const String& request, const String& argument = {});
    void get_source();

    void inspect_dom_tree();
    struct DOMNodeProperties {
        String specified_values_json;
        String computed_values_json;
        String custom_properties_json;
    };
    Optional<DOMNodeProperties> inspect_dom_node(i32 node_id);
    void clear_inspected_dom_node();
    i32 get_hovered_node_id();

    void js_console_input(const String& js_source);
    void js_console_request_messages(i32 start_index);

    void run_javascript(StringView);

    String selected_text();
    void select_all();

    String dump_layout_tree();

    void set_content_filters(Vector<String>);
    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);

    void notify_server_did_layout(Badge<WebContentClient>, const Gfx::IntSize& content_size);
    void notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id);
    void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, const Gfx::IntRect&);
    void notify_server_did_change_selection(Badge<WebContentClient>);
    void notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor);
    void notify_server_did_change_title(Badge<WebContentClient>, const String&);
    void notify_server_did_request_scroll(Badge<WebContentClient>, i32, i32);
    void notify_server_did_request_scroll_to(Badge<WebContentClient>, Gfx::IntPoint const&);
    void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, const Gfx::IntRect&);
    void notify_server_did_enter_tooltip_area(Badge<WebContentClient>, const Gfx::IntPoint&, const String&);
    void notify_server_did_leave_tooltip_area(Badge<WebContentClient>);
    void notify_server_did_hover_link(Badge<WebContentClient>, const AK::URL&);
    void notify_server_did_unhover_link(Badge<WebContentClient>);
    void notify_server_did_click_link(Badge<WebContentClient>, const AK::URL&, const String& target, unsigned modifiers);
    void notify_server_did_middle_click_link(Badge<WebContentClient>, const AK::URL&, const String& target, unsigned modifiers);
    void notify_server_did_start_loading(Badge<WebContentClient>, const AK::URL&);
    void notify_server_did_finish_loading(Badge<WebContentClient>, const AK::URL&);
    void notify_server_did_request_context_menu(Badge<WebContentClient>, const Gfx::IntPoint&);
    void notify_server_did_request_link_context_menu(Badge<WebContentClient>, const Gfx::IntPoint&, const AK::URL&, const String& target, unsigned modifiers);
    void notify_server_did_request_image_context_menu(Badge<WebContentClient>, const Gfx::IntPoint&, const AK::URL&, const String& target, unsigned modifiers, const Gfx::ShareableBitmap&);
    void notify_server_did_request_alert(Badge<WebContentClient>, const String& message);
    bool notify_server_did_request_confirm(Badge<WebContentClient>, const String& message);
    String notify_server_did_request_prompt(Badge<WebContentClient>, const String& message, const String& default_);
    void notify_server_did_get_source(const AK::URL& url, const String& source);
    void notify_server_did_get_dom_tree(const String& dom_tree);
    void notify_server_did_get_dom_node_properties(i32 node_id, String const& specified_style, String const& computed_style, String const& custom_properties);
    void notify_server_did_output_js_console_message(i32 message_index);
    void notify_server_did_get_js_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages);
    void notify_server_did_change_favicon(const Gfx::Bitmap& favicon);
    String notify_server_did_request_cookie(Badge<WebContentClient>, const AK::URL& url, Cookie::Source source);
    void notify_server_did_set_cookie(Badge<WebContentClient>, const AK::URL& url, const Cookie::ParsedCookie& cookie, Cookie::Source source);

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
