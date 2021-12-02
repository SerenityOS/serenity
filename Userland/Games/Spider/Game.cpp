/*
 * Copyright (c) 2021, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Random.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(Spider, Game);

namespace Spider {

static constexpr uint8_t new_game_animation_delay = 2;
static constexpr int s_timer_interval_ms = 1000 / 60;

Game::Game()
{
    m_stacks.append(adopt_ref(*new CardStack({ 10, Game::height - Card::height - 10 }, CardStack::Type::Waste)));
    m_stacks.append(adopt_ref(*new CardStack({ Game::width - Card::width - 10, Game::height - Card::height - 10 }, CardStack::Type::Stock)));

    for (int i = 0; i < 10; i++) {
        m_stacks.append(adopt_ref(*new CardStack({ 10 + i * (Card::width + 10), 10 }, CardStack::Type::Normal)));
    }
}

Game::~Game()
{
}

void Game::setup(Mode mode)
{
    if (m_new_game_animation)
        stop_timer();

    m_mode = mode;

    if (on_game_end)
        on_game_end(GameOverReason::NewGame, m_score);

    for (auto& stack : m_stacks)
        stack.clear();

    m_new_game_animation_pile = 0;

    m_score = 500;
    update_score(0);

    NonnullRefPtrVector<Card> deck;
    deck.ensure_capacity(Card::card_count * 2);

    for (int i = 0; i < Card::card_count; ++i) {
        switch (m_mode) {
        case Mode::SingleSuit:
            for (int j = 0; j < 8; j++) {
                deck.append(Card::construct(Card::Type::Spades, i));
            }
            break;
        case Mode::TwoSuit:
            for (int j = 0; j < 4; j++) {
                deck.append(Card::construct(Card::Type::Spades, i));
                deck.append(Card::construct(Card::Type::Hearts, i));
            }
            break;
        default:
            VERIFY_NOT_REACHED();
            break;
        }
    }

    m_new_deck.clear_with_capacity();
    m_new_deck.ensure_capacity(deck.size());
    while (!deck.is_empty())
        m_new_deck.append(deck.take(get_random_uniform(deck.size())));

    m_focused_stack = nullptr;
    m_focused_cards.clear();

    m_new_game_animation = true;
    start_timer(s_timer_interval_ms);
    update();
}

void Game::start_timer_if_necessary()
{
    if (on_game_start && m_waiting_for_new_game) {
        on_game_start();
        m_waiting_for_new_game = false;
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
    auto& stock_pile = stack(Stock);
    if (stock_pile.is_empty())
        return;

    update_score(-1);

    auto original_stock_rect = stock_pile.bounding_box();
    for (auto pile : piles) {
        auto& current_pile = stack(pile);

        auto card = stock_pile.pop();
        card->set_upside_down(false);
        current_pile.push(card);

        update(current_pile.bounding_box());
    }
    update(original_stock_rect);

    detect_full_stacks();
}

void Game::mark_intersecting_stacks_dirty(Card& intersecting_card)
{
    for (auto& stack : m_stacks) {
        if (intersecting_card.rect().intersects(stack.bounding_box()))
            update(stack.bounding_box());
    }

    update(intersecting_card.rect());
}

void Game::ensure_top_card_is_visible(NonnullRefPtr<CardStack> stack)
{
    if (stack->is_empty())
        return;

    auto& top_card = stack->peek();
    if (top_card.is_upside_down()) {
        top_card.set_upside_down(false);
        update(top_card.rect());
    }
}

void Game::detect_full_stacks()
{
    auto& completed_stack = stack(Completed);
    for (auto pile : piles) {
        auto& current_pile = stack(pile);

        bool started = false;
        uint8_t last_value;
        Color color;
        for (size_t i = current_pile.stack().size(); i > 0; i--) {
            auto& card = current_pile.stack().at(i - 1);
            if (card.is_upside_down())
                break;

            if (!started) {
                if (card.value() != 0) {
                    break;
                }

                started = true;
                color = card.color();
            } else if (card.value() != last_value + 1 || card.color() != color) {
                break;
            } else if (card.value() == Card::card_count - 1) {
                // we have a full set
                auto original_current_rect = current_pile.bounding_box();

                for (size_t j = 0; j < Card::card_count; j++) {
                    completed_stack.push(current_pile.pop());
                }

                update(original_current_rect);
                update(completed_stack.bounding_box());

                ensure_top_card_is_visible(current_pile);

                update_score(101);
            }

            last_value = card.value();
        }
    }

    detect_victory();
}

void Game::detect_victory()
{
    for (auto pile : piles) {
        auto& current_pile = stack(pile);

        if (!current_pile.is_empty())
            return;
    }

    if (on_game_end)
        on_game_end(GameOverReason::Victory, m_score);
}

void Game::paint_event(GUI::PaintEvent& event)
{
    static Gfx::Color s_background_color = palette().color(background_role());

    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    if (!m_focused_cards.is_empty()) {
        for (auto& focused_card : m_focused_cards)
            focused_card.clear(painter, s_background_color);
    }

    for (auto& stack : m_stacks) {
        stack.draw(painter, s_background_color);
    }

    if (!m_focused_cards.is_empty()) {
        for (auto& focused_card : m_focused_cards) {
            focused_card.draw(painter);
            focused_card.save_old_position();
        }
    }

    if (!m_mouse_down) {
        if (!m_focused_cards.is_empty()) {
            for (auto& card : m_focused_cards)
                card.set_moving(false);
            m_focused_cards.clear();
        }

        if (m_focused_stack) {
            m_focused_stack->set_focused(false);
            m_focused_stack = nullptr;
        }
    }
}

void Game::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousedown_event(event);

    if (m_new_game_animation)
        return;

    auto click_location = event.position();
    for (auto& to_check : m_stacks) {
        if (to_check.type() == CardStack::Type::Waste)
            continue;

        if (to_check.bounding_box().contains(click_location)) {
            if (to_check.type() == CardStack::Type::Stock) {
                start_timer_if_necessary();
                draw_cards();
            } else if (!to_check.is_empty()) {
                auto& top_card = to_check.peek();

                if (top_card.is_upside_down()) {
                    if (top_card.rect().contains(click_location)) {
                        top_card.set_upside_down(false);
                        start_timer_if_necessary();
                        update(top_card.rect());
                    }
                } else if (m_focused_cards.is_empty()) {
                    to_check.add_all_grabbed_cards(click_location, m_focused_cards, Cards::CardStack::Same);
                    m_mouse_down_location = click_location;
                    if (m_focused_stack)
                        m_focused_stack->set_focused(false);
                    to_check.set_focused(true);
                    m_focused_stack = &to_check;
                    m_mouse_down = true;
                    start_timer_if_necessary();
                }
            }
            break;
        }
    }
}

void Game::mouseup_event(GUI::MouseEvent& event)
{
    GUI::Frame::mouseup_event(event);

    if (!m_focused_stack || m_focused_cards.is_empty() || m_new_game_animation)
        return;

    bool rebound = true;
    for (auto& stack : m_stacks) {
        if (stack.is_focused())
            continue;

        for (auto& focused_card : m_focused_cards) {
            if (stack.bounding_box().intersects(focused_card.rect())) {
                if (stack.is_allowed_to_push(m_focused_cards.at(0), m_focused_cards.size(), Cards::CardStack::Any)) {
                    for (auto& to_intersect : m_focused_cards) {
                        mark_intersecting_stacks_dirty(to_intersect);
                        stack.push(to_intersect);
                        (void)m_focused_stack->pop();
                    }

                    update_score(-1);

                    update(m_focused_stack->bounding_box());
                    update(stack.bounding_box());

                    detect_full_stacks();

                    ensure_top_card_is_visible(*m_focused_stack);

                    rebound = false;
                    break;
                }
            }
        }
    }

    if (rebound) {
        for (auto& to_intersect : m_focused_cards)
            mark_intersecting_stacks_dirty(to_intersect);

        m_focused_stack->rebound_cards();
        update(m_focused_stack->bounding_box());
    }

    m_mouse_down = false;
}

void Game::mousemove_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousemove_event(event);

    if (!m_mouse_down || m_new_game_animation)
        return;

    auto click_location = event.position();
    int dx = click_location.dx_relative_to(m_mouse_down_location);
    int dy = click_location.dy_relative_to(m_mouse_down_location);

    for (auto& to_intersect : m_focused_cards) {
        mark_intersecting_stacks_dirty(to_intersect);
        to_intersect.rect().translate_by(dx, dy);
        update(to_intersect.rect());
    }

    m_mouse_down_location = click_location;
}

void Game::timer_event(Core::TimerEvent&)
{
    if (m_new_game_animation) {
        if (m_new_game_animation_delay < new_game_animation_delay) {
            ++m_new_game_animation_delay;
        } else {
            m_new_game_animation_delay = 0;
            auto& current_pile = stack(piles.at(m_new_game_animation_pile));

            // for first 4 piles, draw 6 cards
            // for last 6 piles, draw 5 cards
            size_t cards_to_draw = m_new_game_animation_pile < 4 ? 6 : 5;

            if (current_pile.count() < (cards_to_draw - 1)) {
                auto card = m_new_deck.take_last();
                card->set_upside_down(true);
                current_pile.push(card);
            } else {
                current_pile.push(m_new_deck.take_last());
                ++m_new_game_animation_pile;
            }

            update(current_pile.bounding_box());

            if (m_new_game_animation_pile == piles.size()) {
                VERIFY(m_new_deck.size() == 50);

                auto& stock_pile = stack(Stock);
                while (!m_new_deck.is_empty())
                    stock_pile.push(m_new_deck.take_last());

                update(stock_pile.bounding_box());

                m_new_game_animation = false;
                m_waiting_for_new_game = true;
                stop_timer();
            }
        }
    }
}

}
