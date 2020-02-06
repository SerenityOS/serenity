/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <LibCore/Object.h>
#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/DisjointRectSet.h>

namespace Gfx {
class Painter;
}

namespace WindowServer {

class Cursor;

enum class WallpaperMode {
    Simple,
    Tile,
    Center,
    Scaled,
    Unchecked
};

class Compositor final : public Core::Object {
    C_OBJECT(Compositor)
public:
    static Compositor& the();

    void compose();
    void invalidate();
    void invalidate(const Gfx::Rect&);

    void set_resolution(int desired_width, int desired_height);

    bool set_wallpaper(const String& path, Function<void(bool)>&& callback);
    String wallpaper_path() const { return m_wallpaper_path; }

    void invalidate_cursor();
    Gfx::Rect current_cursor_rect() const;

private:
    Compositor();
    void init_bitmaps();
    void flip_buffers();
    void flush(const Gfx::Rect&);
    void draw_cursor();
    void draw_geometry_label();
    void draw_menubar();
    void run_animations();

    unsigned m_compose_count { 0 };
    unsigned m_flush_count { 0 };
    RefPtr<Core::Timer> m_compose_timer;
    RefPtr<Core::Timer> m_immediate_compose_timer;
    bool m_flash_flush { false };
    bool m_buffers_are_flipped { false };
    bool m_screen_can_set_buffer { false };

    RefPtr<Gfx::Bitmap> m_front_bitmap;
    RefPtr<Gfx::Bitmap> m_back_bitmap;
    OwnPtr<Gfx::Painter> m_back_painter;
    OwnPtr<Gfx::Painter> m_front_painter;

    Gfx::DisjointRectSet m_dirty_rects;

    Gfx::Rect m_last_cursor_rect;
    Gfx::Rect m_last_dnd_rect;
    Gfx::Rect m_last_geometry_label_rect;

    String m_wallpaper_path;
    WallpaperMode m_wallpaper_mode { WallpaperMode::Unchecked };
    RefPtr<Gfx::Bitmap> m_wallpaper;
};

}
