/*
 * Copyright (c) 2021, Mim Hufford <mim@hotmail.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Random.h>
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/StandardCursor.h>

namespace FlappyBug {

class Game final : public GUI::Widget {
    C_OBJECT(Game);

public:
    static const int game_width = 560;
    static const int game_height = 480;

private:
    Game();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    void tick();
    void reset();
    void game_over();
    bool ready_to_start() const;

    struct Bug {
        const float x { 50 };
        const float radius { 16 };
        const float starting_y { 200 };
        const RefPtr<Gfx::Bitmap> falling_bitmap { Gfx::Bitmap::load_from_file("/res/icons/flappybug/falling.png") };
        const RefPtr<Gfx::Bitmap> flapping_bitmap { Gfx::Bitmap::load_from_file("/res/icons/flappybug/flapping.png") };
        float y {};
        float velocity {};

        void reset()
        {
            y = starting_y;
        }

        RefPtr<Gfx::Bitmap> current_bitmap() const
        {
            return velocity < 0 ? falling_bitmap : flapping_bitmap;
        }

        Gfx::FloatRect rect() const
        {
            return { x - radius, y - radius, radius * 2, radius * 2 };
        }

        void flap()
        {
            const float flap_strength = 15.0f;
            velocity = -flap_strength;
        }

        void fall()
        {
            const float gravity = 1.0f;
            velocity += gravity;
        }

        void apply_velocity()
        {
            y += velocity;
        }
    };

    struct Obstacle {
        const float width { 20 };
        Color color { Color::DarkGray };
        float x;
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

    Bug m_bug;
    Obstacle m_obstacle;
    bool m_active;
    Optional<float> m_highscore;
    float m_last_score;
    float m_difficulty;
    float m_restart_cooldown;
    Color m_sky_color { 100, 100, 200 };
};

}
