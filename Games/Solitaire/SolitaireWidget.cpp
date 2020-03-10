/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "SolitaireWidget.h"
#include <LibCore/Timer.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <time.h>

static const Color s_background_color { Color::from_rgb(0x008000) };

SolitaireWidget::SolitaireWidget(GUI::Window& window, Function<void(uint32_t)>&& on_score_update)
    : m_on_score_update(move(on_score_update))
{
    set_fill_with_background_color(false);

    m_stacks[Stock] = CardStack({ 10, 10 }, CardStack::Type::Stock, 2, 1, 8);
    m_stacks[Waste] = CardStack({ 10 + Card::width + 10, 10 }, CardStack::Type::Waste, 2, 1, 8);
    m_stacks[Foundation4] = CardStack({ SolitaireWidget::width - Card::width - 10, 10 }, CardStack::Type::Foundation, 2, 1, 4);
    m_stacks[Foundation3] = CardStack({ SolitaireWidget::width - 2 * Card::width - 20, 10 }, CardStack::Type::Foundation, 2, 1, 4);
    m_stacks[Foundation2] = CardStack({ SolitaireWidget::width - 3 * Card::width - 30, 10 }, CardStack::Type::Foundation, 2, 1, 4);
    m_stacks[Foundation1] = CardStack({ SolitaireWidget::width - 4 * Card::width - 40, 10 }, CardStack::Type::Foundation, 2, 1, 4);
    m_stacks[Pile1] = CardStack({ 10, 10 + Card::height + 10 }, CardStack::Type::Normal, 0, 15);
    m_stacks[Pile2] = CardStack({ 10 + Card::width + 10, 10 + Card::height + 10 }, CardStack::Type::Normal, 0, 15);
    m_stacks[Pile3] = CardStack({ 10 + 2 * Card::width + 20, 10 + Card::height + 10 }, CardStack::Type::Normal, 0, 15);
    m_stacks[Pile4] = CardStack({ 10 + 3 * Card::width + 30, 10 + Card::height + 10 }, CardStack::Type::Normal, 0, 15);
    m_stacks[Pile5] = CardStack({ 10 + 4 * Card::width + 40, 10 + Card::height + 10 }, CardStack::Type::Normal, 0, 15);
    m_stacks[Pile6] = CardStack({ 10 + 5 * Card::width + 50, 10 + Card::height + 10 }, CardStack::Type::Normal, 0, 15);
    m_stacks[Pile7] = CardStack({ 10 + 6 * Card::width + 60, 10 + Card::height + 10 }, CardStack::Type::Normal, 0, 15);

    m_timer = Core::Timer::construct(1000 / 60, [&]() { tick(window); });
    m_timer->stop();
}

SolitaireWidget::~SolitaireWidget()
{
}

static float rand_float()
{
    return rand() / static_cast<float>(RAND_MAX);
}

static void make_pile(NonnullRefPtrVector<Card>& cards, CardStack& stack, uint8_t count)
{
    for (int i = 1; i < count; ++i) {
        auto card = cards.take_last();
        card->set_upside_down(true);
        stack.push(card);
    }

    stack.push(cards.take_last());
}

void SolitaireWidget::tick(GUI::Window& window)
{
    if (!is_visible() || !updates_enabled() || !window.is_visible_for_timer_purposes())
        return;

    if (m_game_over_animation) {
        if (m_animation.card()->position().x() > SolitaireWidget::width
            || m_animation.card()->rect().right() < 0) {
            create_new_animation_card();
        }

        m_animation.tick();
    }

    if (m_has_to_repaint || m_game_over_animation) {
        m_repaint_all = false;
        update();
    }
}

void SolitaireWidget::create_new_animation_card()
{
    srand(time(nullptr));

    auto card = Card::construct(static_cast<Card::Type>(rand() % Card::Type::__Count), rand() % Card::card_count);
    card->set_position({ rand() % (SolitaireWidget::width - Card::width), rand() % (SolitaireWidget::height / 8) });

    int x_sgn = card->position().x() > (SolitaireWidget::width / 2) ? -1 : 1;
    m_animation = Animation(card, rand_float() + .4, x_sgn * ((rand() % 3) + 3), .4 + rand_float() * .6);
}

void SolitaireWidget::start_game_over_animation()
{
    if (m_game_over_animation)
        return;

    create_new_animation_card();
    m_game_over_animation = true;
}

void SolitaireWidget::stop_game_over_animation()
{
    if (!m_game_over_animation)
        return;

    m_game_over_animation = false;
    m_repaint_all = true;
    update();
}

void SolitaireWidget::setup()
{
    stop_game_over_animation();

    for (auto& stack : m_stacks)
        stack.clear();

    NonnullRefPtrVector<Card> cards;
    for (int i = 0; i < Card::card_count; ++i) {
        cards.append(Card::construct(Card::Type::Clubs, i));
        cards.append(Card::construct(Card::Type::Spades, i));
        cards.append(Card::construct(Card::Type::Hearts, i));
        cards.append(Card::construct(Card::Type::Diamonds, i));
    }

    srand(time(nullptr));
    for (int i = 0; i < 200; ++i)
        cards.append(cards.take(rand() % cards.size()));

    make_pile(cards, stack(Pile1), 1);
    make_pile(cards, stack(Pile2), 2);
    make_pile(cards, stack(Pile3), 3);
    make_pile(cards, stack(Pile4), 4);
    make_pile(cards, stack(Pile5), 5);
    make_pile(cards, stack(Pile6), 6);
    make_pile(cards, stack(Pile7), 7);

    while (!cards.is_empty())
        stack(Stock).push(cards.take_last());

    m_score = 0;
    update_score(0);
    update();
}

