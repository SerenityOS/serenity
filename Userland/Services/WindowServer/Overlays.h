/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibGfx/Painter.h>
#include <WindowServer/MultiScaleBitmaps.h>
#include <WindowServer/Screen.h>

namespace WindowServer {

class Screen;
class Window;
class WindowStack;

class Overlay {
    friend class Compositor;

public:
    virtual ~Overlay();

    enum class ZOrder {
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
    void clear_invalidated() { m_invalidated = false; }
    void did_recompute_occlusions()
    {
        m_invalidated = false;
        m_current_rect = m_rect;
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

private:
    void update_rect();

    WeakPtr<Window> m_window;
    String m_label;
    Gfx::IntRect m_label_rect;
};

class DndOverlay : public BitmapOverlay {
public:
    DndOverlay(String const&, Gfx::Bitmap const*);

    void cursor_moved()
    {
        update_rect();
    }

    virtual ZOrder zorder() const override { return ZOrder::Dnd; }
    virtual RefPtr<Gfx::Bitmap> create_bitmap(int) override;

private:
    Gfx::Font const& font();
    void update_rect();

    RefPtr<Gfx::Bitmap> m_bitmap;
    String m_text;
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
    const int m_rows;
    const int m_columns;
    const int m_target_row;
    const int m_target_column;
};

}
