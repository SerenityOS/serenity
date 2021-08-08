/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Painter.h>

// RemoteCompositor.h must be included before the endpoint headers
#include <LibRemoteDesktop/RemoteCompositor.h>
#include <LibRemoteDesktop/RemoteCompositorClientEndpoint.h>
#include <LibRemoteDesktop/RemoteCompositorServerEndpoint.h>

// RemoteGfxRenderer.h must be included before the endpoint headers
#include <LibGfx/Remote/RemoteGfxClientEndpoint.h>
#include <LibGfx/Remote/RemoteGfxRenderer.h>
#include <LibGfx/Remote/RemoteGfxServerEndpoint.h>

namespace RemoteDesktop {

class RemoteDesktopServerConnection;

class RendererCallbacks {
public:
    virtual ~RendererCallbacks() = default;

    virtual void invalidate_rects(Gfx::DisjointRectSet const&) = 0;
    virtual void set_surface_size(Gfx::IntSize const&) = 0;
};

class Renderer : public RemoteCompositorClientEndpoint::Stub
    , public RemoteCompositorServerProxy<RemoteCompositorClientEndpoint, RemoteCompositorServerEndpoint, Renderer>
    , public RemoteGfx::RemoteGfxRendererCallbacks {
public:
    Renderer(RendererCallbacks&, RemoteDesktopServerConnection&);
    virtual ~Renderer() override;

    void paint(Gfx::Painter&, Gfx::IntRect const&);

    // Compositor
    virtual void fast_greet(Vector<Gfx::IntRect> const&, Gfx::Color const&, Gfx::IntPoint const&) override;
    virtual void associate_window_client(int, u64) override;
    virtual void disassociate_window_client(int) override;
    virtual void update_display(Vector<Compositor::WindowId> const&, Vector<Compositor::Window> const&, Vector<Compositor::WindowId> const&, Vector<Compositor::WindowDirtyRects> const&) override;
    virtual void cursor_position_changed(Gfx::IntPoint const&) override;

    auto& font_database() { return m_font_database; }
    auto& font_database() const { return m_font_database; }

    void set_cursor_position(Gfx::IntPoint const&);
    void set_mouse_buttons(Gfx::IntPoint const&, unsigned);
    void mouse_wheel_turned(Gfx::IntPoint const&, int);

private:
    void render_desktop();
    void send_new_cusor_position();

    struct WindowData {
        const Compositor::WindowId id;
        const Compositor::ClientId client_id;
        RemoteGfx::RemoteGfxRenderer* backing_store_gfx_renderer { nullptr };
        Compositor::BitmapId backing_bitmap_id { -1 };
        Compositor::BitmapId last_backing_bitmap_id { -1 };
        u32 backing_bitmap_sync_tag { 0 };
        u32 last_backing_bitmap_sync_tag { 0 };
        Gfx::DisjointRectSet backing_dirty_rects;
        Gfx::DisjointRectSet last_backing_dirty_rects;
        RefPtr<Gfx::Bitmap> backing_bitmap;
        Compositor::WindowGeometry geometry {};
        Gfx::DisjointRectSet opaque_rects;
        Gfx::DisjointRectSet transparent_rects;
        Compositor::BitmapId frame_top_bottom_bitmap_id { -1 };
        Compositor::BitmapId frame_left_right_bitmap_id { -1 };
        RefPtr<Gfx::Bitmap> frame_top_bottom_bitmap;
        RefPtr<Gfx::Bitmap> frame_left_right_bitmap;
        bool is_windowserver_backing_bitmap { false };

        WindowData(Renderer& renderer, Compositor::Window const& window)
            : id(window.id)
            , client_id(window.client_id)
        {
            update(renderer, window);
        }

        void update(Renderer&, Compositor::Window const&);
    };
    friend struct WindowData;

    RemoteGfx::RemoteGfxRenderer* windowserver_gfx_client();
    void invalidate_window(WindowData&, Gfx::DisjointRectSet const&);
    void invalidate_window(WindowData&, bool, bool);

    WindowData& window_data(Compositor::WindowId);

    void clients_were_associated(i32, i32);
    virtual void bitmap_was_synced(i32, i32, Gfx::Bitmap&, Gfx::DisjointRectSet const&) override;
    virtual void bitmap_updated(i32, i32, Gfx::IntRect const*) override;

    RendererCallbacks& m_callbacks;
    RemoteGfx::RemoteGfxFontDatabase m_font_database;
    RemoteDesktopServerConnection& m_connection;
    RefPtr<Gfx::Bitmap> m_surface;
    Gfx::DisjointRectSet m_screen_rects;
    Gfx::DisjointRectSet m_outside_rects;
    Gfx::DisjointRectSet m_dirty_rects;
    Gfx::IntRect m_bounds;
    HashMap<Compositor::WindowId, NonnullOwnPtr<WindowData>> m_window_data;
    Vector<WindowData*, 32> m_ordered_window_data;
    HashMap<u32, NonnullOwnPtr<RemoteGfx::RemoteGfxRenderer>> m_remote_gfx_clients;
    RemoteGfx::RemoteGfxRenderer* m_windowserver_gfx_client { nullptr };
    Gfx::Color m_wallpaper_color { Gfx::Color::Black };
    Gfx::IntPoint m_cursor_position;
    Optional<Gfx::IntPoint> m_pending_set_cursor_position;
};

}
