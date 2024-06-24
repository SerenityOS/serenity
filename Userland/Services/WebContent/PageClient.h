/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibAccelGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibWeb/HTML/AudioPlayState.h>
#include <LibWeb/HTML/FileFilter.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/PixelUnits.h>
#include <WebContent/Forward.h>

#ifdef HAS_ACCELERATED_GRAPHICS
#    include <LibAccelGfx/Context.h>
#endif

namespace WebContent {

class PageClient final : public Web::PageClient {
    JS_CELL(PageClient, Web::PageClient);
    JS_DECLARE_ALLOCATOR(PageClient);

public:
    static JS::NonnullGCPtr<PageClient> create(JS::VM& vm, PageHost& page_host, u64 id);

    virtual ~PageClient() override;

    static void set_use_gpu_painter();
    static void set_use_experimental_cpu_transform_support();

    virtual void schedule_repaint() override;
    virtual bool is_ready_to_paint() const override;

    virtual Web::Page& page() override { return *m_page; }
    virtual Web::Page const& page() const override { return *m_page; }

    ErrorOr<void> connect_to_webdriver(ByteString const& webdriver_ipc_path);

    virtual void paint_next_frame() override;
    virtual void paint(Web::DevicePixelRect const& content_rect, Gfx::Bitmap&, Web::PaintOptions = {}) override;

    void set_palette_impl(Gfx::PaletteImpl&);
    void set_viewport_rect(Web::DevicePixelRect const&);
    void set_screen_rects(Vector<Web::DevicePixelRect, 4> const& rects, size_t main_screen_index) { m_screen_rect = rects[main_screen_index]; }
    void set_device_pixels_per_css_pixel(float device_pixels_per_css_pixel) { m_device_pixels_per_css_pixel = device_pixels_per_css_pixel; }
    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);
    void set_should_show_line_box_borders(bool b) { m_should_show_line_box_borders = b; }
    void set_has_focus(bool);
    void set_is_scripting_enabled(bool);
    void set_window_position(Web::DevicePixelPoint);
    void set_window_size(Web::DevicePixelSize);

    Web::DevicePixelSize content_size() const { return m_content_size; }

    Web::WebIDL::ExceptionOr<void> toggle_media_play_state();
    void toggle_media_mute_state();
    Web::WebIDL::ExceptionOr<void> toggle_media_loop_state();
    Web::WebIDL::ExceptionOr<void> toggle_media_controls_state();

    void alert_closed();
    void confirm_closed(bool accepted);
    void prompt_closed(Optional<String> response);
    void color_picker_update(Optional<Color> picked_color, Web::HTML::ColorPickerUpdateState state);
    void select_dropdown_closed(Optional<u32> const& selected_item_id);

    void set_user_style(String source);

    void ready_to_paint();

    void add_backing_store(i32 front_bitmap_id, Gfx::ShareableBitmap const& front_bitmap, i32 back_bitmap_id, Gfx::ShareableBitmap const& back_bitmap);

    void initialize_js_console(Web::DOM::Document& document);
    void destroy_js_console(Web::DOM::Document& document);
    void js_console_input(ByteString const& js_source);
    void run_javascript(ByteString const& js_source);
    void js_console_request_messages(i32 start_index);
    void did_output_js_console_message(i32 message_index);
    void console_peer_did_misbehave(char const* reason);
    void did_get_js_console_messages(i32 start_index, Vector<ByteString> message_types, Vector<ByteString> messages);

    virtual double device_pixels_per_css_pixel() const override { return m_device_pixels_per_css_pixel; }

private:
    PageClient(PageHost&, u64 id);

    virtual void visit_edges(JS::Cell::Visitor&) override;

