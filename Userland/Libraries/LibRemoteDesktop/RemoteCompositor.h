/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Optional.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Remote/RemoteGfx.h>
#include <LibIPC/Forward.h>

namespace RemoteDesktop::Compositor {

typedef i32 WindowId;
typedef i32 BitmapId;
typedef i32 ClientId;

struct WindowDirtyRects {
    WindowId id;
    BitmapId backing_bitmap_id;
    u32 backing_bitmap_sync_tag;
    bool is_windowserver_backing_bitmap;
    BitmapId frame_top_bottom_bitmap_id;
    BitmapId frame_left_right_bitmap_id;
    Vector<Gfx::IntRect> dirty_rects {};
};

struct WindowGeometry {
    Gfx::IntRect render_rect;
    Gfx::IntRect frame_rect;
    Gfx::IntRect rect;
};

struct WindowFrame {
    BitmapId top_bottom_bitmap_id;
    BitmapId left_right_bitmap_id;
};

struct Window {
    WindowId id;
    ClientId client_id;
    Optional<WindowFrame> frame {};
    Optional<WindowGeometry> geometry {};
    Optional<Vector<Gfx::IntRect>> opaque_rects {};
    Optional<Vector<Gfx::IntRect>> transparent_rects {};
};

}

namespace IPC {

bool encode(Encoder&, RemoteDesktop::Compositor::Window const&);
ErrorOr<void> decode(Decoder&, RemoteDesktop::Compositor::Window&);
bool encode(Encoder&, RemoteDesktop::Compositor::WindowFrame const&);
ErrorOr<void> decode(Decoder&, RemoteDesktop::Compositor::WindowFrame&);
bool encode(Encoder&, RemoteDesktop::Compositor::WindowGeometry const&);
ErrorOr<void> decode(Decoder&, RemoteDesktop::Compositor::WindowGeometry&);
bool encode(Encoder&, RemoteDesktop::Compositor::WindowDirtyRects const&);
ErrorOr<void> decode(Decoder&, RemoteDesktop::Compositor::WindowDirtyRects&);

}
