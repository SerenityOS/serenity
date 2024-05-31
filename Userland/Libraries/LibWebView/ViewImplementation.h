/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <AK/Queue.h>
#include <AK/String.h>
#include <LibCore/Promise.h>
#include <LibGfx/Forward.h>
#include <LibGfx/StandardCursor.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWeb/HTML/AudioPlayState.h>
#include <LibWeb/HTML/ColorPickerUpdateState.h>
#include <LibWeb/HTML/FileFilter.h>
#include <LibWeb/HTML/SelectItem.h>
#include <LibWeb/Page/InputEvent.h>
#include <LibWebView/Forward.h>
#include <LibWebView/WebContentClient.h>

namespace WebView {

class ViewImplementation {
public:
    virtual ~ViewImplementation();

    struct DOMNodeProperties {
        String computed_style_json;
        String resolved_style_json;
        String custom_properties_json;
        String node_box_sizing_json;
        String aria_properties_state_json;
    };

    void set_url(Badge<WebContentClient>, URL::URL url) { m_url = move(url); }
    URL::URL const& url() const { return m_url; }

    String const& handle() const { return m_client_state.client_handle; }

    void server_did_paint(Badge<WebContentClient>, i32 bitmap_id, Gfx::IntSize size);

    void load(URL::URL const&);
    void load_html(StringView);
    void load_empty_document();
    void reload();
    void traverse_the_history_by_delta(int delta);

    void zoom_in();
    void zoom_out();
    void reset_zoom();
    float zoom_level() const { return m_zoom_level; }
    float device_pixel_ratio() const { return m_device_pixel_ratio; }

    void enqueue_input_event(Web::InputEvent);
    void did_finish_handling_input_event(Badge<WebContentClient>, bool event_was_accepted);

    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);

    ByteString selected_text();
    Optional<String> selected_text_with_whitespace_collapsed();
    void select_all();
    void find_in_page(String const& query, CaseSensitivity = CaseSensitivity::CaseInsensitive);
    void find_in_page_next_match();
    void find_in_page_previous_match();
    void paste(String const&);

    void get_source();

