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

    void initialize_js_console(Badge<PageHost>, Web::DOM::Document& document);

    void request_file(Web::FileRequest);

    Optional<int> fd() { return socket().fd(); }

    PageHost& page_host() { return *m_page_host; }
    PageHost const& page_host() const { return *m_page_host; }

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>);

    Web::Page& page();
    Web::Page const& page() const;

    virtual Messages::WebContentServer::GetWindowHandleResponse get_window_handle() override;
    virtual void set_window_handle(String const& handle) override;
    virtual void connect_to_webdriver(DeprecatedString const& webdriver_ipc_path) override;
    virtual void update_system_theme(Core::AnonymousBuffer const&) override;
    virtual void update_system_fonts(DeprecatedString const&, DeprecatedString const&, DeprecatedString const&) override;
    virtual void update_screen_rects(Vector<Gfx::IntRect> const&, u32) override;
    virtual void load_url(URL const&) override;
    virtual void load_html(DeprecatedString const&, URL const&) override;
    virtual void paint(Gfx::IntRect const&, i32) override;
    virtual void set_viewport_rect(Gfx::IntRect const&) override;
    virtual void mouse_down(Gfx::IntPoint, unsigned, unsigned, unsigned) override;
    virtual void mouse_move(Gfx::IntPoint, unsigned, unsigned, unsigned) override;
    virtual void mouse_up(Gfx::IntPoint, unsigned, unsigned, unsigned) override;
    virtual void mouse_wheel(Gfx::IntPoint, unsigned, unsigned, unsigned, i32, i32) override;
    virtual void doubleclick(Gfx::IntPoint, unsigned, unsigned, unsigned) override;
    virtual void key_down(i32, unsigned, u32) override;
    virtual void key_up(i32, unsigned, u32) override;
    virtual void add_backing_store(i32, Gfx::ShareableBitmap const&) override;
    virtual void remove_backing_store(i32) override;
    virtual void debug_request(DeprecatedString const&, DeprecatedString const&) override;
    virtual void get_source() override;
    virtual void inspect_dom_tree() override;
    virtual Messages::WebContentServer::InspectDomNodeResponse inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> const& pseudo_element) override;
    virtual void inspect_accessibility_tree() override;
    virtual Messages::WebContentServer::GetHoveredNodeIdResponse get_hovered_node_id() override;
    virtual Messages::WebContentServer::DumpLayoutTreeResponse dump_layout_tree() override;
    virtual Messages::WebContentServer::DumpPaintTreeResponse dump_paint_tree() override;
    virtual Messages::WebContentServer::DumpTextResponse dump_text() override;
    virtual void set_content_filters(Vector<String> const&) override;
    virtual void set_autoplay_allowed_on_all_websites() override;
    virtual void set_autoplay_allowlist(Vector<String> const& allowlist) override;
    virtual void set_proxy_mappings(Vector<DeprecatedString> const&, HashMap<DeprecatedString, size_t> const&) override;
    virtual void set_preferred_color_scheme(Web::CSS::PreferredColorScheme const&) override;
    virtual void set_has_focus(bool) override;
    virtual void set_is_scripting_enabled(bool) override;
    virtual void set_device_pixels_per_css_pixel(float) override;
    virtual void set_window_position(Gfx::IntPoint) override;
    virtual void set_window_size(Gfx::IntSize) override;
    virtual void handle_file_return(i32 error, Optional<IPC::File> const& file, i32 request_id) override;
    virtual void set_system_visibility_state(bool visible) override;

    virtual void js_console_input(DeprecatedString const&) override;
    virtual void run_javascript(DeprecatedString const&) override;
    virtual void js_console_request_messages(i32) override;

    virtual void alert_closed() override;
    virtual void confirm_closed(bool accepted) override;
    virtual void prompt_closed(Optional<String> const& response) override;

    virtual void toggle_media_play_state() override;
    virtual void toggle_media_mute_state() override;
    virtual void toggle_media_loop_state() override;
    virtual void toggle_media_controls_state() override;

    virtual void set_user_style(String const&) override;

    virtual Messages::WebContentServer::TakeDocumentScreenshotResponse take_document_screenshot() override;

    virtual Messages::WebContentServer::GetLocalStorageEntriesResponse get_local_storage_entries() override;
    virtual Messages::WebContentServer::GetSessionStorageEntriesResponse get_session_storage_entries() override;

    virtual Messages::WebContentServer::GetSelectedTextResponse get_selected_text() override;
    virtual void select_all() override;

    void flush_pending_paint_requests();

    void report_finished_handling_input_event(bool event_was_handled);

    NonnullOwnPtr<PageHost> m_page_host;
    struct PaintRequest {
        Gfx::IntRect content_rect;
        NonnullRefPtr<Gfx::Bitmap> bitmap;
        i32 bitmap_id { -1 };
    };
    Vector<PaintRequest> m_pending_paint_requests;
    RefPtr<Web::Platform::Timer> m_paint_flush_timer;

    HashMap<i32, NonnullRefPtr<Gfx::Bitmap>> m_backing_stores;

    WeakPtr<JS::Realm> m_realm;
    OwnPtr<WebContentConsoleClient> m_console_client;
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
        Gfx::IntPoint position {};
        unsigned button {};
        unsigned buttons {};
        unsigned modifiers {};
        int wheel_delta_x {};
        int wheel_delta_y {};
        size_t coalesced_event_count { 0 };
    };

    struct QueuedKeyboardEvent {
        enum class Type {
            KeyDown,
            KeyUp,
        };
        Type type {};
        i32 key {};
        unsigned int modifiers {};
        u32 code_point {};
    };

    void enqueue_input_event(Variant<QueuedMouseEvent, QueuedKeyboardEvent>);
    void process_next_input_event();

    Queue<Variant<QueuedMouseEvent, QueuedKeyboardEvent>> m_input_event_queue;

    RefPtr<Web::Platform::Timer> m_input_event_queue_timer;
};

}
