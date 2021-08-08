/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <LibIPC/ClientConnection.h>
#include <LibRemoteDesktop/RemoteCompositor.h>
#include <LibRemoteDesktop/RemoteCompositorServerConnection.h>
#include <LibRemoteDesktop/RemoteCompositorServerEndpoint.h>

namespace WindowServer {

class Window;

class RemoteCompositorClientConnection final
    : public IPC::ClientConnection<RemoteCompositorClientEndpoint, RemoteCompositorServerEndpoint> {
    C_OBJECT(RemoteCompositorClientConnection)

    friend class Compositor;

public:
    ~RemoteCompositorClientConnection() override;

    template<typename F>
    static void for_each(F f)
    {
        for (auto& it : s_connections)
            f(*it.value);
    }

    void begin_compose();
    void begin_update_occlusions();
    void update_window_occlusions(Window&);
    void end_update_occlusions();
    void update_window_dirty_rects(Window&);
    void flush_dirty();

    bool is_active() const { return m_is_active; }
    bool need_occlusions() const { return m_is_ready && m_occlusions_dirty; }
    void occlusions_did_update() { m_occlusions_dirty = true; }
    bool is_ready() const { return m_is_ready; }

    Messages::RemoteCompositorServer::StartSessionResponse start_session() override;
    void ready_for_more() override;
    void set_cursor_position(Gfx::IntPoint const&) override;
    void set_mouse_buttons(Gfx::IntPoint const&, u32) override;
    void mouse_wheel_turned(Gfx::IntPoint const&, i32) override;

private:
    explicit RemoteCompositorClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id);

    // ^ClientConnection
    virtual void die() override;

    static HashMap<int, NonnullRefPtr<RemoteCompositorClientConnection>> s_connections;

    void update_dirty_rects();

    struct WindowData {
        RemoteDesktop::Compositor::WindowId id { 0 };
        Window* window { nullptr };
        u32 occlusions_tag { 0 };
        Gfx::IntRect last_sent_render_rect;
        Gfx::IntRect last_sent_frame_rect;
        Gfx::IntRect last_sent_rect;
        Gfx::DisjointRectSet opaque_rects;
        Gfx::DisjointRectSet transparent_rects;
        Gfx::DisjointRectSet dirty_rects;
        int last_sent_backing_store_bitmap_id { 0 };
        u32 last_sent_backing_store_bitmap_sync_tag { 0 };
        bool opaque_rects_dirty { true };
        bool transparent_rects_dirty { true };
        bool dirty_rects_dirty { true };

        WindowData(RemoteDesktop::Compositor::WindowId, u32, Window&);

        void update();
    };
    friend struct WindowData;

    RemoteDesktop::Compositor::WindowId allocate_window_id();
    void free_window_id(RemoteDesktop::Compositor::WindowId);
    WindowData& get_window_data(Window&);
    WindowData* find_window_data(Window&);

    Bitmap m_window_ids;
    HashMap<Window*, NonnullOwnPtr<WindowData>> m_window_data;
    Vector<WindowData*, 32> m_ordered_window_data;
    u32 m_current_occlusions_tag { 0 };
    Vector<RemoteDesktop::Compositor::Window, 16> m_pending_occlusions;
    Vector<RemoteDesktop::Compositor::WindowId, 16> m_ordered_window_ids;
    Vector<RemoteDesktop::Compositor::WindowId, 16> m_pending_delete_windows;
    Vector<RemoteDesktop::Compositor::WindowDirtyRects, 16> m_pending_dirty_rects;
    Gfx::IntPoint m_last_sent_cursor_location;

    bool m_is_active { false };
    bool m_is_ready { false };
    bool m_need_greet { false };
    bool m_dirty_rects_dirty { true };
    bool m_occlusions_dirty { true };
    bool m_window_order_dirty { true };
};

}
