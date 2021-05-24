/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Player.h"
#include <LibCards/Card.h>
#include <LibCore/Timer.h>
#include <LibGUI/Frame.h>

using Cards::Card;

namespace Hearts {

class Game final : public GUI::Frame {
    C_OBJECT(Game)
public:
    static constexpr int width = 640;
    static constexpr int height = 480;

    virtual ~Game() override;

    void setup(String player_name);

    Function<void(String const&)> on_status_change;

private:
    Game();

    void reset();

    void dump_state() const;

    void play_card(Player& player, size_t card_index);
    bool are_hearts_broken() const;
    bool is_valid_play(Player& player, Card& card, String* explanation = nullptr) const;
    void let_player_play_card();
    void continue_game_after_delay(int interval_ms = 750);
    void advance_game();
    size_t pick_card(Player& player);
    size_t player_index(Player& player);
    Player& current_player();
    bool game_ended() const { return m_trick_number == 13; }
    bool is_winner(Player& player);
    bool other_player_has_lower_value_card(Player& player, Card& card);
    bool other_player_has_higher_value_card(Player& player, Card& card);

    void reposition_hand(Player& player);

    void start_animation(NonnullRefPtrVector<Card> cards, Gfx::IntPoint const& end, Function<void()> did_finish_callback, int initial_delay_ms, int steps = 30);
    void stop_animation();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    Player m_players[4];
    NonnullRefPtrVector<Card> m_trick;
    Player* m_leading_player { nullptr };
    u8 m_trick_number { 0 };
    RefPtr<Core::Timer> m_delay_timer;
    bool m_human_can_play { false };

    struct AnimatedCard {
        NonnullRefPtr<Card> card;
        Gfx::IntPoint start;
    };

    RefPtr<Core::Timer> m_animation_delay_timer;
    bool m_animation_playing { false };
    Vector<AnimatedCard> m_animation_cards;
    Gfx::IntPoint m_animation_end;
    int m_animation_current_step { 0 };
    int m_animation_steps { 0 };
    OwnPtr<Function<void()>> m_animation_did_finish;
};

}
