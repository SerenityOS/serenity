/*
 * Copyright (c) 2021, Jamie Mansfield <jmansfield@cadixdev.org>
 * Copyright (c) 2022, Jonas Höpner <me@jonashoepner.de>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Random.h>
#include <LibGUI/Event.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(Spider, Game);

namespace Spider {

static constexpr uint8_t new_game_animation_delay = 2;
static constexpr uint8_t draw_animation_delay = 2;
static constexpr int s_timer_interval_ms = 1000 / 60;

ErrorOr<NonnullRefPtr<Game>> Game::try_create()
{
    auto game = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Game()));

    TRY(game->add_stack(Gfx::IntPoint { 10, Game::height - Card::height - 10 }, CardStack::Type::Waste));
    TRY(game->add_stack(Gfx::IntPoint { Game::width - Card::width - 10, Game::height - Card::height - 10 }, CardStack::Type::Stock));

    for (int i = 0; i < 10; i++) {
        TRY(game->add_stack(Gfx::IntPoint { 10 + i * (Card::width + 10), 10 }, CardStack::Type::Normal));
    }

    return game;
}

Game::Game() = default;

void Game::setup(Mode mode)
{
    if (m_state == State::NewGameAnimation)
        stop_timer();

    m_mode = mode;

    if (on_undo_availability_change)
        on_undo_availability_change(false);

    if (on_game_end)
        on_game_end(GameOverReason::NewGame, m_score);

    for (auto& stack : stacks())
        stack->clear();

    m_new_game_animation_pile = 0;

    m_score = 500;
    update_score(0);

    unsigned heart_suits = 0;
    unsigned spade_suits = 0;

    switch (m_mode) {
    case Mode::SingleSuit:
        spade_suits = 8;
        heart_suits = 0;
        break;
    case Mode::TwoSuit:
        spade_suits = 4;
        heart_suits = 4;
        break;
    default:
        VERIFY_NOT_REACHED();
        break;
    }

    m_new_deck = Cards::create_deck(0, 0, heart_suits, spade_suits, Cards::Shuffle::Yes).release_value_but_fixme_should_propagate_errors();

    clear_moving_cards();

    m_state = State::NewGameAnimation;
    start_timer(s_timer_interval_ms);
    update();
}

void Game::perform_undo()
{
    if (m_last_move.type == LastMove::Type::Invalid)
        return;

    if (!m_last_move.was_visible)
        m_last_move.from->peek().set_upside_down(true);

    Vector<NonnullRefPtr<Card>> cards;
    for (size_t i = 0; i < m_last_move.card_count; i++)
        cards.append(m_last_move.to->pop());
    for (ssize_t i = m_last_move.card_count - 1; i >= 0; i--)
        m_last_move.from->push(cards[i]).release_value_but_fixme_should_propagate_errors();

    update_score(-1);

    m_last_move = {};
    if (on_undo_availability_change)
        on_undo_availability_change(false);

    update_disabled_cards();
    invalidate_layout();
}

void Game::start_timer_if_necessary()
{
    if (on_game_start && m_state == State::WaitingForNewGame) {
        on_game_start();
        m_state = State::GameInProgress;
    }
}

void Game::update_score(int delta)
{
    m_score = max(static_cast<int>(m_score) + delta, 0);

    if (on_score_update)
        on_score_update(m_score);
}

void Game::draw_cards()
{
    // draw a single card from the stock for each pile
    auto& stock_pile = stack_at_location(Stock);
    if (stock_pile.is_empty())
        return;

    update_score(-1);

    m_state = State::DrawAnimation;
    m_original_stock_rect = stock_pile.bounding_box();
    start_timer(s_timer_interval_ms);
}

void Game::detect_full_stacks()
{
    auto& completed_stack = stack_at_location(Completed);
    for (auto pile : piles) {
        auto& current_pile = stack_at_location(pile);

        bool started = false;
        uint8_t last_value;
        Color color;
        for (size_t i = current_pile.stack().size(); i > 0; i--) {
            auto card = current_pile.stack().at(i - 1);
            if (card->is_upside_down())
                break;

            if (!started) {
                if (card->rank() != Cards::Rank::Ace)
                    break;

                started = true;
                color = card->color();
            } else if (to_underlying(card->rank()) != last_value + 1 || card->color() != color) {
                break;
            } else if (card->rank() == Cards::Rank::King) {
                // we have a full set
                auto original_current_rect = current_pile.bounding_box();

                for (size_t j = 0; j < Card::card_count; j++) {
                    completed_stack.push(current_pile.pop()).release_value_but_fixme_should_propagate_errors();
                }

                update(original_current_rect);
                update(completed_stack.bounding_box());

                if (current_pile.make_top_card_visible())
                    update(current_pile.peek().rect());

                update_score(101);

                if (on_undo_availability_change)
                    on_undo_availability_change(false);
            }

            last_value = to_underlying(card->rank());
        }
    }

    update_disabled_cards();
    detect_victory();
}

void Game::detect_victory()
{
    for (auto pile : piles) {
        auto& current_pile = stack_at_location(pile);

        if (!current_pile.is_empty())
            return;
    }

    if (on_undo_availability_change)
        on_undo_availability_change(false);

    if (on_game_end)
        on_game_end(GameOverReason::Victory, m_score);
}

void Game::paint_event(GUI::PaintEvent& event)
{
    Gfx::Color background_color = this->background_color();

    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    if (is_moving_cards()) {
        for (auto& card : moving_cards())
            card->clear(painter, background_color);
    }

    for (auto& stack : stacks()) {
        stack->paint(painter, background_color);
    }

    if (is_moving_cards()) {
        for (auto& card : moving_cards()) {
            card->paint(painter);
            card->save_old_position();
        }
    }

    if (!m_mouse_down) {
        if (is_moving_cards()) {
            for (auto& card : moving_cards())
                card->set_moving(false);
        }
        clear_moving_cards();
    }
}

void Game::remember_move_for_undo(CardStack& from, CardStack& to, size_t card_count, bool was_visible)
{
    m_last_move.type = LastMove::Type::MoveCards;
    m_last_move.from = &from;
    m_last_move.card_count = card_count;
    m_last_move.to = &to;
    m_last_move.was_visible = was_visible;
    if (on_undo_availability_change)
        on_undo_availability_change(true);
}

void Game::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousedown_event(event);

    if (m_state == State::NewGameAnimation || m_state == State::DrawAnimation)
        return;

    auto click_location = event.position();
    for (auto& to_check : stacks()) {
        if (to_check->type() == CardStack::Type::Waste)
            continue;

        if (to_check->bounding_box().contains(click_location)) {
            if (to_check->type() == CardStack::Type::Stock) {
                start_timer_if_necessary();
                draw_cards();
            } else if (!to_check->is_empty()) {
                auto& top_card = to_check->peek();

                if (top_card.is_upside_down()) {
                    if (top_card.rect().contains(click_location)) {
                        top_card.set_upside_down(false);
                        start_timer_if_necessary();
                        update(top_card.rect());
                    }
                } else if (!is_moving_cards()) {
                    pick_up_cards_from_stack(to_check, click_location, Cards::CardStack::MovementRule::Same).release_value_but_fixme_should_propagate_errors();
                    m_mouse_down_location = click_location;
                    // When the user wants to automatically move cards, do not go into the drag mode.
                    if (event.button() != GUI::MouseButton::Secondary)
                        m_mouse_down = true;
                    start_timer_if_necessary();
                }
            }
            break;
        }
    }
}

void Game::move_focused_cards(CardStack& stack)
{
    auto card_count = moving_cards().size();
    drop_cards_on_stack(stack, Cards::CardStack::MovementRule::Any).release_value_but_fixme_should_propagate_errors();
    bool was_visible = moving_cards_source_stack()->is_empty() || !moving_cards_source_stack()->peek().is_upside_down();
    remember_move_for_undo(*moving_cards_source_stack(), stack, card_count, was_visible);
    update_score(-1);
    moving_cards_source_stack()->make_top_card_visible();
    detect_full_stacks();
}

void Game::mouseup_event(GUI::MouseEvent& event)
{
    GUI::Frame::mouseup_event(event);
    clear_hovered_stack();

    if (!is_moving_cards() || m_state == State::NewGameAnimation || m_state == State::DrawAnimation)
        return;

    bool rebound = true;
    if (event.button() == GUI::MouseButton::Secondary) {
        // This enables the game to move the focused cards to the first possible stack excluding empty stacks.
        // NOTE: This ignores empty stacks, as the game has no undo button, and a card, which has been moved to an empty stack without any other possibilities is not reversible.
        for (auto& stack : stacks()) {
            if (stack == moving_cards_source_stack())
                continue;

            if (stack->is_allowed_to_push(moving_cards().at(0), moving_cards().size(), Cards::CardStack::MovementRule::Any) && !stack->is_empty()) {
                move_focused_cards(stack);

                rebound = false;
                break;
            }
        }
    } else if (auto target_stack = find_stack_to_drop_on(Cards::CardStack::MovementRule::Any); !target_stack.is_null()) {
        auto& stack = *target_stack;
        move_focused_cards(stack);
        rebound = false;
    }

    if (rebound) {
        for (auto& to_intersect : moving_cards())
            mark_intersecting_stacks_dirty(to_intersect);

        moving_cards_source_stack()->rebound_cards();
        update(moving_cards_source_stack()->bounding_box());
    }

    update_disabled_cards();
    m_mouse_down = false;
}

void Game::mousemove_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousemove_event(event);

    if (!m_mouse_down || m_state == State::NewGameAnimation || m_state == State::DrawAnimation)
        return;

    auto click_location = event.position();
    int dx = click_location.dx_relative_to(m_mouse_down_location);
    int dy = click_location.dy_relative_to(m_mouse_down_location);

    if (auto target_stack = find_stack_to_drop_on(Cards::CardStack::MovementRule::Any)) {
        if (target_stack != m_hovered_stack) {
            clear_hovered_stack();

            m_hovered_stack = move(target_stack);
            m_hovered_stack->set_highlighted(true);
            update(m_hovered_stack->bounding_box());
        }
    } else {
        clear_hovered_stack();
    }

    for (auto& to_intersect : moving_cards()) {
        mark_intersecting_stacks_dirty(to_intersect);
        to_intersect->rect().translate_by(dx, dy);
        update(to_intersect->rect());
    }

    m_mouse_down_location = click_location;
}

void Game::doubleclick_event(GUI::MouseEvent& event)
{
    GUI::Frame::doubleclick_event(event);

    if (m_state == State::NewGameAnimation) {
        while (m_state == State::NewGameAnimation)
            deal_next_card();
        return;
    }
}

void Game::deal_next_card()
{
    auto& current_pile = stack_at_location(piles.at(m_new_game_animation_pile));

    // for first 4 piles, draw 6 cards
    // for last 6 piles, draw 5 cards
    size_t cards_to_draw = m_new_game_animation_pile < 4 ? 6 : 5;

    if (current_pile.count() < (cards_to_draw - 1)) {
        auto card = m_new_deck.take_last();
        card->set_upside_down(true);
        current_pile.push(card).release_value_but_fixme_should_propagate_errors();
    } else {
        current_pile.push(m_new_deck.take_last()).release_value_but_fixme_should_propagate_errors();
        ++m_new_game_animation_pile;
    }

    update(current_pile.bounding_box());

    if (m_new_game_animation_pile == piles.size()) {
        VERIFY(m_new_deck.size() == 50);

        auto& stock_pile = stack_at_location(Stock);
        while (!m_new_deck.is_empty())
            stock_pile.push(m_new_deck.take_last()).release_value_but_fixme_should_propagate_errors();

        update(stock_pile.bounding_box());
        update_disabled_cards();

        m_state = State::WaitingForNewGame;
        stop_timer();
    }
}

void Game::update_disabled_cards()
{
    for (auto& stack : stacks()) {
        if (stack->type() != CardStack::Type::Normal)
            continue;
        stack->update_disabled_cards(CardStack::MovementRule::Same);
        update(stack->bounding_box());
    }
}

void Game::timer_event(Core::TimerEvent&)
{
    if (m_state == State::NewGameAnimation) {
        if (m_new_game_animation_delay < new_game_animation_delay) {
            ++m_new_game_animation_delay;
        } else {
            m_new_game_animation_delay = 0;
            deal_next_card();
        }
    } else if (m_state == State::DrawAnimation) {
        if (m_draw_animation_delay < draw_animation_delay) {
            ++m_draw_animation_delay;
        } else {
            auto& stock_pile = stack_at_location(Stock);
            auto& current_pile = stack_at_location(piles.at(m_draw_animation_pile));
            auto card = stock_pile.pop();
            card->set_upside_down(false);
            current_pile.push(card).release_value_but_fixme_should_propagate_errors();
            update(current_pile.bounding_box());
            ++m_draw_animation_pile;

            if (m_draw_animation_pile == piles.size()) {
                update_disabled_cards();
                update(m_original_stock_rect);
                detect_full_stacks();

                m_state = State::GameInProgress;
                m_draw_animation_delay = 0;
                m_draw_animation_pile = 0;
                stop_timer();
            }
        }
    }
}

void Game::clear_hovered_stack()
{
    if (!m_hovered_stack)
        return;

    m_hovered_stack->set_highlighted(false);
    update(m_hovered_stack->bounding_box());
    m_hovered_stack = nullptr;
}

}