    void inspect_dom_tree();
    void inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> pseudo_element);
    void inspect_accessibility_tree();
    void clear_inspected_dom_node();
    void get_hovered_node_id();

    void set_dom_node_text(i32 node_id, String text);
    void set_dom_node_tag(i32 node_id, String name);
    void add_dom_node_attributes(i32 node_id, Vector<Attribute> attributes);
    void replace_dom_node_attribute(i32 node_id, String name, Vector<Attribute> replacement_attributes);
    void create_child_element(i32 node_id);
    void create_child_text_node(i32 node_id);
    void clone_dom_node(i32 node_id);
    void remove_dom_node(i32 node_id);
    void get_dom_node_html(i32 node_id);

    void debug_request(ByteString const& request, ByteString const& argument = {});

    void run_javascript(StringView);
    void js_console_input(ByteString const& js_source);
    void js_console_request_messages(i32 start_index);

    void alert_closed();
    void confirm_closed(bool accepted);
    void prompt_closed(Optional<String> response);
    void color_picker_update(Optional<Color> picked_color, Web::HTML::ColorPickerUpdateState state);
    void file_picker_closed(Vector<Web::HTML::SelectedFile> selected_files);
    void select_dropdown_closed(Optional<u32> const& selected_item_id);

    void toggle_media_play_state();
    void toggle_media_mute_state();
    void toggle_media_loop_state();
    void toggle_media_controls_state();

    Web::HTML::MuteState page_mute_state() const { return m_mute_state; }
    void toggle_page_mute_state();

    void did_change_audio_play_state(Badge<WebContentClient>, Web::HTML::AudioPlayState);
    Web::HTML::AudioPlayState audio_play_state() const { return m_audio_play_state; }

    void did_update_navigation_buttons_state(Badge<WebContentClient>, bool back_enabled, bool forward_enabled) const;

    enum class ScreenshotType {
        Visible,
        Full,
    };
    NonnullRefPtr<Core::Promise<LexicalPath>> take_screenshot(ScreenshotType);
    NonnullRefPtr<Core::Promise<LexicalPath>> take_dom_node_screenshot(i32);
    virtual void did_receive_screenshot(Badge<WebContentClient>, Gfx::ShareableBitmap const&);

    ErrorOr<LexicalPath> dump_gc_graph();

    void set_user_style_sheet(String source);
    // Load Native.css as the User style sheet, which attempts to make WebView content look as close to
    // native GUI widgets as possible.
    void use_native_user_style_sheet();

    void enable_inspector_prototype();

    Function<void(Gfx::IntSize)> on_did_layout;
    Function<void()> on_ready_to_paint;
    Function<String(Web::HTML::ActivateTab, Web::HTML::WebViewHints, Optional<u64>)> on_new_web_view;
    Function<void()> on_activate_tab;
    Function<void()> on_close;
    Function<void(Gfx::IntPoint screen_position)> on_context_menu_request;
    Function<void(URL::URL const&, Gfx::IntPoint screen_position)> on_link_context_menu_request;
    Function<void(URL::URL const&, Gfx::IntPoint screen_position, Gfx::ShareableBitmap const&)> on_image_context_menu_request;
    Function<void(Gfx::IntPoint screen_position, Web::Page::MediaContextMenu const&)> on_media_context_menu_request;
    Function<void(URL::URL const&)> on_link_hover;
    Function<void()> on_link_unhover;
    Function<void(URL::URL const&, ByteString const& target, unsigned modifiers)> on_link_click;
    Function<void(URL::URL const&, ByteString const& target, unsigned modifiers)> on_link_middle_click;
    Function<void(ByteString const&)> on_title_change;
    Function<void(URL::URL const&)> on_url_change;
    Function<void(URL::URL const&, bool)> on_load_start;
    Function<void(URL::URL const&)> on_load_finish;
    Function<void(ByteString const& path, i32)> on_request_file;
    Function<void()> on_navigate_back;
    Function<void()> on_navigate_forward;
    Function<void()> on_refresh;
    Function<void(Gfx::Bitmap const&)> on_favicon_change;
    Function<void(i32, i32)> on_scroll_by_delta;
    Function<void(Gfx::IntPoint)> on_scroll_to_point;
    Function<void(Gfx::StandardCursor)> on_cursor_change;
    Function<void(Gfx::IntPoint, ByteString const&)> on_enter_tooltip_area;
    Function<void()> on_leave_tooltip_area;
    Function<void(String const& message)> on_request_alert;
    Function<void(String const& message)> on_request_confirm;
    Function<void(String const& message, String const& default_)> on_request_prompt;
    Function<void(String const& message)> on_request_set_prompt_text;
    Function<void()> on_request_accept_dialog;
    Function<void()> on_request_dismiss_dialog;
    Function<void(URL::URL const&, ByteString const&)> on_received_source;
    Function<void(ByteString const&)> on_received_dom_tree;
    Function<void(Optional<DOMNodeProperties>)> on_received_dom_node_properties;
    Function<void(ByteString const&)> on_received_accessibility_tree;
    Function<void(i32 node_id)> on_received_hovered_node_id;
    Function<void(Optional<i32> const& node_id)> on_finshed_editing_dom_node;
    Function<void(String const&)> on_received_dom_node_html;
    Function<void(i32 message_id)> on_received_console_message;
    Function<void(i32 start_index, Vector<ByteString> const& message_types, Vector<ByteString> const& messages)> on_received_console_messages;
    Function<Vector<Web::Cookie::Cookie>(URL::URL const& url)> on_get_all_cookies;
    Function<Optional<Web::Cookie::Cookie>(URL::URL const& url, String const& name)> on_get_named_cookie;
    Function<String(URL::URL const& url, Web::Cookie::Source source)> on_get_cookie;
    Function<void(URL::URL const& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source)> on_set_cookie;
    Function<void(Web::Cookie::Cookie const& cookie)> on_update_cookie;
    Function<void(i32 count_waiting)> on_resource_status_change;
    Function<void()> on_restore_window;
    Function<Gfx::IntPoint(Gfx::IntPoint)> on_reposition_window;
    Function<Gfx::IntSize(Gfx::IntSize)> on_resize_window;
    Function<Gfx::IntRect()> on_maximize_window;
    Function<Gfx::IntRect()> on_minimize_window;
    Function<Gfx::IntRect()> on_fullscreen_window;
    Function<void(Color current_color)> on_request_color_picker;
    Function<void(Web::HTML::FileFilter const& accepted_file_types, Web::HTML::AllowMultipleFiles)> on_request_file_picker;
    Function<void(Gfx::IntPoint content_position, i32 minimum_width, Vector<Web::HTML::SelectItem> items)> on_request_select_dropdown;
    Function<void(Web::KeyEvent const&)> on_finish_handling_key_event;
    Function<void()> on_text_test_finish;
    Function<void(Gfx::Color)> on_theme_color_change;
    Function<void(String const&, String const&, String const&)> on_insert_clipboard_entry;
    Function<void(Web::HTML::AudioPlayState)> on_audio_play_state_changed;
    Function<void(bool, bool)> on_navigation_buttons_state_changed;
    Function<void()> on_inspector_loaded;
    Function<void(i32, Optional<Web::CSS::Selector::PseudoElement::Type> const&)> on_inspector_selected_dom_node;
    Function<void(i32, String const&)> on_inspector_set_dom_node_text;
    Function<void(i32, String const&)> on_inspector_set_dom_node_tag;
    Function<void(i32, Vector<Attribute> const&)> on_inspector_added_dom_node_attributes;
    Function<void(i32, size_t, Vector<Attribute> const&)> on_inspector_replaced_dom_node_attribute;
    Function<void(i32, Gfx::IntPoint, String const&, Optional<String> const&, Optional<size_t> const&)> on_inspector_requested_dom_tree_context_menu;
    Function<void(String const&)> on_inspector_executed_console_script;
    Function<IPC::File()> on_request_worker_agent;

    virtual Web::DevicePixelRect viewport_rect() const = 0;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const = 0;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const = 0;

