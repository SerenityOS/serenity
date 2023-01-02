/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/FixedArray.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/Frame.h>

namespace Snake {

class Game : public GUI::Frame {
    C_OBJECT_ABSTRACT(Game);

public:
    static ErrorOr<NonnullRefPtr<Game>> try_create();

    virtual ~Game() override = default;

    void start();
    void pause();
    void reset();

    ErrorOr<void> set_skin(DeprecatedString const& skin);

    DeprecatedString const& skin() const { return m_skin; }

    Function<bool(u32)> on_score_update;

private:
    explicit Game(NonnullRefPtrVector<Gfx::Bitmap> food_bitmaps);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    struct Coordinate {
        int row { 0 };
        int column { 0 };

        bool operator==(Coordinate const& other) const
        {
            return row == other.row && column == other.column;
        }

        Coordinate operator+(Coordinate const& other) const
        {
            return { row + other.row, column + other.column };
        }
    };

    struct Velocity {
        int vertical { 0 };
        int horizontal { 0 };

        Velocity operator-() const
        {
            return { -vertical, -horizontal };
        }
    };

    struct Tail {
        Coordinate position = { 0, 0 };
        Velocity velocity = { 0, 0 };
    };

    void game_over();
    void spawn_fruit();
    bool is_available(Coordinate const&);
    void queue_velocity(int v, int h);
    Velocity const& last_velocity() const;
    Gfx::IntRect cell_rect(Coordinate const&) const;
    NonnullRefPtr<Gfx::Bitmap> get_head_bitmap() const;

    ErrorOr<void> load_snake_bitmaps();

    int m_rows { 20 };
    int m_columns { 20 };

    Velocity m_velocity { 0, 1 };
    Velocity m_last_velocity { 0, 1 };

    CircularQueue<Velocity, 10> m_velocity_queue;

    DeprecatedString m_skin = "snake";

    Coordinate m_head;
    Vector<Tail> m_tail;

    Coordinate m_fruit;
    int m_fruit_type { 0 };

    size_t m_length { 0 };
    unsigned m_score { 0 };
    bool m_is_new_high_score { false };

    NonnullRefPtrVector<Gfx::Bitmap> m_food_bitmaps;
    NonnullRefPtrVector<Gfx::Bitmap> m_snake_bitmaps;
    NonnullRefPtrVector<Gfx::Bitmap> m_head_bitmaps;
};

}
