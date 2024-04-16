/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include "Helpers.h"
#include "ScoreCard.h"
#include <AK/Debug.h>
#include <AK/QuickSort.h>
#include <AK/Random.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>

namespace Hearts {

Game::Game()
{
    m_delay_timer = Core::Timer::create_single_shot(0, [this] {
        dbgln_if(HEARTS_DEBUG, "Continuing game after delay...");
        advance_game();
    });

    constexpr int card_overlap = 20;
    constexpr int outer_border_size = 15;
    constexpr int player_deck_width = 12 * card_overlap + Card::width;
    constexpr int player_deck_height = 12 * card_overlap + Card::height;
    constexpr int text_height = 15;
    constexpr int text_offset = 5;

    m_players[0].first_card_position = { (width - player_deck_width) / 2, height - outer_border_size - Card::height };
    m_players[0].card_offset = { card_overlap, 0 };
    m_players[0].name_position = {
        (width - player_deck_width) / 2 - 50, height - outer_border_size - text_height - text_offset,
        50 - text_offset, text_height
    };
    m_players[0].name_alignment = Gfx::TextAlignment::BottomRight;
    m_players[0].name = "Gunnar";
    m_players[0].is_human = true;
    m_players[0].taken_cards_target = { width / 2 - Card::width / 2, height };

    m_players[1].first_card_position = { outer_border_size, (height - player_deck_height) / 2 };
    m_players[1].card_offset = { 0, card_overlap };
    m_players[1].name_position = {
        outer_border_size, (height - player_deck_height) / 2 - text_height - text_offset,
        Card::width, text_height
    };
    m_players[1].name_alignment = Gfx::TextAlignment::BottomLeft;
    m_players[1].name = "Paul";
    m_players[1].taken_cards_target = { -Card::width, height / 2 - Card::height / 2 };

    m_players[2].first_card_position = { width - (width - player_deck_width) / 2 - Card::width, outer_border_size };
    m_players[2].card_offset = { -card_overlap, 0 };
    m_players[2].name_position = {
        width - (width - player_deck_width) / 2 + text_offset, outer_border_size + text_offset,
        Card::width, text_height
    };
    m_players[2].name_alignment = Gfx::TextAlignment::TopLeft;
    m_players[2].name = "Simon";
    m_players[2].taken_cards_target = { width / 2 - Card::width / 2, -Card::height };

    m_players[3].first_card_position = { width - outer_border_size - Card::width, height - (height - player_deck_height) / 2 - Card::height };
    m_players[3].card_offset = { 0, -card_overlap };
    m_players[3].name_position = {
        width - outer_border_size - Card::width, height - (height - player_deck_height) / 2 + text_offset,
        Card::width, text_height
    };
    m_players[3].name_alignment = Gfx::TextAlignment::TopRight;
    m_players[3].name = "Lisa";
    m_players[3].taken_cards_target = { width, height / 2 - Card::height / 2 };

    m_passing_button = add<GUI::Button>("Pass Left"_string);
    constexpr int button_width = 120;
    constexpr int button_height = 30;
    m_passing_button->set_relative_rect(width / 2 - button_width / 2, height - 3 * outer_border_size - Card::height - button_height, button_width, button_height);
    m_passing_button->on_click = [this](unsigned int) {
        if (m_state == State::PassingSelect)
            m_state = State::PassingSelectConfirmed;
        else
            m_state = State::Play;
        advance_game();
    };

    reset();
}

void Game::reset()
{
    dbgln_if(HEARTS_DEBUG, "=====");
    dbgln_if(HEARTS_DEBUG, "Resetting game");

    stop_animation();

    m_hand_number = 0;

    m_passing_button->set_enabled(false);
    m_passing_button->set_visible(false);

    m_cards_highlighted.clear();

    m_trick.clear_with_capacity();
    m_trick_number = 0;

    for (auto& player : m_players) {
        player.hand.clear_with_capacity();
        player.cards_taken.clear_with_capacity();
    }
}

void Game::show_score_card(bool game_over)
{
    auto score_dialog = GUI::Dialog::construct(window());
    score_dialog->set_resizable(false);
    score_dialog->set_icon(window()->icon());

    auto score_widget = score_dialog->set_main_widget<GUI::Widget>();
    score_widget->set_fill_with_background_color(true);
    score_widget->set_layout<GUI::HorizontalBoxLayout>(10, 15);

    auto& card_container = score_widget->add<GUI::Widget>();
    auto& score_card = card_container.add<ScoreCard>(m_players, game_over);

    auto& button_container = score_widget->add<GUI::Widget>();
    button_container.set_shrink_to_fit(true);
    button_container.set_layout<GUI::VerticalBoxLayout>();

    auto& close_button = button_container.add<GUI::Button>("OK"_string);
    close_button.on_click = [&score_dialog](auto) {
        score_dialog->done(GUI::Dialog::ExecResult::OK);
    };
    close_button.set_min_width(70);
    close_button.resize(70, 30);

    // FIXME: Why is this necessary?
    score_dialog->resize({ 20 + score_card.width() + 15 + close_button.width(), 20 + score_card.height() });

    StringBuilder title_builder;
    title_builder.append("Score Card"sv);
    if (game_over)
        title_builder.append(" - Game Over"sv);
    score_dialog->set_title(title_builder.to_byte_string());

    RefPtr<Core::Timer> close_timer;
    if (!m_players[0].is_human) {
        close_timer = Core::Timer::create_single_shot(2000, [&] {
            score_dialog->close();
        });
        close_timer->start();
    }

    score_dialog->exec();
}

void Game::setup(ByteString player_name, int hand_number)
{
    m_players[0].name = move(player_name);

    reset();

    m_hand_number = hand_number;

    if (m_hand_number == 0) {
        for (auto& player : m_players)
            player.scores.clear_with_capacity();
    }

    if (m_hand_number % 4 != 3) {
        m_state = State::PassingSelect;
        m_human_can_play = true;
        switch (passing_direction()) {
        case PassingDirection::Left:
            m_passing_button->set_text("Pass Left"_string);
            break;
        case PassingDirection::Across:
            m_passing_button->set_text("Pass Across"_string);
            break;
        case PassingDirection::Right:
            m_passing_button->set_text("Pass Right"_string);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    } else
        m_state = State::Play;

    if (m_hand_number % 4 != 3) {
        m_passing_button->set_visible(true);
        m_passing_button->set_focus(false);
    }

    Vector<NonnullRefPtr<Card>> deck = Cards::create_standard_deck(Cards::Shuffle::Yes).release_value_but_fixme_should_propagate_errors();

    for (auto& player : m_players) {
        player.hand.ensure_capacity(Card::card_count);
        for (uint8_t i = 0; i < Card::card_count; ++i) {
            auto card = deck.take_last();
            if constexpr (!HEARTS_DEBUG) {
                if (&player != &m_players[0])
                    card->set_upside_down(true);
            }
            player.hand.append(card);
        }
        player.sort_hand();
        Gfx::IntRect update_rect;
        reposition_hand(player);
        for (auto& card : player.hand)
            update_rect = update_rect.united(card->rect());
        update(update_rect);
    }

    continue_game_after_delay();
}

void Game::start_animation(Vector<NonnullRefPtr<Card>> cards, Gfx::IntPoint end, Function<void()> did_finish_callback, int initial_delay_ms, int steps)
{
    stop_animation();

    m_animation_end = end;
    m_animation_current_step = 0;
    m_animation_steps = steps;
    m_animation_cards.clear_with_capacity();
    for (auto& card : cards)
        m_animation_cards.empend(card, card->position());
    m_animation_did_finish = make<Function<void()>>(move(did_finish_callback));
    m_animation_delay_timer = Core::Timer::create_single_shot(initial_delay_ms, [&] {
        m_animation_playing = true;
        start_timer(10);
    });
    m_animation_delay_timer->start();
}

void Game::stop_animation()
{
    if (m_animation_playing) {
        for (auto& animation : m_animation_cards)
            animation.card->set_position(m_animation_end);
        m_animation_playing = false;
    }
    if (m_animation_delay_timer)
        m_animation_delay_timer->stop();
    stop_timer();
}

void Game::timer_event(Core::TimerEvent&)
{
    if (!m_animation_playing)
        return;
    for (auto& animation : m_animation_cards) {
        Gfx::IntRect update_rect = animation.card->rect();
        animation.card->set_position(animation.start + (m_animation_end - animation.start) * m_animation_current_step / m_animation_steps);
        update_rect = update_rect.united(animation.card->rect());
        update(update_rect);
    }
    if (m_animation_current_step >= m_animation_steps) {
        stop_timer();
        m_animation_playing = false;
        if (m_animation_did_finish) {
            // The did_finish handler might end up destroying/replacing the handler
            // so we have to save it first.
            OwnPtr<Function<void()>> animation_did_finish = move(m_animation_did_finish);
            (*animation_did_finish)();
        }
    }
    m_animation_current_step++;
}

bool Game::other_player_has_lower_value_card(Player& player, Card& card)
{
    for (auto& other_player : m_players) {
        if (&player != &other_player) {
            for (auto& other_card : other_player.hand) {
                if (other_card && card.suit() == other_card->suit() && hearts_card_value(*other_card) < hearts_card_value(card))
                    return true;
            }
        }
    }
    return false;
}

bool Game::other_player_has_higher_value_card(Player& player, Card& card)
{
    for (auto& other_player : m_players) {
        if (&player != &other_player) {
            for (auto& other_card : other_player.hand) {
                if (other_card && card.suit() == other_card->suit() && hearts_card_value(*other_card) > hearts_card_value(card))
                    return true;
            }
        }
    }
    return false;
}

bool Game::other_player_has_queen_of_spades(Player& player)
{
    for (auto& other_player : m_players) {
        if (&player != &other_player) {
            for (auto& other_card : other_player.hand) {
                if (other_card && other_card->suit() == Cards::Suit::Spades && hearts_card_value(*other_card) == CardValue::Queen)
                    return true;
            }
        }
    }
    return false;
}

#define RETURN_CARD_IF_VALID(card)     \
    do {                               \
        auto card_index = (card);      \
        if (card_index.has_value())    \
            return card_index.value(); \
    } while (0)

size_t Game::pick_card(Player& player)
{
    bool is_leading_player = m_trick.is_empty();
    bool is_first_trick = m_trick_number == 0;
    if (is_leading_player) {
        if (is_first_trick) {
            auto clubs_2 = player.pick_specific_card(Cards::Suit::Clubs, CardValue::Number_2);
            VERIFY(clubs_2.has_value());
            return clubs_2.value();
        } else {
            auto valid_card = [this, &player](Card& card) {
                return is_valid_play(player, card);
            };
            auto prefer_card = [this, &player](Card& card) {
                return !other_player_has_lower_value_card(player, card) && other_player_has_higher_value_card(player, card);
            };
            return player.pick_lead_card(move(valid_card), move(prefer_card));
        }
    }
    auto* high_card = m_trick[0].ptr();
    for (auto& card : m_trick)
        if (high_card->suit() == card->suit() && hearts_card_value(card) > hearts_card_value(*high_card))
            high_card = card;
    if (high_card->suit() == Cards::Suit::Spades && hearts_card_value(*high_card) > CardValue::Queen)
        RETURN_CARD_IF_VALID(player.pick_specific_card(Cards::Suit::Spades, CardValue::Queen));
    auto card_has_points = [](Card& card) { return hearts_card_points(card) > 0; };
    auto trick_has_points = m_trick.first_matching(card_has_points).has_value();
    bool is_trailing_player = m_trick.size() == 3;
    if (!trick_has_points && is_trailing_player) {
        RETURN_CARD_IF_VALID(player.pick_low_points_high_value_card(m_trick[0]->suit()));
        if (is_first_trick)
            return player.pick_low_points_high_value_card().value();
        else {
            auto ignore_card = [this, &player](Card& card) {
                return !other_player_has_higher_value_card(player, card);
            };
            return player.pick_max_points_card(move(ignore_card));
        }
    }
    RETURN_CARD_IF_VALID(player.pick_lower_value_card(*high_card));
    bool is_third_player = m_trick.size() == 2;
    bool play_highest_value_card = false;
    if (is_trailing_player)
        play_highest_value_card = true;
    if (is_third_player && !trick_has_points) {
        play_highest_value_card = true;

        if (high_card->suit() == Cards::Suit::Spades && other_player_has_queen_of_spades(player)) {
            Optional<size_t> chosen_card_index = player.pick_low_points_high_value_card(high_card->suit());
            if (chosen_card_index.has_value()) {
                auto& card = player.hand[chosen_card_index.value()];
                if (hearts_card_value(*card) > CardValue::Queen)
                    play_highest_value_card = false;
            }
        }
    }
    if (play_highest_value_card)
        RETURN_CARD_IF_VALID(player.pick_low_points_high_value_card(high_card->suit()));
    else
        RETURN_CARD_IF_VALID(player.pick_slightly_higher_value_card(*high_card));
    if (is_first_trick)
        return player.pick_low_points_high_value_card().value();
    auto ignore_card = [this, &player](Card& card) {
        return !other_player_has_higher_value_card(player, card);
    };
    return player.pick_max_points_card(move(ignore_card));
}

size_t Game::pick_first_card_ltr(Player& player)
{
    for (size_t i = 0; i < player.hand.size(); i++) {
        auto& card = player.hand[i];
        if (card.is_null())
            continue;
        if (is_valid_play(player, *card)) {
            return i;
        }
    }
    VERIFY_NOT_REACHED();
}

void Game::let_player_play_card()
{
    auto& player = current_player();

    if (&player == &m_players[0])
        on_status_change("Select a card to play."_string);
    else
        on_status_change(String::formatted("Waiting for {} to play a card...", player).release_value_but_fixme_should_propagate_errors());

    if (player.is_human) {
        m_human_can_play = true;
        if constexpr (HEARTS_DEBUG) {
            auto card_index = pick_card(player);
            auto& card = player.hand[card_index];
            card->set_inverted(true);
            update(card->rect());
        }
        return;
    }

    play_card(player, pick_card(player));
}

size_t Game::player_index(Player& player)
{
    return &player - m_players;
}

Player& Game::current_player()
{
    VERIFY(m_trick.size() < 4);
    auto player_index = m_leading_player - m_players;
    auto current_player_index = (player_index + m_trick.size()) % 4;
    dbgln_if(HEARTS_DEBUG, "Leading player: {}, current player: {}", *m_leading_player, m_players[current_player_index]);
    return m_players[current_player_index];
}

void Game::continue_game_after_delay(int interval_ms)
{
    m_delay_timer->start(interval_ms);
}

void Game::advance_game()
{
    if (m_animation_playing)
        return;

    if (m_inverted_card) {
        m_inverted_card->set_inverted(false);
        update(m_inverted_card->rect());
        m_inverted_card.clear();
    }

    if (m_state == State::Play && game_ended()) {
        m_state = State::GameEnded;
        on_status_change("Game ended."_string);
        advance_game();
        return;
    }

    if (m_state == State::GameEnded) {
        int highest_score = 0;
        for (auto& player : m_players) {
            int previous_score = player.scores.is_empty() ? 0 : player.scores[player.scores.size() - 1];
            auto score = previous_score + calculate_score((player));
            player.scores.append(score);
            if (score > highest_score)
                highest_score = score;
        }
        bool game_over = highest_score >= 100;
        show_score_card(game_over);
        auto next_hand_number = m_hand_number + 1;
        if (game_over)
            next_hand_number = 0;
        setup(move(m_players[0].name), next_hand_number);
        return;
    }

    if (m_state == State::PassingSelect) {
        if (!m_players[0].is_human) {
            select_cards_for_passing();
            m_state = State::PassingSelectConfirmed;
            continue_game_after_delay();
        }
        return;
    }

    if (m_state == State::PassingSelectConfirmed) {
        pass_cards();
        continue_game_after_delay();
        return;
    }

    if (m_state == State::PassingAccept) {
        if (!m_players[0].is_human) {
            m_state = State::Play;
            continue_game_after_delay();
        }
        return;
    }

    clear_highlighted_cards();
    m_passing_button->set_visible(false);

    if (m_trick_number == 0 && m_trick.is_empty()) {
        // Find whoever has 2 of Clubs, they get to play the first card
        for (auto& player : m_players) {
            auto clubs_2_card = player.hand.first_matching([](auto& card) {
                return card->suit() == Cards::Suit::Clubs && hearts_card_value(*card) == CardValue::Number_2;
            });
            if (clubs_2_card.has_value()) {
                m_leading_player = &player;
                let_player_play_card();
                return;
            }
        }
    }

    if (m_trick.size() < 4) {
        let_player_play_card();
        return;
    }

    auto leading_card_suit = m_trick[0]->suit();
    size_t taker_index = 0;
    auto taker_value = hearts_card_value(m_trick[0]);
    for (size_t i = 1; i < 4; i++) {
        if (m_trick[i]->suit() != leading_card_suit)
            continue;
        if (hearts_card_value(m_trick[i]) <= taker_value)
            continue;
        taker_index = i;
        taker_value = hearts_card_value(m_trick[i]);
    }
    auto leading_player_index = player_index(*m_leading_player);
    auto taking_player_index = (leading_player_index + taker_index) % 4;
    auto& taking_player = m_players[taking_player_index];
    dbgln_if(HEARTS_DEBUG, "{} takes the trick", taking_player);
    for (auto& card : m_trick) {
        if (hearts_card_points(card) == 0)
            continue;
        dbgln_if(HEARTS_DEBUG, "{} takes card {}", taking_player, card);
        taking_player.cards_taken.append(card);
    }

    start_animation(
        m_trick,
        taking_player.taken_cards_target,
        [&] {
            ++m_trick_number;

            if (game_ended())
                for (auto& player : m_players)
                    quick_sort(player.cards_taken, hearts_card_less);

            m_trick.clear_with_capacity();
            m_leading_player = &taking_player;
            dbgln_if(HEARTS_DEBUG, "-----");
            advance_game();
        },
        750);

    return;
}

void Game::keydown_event(GUI::KeyEvent& event)
{
    if (event.shift() && event.key() == KeyCode::Key_F10) {
        m_players[0].is_human = !m_players[0].is_human;
        advance_game();
    } else if (event.key() == KeyCode::Key_F10) {
        if (m_human_can_play && m_state == State::Play)
            play_card(m_players[0], pick_card(m_players[0]));
        else if (m_state == State::PassingSelect)
            select_cards_for_passing();
    } else if (event.key() == KeyCode::Key_Space) {
        if (m_human_can_play && m_state == State::Play)
            play_card(m_players[0], pick_first_card_ltr(m_players[0]));
    } else if (event.shift() && event.key() == KeyCode::Key_F11) {
        dump_state();
    } else {
        event.ignore();
    }
}

void Game::play_card(Player& player, size_t card_index)
{
    if (player.is_human)
        m_human_can_play = false;
    VERIFY(player.hand[card_index]);
    VERIFY(m_trick.size() < 4);
    RefPtr<Card> card;
    swap(player.hand[card_index], card);
    dbgln_if(HEARTS_DEBUG, "{} plays {}", player, *card);
    VERIFY(is_valid_play(player, *card));
    card->set_upside_down(false);
    m_trick.append(*card);

    Gfx::IntPoint const trick_card_positions[] = {
        { width / 2 - Card::width / 2, height / 2 - 30 },
        { width / 2 - Card::width + 15, height / 2 - Card::height / 2 - 15 },
        { width / 2 - Card::width / 2 + 15, height / 2 - Card::height + 15 },
        { width / 2, height / 2 - Card::height / 2 },
    };

    VERIFY(m_leading_player);
    size_t leading_player_index = player_index(*m_leading_player);

    Vector<NonnullRefPtr<Card>> cards;
    cards.append(*card);
    start_animation(
        cards,
        trick_card_positions[(leading_player_index + m_trick.size() - 1) % 4],
        [&] {
            advance_game();
        },
        0);
}

bool Game::is_valid_play(Player& player, Card& card, ByteString* explanation) const
{
    // First card must be 2 of Clubs.
    if (m_trick_number == 0 && m_trick.is_empty()) {
        if (explanation)
            *explanation = "The first card must be Two of Clubs.";
        return card.suit() == Cards::Suit::Clubs && hearts_card_value(card) == CardValue::Number_2;
    }

    // Can't play hearts or The Queen in the first trick.
    if (m_trick_number == 0 && hearts_card_points(card) > 0) {
        bool all_points_cards = true;
        for (auto& other_card : player.hand) {
            if (hearts_card_points(*other_card) == 0) {
                all_points_cards = false;
                break;
            }
        }
        // ... unless the player only has points cards (e.g. all Hearts or
        // 12 Hearts + Queen of Spades), in which case they're allowed to play Hearts.
        if (all_points_cards && card.suit() == Cards::Suit::Hearts)
            return true;
        if (explanation)
            *explanation = "You can't play a card worth points in the first trick.";
        return false;
    }

    // Leading card can't be hearts until hearts are broken
    // unless the player only has hearts cards.
    if (m_trick.is_empty()) {
        if (are_hearts_broken() || card.suit() != Cards::Suit::Hearts)
            return true;
        auto non_hearts_card = player.hand.first_matching([](auto const& other_card) {
            return !other_card.is_null() && other_card->suit() != Cards::Suit::Hearts;
        });
        auto only_has_hearts = !non_hearts_card.has_value();
        if (!only_has_hearts && explanation)
            *explanation = "Hearts haven't been broken.";
        return only_has_hearts;
    }

    // Player must follow suit unless they don't have any matching cards.
    auto leading_card_suit = m_trick[0]->suit();
    if (leading_card_suit == card.suit())
        return true;
    auto has_matching_card = player.has_card_of_suit(leading_card_suit);
    if (has_matching_card && explanation)
        *explanation = "You must follow suit.";
    return !has_matching_card;
}

bool Game::are_hearts_broken() const
{
    for (auto& player : m_players)
        for (auto& card : player.cards_taken)
            if (card->suit() == Cards::Suit::Hearts)
                return true;
    return false;
}

void Game::card_clicked_during_passing(size_t, Card& card)
{
    if (!is_card_highlighted(card)) {
        if (m_cards_highlighted.size() < 3)
            highlight_card(card);
    } else
        unhighlight_card(card);

    m_passing_button->set_enabled(m_cards_highlighted.size() == 3);
}

void Game::card_clicked_during_play(size_t card_index, Card& card)
{
    ByteString explanation;
    if (!is_valid_play(m_players[0], card, &explanation)) {
        if (m_inverted_card)
            m_inverted_card->set_inverted(false);
        card.set_inverted(true);
        update(card.rect());
        m_inverted_card = card;
        on_status_change(String::formatted("You can't play this card: {}", explanation).release_value_but_fixme_should_propagate_errors());
        continue_game_after_delay();
        return;
    }
    play_card(m_players[0], card_index);
}

void Game::card_clicked(size_t card_index, Card& card)
{
    if (m_state == State::PassingSelect)
        card_clicked_during_passing(card_index, card);
    else
        card_clicked_during_play(card_index, card);
}

void Game::mouseup_event(GUI::MouseEvent& event)
{
    GUI::Frame::mouseup_event(event);

    if (event.button() != GUI::MouseButton::Primary)
        return;

    if (!m_human_can_play)
        return;

    for (ssize_t i = m_players[0].hand.size() - 1; i >= 0; i--) {
        auto& card = m_players[0].hand[i];
        if (card.is_null())
            continue;
        if (card->rect().contains(event.position())) {
            card_clicked(i, *card);
            break;
        }
    }
}

int Game::calculate_score(Player& player)
{
    Optional<int> min_score;
    Optional<int> max_score;
    int player_score = 0;
    for (auto& other_player : m_players) {
        int score = 0;
        for (auto& card : other_player.cards_taken)
            if (card->suit() == Cards::Suit::Spades && card->rank() == Cards::Rank::Queen)
                score += 13;
            else if (card->suit() == Cards::Suit::Hearts)
                score++;
        if (!min_score.has_value() || score < min_score.value())
            min_score = score;
        if (!max_score.has_value() || score > max_score.value())
            max_score = score;
        if (&other_player == &player)
            player_score = score;
    }
    constexpr int sum_points_of_all_cards = 26;
    if (player_score == sum_points_of_all_cards)
        return 0;
    else if (max_score.value() == sum_points_of_all_cards)
        return 26;
    else
        return player_score;
}

static constexpr int card_highlight_offset = -20;

bool Game::is_card_highlighted(Card& card)
{
    return m_cards_highlighted.contains(card);
}

void Game::highlight_card(Card& card)
{
    VERIFY(!m_cards_highlighted.contains(card));
    m_cards_highlighted.set(card);
    Gfx::IntRect update_rect = card.rect();
    card.set_position(card.position().translated(0, card_highlight_offset));
    update_rect = update_rect.united(card.rect());
    update(update_rect);
}

void Game::unhighlight_card(Card& card)
{
    VERIFY(m_cards_highlighted.contains(card));
    m_cards_highlighted.remove(card);
    Gfx::IntRect update_rect = card.rect();
    card.set_position(card.position().translated(0, -card_highlight_offset));
    update_rect = update_rect.united(card.rect());
    update(update_rect);
}

void Game::clear_highlighted_cards()
{
    for (auto& card : m_cards_highlighted)
        card->set_position(card->position().translated(0, -card_highlight_offset));
    m_cards_highlighted.clear();
}

void Game::reposition_hand(Player& player)
{
    auto card_position = player.first_card_position;
    for (auto& card : player.hand) {
        card->set_position(is_card_highlighted(*card) ? card_position.translated(0, card_highlight_offset) : card_position);
        card_position.translate_by(player.card_offset);
    }
}

void Game::select_cards_for_passing()
{
    clear_highlighted_cards();
    auto selected_cards = m_players[0].pick_cards_to_pass(passing_direction());
    highlight_card(selected_cards[0]);
    highlight_card(selected_cards[1]);
    highlight_card(selected_cards[2]);
    m_passing_button->set_enabled(true);
}

void Game::pass_cards()
{
    Vector<NonnullRefPtr<Card>> first_player_cards;
    for (auto& card : m_cards_highlighted)
        first_player_cards.append(*card);
    clear_highlighted_cards();
    VERIFY(first_player_cards.size() == 3);

    Vector<NonnullRefPtr<Card>> passed_cards[4];
    passed_cards[0] = first_player_cards;
    passed_cards[1] = m_players[1].pick_cards_to_pass(passing_direction());
    passed_cards[2] = m_players[2].pick_cards_to_pass(passing_direction());
    passed_cards[3] = m_players[3].pick_cards_to_pass(passing_direction());

    for (size_t i = 0; i < 4; i++) {
        m_players[i].remove_cards(passed_cards[i]);

        int destination_player_index = i;
        switch (passing_direction()) {
        case PassingDirection::Left:
            destination_player_index += 1;
            break;
        case PassingDirection::Across:
            destination_player_index += 2;
            break;
        case PassingDirection::Right:
            destination_player_index += 3;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        destination_player_index %= 4;

        for (auto& card : passed_cards[i]) {
            m_players[destination_player_index].hand.append(card);
            if constexpr (!HEARTS_DEBUG)
                card->set_upside_down(destination_player_index != 0);
            if (destination_player_index == 0)
                highlight_card(card);
        }
    }

    for (auto& player : m_players) {
        VERIFY(player.hand.size() == 13);
        player.sort_hand();
        reposition_hand(player);
        Gfx::IntRect update_rect;
        for (auto& card : player.hand)
            update_rect = update_rect.united(card->rect());
        update(update_rect);
    }

    m_state = State::PassingAccept;
    m_passing_button->set_text("OK"_string);
    m_passing_button->set_enabled(true);
}

PassingDirection Game::passing_direction() const
{
    VERIFY(m_hand_number % 4 != 3);
    return static_cast<PassingDirection>(m_hand_number % 4);
}

void Game::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    Gfx::Color background_color = this->background_color();
    painter.clear_rect(frame_inner_rect(), background_color);

    for (auto& player : m_players) {
        auto& font = painter.font().bold_variant();
        Gfx::Color text_color = background_color.luminosity() > 80 ? Color::Black : Color::White;
        painter.draw_text(player.name_position, player.name, font, player.name_alignment, text_color, Gfx::TextElision::None);

        if (!game_ended()) {
            for (auto& card : player.hand)
                if (!card.is_null())
                    card->paint(painter);
        } else {
            // FIXME: reposition cards in advance_game() maybe
            auto card_position = player.first_card_position;
            for (auto& card : player.cards_taken) {
                card->set_upside_down(false);
                card->set_position(card_position);
                card->paint(painter);
                card_position.translate_by(player.card_offset);
            }
        }
    }

    for (size_t i = 0; i < m_trick.size(); i++)
        m_trick[i]->paint(painter);
}

void Game::dump_state() const
{
    if constexpr (HEARTS_DEBUG) {
        dbgln("------------------------------");
        for (uint8_t i = 0; i < 4; ++i) {
            auto& player = m_players[i];
            dbgln("Player {}", player.name);
            dbgln("Hand:");
            for (auto const& card : player.hand)
                if (card.is_null())
                    dbgln("  <empty>");
                else
                    dbgln("  {}", *card);
            dbgln("Taken:");
            for (auto const& card : player.cards_taken)
                dbgln("  {}", card);
        }
    }
}

}
