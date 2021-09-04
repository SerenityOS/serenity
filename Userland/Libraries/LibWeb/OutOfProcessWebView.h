/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Widget.h>
#include <LibWeb/WebViewHooks.h>

namespace Web {

class WebContentClient;

class OutOfProcessWebView final
    : public GUI::AbstractScrollableWidget
    , public Web::WebViewHooks {
    C_OBJECT(OutOfProcessWebView);

public:
    virtual ~OutOfProcessWebView() override;

    URL url() const { return m_url; }
    void load(URL const&);

    void load_html(StringView const&, URL const&);
    void load_empty_document();

    void debug_request(String const& request, String const& argument = {});
    void get_source();

    void inspect_dom_tree();
    struct DOMNodeProperties {
        String specified_values_json;
        String computed_values_json;
    };
    Optional<DOMNodeProperties> inspect_dom_node(i32 node_id);
    void clear_inspected_dom_node();
    i32 get_hovered_node_id();

    void js_console_initialize();
    void js_console_input(String const& js_source);

    void run_javascript(StringView);

    String selected_text();
    void select_all();

    void notify_server_did_layout(Badge<WebContentClient>, const Gfx::IntSize& content_size);
    void notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id);
    void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, const Gfx::IntRect&);
    void notify_server_did_change_selection(Badge<WebContentClient>);
    void notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor);
    void notify_server_did_change_title(Badge<WebContentClient>, String const&);
    void notify_server_did_request_scroll(Badge<WebContentClient>, int);
    void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, const Gfx::IntRect&);
    void notify_server_did_enter_tooltip_area(Badge<WebContentClient>, const Gfx::IntPoint&, String const&);
    void notify_server_did_leave_tooltip_area(Badge<WebContentClient>);
    void notify_server_did_hover_link(Badge<WebContentClient>, URL const&);
    void notify_server_did_unhover_link(Badge<WebContentClient>);
    void notify_server_did_click_link(Badge<WebContentClient>, URL const&, String const& target, unsigned modifiers);
    void notify_server_did_middle_click_link(Badge<WebContentClient>, URL const&, String const& target, unsigned modifiers);
    void notify_server_did_start_loading(Badge<WebContentClient>, URL const&);
    void notify_server_did_finish_loading(Badge<WebContentClient>, URL const&);
    void notify_server_did_request_context_menu(Badge<WebContentClient>, const Gfx::IntPoint&);
    void notify_server_did_request_link_context_menu(Badge<WebContentClient>, const Gfx::IntPoint&, URL const&, String const& target, unsigned modifiers);
    void notify_server_did_request_image_context_menu(Badge<WebContentClient>, const Gfx::IntPoint&, URL const&, String const& target, unsigned modifiers, const Gfx::ShareableBitmap&);
    void notify_server_did_request_alert(Badge<WebContentClient>, String const& message);
    bool notify_server_did_request_confirm(Badge<WebContentClient>, String const& message);
    String notify_server_did_request_prompt(Badge<WebContentClient>, String const& message, String const& default_);
    void notify_server_did_get_source(URL const& url, String const& source);
    void notify_server_did_get_dom_tree(String const& dom_tree);
    void notify_server_did_get_dom_node_properties(i32 node_id, String const& specified_style, String const& computed_style);
    void notify_server_did_js_console_output(String const& method, String const& line);
    void notify_server_did_change_favicon(const Gfx::Bitmap& favicon);
    String notify_server_did_request_cookie(Badge<WebContentClient>, URL const& url, Cookie::Source source);
    void notify_server_did_set_cookie(Badge<WebContentClient>, URL const& url, const Cookie::ParsedCookie& cookie, Cookie::Source source);

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
    virtual void theme_change_event(GUI::ThemeChangeEvent&) override;
    virtual void screen_rects_change_event(GUI::ScreenRectsChangeEvent&) override;

    // ^AbstractScrollableWidget
    virtual void did_scroll() override;

    void request_repaint();
    void handle_resize();

    void create_client();
    WebContentClient& client();

    void handle_web_content_process_crash();

    URL m_url;

    struct ClientState {
        RefPtr<WebContentClient> client;
        RefPtr<Gfx::Bitmap> front_bitmap;
        RefPtr<Gfx::Bitmap> back_bitmap;
        i32 front_bitmap_id { -1 };
        i32 back_bitmap_id { -1 };
        i32 next_bitmap_id { 0 };
        bool has_usable_bitmap { false };
    } m_client_state;

    RefPtr<Gfx::Bitmap> m_backup_bitmap;
};

}
