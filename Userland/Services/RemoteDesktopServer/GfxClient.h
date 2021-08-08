/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Remote/RemoteGfx.h>
#include <LibGfx/Remote/RemoteGfxClientEndpoint.h>
#include <LibGfx/Remote/RemoteGfxServerEndpoint.h>
#include <LibIPC/Connection.h>

namespace RemoteDesktopServer {

class Server;

class GfxClient : public IPC::Connection<RemoteGfxServerEndpoint, RemoteGfxClientEndpoint, Core::LocalSocket>
    , public RemoteGfxServerEndpoint::Stub
    , public RemoteGfxClientProxy<RemoteGfxServerEndpoint, RemoteGfxClientEndpoint, GfxClient> {
    C_OBJECT(GfxClient)
public:
    GfxClient(NonnullRefPtr<Core::LocalSocket>, Server&);
    ~GfxClient();

    template<typename F>
    static void for_each(F f)
    {
        for (auto& it : s_clients)
            f(*it.value);
    }

    void set_forwarding(bool forwarding) { m_forwarding = forwarding; }
    void notify_enable_remote_gfx(bool);
    virtual void handle_raw_message(NonnullOwnPtr<IPC::Message>&&, ReadonlyBytes const&, bool) override;

    // We don't actually implement them right now, but we would have to if we were
    // to provide e.g. a VNC port
    virtual void create_bitmap(i32, Gfx::BitmapFormat const&, Gfx::IntSize const&, int) override { }
    virtual void destroy_bitmap(i32) override { }
    virtual void sync_bitmap(i32, u32) override { }
    virtual void set_bitmap_data(i32, RemoteGfx::BitmapData const&) override { }
    virtual void apply_bitmap_diff(i32, RemoteGfx::BitmapDiff const&) override { }

    virtual void create_bitmap_font_from_data(i32, ByteBuffer const&) override { }
    virtual void create_scalable_font_from_data(i32, ByteBuffer const&, u32) override { }
    virtual void create_bitmap_font_from_digest(i32, ByteBuffer const&) override { }
    virtual void create_scalable_font_from_digest(i32, ByteBuffer const&, u32) override { }

    virtual void create_onebit_bitmap(i32, Gfx::IntSize const&, Gfx::OneBitBitmap::Type const&, ByteBuffer const&) override { }
    virtual void destroy_onebit_bitmap(i32) override { }
    virtual void set_onebit_bitmap_data(i32, ByteBuffer const&) override { }

    virtual void create_palette(i32, RemoteGfx::PaletteData const&) override { }
    virtual void destroy_palette(i32) override { }

    virtual void set_painter_state(i32, Gfx::IntRect const&, Gfx::IntPoint const&, Gfx::Painter::DrawOp const&) override { }
    virtual void clear_rect(i32, Gfx::IntRect const&, Gfx::Color const&) override { }
    virtual void fill_rect(i32, Gfx::IntRect const&, Gfx::Color const&) override { }
    virtual void draw_line(i32, Gfx::IntPoint const&, Gfx::IntPoint const&, Gfx::Color const&, int, Gfx::Painter::LineStyle const&, Optional<Gfx::Color> const&) override { }
    virtual void fill_rect_with_dither_pattern(i32, Gfx::IntRect const&, Gfx::Color const&, Gfx::Color const&) override { }
    virtual void fill_rect_with_checkerboard(i32, Gfx::IntRect const&, Gfx::IntSize const&, Gfx::Color const&, Gfx::Color const&) override { }
    virtual void fill_rect_with_gradient(i32, Gfx::Orientation const&, Gfx::IntRect const&, Gfx::Color const&, Gfx::Color const&) override { }
    virtual void blit_opaque(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, bool) override { }
    virtual void blit_with_opacity(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, float, bool) override { }
    virtual void blit_dimmed(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&) override { }
    virtual void blit_brightened(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&) override { }
    virtual void blit_blended(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, Gfx::Color const&) override { }
    virtual void blit_multiplied(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, Gfx::Color const&) override { }
    virtual void blit_disabled(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, i32) override { }
    virtual void draw_rect(i32, Gfx::IntRect const&, Gfx::Color const&, bool) override { }
    virtual void draw_text(i32, Gfx::IntRect const&, String const&, i32, Gfx::TextAlignment const&, Gfx::Color const&, Gfx::TextElision const&, Gfx::TextWrapping const&) override { }
    virtual void draw_glyph(i32, Gfx::IntRect const&, u32, i32, Gfx::Color const&) override { }
    virtual void draw_bitmap(i32, Gfx::IntPoint const&, i32, Gfx::Color const&) override { }

    String to_string() const
    {
        return String::formatted("GfxClient[{}]", m_client_id);
    }

private:
    static HashMap<u32, NonnullRefPtr<GfxClient>> s_clients;
    const int m_client_id;
    Server& m_server;
    Optional<u64> m_cookie;
    bool m_forwarding { false };
};

}

namespace AK {

template<>
struct Formatter<RemoteDesktopServer::GfxClient> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, RemoteDesktopServer::GfxClient const& value)
    {
        return Formatter<StringView>::format(builder, value.to_string());
    }
};

}