protected:
    static constexpr auto ZOOM_MIN_LEVEL = 0.3f;
    static constexpr auto ZOOM_MAX_LEVEL = 5.0f;
    static constexpr auto ZOOM_STEP = 0.1f;

    ViewImplementation();

    WebContentClient& client();
    WebContentClient const& client() const;
    u64 page_id() const;
    virtual void update_zoom() = 0;

    enum class WindowResizeInProgress {
        No,
        Yes,
    };
    void resize_backing_stores_if_needed(WindowResizeInProgress);

    void handle_resize();

    enum class CreateNewClient {
        No,
        Yes,
    };
    virtual void initialize_client(CreateNewClient = CreateNewClient::Yes) { }

    void handle_web_content_process_crash();

    struct SharedBitmap {
        i32 id { -1 };
        Web::DevicePixelSize last_painted_size;
        RefPtr<Gfx::Bitmap> bitmap;
    };

    struct ClientState {
        RefPtr<WebContentClient> client;
        String client_handle;
        SharedBitmap front_bitmap;
        SharedBitmap back_bitmap;
        u64 page_index { 0 };
        i32 next_bitmap_id { 0 };
        bool has_usable_bitmap { false };
    } m_client_state;

    URL::URL m_url;

    float m_zoom_level { 1.0 };
    float m_device_pixel_ratio { 1.0 };

    Queue<Web::InputEvent> m_pending_input_events;

    RefPtr<Core::Timer> m_backing_store_shrink_timer;

    RefPtr<Gfx::Bitmap> m_backup_bitmap;
    Web::DevicePixelSize m_backup_bitmap_size;

    size_t m_crash_count = 0;
    RefPtr<Core::Timer> m_repeated_crash_timer;

    RefPtr<Core::Promise<LexicalPath>> m_pending_screenshot;

    Web::HTML::AudioPlayState m_audio_play_state { Web::HTML::AudioPlayState::Paused };
    size_t m_number_of_elements_playing_audio { 0 };

    Web::HTML::MuteState m_mute_state { Web::HTML::MuteState::Unmuted };
};

}
