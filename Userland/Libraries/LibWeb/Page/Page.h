/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/API/KeyCode.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibGfx/StandardCursor.h>
#include <LibIPC/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Heap/Heap.h>
#include <LibURL/URL.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWeb/HTML/AudioPlayState.h>
#include <LibWeb/HTML/ColorPickerUpdateState.h>
#include <LibWeb/HTML/FileFilter.h>
#include <LibWeb/HTML/SelectItem.h>
#include <LibWeb/HTML/TokenizedFeatures.h>
#include <LibWeb/HTML/WebViewHints.h>
#include <LibWeb/Loader/FileRequest.h>
#include <LibWeb/PixelUnits.h>

namespace Web {

class PageClient;

class Page final : public JS::Cell {
    JS_CELL(Page, JS::Cell);
    JS_DECLARE_ALLOCATOR(Page);

public:
    static JS::NonnullGCPtr<Page> create(JS::VM&, JS::NonnullGCPtr<PageClient>);

    ~Page();

    PageClient& client() { return m_client; }
    PageClient const& client() const { return m_client; }

    void set_top_level_traversable(JS::NonnullGCPtr<HTML::TraversableNavigable>);

    // FIXME: This is a hack.
    bool top_level_traversable_is_initialized() const;

    HTML::BrowsingContext& top_level_browsing_context();
    HTML::BrowsingContext const& top_level_browsing_context() const;

    JS::NonnullGCPtr<HTML::TraversableNavigable> top_level_traversable() const;

    HTML::Navigable& focused_navigable();
    HTML::Navigable const& focused_navigable() const { return const_cast<Page*>(this)->focused_navigable(); }

    void set_focused_navigable(Badge<EventHandler>, HTML::Navigable&);

    void load(URL::URL const&);

    void load_html(StringView);

    void reload();

    void traverse_the_history_by_delta(int delta);

    CSSPixelPoint device_to_css_point(DevicePixelPoint) const;
    DevicePixelPoint css_to_device_point(CSSPixelPoint) const;
    DevicePixelRect css_to_device_rect(CSSPixelRect) const;
    CSSPixelRect device_to_css_rect(DevicePixelRect) const;
    DevicePixelRect enclosing_device_rect(CSSPixelRect) const;
    DevicePixelRect rounded_device_rect(CSSPixelRect) const;

    bool handle_mouseup(DevicePixelPoint, DevicePixelPoint screen_position, unsigned button, unsigned buttons, unsigned modifiers);
    bool handle_mousedown(DevicePixelPoint, DevicePixelPoint screen_position, unsigned button, unsigned buttons, unsigned modifiers);
    bool handle_mousemove(DevicePixelPoint, DevicePixelPoint screen_position, unsigned buttons, unsigned modifiers);
    bool handle_mousewheel(DevicePixelPoint, DevicePixelPoint screen_position, unsigned button, unsigned buttons, unsigned modifiers, DevicePixels wheel_delta_x, DevicePixels wheel_delta_y);
    bool handle_doubleclick(DevicePixelPoint, DevicePixelPoint screen_position, unsigned button, unsigned buttons, unsigned modifiers);

    bool handle_keydown(KeyCode, unsigned modifiers, u32 code_point);
    bool handle_keyup(KeyCode, unsigned modifiers, u32 code_point);

    Gfx::Palette palette() const;
    CSSPixelRect web_exposed_screen_area() const;
    CSS::PreferredColorScheme preferred_color_scheme() const;

    bool is_same_origin_policy_enabled() const { return m_same_origin_policy_enabled; }
    void set_same_origin_policy_enabled(bool b) { m_same_origin_policy_enabled = b; }

    bool is_scripting_enabled() const { return m_is_scripting_enabled; }
    void set_is_scripting_enabled(bool b) { m_is_scripting_enabled = b; }

    bool should_block_pop_ups() const { return m_should_block_pop_ups; }
    void set_should_block_pop_ups(bool b) { m_should_block_pop_ups = b; }

    bool is_webdriver_active() const { return m_is_webdriver_active; }
    void set_is_webdriver_active(bool b) { m_is_webdriver_active = b; }

    DevicePixelPoint window_position() const { return m_window_position; }
    void set_window_position(DevicePixelPoint position) { m_window_position = position; }

