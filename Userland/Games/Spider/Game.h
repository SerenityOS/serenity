/*
 * Copyright (c) 2021, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <LibCards/CardStack.h>
#include <LibGUI/Frame.h>

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

class Game final : public GUI::Frame {
    C_OBJECT(Game)
public:
    static constexpr int width = 10 + 10 * Card::width + 90 + 10;
    static constexpr int height = 480;

    ~Game() override;

    Mode mode() const { return m_mode; }
    void setup(Mode);

    Function<void(uint32_t)> on_score_update;
    Function<void()> on_game_start;
    Function<void(GameOverReason, uint32_t)> on_game_end;

private:
    Game();

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
    void update_score(int delta);
    void draw_cards();
    void mark_intersecting_stacks_dirty(Card& intersecting_card);
    void ensure_top_card_is_visible(NonnullRefPtr<CardStack> stack);
    void detect_full_stacks();
    void detect_victory();
    void move_focused_cards(CardStack& stack);

    ALWAYS_INLINE CardStack& stack(StackLocation location)
    {
        return m_stacks[location];
    }

    void paint_event(GUI::PaintEvent&) override;
    void mousedown_event(GUI::MouseEvent&) override;
    void mouseup_event(GUI::MouseEvent&) override;
    void mousemove_event(GUI::MouseEvent&) override;
    void timer_event(Core::TimerEvent&) override;

    Mode m_mode { Mode::SingleSuit };

    NonnullRefPtrVector<Card> m_focused_cards;
    NonnullRefPtrVector<Card> m_new_deck;
    NonnullRefPtrVector<CardStack> m_stacks;
    CardStack* m_focused_stack { nullptr };
    Gfx::IntPoint m_mouse_down_location;

    bool m_mouse_down { false };

    bool m_waiting_for_new_game { true };
    bool m_new_game_animation { false };
    uint8_t m_new_game_animation_delay { 0 };
    uint8_t m_new_game_animation_pile { 0 };

    bool m_draw_animation { false };
    uint8_t m_draw_animation_delay { 0 };
    uint8_t m_draw_animation_pile { 0 };
    Gfx::IntRect m_original_stock_rect;

    uint32_t m_score { 500 };
};

}