void SolitaireWidget::update_score(int to_add)
{
    m_score = max(static_cast<int>(m_score) + to_add, 0);
    m_on_score_update(m_score);
}

void SolitaireWidget::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == KeyCode::Key_F12)
        start_game_over_animation();
}

void SolitaireWidget::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Widget::mousedown_event(event);

    if (m_game_over_animation)
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

                    while (!waste.is_empty()) {
                        auto card = waste.pop();
                        stock.push(card);
                    }

                    stock.set_dirty();
                    waste.set_dirty();
                    m_has_to_repaint = true;
                    update_score(-100);
                } else
                    move_card(stock, waste);
            } else if (!to_check.is_empty()) {
                auto& top_card = to_check.peek();

                if (top_card.is_upside_down()) {
                    if (top_card.rect().contains(click_location)) {
                        top_card.set_upside_down(false);
                        to_check.set_dirty();
                        update_score(5);
                        m_has_to_repaint = true;
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

void SolitaireWidget::mouseup_event(GUI::MouseEvent& event)
{
    GUI::Widget::mouseup_event(event);

    if (!m_focused_stack || m_focused_cards.is_empty() || m_game_over_animation)
        return;

    bool rebound = true;
    for (auto& stack : m_stacks) {
        if (stack.is_focused())
            continue;

        for (auto& focused_card : m_focused_cards) {
            if (stack.bounding_box().intersects(focused_card.rect())
                || stack.bounding_box().contains(event.position())) {
                if (stack.is_allowed_to_push(m_focused_cards.at(0))) {
                    for (auto& to_intersect : m_focused_cards) {
                        mark_intersecting_stacks_dirty(to_intersect);
                        stack.push(to_intersect);
                        m_focused_stack->pop();
                    }

                    m_focused_stack->set_dirty();
                    stack.set_dirty();

                    if (m_focused_stack->type() == CardStack::Type::Waste
                        && stack.type() == CardStack::Type::Normal) {
                        update_score(5);
                    } else if (m_focused_stack->type() == CardStack::Type::Waste
                        && stack.type() == CardStack::Type::Foundation) {
                        update_score(10);
                    } else if (m_focused_stack->type() == CardStack::Type::Normal
                        && stack.type() == CardStack::Type::Foundation) {
                        update_score(10);
                    } else if (m_focused_stack->type() == CardStack::Type::Foundation
                        && stack.type() == CardStack::Type::Normal) {
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
        m_focused_stack->set_dirty();
    }

    m_mouse_down = false;
    m_has_to_repaint = true;
}

void SolitaireWidget::mousemove_event(GUI::MouseEvent& event)
{
    GUI::Widget::mousemove_event(event);

    if (!m_mouse_down || m_game_over_animation)
        return;

    auto click_location = event.position();
    int dx = click_location.dx_relative_to(m_mouse_down_location);
    int dy = click_location.dy_relative_to(m_mouse_down_location);

    for (auto& to_intersect : m_focused_cards) {
        mark_intersecting_stacks_dirty(to_intersect);
        to_intersect.rect().move_by(dx, dy);
    }

    m_mouse_down_location = click_location;
    m_has_to_repaint = true;
}

void SolitaireWidget::doubleclick_event(GUI::MouseEvent& event)
{
    GUI::Widget::doubleclick_event(event);

    if (m_game_over_animation) {
        start_game_over_animation();
        setup();
        return;
    }

    if (!m_focused_cards.is_empty())
        return;

    auto click_location = event.position();
    for (auto& to_check : m_stacks) {
        if (to_check.type() == CardStack::Type::Foundation)
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

    m_has_to_repaint = true;
}

void SolitaireWidget::check_for_game_over()
{
    for (auto& stack : m_stacks) {
        if (stack.type() != CardStack::Type::Foundation)
            continue;
        if (stack.count() != Card::card_count)
            return;
    }

    start_game_over_animation();
}

void SolitaireWidget::move_card(CardStack& from, CardStack& to)
{
    auto card = from.pop();

    card->set_moving(true);
    m_focused_cards.append(card);
    mark_intersecting_stacks_dirty(card);
    to.push(card);

    from.set_dirty();
    to.set_dirty();

    m_has_to_repaint = true;
}

void SolitaireWidget::mark_intersecting_stacks_dirty(Card& intersecting_card)
{
    for (auto& stack : m_stacks) {
        if (intersecting_card.rect().intersects(stack.bounding_box())) {
            stack.set_dirty();
            m_has_to_repaint = true;
        }
    }
}

void SolitaireWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Widget::paint_event(event);

    m_has_to_repaint = false;
    if (m_game_over_animation && m_repaint_all)
        return;

    GUI::Painter painter(*this);

    if (m_repaint_all) {
        /* Only start the timer when update() got called from the
           window manager, or else we might end up with a blank screen */
        if (!m_timer->is_active())
            m_timer->start();

        painter.fill_rect(event.rect(), s_background_color);

        for (auto& stack : m_stacks)
            stack.draw(painter, s_background_color);
    } else if (!m_game_over_animation) {
        if (!m_focused_cards.is_empty()) {
            for (auto& focused_card : m_focused_cards)
                focused_card.clear(painter, s_background_color);
        }

        for (auto& stack : m_stacks) {
            if (stack.is_dirty())
                stack.draw(painter, s_background_color);
        }

        if (!m_focused_cards.is_empty()) {
            for (auto& focused_card : m_focused_cards) {
                focused_card.draw(painter);
                focused_card.save_old_position();
            }
        }
    } else if (m_animation.card() != nullptr)
        m_animation.card()->draw(painter);

    m_repaint_all = true;
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
