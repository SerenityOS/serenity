/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <time.h>

REGISTER_WIDGET(Solitaire, Game);

namespace Solitaire {

static constexpr uint8_t new_game_animation_delay = 2;
static constexpr int s_timer_interval_ms = 1000 / 60;

Game::Game()
{
    srand(time(nullptr));

    m_stacks[Stock] = CardStack({ 10, 10 }, CardStack::Type::Stock);
    m_stacks[Waste] = CardStack({ 10 + Card::width + 10, 10 }, CardStack::Type::Waste);
    m_stacks[Foundation4] = CardStack({ Game::width - Card::width - 10, 10 }, CardStack::Type::Foundation);
    m_stacks[Foundation3] = CardStack({ Game::width - 2 * Card::width - 20, 10 }, CardStack::Type::Foundation);
    m_stacks[Foundation2] = CardStack({ Game::width - 3 * Card::width - 30, 10 }, CardStack::Type::Foundation);
    m_stacks[Foundation1] = CardStack({ Game::width - 4 * Card::width - 40, 10 }, CardStack::Type::Foundation);
    m_stacks[Pile1] = CardStack({ 10, 10 + Card::height + 10 }, CardStack::Type::Normal);
    m_stacks[Pile2] = CardStack({ 10 + Card::width + 10, 10 + Card::height + 10 }, CardStack::Type::Normal);
    m_stacks[Pile3] = CardStack({ 10 + 2 * Card::width + 20, 10 + Card::height + 10 }, CardStack::Type::Normal);
    m_stacks[Pile4] = CardStack({ 10 + 3 * Card::width + 30, 10 + Card::height + 10 }, CardStack::Type::Normal);
    m_stacks[Pile5] = CardStack({ 10 + 4 * Card::width + 40, 10 + Card::height + 10 }, CardStack::Type::Normal);
    m_stacks[Pile6] = CardStack({ 10 + 5 * Card::width + 50, 10 + Card::height + 10 }, CardStack::Type::Normal);
    m_stacks[Pile7] = CardStack({ 10 + 6 * Card::width + 60, 10 + Card::height + 10 }, CardStack::Type::Normal);
}

Game::~Game()
{
}

static float rand_float()
{
    return rand() / static_cast<float>(RAND_MAX);
}

void Game::timer_event(Core::TimerEvent&)
{
    if (m_game_over_animation) {
        VERIFY(!m_animation.card().is_null());
        if (m_animation.card()->position().x() >= Game::width || m_animation.card()->rect().right() <= 0)
            create_new_animation_card();

        if (m_animation.tick())
            update(m_animation.card()->rect());
    } else if (m_new_game_animation) {
        update();
    }
}

void Game::create_new_animation_card()
{
    auto card = Card::construct(static_cast<Card::Type>(rand() % Card::Type::__Count), rand() % Card::card_count);
    card->set_position({ rand() % (Game::width - Card::width), rand() % (Game::height / 8) });

    int x_sgn = card->position().x() > (Game::width / 2) ? -1 : 1;
    m_animation = Animation(card, rand_float() + .4f, x_sgn * ((rand() % 3) + 2), .6f + rand_float() * .4f);
}

void Game::start_game_over_animation()
{
    if (m_game_over_animation)
        return;

    create_new_animation_card();
    m_game_over_animation = true;

    start_timer(s_timer_interval_ms);

    if (on_game_end)
        on_game_end();
}

void Game::stop_game_over_animation()
{
    if (!m_game_over_animation)
        return;

    m_game_over_animation = false;
    update();

    stop_timer();
}

void Game::setup()
{
    stop_game_over_animation();

    if (on_game_end)
        on_game_end();

    for (auto& stack : m_stacks)
        stack.clear();

    m_new_deck.clear();
    m_new_game_animation_pile = 0;
    m_score = 0;
    update_score(0);

    for (int i = 0; i < Card::card_count; ++i) {
        m_new_deck.append(Card::construct(Card::Type::Clubs, i));
        m_new_deck.append(Card::construct(Card::Type::Spades, i));
        m_new_deck.append(Card::construct(Card::Type::Hearts, i));
        m_new_deck.append(Card::construct(Card::Type::Diamonds, i));
    }

    for (uint8_t i = 0; i < 200; ++i)
        m_new_deck.append(m_new_deck.take(rand() % m_new_deck.size()));

    m_new_game_animation = true;
    start_timer(s_timer_interval_ms);
    update();
}

void Game::update_score(int to_add)
{
    m_score = max(static_cast<int>(m_score) + to_add, 0);

    if (on_score_update)
        on_score_update(m_score);
}

void Game::keydown_event(GUI::KeyEvent& event)
{
    if (m_new_game_animation || m_game_over_animation)
        return;

    if (event.key() == KeyCode::Key_F12)
        start_game_over_animation();
}

void Game::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousedown_event(event);

    if (m_new_game_animation || m_game_over_animation)
        return;

