/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibGfx/StandardCursor.h>
#include <LibWeb/Forward.h>
#include <LibWebView/Forward.h>
#include <LibWebView/WebContentClient.h>

namespace WebView {

class ViewImplementation {
public:
    virtual ~ViewImplementation() { }

    struct DOMNodeProperties {
        String computed_style_json;
        String resolved_style_json;
        String custom_properties_json;
        String node_box_sizing_json;
    };

    AK::URL const& url() const { return m_url; }

    void load(AK::URL const&);
    void load_html(StringView, AK::URL const&);
    void load_empty_document();

    void zoom_in();
    void zoom_out();
    void reset_zoom();

    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);

    DeprecatedString selected_text();
    void select_all();

    void get_source();

    void inspect_dom_tree();
    void inspect_accessibility_tree();
    ErrorOr<DOMNodeProperties> inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> pseudo_element);
    void clear_inspected_dom_node();
    i32 get_hovered_node_id();

    void debug_request(DeprecatedString const& request, DeprecatedString const& argument = {});

    void run_javascript(StringView);

    virtual void notify_server_did_layout(Badge<WebContentClient>, Gfx::IntSize content_size) = 0;
    virtual void notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id) = 0;
    virtual void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, Gfx::IntRect const&) = 0;
    virtual void notify_server_did_change_selection(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor) = 0;
    virtual void notify_server_did_change_title(Badge<WebContentClient>, DeprecatedString const&) = 0;
    virtual void notify_server_did_request_scroll(Badge<WebContentClient>, i32, i32) = 0;
    virtual void notify_server_did_request_scroll_to(Badge<WebContentClient>, Gfx::IntPoint) = 0;
    virtual void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, Gfx::IntRect const&) = 0;
    virtual void notify_server_did_enter_tooltip_area(Badge<WebContentClient>, Gfx::IntPoint, DeprecatedString const&) = 0;
    virtual void notify_server_did_leave_tooltip_area(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_hover_link(Badge<WebContentClient>, const AK::URL&) = 0;
    virtual void notify_server_did_unhover_link(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_click_link(Badge<WebContentClient>, const AK::URL&, DeprecatedString const& target, unsigned modifiers) = 0;
    virtual void notify_server_did_middle_click_link(Badge<WebContentClient>, const AK::URL&, DeprecatedString const& target, unsigned modifiers) = 0;
    virtual void notify_server_did_start_loading(Badge<WebContentClient>, const AK::URL&, bool is_redirect) = 0;
    virtual void notify_server_did_finish_loading(Badge<WebContentClient>, const AK::URL&) = 0;
    virtual void notify_server_did_request_navigate_back(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_request_navigate_forward(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_request_refresh(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_request_context_menu(Badge<WebContentClient>, Gfx::IntPoint) = 0;
    virtual void notify_server_did_request_link_context_menu(Badge<WebContentClient>, Gfx::IntPoint, const AK::URL&, DeprecatedString const& target, unsigned modifiers) = 0;
    virtual void notify_server_did_request_image_context_menu(Badge<WebContentClient>, Gfx::IntPoint, const AK::URL&, DeprecatedString const& target, unsigned modifiers, Gfx::ShareableBitmap const&) = 0;
    virtual void notify_server_did_request_alert(Badge<WebContentClient>, DeprecatedString const& message) = 0;
    virtual void notify_server_did_request_confirm(Badge<WebContentClient>, DeprecatedString const& message) = 0;
    virtual void notify_server_did_request_prompt(Badge<WebContentClient>, DeprecatedString const& message, DeprecatedString const& default_) = 0;
    virtual void notify_server_did_request_set_prompt_text(Badge<WebContentClient>, DeprecatedString const& message) = 0;
    virtual void notify_server_did_request_accept_dialog(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_request_dismiss_dialog(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_get_source(const AK::URL& url, DeprecatedString const& source) = 0;
    virtual void notify_server_did_get_dom_tree(DeprecatedString const& dom_tree) = 0;
    virtual void notify_server_did_get_dom_node_properties(i32 node_id, DeprecatedString const& computed_style, DeprecatedString const& resolved_style, DeprecatedString const& custom_properties, DeprecatedString const& node_box_sizing) = 0;
    virtual void notify_server_did_get_accessibility_tree(DeprecatedString const& accessibility_tree) = 0;
    virtual void notify_server_did_output_js_console_message(i32 message_index) = 0;
    virtual void notify_server_did_get_js_console_messages(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages) = 0;
    virtual void notify_server_did_change_favicon(Gfx::Bitmap const& favicon) = 0;
    virtual Vector<Web::Cookie::Cookie> notify_server_did_request_all_cookies(Badge<WebContentClient>, AK::URL const& url) = 0;
    virtual Optional<Web::Cookie::Cookie> notify_server_did_request_named_cookie(Badge<WebContentClient>, AK::URL const& url, DeprecatedString const& name) = 0;
    virtual DeprecatedString notify_server_did_request_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::Source source) = 0;
    virtual void notify_server_did_set_cookie(Badge<WebContentClient>, const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source) = 0;
    virtual void notify_server_did_update_cookie(Badge<WebContentClient>, Web::Cookie::Cookie const& cookie) = 0;
    virtual void notify_server_did_close_browsing_context(Badge<WebContentClient>) = 0;
    virtual void notify_server_did_update_resource_count(i32 count_waiting) = 0;
    virtual void notify_server_did_request_restore_window() = 0;
    virtual Gfx::IntPoint notify_server_did_request_reposition_window(Gfx::IntPoint) = 0;
    virtual Gfx::IntSize notify_server_did_request_resize_window(Gfx::IntSize) = 0;
    virtual Gfx::IntRect notify_server_did_request_maximize_window() = 0;
    virtual Gfx::IntRect notify_server_did_request_minimize_window() = 0;
    virtual Gfx::IntRect notify_server_did_request_fullscreen_window() = 0;
    virtual void notify_server_did_request_file(Badge<WebContentClient>, DeprecatedString const& path, i32) = 0;
    virtual void notify_server_did_finish_handling_input_event(bool event_was_accepted) = 0;

protected:
    static constexpr auto ZOOM_MIN_LEVEL = 0.3f;
    static constexpr auto ZOOM_MAX_LEVEL = 5.0f;
    static constexpr auto ZOOM_STEP = 0.1f;

    WebContentClient& client();
    WebContentClient const& client() const;
    virtual void create_client() = 0;
    virtual void update_zoom() = 0;

#if !defined(AK_OS_SERENITY)
    ErrorOr<NonnullRefPtr<WebView::WebContentClient>> launch_web_content_process(ReadonlySpan<String> candidate_web_content_paths, StringView webdriver_content_ipc_path);
#endif

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

    AK::URL m_url;

    float m_zoom_level { 1.0 };
    float m_device_pixel_ratio { 1.0 };
};

}
