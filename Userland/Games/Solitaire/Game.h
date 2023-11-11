/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <LibCards/CardGame.h>
#include <LibCards/CardPainter.h>
#include <LibCards/CardStack.h>

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

class Game final : public Cards::CardGame {
    C_OBJECT_ABSTRACT(Game)
public:
    static constexpr int width = 640;
    static constexpr int height = 480;

    static ErrorOr<NonnullRefPtr<Game>> try_create();
    virtual ~Game() override = default;

    Mode mode() const { return m_mode; }
    void setup(Mode);
    void perform_undo();

    bool is_auto_collecting() const { return m_auto_collect; }
    void set_auto_collect(bool collect) { m_auto_collect = collect; }

    bool can_solve();
    void start_solving();

    Function<void(uint32_t)> on_score_update;
    Function<void()> on_game_start;
    Function<void(GameOverReason, uint32_t)> on_game_end;
    Function<void(bool)> on_undo_availability_change;
    Function<void()> on_move;

private:
    Game();

    class Animation {
    public:
        Animation()
        {
        }

        Animation(Cards::Suit suit, Cards::Rank rank, Gfx::IntPoint start_position, float gravity, int x_vel, float bouncyness)
            : m_suit(suit)
            , m_rank(rank)
            , m_position(start_position)
            , m_gravity(gravity)
            , m_x_velocity(x_vel)
            , m_bouncyness(bouncyness)
        {
        }

        Gfx::IntRect card_rect() const
        {
            return {
                m_position.x(), m_position.y(), Card::width, Card::height
            };
        }

        Gfx::IntPoint position() const { return m_position; }

        void draw(GUI::Painter& painter)
        {
            auto bitmap = Cards::CardPainter::the().card_front(m_suit, m_rank);
            painter.blit(m_position, bitmap, bitmap->rect());
            m_dirty = false;
        }

        bool tick()
        {
            // Don't move the animation card until the event loop has had a chance to paint its current location.
            if (m_dirty)
                return false;

            m_y_velocity += m_gravity;

            if (m_position.y() + Card::height + m_y_velocity > Game::height + 1 && m_y_velocity > 0) {
                m_y_velocity = min((m_y_velocity * -m_bouncyness), -8.f);
                m_position.set_y(Game::height - Card::height);
                m_position.translate_by(m_x_velocity, 0);
            } else {
                m_position.translate_by(m_x_velocity, m_y_velocity);
            }

            m_dirty = true;
            return true;
        }

    private:
        Cards::Suit m_suit { Cards::Suit::Spades };
        Cards::Rank m_rank { Cards::Rank::Ace };
        Gfx::IntPoint m_position { 0, 0 };
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
        Vector<NonnullRefPtr<Card>> cards;
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

    ALWAYS_INLINE WasteRecycleRules const& recycle_rules()
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

    void score_move(CardStack& from, CardStack& to, bool inverse = false);
    void score_flip(bool inverse = false);
    void remember_move_for_undo(CardStack& from, CardStack& to, Vector<NonnullRefPtr<Card>> moved_cards);
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
    void clear_hovered_stack();
    void deal_next_card();
    void step_solve();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    Mode m_mode { Mode::SingleCardDraw };

    LastMove m_last_move;
    Vector<NonnullRefPtr<Card>> m_new_deck;
    Gfx::IntPoint m_mouse_down_location;

    bool m_mouse_down { false };

    enum class State {
        WaitingForNewGame,
        NewGameAnimation,
        GameInProgress,
        StartGameOverAnimationNextFrame,
        GameOverAnimation,
        Solving,
    };
    State m_state { State::WaitingForNewGame };

    Animation m_animation;
    uint8_t m_new_game_animation_pile { 0 };
    uint8_t m_new_game_animation_delay { 0 };

    uint32_t m_score { 0 };
    uint8_t m_passes_left_before_punishment { 0 };

    bool m_auto_collect { false };

    RefPtr<CardStack> m_hovered_stack;
};

}
