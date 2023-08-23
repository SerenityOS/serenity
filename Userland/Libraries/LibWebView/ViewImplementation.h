/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibGfx/StandardCursor.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWebView/Forward.h>
#include <LibWebView/WebContentClient.h>

namespace WebView {

// Note: This only exists inside Serenity to avoid #ifdefs in all implementors of ViewImplementation.
enum class EnableCallgrindProfiling {
    No,
    Yes
};

enum class IsLayoutTestMode {
    No,
    Yes
};

class ViewImplementation {
public:
    virtual ~ViewImplementation() { }

    struct DOMNodeProperties {
        String computed_style_json;
        String resolved_style_json;
        String custom_properties_json;
        String node_box_sizing_json;
        String aria_properties_state_json;
    };

    void set_url(Badge<WebContentClient>, AK::URL url) { m_url = move(url); }
    AK::URL const& url() const { return m_url; }

    String const& handle() const { return m_client_state.client_handle; }

    void server_did_paint(Badge<WebContentClient>, i32 bitmap_id, Gfx::IntSize size);
    void server_did_invalidate_content_rect(Badge<WebContentClient>, Gfx::IntRect rect);
    void server_did_change_selection(Badge<WebContentClient>);

    void load(AK::URL const&);
    void load_html(StringView, AK::URL const&);
    void load_empty_document();

    void zoom_in();
    void zoom_out();
    void reset_zoom();
    float zoom_level() const { return m_zoom_level; }

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
    void js_console_input(DeprecatedString const& js_source);
    void js_console_request_messages(i32 start_index);

    void alert_closed();
    void confirm_closed(bool accepted);
    void prompt_closed(Optional<String> response);

    void toggle_media_play_state();
    void toggle_media_mute_state();
    void toggle_media_loop_state();
    void toggle_media_controls_state();

    enum class ScreenshotType {
        Visible,
        Full,
    };
    ErrorOr<void> take_screenshot(ScreenshotType);

    Function<void(Gfx::IntSize)> on_did_layout;
    Function<void()> on_ready_to_paint;
    Function<String(Web::HTML::ActivateTab)> on_new_tab;
    Function<void()> on_activate_tab;
    Function<void()> on_close;
    Function<void(Gfx::IntPoint screen_position)> on_context_menu_request;
    Function<void(const AK::URL&, Gfx::IntPoint screen_position)> on_link_context_menu_request;
    Function<void(const AK::URL&, Gfx::IntPoint screen_position, Gfx::ShareableBitmap const&)> on_image_context_menu_request;
    Function<void(Gfx::IntPoint screen_position, Web::Page::MediaContextMenu const&)> on_media_context_menu_request;
    Function<void(const AK::URL&)> on_link_hover;
    Function<void()> on_link_unhover;
    Function<void(const AK::URL&, DeprecatedString const& target, unsigned modifiers)> on_link_click;
    Function<void(const AK::URL&, DeprecatedString const& target, unsigned modifiers)> on_link_middle_click;
    Function<void(DeprecatedString const&)> on_title_change;
    Function<void(const AK::URL&, bool)> on_load_start;
    Function<void(const AK::URL&)> on_load_finish;
    Function<void(DeprecatedString const& path, i32)> on_request_file;
    Function<void()> on_navigate_back;
    Function<void()> on_navigate_forward;
    Function<void()> on_refresh;
    Function<void(Gfx::Bitmap const&)> on_favicon_change;
    Function<void(i32, i32)> on_scroll_by_delta;
    Function<void(Gfx::IntPoint)> on_scroll_to_point;
    Function<void(Gfx::IntRect)> on_scroll_into_view;
    Function<void(Gfx::StandardCursor)> on_cursor_change;
    Function<void(Gfx::IntPoint, DeprecatedString const&)> on_enter_tooltip_area;
    Function<void()> on_leave_tooltip_area;
    Function<void(String const& message)> on_request_alert;
    Function<void(String const& message)> on_request_confirm;
    Function<void(String const& message, String const& default_)> on_request_prompt;
    Function<void(String const& message)> on_request_set_prompt_text;
    Function<void()> on_request_accept_dialog;
    Function<void()> on_request_dismiss_dialog;
    Function<void(const AK::URL&, DeprecatedString const&)> on_get_source;
    Function<void(DeprecatedString const&)> on_get_dom_tree;
    Function<void(i32 node_id, DeprecatedString const& computed_style, DeprecatedString const& resolved_style, DeprecatedString const& custom_properties, DeprecatedString const& node_box_sizing, DeprecatedString const& aria_properties_state)> on_get_dom_node_properties;
    Function<void(DeprecatedString const&)> on_get_accessibility_tree;
    Function<void(i32 message_id)> on_js_console_new_message;
    Function<void(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages)> on_get_js_console_messages;
    Function<Vector<Web::Cookie::Cookie>(AK::URL const& url)> on_get_all_cookies;
    Function<Optional<Web::Cookie::Cookie>(AK::URL const& url, DeprecatedString const& name)> on_get_named_cookie;
    Function<DeprecatedString(const AK::URL& url, Web::Cookie::Source source)> on_get_cookie;
    Function<void(const AK::URL& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)> on_set_cookie;
    Function<void(Web::Cookie::Cookie const& cookie)> on_update_cookie;
    Function<void(i32 count_waiting)> on_resource_status_change;
    Function<void()> on_restore_window;
    Function<Gfx::IntPoint(Gfx::IntPoint)> on_reposition_window;
    Function<Gfx::IntSize(Gfx::IntSize)> on_resize_window;
    Function<Gfx::IntRect()> on_maximize_window;
    Function<Gfx::IntRect()> on_minimize_window;
    Function<Gfx::IntRect()> on_fullscreen_window;
    Function<void(bool)> on_finish_handling_input_event;

    virtual Gfx::IntRect viewport_rect() const = 0;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const = 0;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const = 0;

protected:
    static constexpr auto ZOOM_MIN_LEVEL = 0.3f;
    static constexpr auto ZOOM_MAX_LEVEL = 5.0f;
    static constexpr auto ZOOM_STEP = 0.1f;

    ViewImplementation();

    WebContentClient& client();
    WebContentClient const& client() const;
    virtual void update_zoom() = 0;

    enum class WindowResizeInProgress {
        No,
        Yes,
    };
    void resize_backing_stores_if_needed(WindowResizeInProgress);

    void request_repaint();
    void handle_resize();

    virtual void create_client(EnableCallgrindProfiling = EnableCallgrindProfiling::No) { }

    void handle_web_content_process_crash();

    struct SharedBitmap {
        i32 id { -1 };
        i32 pending_paints { 0 };
        Gfx::IntSize last_painted_size;
        RefPtr<Gfx::Bitmap> bitmap;
    };

    struct ClientState {
        RefPtr<WebContentClient> client;
        String client_handle;
        SharedBitmap front_bitmap;
        SharedBitmap back_bitmap;
        i32 next_bitmap_id { 0 };
        bool has_usable_bitmap { false };
        bool got_repaint_requests_while_painting { false };
    } m_client_state;

    AK::URL m_url;

    float m_zoom_level { 1.0 };
    float m_device_pixel_ratio { 1.0 };

    RefPtr<Core::Timer> m_backing_store_shrink_timer;

    RefPtr<Gfx::Bitmap> m_backup_bitmap;
    Gfx::IntSize m_backup_bitmap_size;

    size_t m_crash_count = 0;
    RefPtr<Core::Timer> m_repeated_crash_timer;
};

}
