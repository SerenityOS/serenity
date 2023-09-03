/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Geometry.h"
#include "Skins/SnakeSkin.h"
#include <AK/CircularQueue.h>
#include <AK/String.h>
#include <LibConfig/Listener.h>
#include <LibGUI/Frame.h>

namespace Snake {

class Game
    : public GUI::Frame
    , public Config::Listener {
    C_OBJECT_ABSTRACT(Game);

public:
    static ErrorOr<NonnullRefPtr<Game>> try_create();

    virtual ~Game() override = default;

    bool is_paused() const { return !has_timer(); }
    void start();
    void pause();
    void reset();

    Function<bool(u32)> on_score_update;

    void set_skin_color(Color);
    Gfx::Color get_skin_color() const { return m_snake_color; }
    void set_skin_name(String);
    void set_skin(NonnullOwnPtr<SnakeSkin> skin);

private:
    explicit Game(Vector<NonnullRefPtr<Gfx::Bitmap>> food_bitmaps, Color snake_color, String snake_skin_name, NonnullOwnPtr<SnakeSkin> skin);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value) override;
    void config_u32_did_change(StringView domain, StringView group, StringView key, u32 value) override;

    void game_over();
    void spawn_fruit();
    bool is_available(Coordinate const&);
    void queue_velocity(int v, int h);
    Velocity const& last_velocity() const;
    Gfx::IntRect cell_rect(Coordinate const&) const;
    Direction direction_to_position(Coordinate const& from, Coordinate const& to) const;

    int m_rows { 20 };
    int m_columns { 20 };

    Velocity m_velocity { 0, 1 };
    Velocity m_last_velocity { 0, 1 };

    CircularQueue<Velocity, 10> m_velocity_queue;

    Coordinate m_head;
    Vector<Coordinate> m_tail;

    Coordinate m_fruit;
    int m_fruit_type { 0 };

    size_t m_length { 0 };
    unsigned m_score { 0 };
    bool m_is_new_high_score { false };

    Vector<NonnullRefPtr<Gfx::Bitmap>> m_food_bitmaps;

    Color m_snake_color;
    String m_snake_skin_name;
    NonnullOwnPtr<SnakeSkin> m_snake_skin;
};

}
