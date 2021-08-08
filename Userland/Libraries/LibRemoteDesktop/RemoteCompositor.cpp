/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Bitmap.h>
#include <LibRemoteDesktop/RemoteCompositor.h>

// Must be included after LibIPC/Forward.h
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace IPC {

bool encode(Encoder& encoder, RemoteDesktop::Compositor::Window const& window)
{
    encoder << window.id << window.client_id << window.frame << window.geometry << window.opaque_rects << window.transparent_rects;
    return true;
}

ErrorOr<void> decode(Decoder& decoder, RemoteDesktop::Compositor::Window& window)
{
    if (auto result = decoder.decode(window.id); result.is_error())
        return result;
    if (auto result = decoder.decode(window.client_id); result.is_error())
        return result;
    if (auto result = decoder.decode(window.frame); result.is_error())
        return result;
    if (auto result = decoder.decode(window.geometry); result.is_error())
        return result;
    if (auto result = decoder.decode(window.opaque_rects); result.is_error())
        return result;
    if (auto result = decoder.decode(window.transparent_rects); result.is_error())
        return result;
    return {};
}

bool encode(Encoder& encoder, RemoteDesktop::Compositor::WindowFrame const& window_frame)
{
    encoder << window_frame.top_bottom_bitmap_id << window_frame.left_right_bitmap_id;
    return true;
}

ErrorOr<void> decode(Decoder& decoder, RemoteDesktop::Compositor::WindowFrame& window_frame)
{
    if (auto result = decoder.decode(window_frame.top_bottom_bitmap_id); result.is_error())
        return result;
    if (auto result = decoder.decode(window_frame.left_right_bitmap_id); result.is_error())
        return result;
    return {};
}

bool encode(Encoder& encoder, RemoteDesktop::Compositor::WindowGeometry const& window_geometry)
{
    encoder << window_geometry.render_rect << window_geometry.frame_rect << window_geometry.rect;
    return true;
}

ErrorOr<void> decode(Decoder& decoder, RemoteDesktop::Compositor::WindowGeometry& window_geometry)
{
    if (auto result = decoder.decode(window_geometry.render_rect); result.is_error())
        return result;
    if (auto result = decoder.decode(window_geometry.frame_rect); result.is_error())
        return result;
    if (auto result = decoder.decode(window_geometry.rect); result.is_error())
        return result;
    return {};
}

bool encode(Encoder& encoder, RemoteDesktop::Compositor::WindowDirtyRects const& window_dirty_rects)
{
    encoder << window_dirty_rects.id << window_dirty_rects.backing_bitmap_id << window_dirty_rects.is_windowserver_backing_bitmap << window_dirty_rects.backing_bitmap_sync_tag << window_dirty_rects.frame_left_right_bitmap_id << window_dirty_rects.frame_top_bottom_bitmap_id << window_dirty_rects.dirty_rects;
    return true;
}

ErrorOr<void> decode(Decoder& decoder, RemoteDesktop::Compositor::WindowDirtyRects& window_dirty_rects)
{
    if (auto result = decoder.decode(window_dirty_rects.id); result.is_error())
        return result;
    if (auto result = decoder.decode(window_dirty_rects.backing_bitmap_id); result.is_error())
        return result;
    if (auto result = decoder.decode(window_dirty_rects.is_windowserver_backing_bitmap); result.is_error())
        return result;
    if (auto result = decoder.decode(window_dirty_rects.backing_bitmap_sync_tag); result.is_error())
        return result;
    if (auto result = decoder.decode(window_dirty_rects.frame_left_right_bitmap_id); result.is_error())
        return result;
    if (auto result = decoder.decode(window_dirty_rects.frame_top_bottom_bitmap_id); result.is_error())
        return result;
    if (auto result = decoder.decode(window_dirty_rects.dirty_rects); result.is_error())
        return result;
    return {};
}

}
