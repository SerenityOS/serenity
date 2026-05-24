/*
 * Copyright (c) 2026, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Vector.h>
#include <LibCards/CardGame.h>
#include <LibCards/CardStack.h>

using Cards::Card;
using Cards::CardStack;

namespace TriPeaks {

enum class GameOverReason {
    Victory,
    NewGame,
    Quit,
};

class Game final : public Cards::CardGame {
    C_OBJECT_ABSTRACT(Game)
public:
    static constexpr int width = 820;
    static constexpr int height = 500;

    static ErrorOr<NonnullRefPtr<Game>> try_create();
    virtual ~Game() override = default;

    void setup();

    Function<void(u32)> on_score_update;
    Function<void(u32)> on_streak_update;
    Function<void()> on_game_start;
    Function<void(GameOverReason, u32)> on_game_end;
    Function<void(bool)> on_undo_availability_change;

    void perform_undo();

private:
    Game();

    struct LastMove {
        enum class Type {
            Invalid,
            TableauRemove,
            StockDraw,
        };
        Type type { Type::Invalid };
        u32 tableau_peak { 0 };
        u32 tableau_pos { 0 };
        Vector<u32> flipped_positions;
        u32 score_before { 0 };
        u32 streak_before { 0 };
    };

    struct CoverPair {
        u8 covered;
        u8 coverer_left;
        u8 coverer_right;
    };
    static constexpr size_t cards_per_peak = 10u;
    static constexpr size_t tableau_cards = 30u;

    static constexpr Array tableau_cover_pairs = {
        CoverPair { 0u, 1u, 2u },
        CoverPair { 1u, 3u, 4u },
        CoverPair { 2u, 4u, 5u },
        CoverPair { 3u, 6u, 7u },
        CoverPair { 4u, 7u, 8u },
        CoverPair { 5u, 8u, 9u },
    };

    enum StackLocation {
        Stock,
        Waste,
        TableauFirst,
        TableauLast = TableauFirst + tableau_cards - 1,
        __Count
    };

    static Gfx::IntPoint card_position_for_location(u32 peak, u32 row, u32 col);
    void update_card_disabled_status(u32 peak, u32 pos);
    void remove_tableau_card(u32 peak, u32 pos);
    void flip_uncovered_cards(u32 peak, u32 removed_pos);
    bool has_valid_moves() const;
    void draw_from_stock();
    void update_score(u32 delta);
    void update_streak();
    void check_for_victory();
    void check_for_game_over();
    void start_timer_if_necessary();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    u32 m_score { 0 };
    u32 m_streak { 0 };
    LastMove m_last_move;

    enum class State {
        WaitingForNewGame,
        GameInProgress,
        GameOver,
    };
    State m_state { State::WaitingForNewGame };
};

}
