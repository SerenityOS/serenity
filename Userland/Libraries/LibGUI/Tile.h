/*
 * Copyright (c) 2022, Filiph Sandstrom <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/Button.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

class Tile final : public Button {
    C_OBJECT(Tile);

public:
    virtual ~Tile() override = default;

    static int animation_idle() { return 60 * 5; }
    static int animation_duration() { return 60 * 1; }

    struct TileContent {
        enum class ContentKind {
            Branding,
            Normal,
            Date
        };
        ContentKind content_kind;

        enum class ContentAlignment {
            Center,
            Bottom
        };
        ContentAlignment content_alignment = ContentAlignment::Bottom;

        String content = String::empty();
    };
    Vector<TileContent> contents() { return m_contents; }
    void set_contents(Vector<TileContent> contents) { m_contents = contents; }
    void append_contents(TileContent content) { m_contents.append(content); }

    enum class TileAnimation {
        None,
        Slide
    };

    bool animated() { return m_animation != TileAnimation::None; }
    TileAnimation animation() { return m_animation; }
    void set_animation(TileAnimation animation) { m_animation = animation; }

    int animation_start() { return m_animation_start; }
    void set_animation_start(int animation_start) { m_animation_start = animation_start; }

    enum class TileBranding {
        None,
        Label
    };

    TileBranding branding() { return m_branding; }
    void set_branding(TileBranding branding) { m_branding = branding; }

private:
    explicit Tile();

    void tick();

    struct Animation {
        Gfx::IntRect previous_rect;
        Gfx::IntRect current_rect;
    };

    virtual void paint_event(PaintEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override
    {
        tick();
    }

    void draw_branding_tile(Painter painter, Gfx::IntRect content_rect);
    void draw_normal_tile(Painter painter, Gfx::IntRect content_rect, TileContent content);
    void draw_date_tile(Painter painter, Gfx::IntRect content_rect);

    void tick_tile(Painter painter);
    Animation process_animation(int tick);

    TileAnimation m_animation = TileAnimation::None;
    TileBranding m_branding = TileBranding::Label;

    Vector<TileContent> m_contents;

    int m_animation_start = 0;
    bool m_animation_started = false;
    int m_tick = 0;
};

}
