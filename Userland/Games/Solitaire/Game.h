/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CardStack.h"
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>

namespace Solitaire {

class Game final : public GUI::Frame {
    C_OBJECT(Game)
public:
    static constexpr int width = 640;
    static constexpr int height = 480;

    virtual ~Game() override;
    void setup();

    Function<void(uint32_t)> on_score_update;
    Function<void()> on_game_start;
    Function<void()> on_game_end;

private:
    Game();

    class Animation {
    public:
        Animation()
        {
        }

        Animation(RefPtr<Card> animation_card, float gravity, int x_vel, float bouncyness)
            : m_animation_card(animation_card)
            , m_gravity(gravity)
            , m_x_velocity(x_vel)
            , m_bouncyness(bouncyness)
        {
        }

        RefPtr<Card> card() { return m_animation_card; }

        void draw(GUI::Painter& painter)
        {
            VERIFY(!m_animation_card.is_null());
            m_animation_card->draw(painter);
            m_dirty = false;
        }

        bool tick()
        {
            // Don't move the animation card until the event loop has had a chance to paint its current location.
            if (m_dirty)
                return false;

            VERIFY(!m_animation_card.is_null());
            m_y_velocity += m_gravity;

            if (m_animation_card->position().y() + Card::height + m_y_velocity > Game::height + 1 && m_y_velocity > 0) {
                m_y_velocity = min((m_y_velocity * -m_bouncyness), -8.f);
                m_animation_card->rect().set_y(Game::height - Card::height);
                m_animation_card->rect().translate_by(m_x_velocity, 0);
            } else {
                m_animation_card->rect().translate_by(m_x_velocity, m_y_velocity);
            }

            m_dirty = true;
            return true;
        }

    private:
        RefPtr<Card> m_animation_card;
        float m_gravity { 0 };
        int m_x_velocity { 0 };
        float m_y_velocity { 0 };
        float m_bouncyness { 0 };
        bool m_dirty { false };
    };

    enum StackLocation {
        Stock,
        Waste,
        Foundation1,
        Foundation2,
        Foundation3,
        Foundation4,
        Pile1,
        Pile2,
        Pile3,
        Pile4,
        Pile5,
        Pile6,
        Pile7,
        __Count
    };
    static constexpr Array piles = { Pile1, Pile2, Pile3, Pile4, Pile5, Pile6, Pile7 };

    void mark_intersecting_stacks_dirty(Card& intersecting_card);
    void update_score(int to_add);
    void move_card(CardStack& from, CardStack& to);
    void start_game_over_animation();
    void stop_game_over_animation();
    void create_new_animation_card();
    void check_for_game_over();

    ALWAYS_INLINE CardStack& stack(StackLocation location)
    {
        return m_stacks[location];
    }

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    NonnullRefPtrVector<Card> m_focused_cards;
    NonnullRefPtrVector<Card> m_new_deck;
    CardStack m_stacks[StackLocation::__Count];
    CardStack* m_focused_stack { nullptr };
    Gfx::IntPoint m_mouse_down_location;

    bool m_mouse_down { false };

    Animation m_animation;
    bool m_game_over_animation { false };

    bool m_new_game_animation { false };
    uint8_t m_new_game_animation_pile { 0 };
    uint8_t m_new_game_animation_delay { 0 };

    uint32_t m_score { 0 };
};

}