    DevicePixelSize window_size() const { return m_window_size; }
    void set_window_size(DevicePixelSize size) { m_window_size = size; }

    void did_request_alert(String const& message);
    void alert_closed();

    bool did_request_confirm(String const& message);
    void confirm_closed(bool accepted);

    Optional<String> did_request_prompt(String const& message, String const& default_);
    void prompt_closed(Optional<String> response);

    enum class PendingDialog {
        None,
        Alert,
        Confirm,
        Prompt,
    };
    bool has_pending_dialog() const { return m_pending_dialog != PendingDialog::None; }
    PendingDialog pending_dialog() const { return m_pending_dialog; }
    Optional<String> const& pending_dialog_text() const { return m_pending_dialog_text; }
    void dismiss_dialog();
    void accept_dialog();

    void did_request_color_picker(WeakPtr<HTML::HTMLInputElement> target, Color current_color);
    void color_picker_update(Optional<Color> picked_color, HTML::ColorPickerUpdateState state);

    void did_request_file_picker(WeakPtr<HTML::HTMLInputElement> target, HTML::FileFilter accepted_file_types, HTML::AllowMultipleFiles);
    void file_picker_closed(Span<HTML::SelectedFile> selected_files);

    void did_request_select_dropdown(WeakPtr<HTML::HTMLSelectElement> target, Web::CSSPixelPoint content_position, Web::CSSPixels minimum_width, Vector<Web::HTML::SelectItem> items);
    void select_dropdown_closed(Optional<u32> const& selected_item_id);

    enum class PendingNonBlockingDialog {
        None,
        ColorPicker,
        FilePicker,
        Select,
    };

    void register_media_element(Badge<HTML::HTMLMediaElement>, int media_id);
    void unregister_media_element(Badge<HTML::HTMLMediaElement>, int media_id);

    struct MediaContextMenu {
        URL::URL media_url;
        bool is_video { false };
        bool is_playing { false };
        bool is_muted { false };
        bool has_user_agent_controls { false };
        bool is_looping { false };
    };
    void did_request_media_context_menu(i32 media_id, CSSPixelPoint, ByteString const& target, unsigned modifiers, MediaContextMenu);
    WebIDL::ExceptionOr<void> toggle_media_play_state();
    void toggle_media_mute_state();
    WebIDL::ExceptionOr<void> toggle_media_loop_state();
    WebIDL::ExceptionOr<void> toggle_media_controls_state();

    HTML::MuteState page_mute_state() const { return m_mute_state; }
    void toggle_page_mute_state();

    Optional<String> const& user_style() const { return m_user_style_sheet_source; }
    void set_user_style(String source);

    bool pdf_viewer_supported() const { return m_pdf_viewer_supported; }

    void clear_selection();

    void find_in_page(String const& query, CaseSensitivity);
    void find_in_page_next_match();
    void find_in_page_previous_match();

private:
    explicit Page(JS::NonnullGCPtr<PageClient>);
    virtual void visit_edges(Visitor&) override;

    JS::GCPtr<HTML::HTMLMediaElement> media_context_menu_element();

    void update_find_in_page_selection();

    JS::NonnullGCPtr<PageClient> m_client;

    WeakPtr<HTML::Navigable> m_focused_navigable;

    JS::GCPtr<HTML::TraversableNavigable> m_top_level_traversable;

    // FIXME: Enable this by default once CORS preflight checks are supported.
    bool m_same_origin_policy_enabled { false };

    bool m_is_scripting_enabled { true };

    bool m_should_block_pop_ups { true };

    // https://w3c.github.io/webdriver/#dfn-webdriver-active-flag
    // The webdriver-active flag is set to true when the user agent is under remote control. It is initially false.
    bool m_is_webdriver_active { false };

    DevicePixelPoint m_window_position {};
    DevicePixelSize m_window_size {};

    PendingDialog m_pending_dialog { PendingDialog::None };
    Optional<String> m_pending_dialog_text;
    Optional<Empty> m_pending_alert_response;
    Optional<bool> m_pending_confirm_response;
    Optional<Optional<String>> m_pending_prompt_response;