    auto click_location = event.position();
    for (auto& to_check : m_stacks) {
        if (to_check.bounding_box().contains(click_location)) {
            if (to_check.type() == CardStack::Type::Stock) {
                auto& waste = stack(Waste);
                auto& stock = stack(Stock);

                if (stock.is_empty()) {
                    if (waste.is_empty())
                        return;

                    update(waste.bounding_box());

                    while (!waste.is_empty()) {
                        auto card = waste.pop();
                        stock.push(card);
                    }

                    update_score(-100);
                    update(stock.bounding_box());
                } else {
                    move_card(stock, waste);
                }

            } else if (!to_check.is_empty()) {
                auto& top_card = to_check.peek();

                if (top_card.is_upside_down()) {
                    if (top_card.rect().contains(click_location)) {
                        top_card.set_upside_down(false);
                        update_score(5);
                        update(top_card.rect());
                    }
                } else if (m_focused_cards.is_empty()) {
                    to_check.add_all_grabbed_cards(click_location, m_focused_cards);
                    m_mouse_down_location = click_location;
                    to_check.set_focused(true);
                    m_focused_stack = &to_check;
                    m_mouse_down = true;
                }
            }
            break;
        }
    }
}

void Game::mouseup_event(GUI::MouseEvent& event)
{
    GUI::Frame::mouseup_event(event);

    if (!m_focused_stack || m_focused_cards.is_empty() || m_game_over_animation || m_new_game_animation)
        return;

    bool rebound = true;
    for (auto& stack : m_stacks) {
        if (stack.is_focused())
            continue;

        for (auto& focused_card : m_focused_cards) {
            if (stack.bounding_box().intersects(focused_card.rect())) {
                if (stack.is_allowed_to_push(m_focused_cards.at(0))) {
                    for (auto& to_intersect : m_focused_cards) {
                        mark_intersecting_stacks_dirty(to_intersect);
                        stack.push(to_intersect);
                        m_focused_stack->pop();
                    }

                    update(m_focused_stack->bounding_box());
                    update(stack.bounding_box());

                    if (m_focused_stack->type() == CardStack::Type::Waste && stack.type() == CardStack::Type::Normal) {
                        update_score(5);
                    } else if (m_focused_stack->type() == CardStack::Type::Waste && stack.type() == CardStack::Type::Foundation) {
                        update_score(10);
                    } else if (m_focused_stack->type() == CardStack::Type::Normal && stack.type() == CardStack::Type::Foundation) {
                        update_score(10);
                    } else if (m_focused_stack->type() == CardStack::Type::Foundation && stack.type() == CardStack::Type::Normal) {
                        update_score(-15);
                    }

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

    if (!m_mouse_down || m_game_over_animation || m_new_game_animation)
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

void Game::doubleclick_event(GUI::MouseEvent& event)
{
    GUI::Frame::doubleclick_event(event);

    if (m_game_over_animation) {
        start_game_over_animation();
        setup();
        return;
    }

    if (m_new_game_animation)
        return;

    auto click_location = event.position();
    for (auto& to_check : m_stacks) {
        if (to_check.type() == CardStack::Type::Foundation || to_check.type() == CardStack::Type::Stock)
            continue;

        if (to_check.bounding_box().contains(click_location) && !to_check.is_empty()) {
            auto& top_card = to_check.peek();
            if (!top_card.is_upside_down() && top_card.rect().contains(click_location)) {
                if (stack(Foundation1).is_allowed_to_push(top_card))
                    move_card(to_check, stack(Foundation1));
                else if (stack(Foundation2).is_allowed_to_push(top_card))
                    move_card(to_check, stack(Foundation2));
                else if (stack(Foundation3).is_allowed_to_push(top_card))
                    move_card(to_check, stack(Foundation3));
                else if (stack(Foundation4).is_allowed_to_push(top_card))
                    move_card(to_check, stack(Foundation4));
                else
                    break;

                update_score(10);
            }
            break;
        }
    }
}

void Game::check_for_game_over()
{
    for (auto& stack : m_stacks) {
        if (stack.type() != CardStack::Type::Foundation)
            continue;
        if (stack.count() != Card::card_count)
            return;
    }

    start_game_over_animation();
}

void Game::move_card(CardStack& from, CardStack& to)
{
    update(from.bounding_box());

    auto card = from.pop();

    card->set_moving(true);
    m_focused_cards.clear();
    m_focused_cards.append(card);
    mark_intersecting_stacks_dirty(card);
    to.push(card);

    update(to.bounding_box());
}

void Game::mark_intersecting_stacks_dirty(Card& intersecting_card)
{
    for (auto& stack : m_stacks) {
        if (intersecting_card.rect().intersects(stack.bounding_box()))
            update(stack.bounding_box());
    }

    update(intersecting_card.rect());
}

void Game::paint_event(GUI::PaintEvent& event)
{
    static Gfx::Color s_background_color = palette().color(background_role());

    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    if (m_game_over_animation) {
        m_animation.draw(painter);
        return;
    }

    if (m_new_game_animation) {
        if (m_new_game_animation_delay < new_game_animation_delay) {
            ++m_new_game_animation_delay;
        } else {
            m_new_game_animation_delay = 0;
            auto& current_pile = stack(piles.at(m_new_game_animation_pile));

            if (current_pile.count() < m_new_game_animation_pile) {
                auto card = m_new_deck.take_last();
                card->set_upside_down(true);
                current_pile.push(card);
            } else {
                current_pile.push(m_new_deck.take_last());
                ++m_new_game_animation_pile;
            }

            if (m_new_game_animation_pile == piles.size()) {
                while (!m_new_deck.is_empty())
                    stack(Stock).push(m_new_deck.take_last());
                m_new_game_animation = false;
                stop_timer();

                if (on_game_start)
                    on_game_start();
            }
        }
    }

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
            check_for_game_over();
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

}
