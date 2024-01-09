/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Queue.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Loader/FileRequest.h>
#include <LibWeb/Platform/Timer.h>
#include <LibWebView/Forward.h>
#include <WebContent/Forward.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebContentConsoleClient.h>
#include <WebContent/WebContentServerEndpoint.h>

namespace WebContent {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<WebContentClientEndpoint, WebContentServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    ~ConnectionFromClient() override = default;

    virtual void die() override;

    void initialize_js_console(Badge<PageClient>, Web::DOM::Document& document);
    void destroy_js_console(Badge<PageClient>, Web::DOM::Document& document);

    void request_file(Web::FileRequest);

    Optional<int> fd() { return socket().fd(); }

    PageHost& page_host() { return *m_page_host; }
    PageHost const& page_host() const { return *m_page_host; }

    auto& backing_stores() { return m_backing_stores; }

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>);

    PageClient& page(u64 index = 0);
    PageClient const& page(u64 index = 0) const;

    virtual Messages::WebContentServer::GetWindowHandleResponse get_window_handle() override;
    virtual void set_window_handle(String const& handle) override;
    virtual void connect_to_webdriver(ByteString const& webdriver_ipc_path) override;
    virtual void update_system_theme(Core::AnonymousBuffer const&) override;
    virtual void update_system_fonts(ByteString const&, ByteString const&, ByteString const&) override;
    virtual void update_screen_rects(Vector<Web::DevicePixelRect> const&, u32) override;
    virtual void load_url(URL const&) override;
    virtual void load_html(ByteString const&) override;
    virtual void set_viewport_rect(Web::DevicePixelRect const&) override;
    virtual void mouse_down(Web::DevicePixelPoint, Web::DevicePixelPoint, u32, u32, u32) override;
    virtual void mouse_move(Web::DevicePixelPoint, Web::DevicePixelPoint, u32, u32, u32) override;
    virtual void mouse_up(Web::DevicePixelPoint, Web::DevicePixelPoint, u32, u32, u32) override;
    virtual void mouse_wheel(Web::DevicePixelPoint, Web::DevicePixelPoint, u32, u32, u32, Web::DevicePixels, Web::DevicePixels) override;
    virtual void doubleclick(Web::DevicePixelPoint, Web::DevicePixelPoint, u32, u32, u32) override;
    virtual void key_down(i32, u32, u32) override;
    virtual void key_up(i32, u32, u32) override;
    virtual void add_backing_store(i32 front_bitmap_id, Gfx::ShareableBitmap const& front_bitmap, i32 back_bitmap_id, Gfx::ShareableBitmap const& back_bitmap) override;
    virtual void ready_to_paint() override;
    virtual void debug_request(ByteString const&, ByteString const&) override;
    virtual void get_source() override;
    virtual void inspect_dom_tree() override;
    virtual void inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement::Type> const& pseudo_element) override;
    virtual void inspect_accessibility_tree() override;
    virtual void get_hovered_node_id() override;

    virtual void set_dom_node_text(i32 node_id, String const& text) override;
    virtual void set_dom_node_tag(i32 node_id, String const& name) override;
    virtual void add_dom_node_attributes(i32 node_id, Vector<WebView::Attribute> const& attributes) override;
    virtual void replace_dom_node_attribute(i32 node_id, String const& name, Vector<WebView::Attribute> const& replacement_attributes) override;
    virtual void create_child_element(i32 node_id) override;
    virtual void create_child_text_node(i32 node_id) override;
    virtual void clone_dom_node(i32 node_id) override;
    virtual void remove_dom_node(i32 node_id) override;
    virtual void get_dom_node_html(i32 node_id) override;

    virtual Messages::WebContentServer::DumpLayoutTreeResponse dump_layout_tree() override;
    virtual Messages::WebContentServer::DumpPaintTreeResponse dump_paint_tree() override;
    virtual Messages::WebContentServer::DumpTextResponse dump_text() override;
    virtual void set_content_filters(Vector<String> const&) override;
    virtual void set_autoplay_allowed_on_all_websites() override;
    virtual void set_autoplay_allowlist(Vector<String> const& allowlist) override;
    virtual void set_proxy_mappings(Vector<ByteString> const&, HashMap<ByteString, size_t> const&) override;
    virtual void set_preferred_color_scheme(Web::CSS::PreferredColorScheme const&) override;
    virtual void set_has_focus(bool) override;
    virtual void set_is_scripting_enabled(bool) override;
    virtual void set_device_pixels_per_css_pixel(float) override;
    virtual void set_window_position(Web::DevicePixelPoint) override;
    virtual void set_window_size(Web::DevicePixelSize) override;
    virtual void handle_file_return(i32 error, Optional<IPC::File> const& file, i32 request_id) override;
    virtual void set_system_visibility_state(bool visible) override;

    virtual void js_console_input(ByteString const&) override;
    virtual void run_javascript(ByteString const&) override;
    virtual void js_console_request_messages(i32) override;

    virtual void alert_closed() override;
    virtual void confirm_closed(bool accepted) override;
    virtual void prompt_closed(Optional<String> const& response) override;
    virtual void color_picker_update(Optional<Color> const& picked_color, Web::HTML::ColorPickerUpdateState const& state) override;
    virtual void select_dropdown_closed(Optional<String> const& value) override;

    virtual void toggle_media_play_state() override;
    virtual void toggle_media_mute_state() override;
    virtual void toggle_media_loop_state() override;
    virtual void toggle_media_controls_state() override;

    virtual void set_user_style(String const&) override;

    virtual void enable_inspector_prototype() override;

    virtual void take_document_screenshot() override;
    virtual void take_dom_node_screenshot(i32 node_id) override;

    virtual Messages::WebContentServer::DumpGcGraphResponse dump_gc_graph() override;

    virtual Messages::WebContentServer::GetLocalStorageEntriesResponse get_local_storage_entries() override;
    virtual Messages::WebContentServer::GetSessionStorageEntriesResponse get_session_storage_entries() override;

    virtual Messages::WebContentServer::GetSelectedTextResponse get_selected_text() override;
    virtual void select_all() override;

    void report_finished_handling_input_event(bool event_was_handled);

    NonnullOwnPtr<PageHost> m_page_host;

    struct BackingStores {
        i32 front_bitmap_id { -1 };
        i32 back_bitmap_id { -1 };
        RefPtr<Gfx::Bitmap> front_bitmap;
        RefPtr<Gfx::Bitmap> back_bitmap;
    };
    BackingStores m_backing_stores;

    HashMap<Web::DOM::Document*, NonnullOwnPtr<WebContentConsoleClient>> m_console_clients;
    WeakPtr<WebContentConsoleClient> m_top_level_document_console_client;

    JS::Handle<JS::GlobalObject> m_console_global_object;

    HashMap<int, Web::FileRequest> m_requested_files {};
    int last_id { 0 };

    struct QueuedMouseEvent {
        enum class Type {
            MouseMove,
            MouseDown,
            MouseUp,
            MouseWheel,
            DoubleClick,
        };
        Type type {};
        Web::DevicePixelPoint position {};
        Web::DevicePixelPoint screen_position {};
        u32 button {};
        u32 buttons {};
        u32 modifiers {};
        Web::DevicePixels wheel_delta_x {};
        Web::DevicePixels wheel_delta_y {};
        size_t coalesced_event_count { 0 };
    };

    struct QueuedKeyboardEvent {
        enum class Type {
            KeyDown,
            KeyUp,
        };
        Type type {};
        i32 key {};
        u32 modifiers {};
        u32 code_point {};
    };

    void enqueue_input_event(Variant<QueuedMouseEvent, QueuedKeyboardEvent>);
    void process_next_input_event();

    Queue<Variant<QueuedMouseEvent, QueuedKeyboardEvent>> m_input_event_queue;

    RefPtr<Web::Platform::Timer> m_input_event_queue_timer;
};

}
