/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
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

#include "CardStack.h"
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>

class SolitaireWidget final : public GUI::Widget {
    C_OBJECT(SolitaireWidget)
public:
    static constexpr int width = 640;
    static constexpr int height = 480;

    virtual ~SolitaireWidget() override;
    void setup();

private:
    SolitaireWidget(GUI::Window&, Function<void(uint32_t)>&& on_score_update);

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

        Card* card() { return m_animation_card; }

        void tick()
        {
            ASSERT(!m_animation_card.is_null());
            m_y_velocity += m_gravity;

            if (m_animation_card->position().y() + Card::height + m_y_velocity > SolitaireWidget::height + 1
                && m_y_velocity > 0) {
                m_y_velocity = min((m_y_velocity * -m_bouncyness), -8.f);
                m_animation_card->rect().set_y(SolitaireWidget::height - Card::height);
                m_animation_card->rect().move_by(m_x_velocity, 0);
            } else {
                m_animation_card->rect().move_by(m_x_velocity, m_y_velocity);
            }
        }

    private:
        RefPtr<Card> m_animation_card;
        float m_gravity { 0 };
        int m_x_velocity { 0 };
        float m_y_velocity { 0 };
        float m_bouncyness { 0 };
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

    void mark_intersecting_stacks_dirty(Card& intersecting_card);
    void update_score(int to_add);
    void move_card(CardStack& from, CardStack& to);
    void start_game_over_animation();
    void stop_game_over_animation();
    void create_new_animation_card();
    void check_for_game_over();
    void tick(GUI::Window&);

    inline CardStack& stack(StackLocation location)
    {
        return m_stacks[location];
    }

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    RefPtr<Core::Timer> m_timer;
    NonnullRefPtrVector<Card> m_focused_cards;
    Animation m_animation;
    CardStack* m_focused_stack { nullptr };
    CardStack m_stacks[StackLocation::__Count];
    Gfx::IntPoint m_mouse_down_location;
    bool m_mouse_down { false };
    bool m_repaint_all { true };
    bool m_has_to_repaint { true };
    bool m_game_over_animation { false };
    uint32_t m_score { 0 };
    Function<void(uint32_t)> m_on_score_update;
};
