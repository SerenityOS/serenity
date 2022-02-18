/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <LibCards/CardStack.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Painter.h>

using Cards::Card;
using Cards::CardStack;

namespace Solitaire {

enum class Mode : u8 {
    SingleCardDraw,
    ThreeCardDraw,
    __Count
};

enum class GameOverReason {
    Victory,
    NewGame,
};

class Game final : public GUI::Frame {
    C_OBJECT(Game)
public:
    static constexpr int width = 640;
    static constexpr int height = 480;

    virtual ~Game() override = default;

    Mode mode() const { return m_mode; }
    void setup(Mode);
    void perform_undo();

    bool is_auto_collecting() const { return m_auto_collect; }
    void set_auto_collect(bool collect) { m_auto_collect = collect; }

    Function<void(uint32_t)> on_score_update;
    Function<void()> on_game_start;
    Function<void(GameOverReason, uint32_t)> on_game_end;
    Function<void(bool)> on_undo_availability_change;

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

    struct WasteRecycleRules {
        uint8_t passes_allowed_before_punishment { 0 };
        int8_t punishment { 0 };
    };

    struct LastMove {
        enum class Type {
            Invalid,
            MoveCards,
            FlipCard
        };

        Type type { Type::Invalid };
        CardStack* from { nullptr };
        NonnullRefPtrVector<Card> cards;
        CardStack* to { nullptr };
    };

    enum StackLocation {
        Stock,
        Waste,
        Play,
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
    static constexpr Array foundations = { Foundation1, Foundation2, Foundation3, Foundation4 };

    ALWAYS_INLINE const WasteRecycleRules& recycle_rules()
    {
        static constexpr Array<WasteRecycleRules, 2> rules { {
            { 0, -100 },
            { 2, -20 },
        } };

        switch (m_mode) {
        case Mode::SingleCardDraw:
            return rules[0];
        case Mode::ThreeCardDraw:
            return rules[1];
        default:
            VERIFY_NOT_REACHED();
        }
    }

    void mark_intersecting_stacks_dirty(Card& intersecting_card);
    void score_move(CardStack& from, CardStack& to, bool inverse = false);
    void score_flip(bool inverse = false);
    void remember_move_for_undo(CardStack& from, CardStack& to, NonnullRefPtrVector<Card> moved_cards);
    void remember_flip_for_undo(Card& card);
    void update_score(int to_add);
    void draw_cards();
    void pop_waste_to_play_stack();
    bool attempt_to_move_card_to_foundations(CardStack& from);
    void auto_move_eligible_cards_to_foundations();
    void start_timer_if_necessary();
    void start_game_over_animation();
    void stop_game_over_animation();
    void create_new_animation_card();
    void set_background_fill_enabled(bool);
    void check_for_game_over();
    void dump_layout() const;

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

    Mode m_mode { Mode::SingleCardDraw };

    LastMove m_last_move;
    NonnullRefPtrVector<Card> m_focused_cards;
    NonnullRefPtrVector<Card> m_new_deck;
    NonnullRefPtrVector<CardStack> m_stacks;
    CardStack* m_focused_stack { nullptr };
    Gfx::IntPoint m_mouse_down_location;

    bool m_mouse_down { false };

    Animation m_animation;
    bool m_start_game_over_animation_next_frame { false };
    bool m_game_over_animation { false };
    bool m_waiting_for_new_game { true };
    bool m_new_game_animation { false };
    uint8_t m_new_game_animation_pile { 0 };
    uint8_t m_new_game_animation_delay { 0 };

    uint32_t m_score { 0 };
    uint8_t m_passes_left_before_punishment { 0 };

    bool m_auto_collect { false };
};

}
