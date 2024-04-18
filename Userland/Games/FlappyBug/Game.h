/*
 * Copyright (c) 2021, Mim Hufford <mim@hotmail.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Random.h>
#include <AK/Vector.h>
#include <LibGUI/Application.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/StandardCursor.h>

namespace FlappyBug {

class Game final : public GUI::Frame {
    C_OBJECT(Game);

public:
    static constexpr int game_width = 560;
    static constexpr int game_height = 480;

    Function<u32(u32)> on_game_end;

private:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    void tick();
    void reset();
    void game_over();
    bool ready_to_start() const;
    void player_input();

    static u32 get_final_score(float score);

public:
    struct Bug {
        float const x { 50 };
        float const radius { 16 };
        float const starting_y { 200 };
        NonnullRefPtr<Gfx::Bitmap> falling_bitmap;
        NonnullRefPtr<Gfx::Bitmap> flapping_bitmap;
        float y {};
        float velocity {};

    private:
        Bug(NonnullRefPtr<Gfx::Bitmap> falling_bitmap_value, NonnullRefPtr<Gfx::Bitmap> flapping_bitmap_value)
            : falling_bitmap(move(falling_bitmap_value))
            , flapping_bitmap(move(flapping_bitmap_value))
        {
        }

    public:
        static ErrorOr<Bug> construct()
        {
            NonnullRefPtr<Gfx::Bitmap> falling_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/flappybug/falling.png"sv));
            NonnullRefPtr<Gfx::Bitmap> flapping_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/flappybug/flapping.png"sv));
            return Bug(move(falling_bitmap), move(flapping_bitmap));
        }

        void reset()
        {
            y = starting_y;
        }

        NonnullRefPtr<Gfx::Bitmap> current_bitmap() const
        {
            return velocity < 0 ? falling_bitmap : flapping_bitmap;
        }

        Gfx::FloatRect rect() const
        {
            return { x - radius, y - radius, radius * 2, radius * 2 };
        }

        void flap()
        {
            float const flap_strength = 10.0f;
            velocity = -flap_strength;
        }

        void fall()
        {
            float const gravity = 1.0f;
            velocity += gravity;
        }

        void apply_velocity()
        {
            y += velocity;
        }
    };

    struct Obstacle {
        float const width { 20 };
        Color color { Color::DarkGray };
        float x {};
        float gap_top_y { 200 };
        float gap_height { 175 };

        void reset()
        {
            x = game_width + width;
            gap_top_y = get_random_uniform(game_height - gap_height);
        }

        Gfx::FloatRect top_rect() const
        {
            return { x - width, 0, width, gap_top_y };
        }

        Gfx::FloatRect bottom_rect() const
        {
            return { x - width, gap_top_y + gap_height, width, game_height - gap_top_y - gap_height };
        }
    };

    struct Cloud {
        Vector<NonnullRefPtr<Gfx::Bitmap>> const cloud_bitmaps;
        float x {};
        float y {};
        int bitmap_id {};

    private:
        Cloud(Vector<NonnullRefPtr<Gfx::Bitmap>> const cloud_bitmaps_value)
            : cloud_bitmaps(move(cloud_bitmaps_value))
        {
            reset();
            x = get_random_uniform(game_width);
        }

    public:
        static ErrorOr<Cloud> construct()
        {
            Vector<NonnullRefPtr<Gfx::Bitmap>> const cloud_bitmaps {
                TRY(Gfx::Bitmap::load_from_file("/res/graphics/flappybug/cloud_0.png"sv)),
                TRY(Gfx::Bitmap::load_from_file("/res/graphics/flappybug/cloud_1.png"sv)),
                TRY(Gfx::Bitmap::load_from_file("/res/graphics/flappybug/cloud_2.png"sv)),
            };
            return Cloud(move(cloud_bitmaps));
        }

        void reset()
        {
            bitmap_id = get_random_uniform(cloud_bitmaps.size());
            x = game_width + bitmap()->width();
            y = get_random_uniform(game_height / 2) + bitmap()->height();
        }

        NonnullRefPtr<Gfx::Bitmap> bitmap() const
        {
            return cloud_bitmaps[bitmap_id];
        }

        Gfx::IntRect rect() const
        {
            return { (int)x - bitmap()->width(), (int)y - bitmap()->height(), bitmap()->width(), bitmap()->height() };
        }
    };

private:
    Bug m_bug;
    Obstacle m_obstacle;
    Cloud m_cloud;
    bool m_active;
    Optional<u32> m_high_score {};
    float m_last_score {};
    float m_difficulty {};
    float m_restart_cooldown {};
    NonnullRefPtr<Gfx::Bitmap> m_background_bitmap { Gfx::Bitmap::load_from_file("/res/graphics/flappybug/background.png"sv).release_value_but_fixme_should_propagate_errors() };
    Gfx::IntRect const m_score_rect { 10, 10, 20, 20 };
    Gfx::IntRect const m_text_rect { game_width / 2 - 80, game_height / 2 - 40, 160, 80 };

    Game(Bug, Cloud);
};

}
