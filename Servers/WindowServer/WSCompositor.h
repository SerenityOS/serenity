#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <LibCore/CObject.h>
#include <LibCore/CTimer.h>
#include <LibDraw/DisjointRectSet.h>
#include <LibDraw/GraphicsBitmap.h>

class Painter;
class WSCursor;

enum class WallpaperMode {
    Simple,
    Tile,
    Center,
    Scaled,
    Unchecked
};

class WSCompositor final : public CObject {
    C_OBJECT(WSCompositor)
public:
    static WSCompositor& the();

    void compose();
    void invalidate();
    void invalidate(const Rect&);

    void set_resolution(int desired_width, int desired_height);

    bool set_wallpaper(const String& path, Function<void(bool)>&& callback);
    String wallpaper_path() const { return m_wallpaper_path; }

    void invalidate_cursor();
    Rect current_cursor_rect() const;

private:
    WSCompositor();
    void init_bitmaps();
    void flip_buffers();
    void flush(const Rect&);
    void draw_cursor();
    void draw_geometry_label();
    void draw_menubar();
    void run_animations();

    unsigned m_compose_count { 0 };
    unsigned m_flush_count { 0 };
    RefPtr<CTimer> m_compose_timer;
    RefPtr<CTimer> m_immediate_compose_timer;
    bool m_flash_flush { false };
    bool m_buffers_are_flipped { false };
    bool m_screen_can_set_buffer { false };

    RefPtr<GraphicsBitmap> m_front_bitmap;
    RefPtr<GraphicsBitmap> m_back_bitmap;
    OwnPtr<Painter> m_back_painter;
    OwnPtr<Painter> m_front_painter;

    DisjointRectSet m_dirty_rects;

    Rect m_last_cursor_rect;
    Rect m_last_dnd_rect;
    Rect m_last_geometry_label_rect;

    String m_wallpaper_path;
    WallpaperMode m_wallpaper_mode { WallpaperMode::Unchecked };
    RefPtr<GraphicsBitmap> m_wallpaper;
};