    // ^PageClient
    virtual bool is_connection_open() const override;
    virtual Gfx::Palette palette() const override;
    virtual Web::DevicePixelRect screen_rect() const override { return m_screen_rect; }
    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override { return m_preferred_color_scheme; }
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) override;
    virtual void page_did_layout() override;
    virtual void page_did_change_title(ByteString const&) override;
    virtual void page_did_change_url(URL::URL const&) override;
    virtual void page_did_request_navigate_back() override;
    virtual void page_did_request_navigate_forward() override;
    virtual void page_did_request_refresh() override;
    virtual Gfx::IntSize page_did_request_resize_window(Gfx::IntSize) override;
    virtual Gfx::IntPoint page_did_request_reposition_window(Gfx::IntPoint) override;
    virtual void page_did_request_restore_window() override;
    virtual Gfx::IntRect page_did_request_maximize_window() override;
    virtual Gfx::IntRect page_did_request_minimize_window() override;
    virtual Gfx::IntRect page_did_request_fullscreen_window() override;
    virtual void page_did_request_scroll(i32, i32) override;
    virtual void page_did_request_scroll_to(Web::CSSPixelPoint) override;
    virtual void page_did_enter_tooltip_area(Web::CSSPixelPoint, ByteString const&) override;
    virtual void page_did_leave_tooltip_area() override;
    virtual void page_did_hover_link(URL::URL const&) override;
    virtual void page_did_unhover_link() override;
    virtual void page_did_middle_click_link(URL::URL const&, ByteString const& target, unsigned modifiers) override;
    virtual void page_did_request_context_menu(Web::CSSPixelPoint) override;
    virtual void page_did_request_link_context_menu(Web::CSSPixelPoint, URL::URL const&, ByteString const& target, unsigned modifiers) override;
    virtual void page_did_request_image_context_menu(Web::CSSPixelPoint, URL::URL const&, ByteString const& target, unsigned modifiers, Gfx::Bitmap const*) override;
    virtual void page_did_request_media_context_menu(Web::CSSPixelPoint, ByteString const& target, unsigned modifiers, Web::Page::MediaContextMenu) override;
    virtual void page_did_start_loading(URL::URL const&, bool) override;
    virtual void page_did_create_new_document(Web::DOM::Document&) override;
    virtual void page_did_change_active_document_in_top_level_browsing_context(Web::DOM::Document&) override;
    virtual void page_did_destroy_document(Web::DOM::Document&) override;
    virtual void page_did_finish_loading(URL::URL const&) override;
    virtual void page_did_request_alert(String const&) override;
    virtual void page_did_request_confirm(String const&) override;
    virtual void page_did_request_prompt(String const&, String const&) override;
    virtual void page_did_request_set_prompt_text(String const&) override;
    virtual void page_did_request_accept_dialog() override;
    virtual void page_did_request_dismiss_dialog() override;
    virtual void page_did_change_favicon(Gfx::Bitmap const&) override;
    virtual Vector<Web::Cookie::Cookie> page_did_request_all_cookies(URL::URL const&) override;
    virtual Optional<Web::Cookie::Cookie> page_did_request_named_cookie(URL::URL const&, String const&) override;
    virtual String page_did_request_cookie(URL::URL const&, Web::Cookie::Source) override;
    virtual void page_did_set_cookie(URL::URL const&, Web::Cookie::ParsedCookie const&, Web::Cookie::Source) override;
    virtual void page_did_update_cookie(Web::Cookie::Cookie) override;
    virtual void page_did_update_resource_count(i32) override;
    virtual NewWebViewResult page_did_request_new_web_view(Web::HTML::ActivateTab, Web::HTML::WebViewHints, Web::HTML::TokenizedFeature::NoOpener) override;
    virtual void page_did_request_activate_tab() override;
    virtual void page_did_close_top_level_traversable() override;
    virtual void page_did_update_navigation_buttons_state(bool back_enabled, bool forward_enabled) override;
    virtual void request_file(Web::FileRequest) override;
    virtual void page_did_request_color_picker(Color current_color) override;
    virtual void page_did_request_file_picker(Web::HTML::FileFilter accepted_file_types, Web::HTML::AllowMultipleFiles) override;
    virtual void page_did_request_select_dropdown(Web::CSSPixelPoint content_position, Web::CSSPixels minimum_width, Vector<Web::HTML::SelectItem> items) override;
    virtual void page_did_finish_text_test() override;
    virtual void page_did_change_theme_color(Gfx::Color color) override;
    virtual void page_did_insert_clipboard_entry(String data, String presentation_style, String mime_type) override;
    virtual void page_did_change_audio_play_state(Web::HTML::AudioPlayState) override;
    virtual IPC::File request_worker_agent() override;
    virtual void inspector_did_load() override;
    virtual void inspector_did_select_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> const& pseudo_element) override;
    virtual void inspector_did_set_dom_node_text(i32 node_id, String const& text) override;
    virtual void inspector_did_set_dom_node_tag(i32 node_id, String const& tag) override;
    virtual void inspector_did_add_dom_node_attributes(i32 node_id, JS::NonnullGCPtr<Web::DOM::NamedNodeMap> attributes) override;
    virtual void inspector_did_replace_dom_node_attribute(i32 node_id, size_t attribute_index, JS::NonnullGCPtr<Web::DOM::NamedNodeMap> replacement_attributes) override;
    virtual void inspector_did_request_dom_tree_context_menu(i32 node_id, Web::CSSPixelPoint position, String const& type, Optional<String> const& tag, Optional<size_t> const& attribute_index) override;
    virtual void inspector_did_execute_console_script(String const& script) override;

    Web::Layout::Viewport* layout_root();
    void setup_palette();
    ConnectionFromClient& client() const;

    PageHost& m_owner;
    JS::NonnullGCPtr<Web::Page> m_page;
    RefPtr<Gfx::PaletteImpl> m_palette_impl;
    Web::DevicePixelRect m_screen_rect;
    Web::DevicePixelSize m_content_size;
    float m_device_pixels_per_css_pixel { 1.0f };
    u64 m_id { 0 };
    bool m_should_show_line_box_borders { false };
    bool m_has_focus { false };

    enum class PaintState {
        Ready,
        WaitingForClient,
        PaintWhenReady,
    };

    PaintState m_paint_state { PaintState::Ready };

    Web::CSS::PreferredColorScheme m_preferred_color_scheme { Web::CSS::PreferredColorScheme::Auto };

    RefPtr<WebDriverConnection> m_webdriver;

#ifdef HAS_ACCELERATED_GRAPHICS
    OwnPtr<AccelGfx::Context> m_accelerated_graphics_context;
#endif

    struct BackingStores {
        i32 front_bitmap_id { -1 };
        i32 back_bitmap_id { -1 };
        RefPtr<Gfx::Bitmap> front_bitmap;
        RefPtr<Gfx::Bitmap> back_bitmap;
    };
    BackingStores m_backing_stores;

    // NOTE: These documents are not visited, but manually removed from the map on document finalization.
    HashMap<JS::RawGCPtr<Web::DOM::Document>, JS::NonnullGCPtr<WebContentConsoleClient>> m_console_clients;
    WeakPtr<WebContentConsoleClient> m_top_level_document_console_client;

    JS::Handle<JS::GlobalObject> m_console_global_object;
};

}
