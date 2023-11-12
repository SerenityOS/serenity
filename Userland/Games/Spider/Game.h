/*
 * Copyright (c) 2021, Jamie Mansfield <jmansfield@cadixdev.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <LibCards/CardGame.h>
#include <LibCards/CardStack.h>

using Cards::Card;
using Cards::CardStack;

namespace Spider {

enum class Mode : u8 {
    SingleSuit,
    TwoSuit,
    __Count
};

enum class GameOverReason {
    Victory,
    NewGame,
    Quit,
};

class Game final : public Cards::CardGame {
    C_OBJECT_ABSTRACT(Game)
public:
    static constexpr int width = 10 + 10 * Card::width + 90 + 10;
    static constexpr int height = 480;

    static ErrorOr<NonnullRefPtr<Game>> try_create();
    ~Game() override = default;

    Mode mode() const { return m_mode; }
    void setup(Mode);

    void perform_undo();

    Function<void(uint32_t)> on_score_update;
    Function<void()> on_game_start;
    Function<void(GameOverReason, uint32_t)> on_game_end;
    Function<void(bool)> on_undo_availability_change;

private:
    Game();

    struct LastMove {
        enum class Type {
            Invalid,
            MoveCards
        };

        Type type { Type::Invalid };
        CardStack* from { nullptr };
        size_t card_count;
        bool was_visible;
        CardStack* to { nullptr };
    };

    enum StackLocation {
        Completed,
        Stock,
        Pile1,
        Pile2,
        Pile3,
        Pile4,
        Pile5,
        Pile6,
        Pile7,
        Pile8,
        Pile9,
        Pile10,
        __Count
    };
    static constexpr Array piles = {
        Pile1, Pile2, Pile3, Pile4, Pile5,
        Pile6, Pile7, Pile8, Pile9, Pile10
    };

    void start_timer_if_necessary();
    void remember_move_for_undo(CardStack&, CardStack&, size_t, bool);
    void update_score(int delta);
    void draw_cards();
    void detect_full_stacks();
    void detect_victory();
    void move_focused_cards(CardStack& stack);
    void clear_hovered_stack();
    void deal_next_card();
    void update_disabled_cards();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(GUI::MouseEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    Mode m_mode { Mode::SingleSuit };

    LastMove m_last_move;
    Vector<NonnullRefPtr<Card>> m_new_deck;
    Gfx::IntPoint m_mouse_down_location;

    bool m_mouse_down { false };

    enum class State {
        WaitingForNewGame,
        NewGameAnimation,
        GameInProgress,
        DrawAnimation,
        Victory,
    };
    State m_state { State::WaitingForNewGame };
    uint8_t m_new_game_animation_delay { 0 };
    uint8_t m_new_game_animation_pile { 0 };

    uint8_t m_draw_animation_delay { 0 };
    uint8_t m_draw_animation_pile { 0 };
    Gfx::IntRect m_original_stock_rect;

    uint32_t m_score { 500 };

    RefPtr<CardStack> m_hovered_stack;
};

}
