/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Player.h"
#include <LibCards/Card.h>
#include <LibCards/CardGame.h>
#include <LibCore/Timer.h>

using Cards::Card;

namespace Hearts {

class Game final : public Cards::CardGame {
    C_OBJECT(Game)
public:
    static constexpr int width = 640;
    static constexpr int height = 480;

    virtual ~Game() override = default;

    void setup(ByteString player_name, int hand_number = 0);

    Function<void(String const&)> on_status_change;

private:
    Game();

    void reset();

    void show_score_card(bool game_over);

    void dump_state() const;

    void play_card(Player& player, size_t card_index);
    bool are_hearts_broken() const;
    bool is_valid_play(Player& player, Card& card, ByteString* explanation = nullptr) const;
    void let_player_play_card();
    void continue_game_after_delay(int interval_ms = 750);
    void advance_game();
    size_t pick_card(Player& player);
    size_t pick_first_card_ltr(Player& player);
    size_t player_index(Player& player);
    Player& current_player();
    bool game_ended() const { return m_trick_number == 13; }
    int calculate_score(Player& player);
    bool other_player_has_lower_value_card(Player& player, Card& card);
    bool other_player_has_higher_value_card(Player& player, Card& card);
    bool other_player_has_queen_of_spades(Player& player);

    void reposition_hand(Player&);
    bool is_card_highlighted(Card& card);
    void clear_highlighted_cards();
    void highlight_card(Card& card);
    void unhighlight_card(Card& card);
    void select_cards_for_passing();
    void pass_cards();
    PassingDirection passing_direction() const;

    void start_animation(Vector<NonnullRefPtr<Card>> cards, Gfx::IntPoint end, Function<void()> did_finish_callback, int initial_delay_ms, int steps = 30);
    void stop_animation();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void timer_event(Core::TimerEvent&) override;

    void card_clicked(size_t card_index, Card& card);
    void card_clicked_during_passing(size_t card_index, Card& card);
    void card_clicked_during_play(size_t card_index, Card& card);

    RefPtr<GUI::Button> m_passing_button;

    enum class State {
        PassingSelect,
        PassingSelectConfirmed,
        PassingAccept,
        Play,
        GameEnded,
    };

    State m_state { State::PassingSelect };
    int m_hand_number { 0 };

    HashTable<NonnullRefPtr<Card>> m_cards_highlighted;

    Player m_players[4];
    Vector<NonnullRefPtr<Card>> m_trick;
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

    RefPtr<Card> m_inverted_card;
};

}
