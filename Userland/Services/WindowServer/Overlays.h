/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <WindowServer/MultiScaleBitmaps.h>
#include <WindowServer/Screen.h>

namespace WindowServer {

class Animation;
class Screen;
class TileWindowOverlay;
class Window;
class WindowStack;

class Overlay {
    friend class Compositor;

public:
    virtual ~Overlay();

    enum class ZOrder {
        SnapWindow,
        WindowGeometry,
        Dnd,
        WindowStackSwitch,
        ScreenNumber,
    };
    [[nodiscard]] virtual ZOrder zorder() const = 0;
    virtual void render(Gfx::Painter&, Screen const&) = 0;

    Gfx::IntRect const& rect() const { return m_rect; }
    Gfx::IntRect const& current_render_rect() const { return m_current_rect; }

    void set_enabled(bool);
    bool is_enabled() const { return m_list_node.is_in_list(); }

    virtual void theme_changed()
    {
        rect_changed(m_rect);
    }

    bool invalidate();

protected:
    Overlay() = default;

    void set_rect(Gfx::IntRect const&);

    virtual void rect_changed(Gfx::IntRect const&) {};

private:
    [[nodiscard]] bool apply_render_rect()
    {
        bool needs_invalidation = m_invalidated;
        m_invalidated = false;
        m_current_rect = m_rect;
        return needs_invalidation;
    }

    Gfx::IntRect m_rect;
    Gfx::IntRect m_current_rect;
    Vector<Screen*, default_screen_count> m_screens;
    IntrusiveListNode<Overlay> m_list_node;
    bool m_invalidated { false };
};

class BitmapOverlay : public Overlay {
public:
    virtual RefPtr<Gfx::Bitmap> create_bitmap(int) = 0;

    virtual void render(Gfx::Painter&, Screen const&) override;

protected:
    BitmapOverlay();

    void clear_bitmaps();
    virtual void rect_changed(Gfx::IntRect const&) override;

private:
    RefPtr<MultiScaleBitmaps> m_bitmaps;
};

class RectangularOverlay : public Overlay {
public:
    static constexpr int default_minimum_size = 10;
    static constexpr int default_frame_thickness = 5;

    virtual void render(Gfx::Painter&, Screen const&) override;
    virtual void render_overlay_bitmap(Gfx::Painter&) = 0;

protected:
    RectangularOverlay();

    static Gfx::IntRect calculate_frame_rect(Gfx::IntRect const&);
    void set_content_rect(Gfx::IntRect const&);

    void clear_bitmaps();
    virtual void rect_changed(Gfx::IntRect const&) override;

    void invalidate_content();

private:
    RefPtr<MultiScaleBitmaps> m_rendered_bitmaps;
    bool m_content_invalidated { false };
};

class ScreenNumberOverlay : public RectangularOverlay {
public:
    static constexpr int default_offset = 20;
    static constexpr int default_size = 120;
    static constexpr int screen_number_padding = 10;

    ScreenNumberOverlay(Screen&);

    static Gfx::IntRect calculate_content_rect_for_screen(Screen&);

    virtual ZOrder zorder() const override { return ZOrder::ScreenNumber; }
    virtual void render_overlay_bitmap(Gfx::Painter&) override;

    static void pick_font();

private:
    Gfx::Font const& font();

    Screen& m_screen;

    static Gfx::Font const* s_font;
};

class WindowGeometryOverlay : public RectangularOverlay {
public:
    WindowGeometryOverlay(Window&);

    void window_rect_changed();

    virtual ZOrder zorder() const override { return ZOrder::WindowGeometry; }
    virtual void render_overlay_bitmap(Gfx::Painter&) override;

    void set_actual_rect();
    void start_or_stop_move_to_tile_overlay_animation(TileWindowOverlay*);

private:
    Gfx::IntRect calculate_ideal_overlay_rect() const;

    WeakPtr<Window> m_window;
    ByteString m_label;
    Gfx::IntRect m_label_rect;
    Gfx::IntRect m_ideal_overlay_rect;

    struct UpdateState {
        Gfx::IntRect geometry;
        bool is_for_tile_overlay;

        bool operator==(UpdateState const& other) const
        {
            if (this == &other)
                return true;
            return geometry == other.geometry && is_for_tile_overlay == other.is_for_tile_overlay;
        }
    };
    UpdateState m_last_updated;

    struct {
        RefPtr<Animation> animation;
        float progress { 0.0f };
        Gfx::IntRect tile_overlay_rect_at_start;
        Gfx::IntRect current_rect;
        Optional<Gfx::IntRect> starting_rect;
    } m_move_into_overlay_rect_animation;
};

class DndOverlay : public BitmapOverlay {
public:
    DndOverlay(ByteString const&, Gfx::Bitmap const*);

    void cursor_moved()
    {
        update_rect();
    }

    virtual ZOrder zorder() const override { return ZOrder::Dnd; }
    virtual RefPtr<Gfx::Bitmap> create_bitmap(int) override;

private:
    Gfx::Font const& font();
    void update_rect();

    RefPtr<Gfx::Bitmap const> m_bitmap;
    ByteString m_text;
    Gfx::IntRect m_label_rect;
};

class WindowStackSwitchOverlay : public RectangularOverlay {
public:
    static constexpr int default_screen_rect_width = 40;
    static constexpr int default_screen_rect_height = 30;
    static constexpr int default_screen_rect_margin = 16;
    static constexpr int default_screen_rect_padding = 8;

    WindowStackSwitchOverlay(Screen&, WindowStack&);

    virtual ZOrder zorder() const override { return ZOrder::WindowStackSwitch; }
    virtual void render_overlay_bitmap(Gfx::Painter&) override;

private:
    Gfx::IntSize m_content_size;
    int const m_rows;
    int const m_columns;
    int const m_target_row;
    int const m_target_column;
};

class TileWindowOverlay : public Overlay {
public:
    TileWindowOverlay(Window&, Gfx::IntRect const&, Gfx::Palette&&);

    virtual ZOrder zorder() const override { return ZOrder::SnapWindow; }
    virtual void render(Gfx::Painter&, Screen const&) override;

    void set_overlay_rect(Gfx::IntRect const& rect)
    {
        set_rect(rect);
    }

    void set_tiled_frame_rect(Gfx::IntRect const& rect) { m_tiled_frame_rect = rect; }
    Gfx::IntRect const& tiled_frame_rect() const { return m_tiled_frame_rect; }
    bool is_window(Window& window) const { return &m_window == &window; }

private:
    Window& m_window;
    Gfx::IntRect m_tiled_frame_rect;
    Gfx::Palette m_palette;
};

}
