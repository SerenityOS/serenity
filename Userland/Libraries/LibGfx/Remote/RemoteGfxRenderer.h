/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Remote/RemoteFontDatabase.h>
#include <LibGfx/Remote/RemoteGfx.h>
#include <LibIPC/ServerConnection.h>

#include <LibGfx/Remote/RemoteGfxClientEndpoint.h>
#include <LibGfx/Remote/RemoteGfxServerEndpoint.h>

namespace RemoteGfx {

class RemoteGfxRendererCallbacks {
public:
    virtual ~RemoteGfxRendererCallbacks() = default;

    virtual void bitmap_updated(i32, i32, Gfx::IntRect const*) = 0;
    virtual void bitmap_was_synced(i32, i32, Gfx::Bitmap&, Gfx::DisjointRectSet const&) = 0;
};

class RemoteGfxRenderer : public RemoteGfxServerEndpoint::Stub
    , public RemoteGfxClientProxy<RemoteGfxServerEndpoint, RemoteGfxClientEndpoint, RemoteGfxRenderer> {
public:
    RemoteGfxRenderer(RemoteGfxRendererCallbacks&, RemoteGfxFontDatabase&, u32);
    ~RemoteGfxRenderer();

    // RemoteGfxServerEndpoint::Stub
    virtual void create_bitmap(i32, Gfx::BitmapFormat const&, Gfx::IntSize const&, int) override;
    virtual void destroy_bitmap(i32) override;
    virtual void sync_bitmap(i32, u32) override;
    virtual void set_bitmap_data(i32, RemoteGfx::BitmapData const&) override;
    virtual void apply_bitmap_diff(i32, BitmapDiff const&) override;

    virtual void create_bitmap_font_from_data(i32, ByteBuffer const&) override;
    virtual void create_scalable_font_from_data(i32, ByteBuffer const&, u32) override;
    virtual void create_bitmap_font_from_digest(i32, ByteBuffer const&) override;
    virtual void create_scalable_font_from_digest(i32, ByteBuffer const&, u32) override;

    virtual void create_onebit_bitmap(i32, Gfx::IntSize const&, Gfx::OneBitBitmap::Type const&, ByteBuffer const&) override;
    virtual void destroy_onebit_bitmap(i32) override;
    virtual void set_onebit_bitmap_data(i32, ByteBuffer const&) override;

    virtual void create_palette(i32, RemoteGfx::PaletteData const&) override;
    virtual void destroy_palette(i32) override;

    virtual void set_painter_state(i32, Gfx::IntRect const&, Gfx::IntPoint const&, Gfx::Painter::DrawOp const&) override;
    virtual void clear_rect(i32, Gfx::IntRect const&, Gfx::Color const&) override;
    virtual void fill_rect(i32, Gfx::IntRect const&, Gfx::Color const&) override;
    virtual void draw_line(i32, Gfx::IntPoint const&, Gfx::IntPoint const&, Gfx::Color const&, int, Gfx::Painter::LineStyle const&, Optional<Gfx::Color> const&) override;
    virtual void fill_rect_with_dither_pattern(i32, Gfx::IntRect const&, Gfx::Color const&, Gfx::Color const&) override;
    virtual void fill_rect_with_checkerboard(i32, Gfx::IntRect const&, Gfx::IntSize const&, Gfx::Color const&, Gfx::Color const&) override;
    virtual void fill_rect_with_gradient(i32, Gfx::Orientation const&, Gfx::IntRect const&, Gfx::Color const&, Gfx::Color const&) override;
    virtual void blit_opaque(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, bool) override;
    virtual void blit_with_opacity(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, float, bool) override;
    virtual void blit_dimmed(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&) override;
    virtual void blit_brightened(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&) override;
    virtual void blit_blended(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, Gfx::Color const&) override;
    virtual void blit_multiplied(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, Gfx::Color const&) override;
    virtual void blit_disabled(i32, Gfx::IntPoint const&, i32, Gfx::IntRect const&, i32) override;
    virtual void draw_rect(i32, Gfx::IntRect const&, Gfx::Color const&, bool) override;
    virtual void draw_text(i32, Gfx::IntRect const&, String const&, i32, Gfx::TextAlignment const&, Gfx::Color const&, Gfx::TextElision const&, Gfx::TextWrapping const&) override;
    virtual void draw_glyph(i32, Gfx::IntRect const&, u32, i32, Gfx::Color const&) override;
    virtual void draw_bitmap(i32, Gfx::IntPoint const&, i32, Gfx::Color const&) override;

    Gfx::Bitmap* find_bitmap(i32 id)
    {
        auto it = m_bitmaps.find(id);
        return it != m_bitmaps.end() ? it->value.bitmap.ptr() : nullptr;
    }
    Gfx::Bitmap* find_bitmap(i32 id, u32 sync_tag)
    {
        auto it = m_bitmaps.find(id);
        if (it == m_bitmaps.end())
            return nullptr;
        auto& bitmap_data = it->value;
        if (bitmap_data.sync_tag == sync_tag && bitmap_data.bitmap_synced)
            return bitmap_data.bitmap_synced.ptr();
        return bitmap_data.bitmap.ptr();
    }
    Gfx::Bitmap& bitmap(i32 id)
    {
        auto it = m_bitmaps.find(id);
        VERIFY(it != m_bitmaps.end());
        auto& bitmap_data = it->value;
        return bitmap_data.bitmap_synced ? *bitmap_data.bitmap_synced : *bitmap_data.bitmap;
    }

private:
    struct BitmapData {
        RemoteGfxRenderer* renderer { nullptr };
        i32 id { 0 };
        NonnullRefPtr<Gfx::Bitmap> bitmap;
        RefPtr<Gfx::Bitmap> bitmap_synced;
        Gfx::DisjointRectSet dirty_rects;

        struct PainterState {
            Gfx::IntRect clip_rect;
            Gfx::IntPoint translation;
            Gfx::Painter::DrawOp draw_op { Gfx::Painter::DrawOp::Copy };
        };
        PainterState painter_state;
        Gfx::Painter painter;

        u32 sync_tag { 0 };
        bool copy_on_write { false };

        BitmapData(i32, RemoteGfxRenderer& renderer, Gfx::BitmapFormat, Gfx::IntSize const&, int);

        void dirty_painter_rect(Gfx::IntRect const&);
        void update_painter();
        void do_copy_on_write();
    };
    friend struct BitmapData;

    struct OneBitBitmapData {
        RemoteGfxRenderer* renderer { nullptr };
        i32 id { 0 };
        RefPtr<Gfx::CharacterBitmap> character_bitmap;
        OwnPtr<Gfx::GlyphBitmap> glyph_bitmap;

        OneBitBitmapData(i32, RemoteGfxRenderer&, Gfx::OneBitBitmap::Type, Gfx::IntSize, ByteBuffer const&);

        void set_bits(ByteBuffer const&);
    };
    friend struct OneBitBitmapData;

    BitmapData& bitmap_data(i32 id)
    {
        auto it = m_bitmaps.find(id);
        VERIFY(it != m_bitmaps.end());
        return it->value;
    }

    BitmapData& bitmap_data_for_write(i32 id)
    {
        auto it = m_bitmaps.find(id);
        VERIFY(it != m_bitmaps.end());
        auto& bitmap_data = it->value;
        if (bitmap_data.copy_on_write)
            bitmap_data.do_copy_on_write();
        return bitmap_data;
    }

    OneBitBitmapData& onebit_bitmap_data(i32 id)
    {
        auto it = m_onebit_bitmaps.find(id);
        VERIFY(it != m_onebit_bitmaps.end());
        return it->value;
    }

    Gfx::Palette& palette(i32 id)
    {
        auto it = m_palettes.find(id);
        VERIFY(it != m_palettes.end());
        return *it->value;
    }

    struct FontData {
        NonnullRefPtr<RemoteGfxFontDatabase::RemoteFontData> font_data;
        NonnullRefPtr<Gfx::Font> font;

        FontData(NonnullRefPtr<RemoteGfxFontDatabase::RemoteFontData>&& font_data, NonnullRefPtr<Gfx::Font>&& font)
            : font_data(move(font_data))
            , font(move(font))
        {
        }
    };
    Gfx::Font& font(i32 id)
    {
        auto it = m_fonts.find(id);
        VERIFY(it != m_fonts.end());
        auto& font_data = *it->value;
        return *font_data.font;
    }

    RemoteGfxRendererCallbacks& m_callbacks;
    RemoteGfxFontDatabase& m_font_database;
    const u32 m_client_id;
    HashMap<i32, BitmapData> m_bitmaps;
    HashMap<u32, OneBitBitmapData> m_onebit_bitmaps;
    HashMap<i32, NonnullOwnPtr<Gfx::Palette>> m_palettes;
    HashMap<i32, NonnullOwnPtr<FontData>> m_fonts;
};

}
