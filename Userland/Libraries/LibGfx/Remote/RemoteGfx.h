/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibIPC/Forward.h>

namespace RemoteGfx {

typedef i32 BitmapId;

class RemoteGfxRenderer;
class RemoteGfxSession;
class RemoteGfxServerConnection;

class BitmapData {
public:
    BitmapData() = default;
    BitmapData(Gfx::Bitmap const&, Gfx::IntRect const&);

    auto& bytes() { return m_bytes; }
    auto& bytes() const { return m_bytes; }
    auto& physical_rect() { return m_physical_rect; }
    auto& physical_rect() const { return m_physical_rect; }

    void apply_to(Gfx::Bitmap&) const;

private:
    Gfx::IntRect m_physical_rect;
    ByteBuffer m_bytes;
};

struct BitmapDiff {
    constexpr static int max_tile_size = 16;
    enum class DiffFlags : u8 {
        OneBitmap = (1 << 0),
        Deflated = (1 << 1),
    };

    BitmapId id;
    DiffFlags flags {};
    Gfx::IntPoint location {};
    Gfx::IntSize size {};
    ByteBuffer bytes {};

    bool is_empty() const { return bytes.is_empty(); }
    static BitmapDiff create(BitmapId, Gfx::Bitmap const& original, Gfx::Bitmap const& changed, Gfx::DisjointRectSet const&);
    void apply_to_bitmap(Gfx::Bitmap&, Gfx::DisjointRectSet* = nullptr) const;
};

class PaletteData {
public:
    PaletteData() = default;
    PaletteData(Gfx::Palette const&);

    NonnullOwnPtr<Gfx::Palette> create_palette() const;

    auto& bytes() { return m_bytes; }
    auto& bytes() const { return m_bytes; }

private:
    ByteBuffer m_bytes;
};

}

namespace IPC {

bool encode(Encoder&, Gfx::Painter::LineStyle const&);
ErrorOr<void> decode(Decoder&, Gfx::Painter::LineStyle&);
bool encode(Encoder&, Gfx::Painter::DrawOp const&);
ErrorOr<void> decode(Decoder&, Gfx::Painter::DrawOp&);
bool encode(Encoder&, Gfx::Orientation const&);
ErrorOr<void> decode(Decoder&, Gfx::Orientation&);
bool encode(Encoder&, RemoteGfx::BitmapData const&);
ErrorOr<void> decode(Decoder&, RemoteGfx::BitmapData&);
bool encode(Encoder&, RemoteGfx::BitmapDiff const&);
ErrorOr<void> decode(Decoder&, RemoteGfx::BitmapDiff&);
bool encode(Encoder&, RemoteGfx::PaletteData const&);
ErrorOr<void> decode(Decoder&, RemoteGfx::PaletteData&);

}