    PendingNonBlockingDialog m_pending_non_blocking_dialog { PendingNonBlockingDialog::None };
    WeakPtr<HTML::HTMLElement> m_pending_non_blocking_dialog_target;

    Vector<int> m_media_elements;
    Optional<int> m_media_context_menu_element_id;

    Web::HTML::MuteState m_mute_state { Web::HTML::MuteState::Unmuted };

    Optional<String> m_user_style_sheet_source;

    // https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-supported
    // Each user agent has a PDF viewer supported boolean, whose value is implementation-defined (and might vary according to user preferences).
    // Spec Note: This value also impacts the navigation processing model.
    // FIXME: Actually support pdf viewing
    bool m_pdf_viewer_supported { false };
    size_t m_find_in_page_match_index { 0 };
    Vector<JS::NonnullGCPtr<DOM::Range>> m_find_in_page_matches;
};

struct PaintOptions {
    enum class PaintOverlay {
        No,
        Yes,
    };

    PaintOverlay paint_overlay { PaintOverlay::Yes };
};

class PageClient : public JS::Cell {
    JS_CELL(PageClient, JS::Cell);

public:
    virtual Page& page() = 0;
    virtual Page const& page() const = 0;
    virtual bool is_connection_open() const = 0;
    virtual Gfx::Palette palette() const = 0;
    virtual DevicePixelRect screen_rect() const = 0;
    virtual double device_pixels_per_css_pixel() const = 0;
    virtual CSS::PreferredColorScheme preferred_color_scheme() const = 0;
    virtual void paint_next_frame() = 0;
    virtual void paint(DevicePixelRect const&, Gfx::Bitmap&, PaintOptions = {}) = 0;
    virtual void page_did_change_title(ByteString const&) { }
    virtual void page_did_change_url(URL::URL const&) { }
    virtual void page_did_request_navigate_back() { }
    virtual void page_did_request_navigate_forward() { }
    virtual void page_did_request_refresh() { }
    virtual Gfx::IntSize page_did_request_resize_window(Gfx::IntSize) { return {}; }
    virtual Gfx::IntPoint page_did_request_reposition_window(Gfx::IntPoint) { return {}; }
    virtual void page_did_request_restore_window() { }
    virtual Gfx::IntRect page_did_request_maximize_window() { return {}; }
    virtual Gfx::IntRect page_did_request_minimize_window() { return {}; }
    virtual Gfx::IntRect page_did_request_fullscreen_window() { return {}; }
    virtual void page_did_start_loading(URL::URL const&, bool is_redirect) { (void)is_redirect; }
    virtual void page_did_create_new_document(Web::DOM::Document&) { }
    virtual void page_did_change_active_document_in_top_level_browsing_context(Web::DOM::Document&) { }
    virtual void page_did_destroy_document(Web::DOM::Document&) { }
    virtual void page_did_finish_loading(URL::URL const&) { }
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) { }
    virtual void page_did_request_context_menu(CSSPixelPoint) { }
    virtual void page_did_request_link_context_menu(CSSPixelPoint, URL::URL const&, [[maybe_unused]] ByteString const& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_request_image_context_menu(CSSPixelPoint, URL::URL const&, [[maybe_unused]] ByteString const& target, [[maybe_unused]] unsigned modifiers, Gfx::Bitmap const*) { }
    virtual void page_did_request_media_context_menu(CSSPixelPoint, [[maybe_unused]] ByteString const& target, [[maybe_unused]] unsigned modifiers, Page::MediaContextMenu) { }
    virtual void page_did_middle_click_link(URL::URL const&, [[maybe_unused]] ByteString const& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_enter_tooltip_area(CSSPixelPoint, ByteString const&) { }
    virtual void page_did_leave_tooltip_area() { }
    virtual void page_did_hover_link(URL::URL const&) { }
    virtual void page_did_unhover_link() { }
    virtual void page_did_change_favicon(Gfx::Bitmap const&) { }
    virtual void page_did_layout() { }
    virtual void page_did_request_scroll(i32, i32) { }
    virtual void page_did_request_scroll_to(CSSPixelPoint) { }
    virtual void page_did_request_alert(String const&) { }
    virtual void page_did_request_confirm(String const&) { }
    virtual void page_did_request_prompt(String const&, String const&) { }
    virtual void page_did_request_set_prompt_text(String const&) { }
    virtual void page_did_request_accept_dialog() { }
    virtual void page_did_request_dismiss_dialog() { }
    virtual Vector<Web::Cookie::Cookie> page_did_request_all_cookies(URL::URL const&) { return {}; }
    virtual Optional<Web::Cookie::Cookie> page_did_request_named_cookie(URL::URL const&, String const&) { return {}; }
    virtual String page_did_request_cookie(URL::URL const&, Cookie::Source) { return {}; }
    virtual void page_did_set_cookie(URL::URL const&, Cookie::ParsedCookie const&, Cookie::Source) { }
    virtual void page_did_update_cookie(Web::Cookie::Cookie) { }
    virtual void page_did_update_resource_count(i32) { }
    struct NewWebViewResult {
        JS::GCPtr<Page> page;
        String window_handle;
    };
    virtual NewWebViewResult page_did_request_new_web_view(HTML::ActivateTab, HTML::WebViewHints, HTML::TokenizedFeature::NoOpener) { return {}; }
    virtual void page_did_request_activate_tab() { }
    virtual void page_did_close_top_level_traversable() { }
    virtual void page_did_update_navigation_buttons_state([[maybe_unused]] bool back_enabled, [[maybe_unused]] bool forward_enabled) { }

    virtual void request_file(FileRequest) = 0;

    // https://html.spec.whatwg.org/multipage/input.html#show-the-picker,-if-applicable
    virtual void page_did_request_color_picker([[maybe_unused]] Color current_color) { }
    virtual void page_did_request_file_picker([[maybe_unused]] HTML::FileFilter accepted_file_types, Web::HTML::AllowMultipleFiles) { }
    virtual void page_did_request_select_dropdown([[maybe_unused]] Web::CSSPixelPoint content_position, [[maybe_unused]] Web::CSSPixels minimum_width, [[maybe_unused]] Vector<Web::HTML::SelectItem> items) { }

    virtual void page_did_finish_text_test() { }

    virtual void page_did_change_theme_color(Gfx::Color) { }

    virtual void page_did_insert_clipboard_entry([[maybe_unused]] String data, [[maybe_unused]] String presentation_style, [[maybe_unused]] String mime_type) { }

    virtual void page_did_change_audio_play_state(HTML::AudioPlayState) { }

    virtual IPC::File request_worker_agent() { return IPC::File {}; }

    virtual void inspector_did_load() { }
    virtual void inspector_did_select_dom_node([[maybe_unused]] i32 node_id, [[maybe_unused]] Optional<CSS::Selector::PseudoElement::Type> const& pseudo_element) { }
    virtual void inspector_did_set_dom_node_text([[maybe_unused]] i32 node_id, [[maybe_unused]] String const& text) { }
    virtual void inspector_did_set_dom_node_tag([[maybe_unused]] i32 node_id, [[maybe_unused]] String const& tag) { }
    virtual void inspector_did_add_dom_node_attributes([[maybe_unused]] i32 node_id, [[maybe_unused]] JS::NonnullGCPtr<DOM::NamedNodeMap> attributes) { }
    virtual void inspector_did_replace_dom_node_attribute([[maybe_unused]] i32 node_id, [[maybe_unused]] size_t attribute_index, [[maybe_unused]] JS::NonnullGCPtr<DOM::NamedNodeMap> replacement_attributes) { }
    virtual void inspector_did_request_dom_tree_context_menu([[maybe_unused]] i32 node_id, [[maybe_unused]] CSSPixelPoint position, [[maybe_unused]] String const& type, [[maybe_unused]] Optional<String> const& tag, [[maybe_unused]] Optional<size_t> const& attribute_index) { }
    virtual void inspector_did_execute_console_script([[maybe_unused]] String const& script) { }

    virtual void schedule_repaint() = 0;
    virtual bool is_ready_to_paint() const = 0;

protected:
    virtual ~PageClient() = default;
};

}

namespace IPC {

template<>
ErrorOr<void> encode(Encoder&, Web::Page::MediaContextMenu const&);

template<>
ErrorOr<Web::Page::MediaContextMenu> decode(Decoder&);

}
