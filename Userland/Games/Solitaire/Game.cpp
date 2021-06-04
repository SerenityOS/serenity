/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Debug.h>
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

    m_stacks.append(adopt_ref(*new CardStack({ 10, 10 }, CardStack::Type::Stock)));
    m_stacks.append(adopt_ref(*new CardStack({ 10 + Card::width + 10, 10 }, CardStack::Type::Waste)));
    m_stacks.append(adopt_ref(*new CardStack({ 10 + Card::width + 10, 10 }, CardStack::Type::Play, m_stacks.ptr_at(Waste))));
    m_stacks.append(adopt_ref(*new CardStack({ Game::width - 4 * Card::width - 40, 10 }, CardStack::Type::Foundation)));
    m_stacks.append(adopt_ref(*new CardStack({ Game::width - 3 * Card::width - 30, 10 }, CardStack::Type::Foundation)));
    m_stacks.append(adopt_ref(*new CardStack({ Game::width - 2 * Card::width - 20, 10 }, CardStack::Type::Foundation)));
    m_stacks.append(adopt_ref(*new CardStack({ Game::width - Card::width - 10, 10 }, CardStack::Type::Foundation)));
    m_stacks.append(adopt_ref(*new CardStack({ 10, 10 + Card::height + 10 }, CardStack::Type::Normal)));
    m_stacks.append(adopt_ref(*new CardStack({ 10 + Card::width + 10, 10 + Card::height + 10 }, CardStack::Type::Normal)));
    m_stacks.append(adopt_ref(*new CardStack({ 10 + 2 * Card::width + 20, 10 + Card::height + 10 }, CardStack::Type::Normal)));
    m_stacks.append(adopt_ref(*new CardStack({ 10 + 3 * Card::width + 30, 10 + Card::height + 10 }, CardStack::Type::Normal)));
    m_stacks.append(adopt_ref(*new CardStack({ 10 + 4 * Card::width + 40, 10 + Card::height + 10 }, CardStack::Type::Normal)));
    m_stacks.append(adopt_ref(*new CardStack({ 10 + 5 * Card::width + 50, 10 + Card::height + 10 }, CardStack::Type::Normal)));
    m_stacks.append(adopt_ref(*new CardStack({ 10 + 6 * Card::width + 60, 10 + Card::height + 10 }, CardStack::Type::Normal)));
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
        on_game_end(GameOverReason::Victory, m_score);
}

void Game::stop_game_over_animation()
{
    if (!m_game_over_animation)
        return;

    m_game_over_animation = false;
    update();

    stop_timer();
}

void Game::setup(Mode mode)
{
    if (m_new_game_animation)
        stop_timer();

    stop_game_over_animation();
    m_mode = mode;

    if (on_game_end)
        on_game_end(GameOverReason::NewGame, m_score);

    for (auto& stack : m_stacks)
        stack.clear();

    m_new_deck.clear();
    m_new_game_animation_pile = 0;
    m_passes_left_before_punishment = recycle_rules().passes_allowed_before_punishment;
    m_score = 0;
    update_score(0);
    if (on_undo_availability_change)
        on_undo_availability_change(false);

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

void Game::start_timer_if_necessary()
{
    if (on_game_start && m_waiting_for_new_game) {
        on_game_start();
        m_waiting_for_new_game = false;
    }
}

void Game::score_move(CardStack& from, CardStack& to, bool inverse = false)
{
    if (from.type() == CardStack::Type::Play && to.type() == CardStack::Type::Normal) {
        update_score(5 * (inverse ? -1 : 1));
    } else if (from.type() == CardStack::Type::Play && to.type() == CardStack::Type::Foundation) {
        update_score(10 * (inverse ? -1 : 1));
    } else if (from.type() == CardStack::Type::Normal && to.type() == CardStack::Type::Foundation) {
        update_score(10 * (inverse ? -1 : 1));
    } else if (from.type() == CardStack::Type::Foundation && to.type() == CardStack::Type::Normal) {
        update_score(-15 * (inverse ? -1 : 1));
    }
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

    if (event.shift() && event.key() == KeyCode::Key_F12) {
        start_game_over_animation();
    } else if (event.key() == KeyCode::Key_Tab) {
        auto_move_eligible_cards_to_stacks();
    } else if (event.key() == KeyCode::Key_Space) {
        draw_cards();
        invalidate_layout(); // FIXME: Stock stack won't render properly after draw_cards() without this
    } else if (event.shift() && event.key() == KeyCode::Key_F11) {
        dump_layout();
    }
}

void Game::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousedown_event(event);

    if (m_new_game_animation || m_game_over_animation)
        return;

    start_timer_if_necessary();

    auto click_location = event.position();
    for (auto& to_check : m_stacks) {
        if (to_check.type() == CardStack::Type::Waste)
            continue;

        if (to_check.bounding_box().contains(click_location)) {
            if (to_check.type() == CardStack::Type::Stock) {
                draw_cards();
            } else if (!to_check.is_empty()) {
                auto& top_card = to_check.peek();

                if (top_card.is_upside_down()) {
                    if (top_card.rect().contains(click_location)) {
                        top_card.set_upside_down(false);
                        update_score(5);
                        update(top_card.rect());
                        remember_flip_for_undo(top_card);
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
                if (stack.is_allowed_to_push(m_focused_cards.at(0), m_focused_cards.size())) {
                    for (auto& to_intersect : m_focused_cards) {
                        mark_intersecting_stacks_dirty(to_intersect);
                        stack.push(to_intersect);
                        m_focused_stack->pop();
                    }

                    remember_move_for_undo(*m_focused_stack, stack, m_focused_cards);

                    if (m_focused_stack->type() == CardStack::Type::Play) {
                        pop_waste_to_play_stack();
                    }

                    update(m_focused_stack->bounding_box());
                    update(stack.bounding_box());

                    score_move(*m_focused_stack, stack);

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
        setup(mode());
        return;
    }

    if (m_new_game_animation)
        return;

    auto click_location = event.position();
    for (auto& to_check : m_stacks) {
        if (to_check.type() == CardStack::Type::Foundation || to_check.type() == CardStack::Type::Stock || to_check.type() == CardStack::Type::Waste)
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

                if (to_check.type() == CardStack::Type::Play) {
                    auto& waste = this->stack(Waste);
                    if (to_check.is_empty() && !waste.is_empty()) {
                        auto card = waste.pop();
                        m_focused_cards.append(card);
                        to_check.push(move(card));
                    }
                }

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

    remember_move_for_undo(from, to, m_focused_cards);

    update(to.bounding_box());
}

void Game::draw_cards()
{
    auto& waste = stack(Waste);
    auto& stock = stack(Stock);
    auto& play = stack(Play);

    if (stock.is_empty()) {
        if (waste.is_empty() && play.is_empty())
            return;

        update(waste.bounding_box());
        update(play.bounding_box());

        NonnullRefPtrVector<Card> moved_cards;
        while (!play.is_empty()) {
            auto card = play.pop();
            stock.push(card);
            moved_cards.prepend(card);
        }

        while (!waste.is_empty()) {
            auto card = waste.pop();
            stock.push(card);
            moved_cards.prepend(card);
        }

        remember_move_for_undo(waste, stock, moved_cards);

        if (m_passes_left_before_punishment == 0)
            update_score(recycle_rules().punishment);
        else
            --m_passes_left_before_punishment;

        update(stock.bounding_box());
    } else {
        auto play_bounding_box = play.bounding_box();
        play.move_to_stack(waste);

        size_t cards_to_draw = 0;
        switch (m_mode) {
        case Mode::SingleCardDraw:
            cards_to_draw = 1;
            break;
        case Mode::ThreeCardDraw:
            cards_to_draw = 3;
            break;
        default:
            VERIFY_NOT_REACHED();
            break;
        }

        update(stock.bounding_box());

        NonnullRefPtrVector<Card> cards_drawn;
        for (size_t i = 0; (i < cards_to_draw) && !stock.is_empty(); ++i) {
            auto card = stock.pop();
            cards_drawn.append(card);
            play.push(move(card));
        }

        remember_move_for_undo(stock, play, cards_drawn);

        if (play.bounding_box().size().width() > play_bounding_box.size().width())
            update(play.bounding_box());
        else
            update(play_bounding_box);
    }
}

void Game::pop_waste_to_play_stack()
{
    auto& waste = this->stack(Waste);
    auto& play = this->stack(Play);
    if (play.is_empty() && !waste.is_empty()) {
        auto card = waste.pop();
        m_focused_cards.append(card);
        play.push(move(card));
    }
}

void Game::auto_move_eligible_cards_to_stacks()
{
    bool card_was_moved = false;

    for (auto& to_check : m_stacks) {
        if (to_check.type() != CardStack::Type::Normal && to_check.type() != CardStack::Type::Play)
            continue;

        if (to_check.is_empty())
            continue;

        auto& top_card = to_check.peek();
        if (top_card.is_upside_down())
            continue;

        if (stack(Foundation1).is_allowed_to_push(top_card)) {
            move_card(to_check, stack(Foundation1));
            card_was_moved = true;
            if (to_check.type() == CardStack::Type::Play)
                pop_waste_to_play_stack();
        } else if (stack(Foundation2).is_allowed_to_push(top_card)) {
            move_card(to_check, stack(Foundation2));
            card_was_moved = true;
            if (to_check.type() == CardStack::Type::Play)
                pop_waste_to_play_stack();
        } else if (stack(Foundation3).is_allowed_to_push(top_card)) {
            move_card(to_check, stack(Foundation3));
            card_was_moved = true;
            if (to_check.type() == CardStack::Type::Play)
                pop_waste_to_play_stack();
        } else if (stack(Foundation4).is_allowed_to_push(top_card)) {
            move_card(to_check, stack(Foundation4));
            card_was_moved = true;
            if (to_check.type() == CardStack::Type::Play)
                pop_waste_to_play_stack();
        }
    }

    // If at least one card was moved, check again to see if now any additional cards can now be moved
    if (card_was_moved) {
        start_timer_if_necessary();
        auto_move_eligible_cards_to_stacks();
    }
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
                m_waiting_for_new_game = true;
                stop_timer();
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

void Game::remember_move_for_undo(CardStack& from, CardStack& to, NonnullRefPtrVector<Card> moved_cards)
{
    m_last_move.type = LastMove::Type::MoveCards;
    m_last_move.from = &from;
    m_last_move.cards = moved_cards;
    m_last_move.to = &to;
    if (on_undo_availability_change)
        on_undo_availability_change(true);
}

void Game::remember_flip_for_undo(Card& card)
{
    NonnullRefPtrVector<Card> cards;
    cards.append(card);
    m_last_move.type = LastMove::Type::FlipCard;
    m_last_move.cards = cards;
    if (on_undo_availability_change)
        on_undo_availability_change(true);
}

void Game::perform_undo()
{
    if (m_last_move.type == LastMove::Type::Invalid)
        return;

    if (m_last_move.type == LastMove::Type::FlipCard) {
        m_last_move.cards.at(0).set_upside_down(true);
        if (on_undo_availability_change)
            on_undo_availability_change(false);
        invalidate_layout();
        return;
    }

    if (m_last_move.from->type() == CardStack::Type::Play && m_mode == Mode::SingleCardDraw) {
        auto& waste = stack(Waste);
        if (!m_last_move.from->is_empty())
            waste.push(m_last_move.from->pop());
    }

    for (auto& to_intersect : m_last_move.cards) {
        mark_intersecting_stacks_dirty(to_intersect);
        m_last_move.from->push(to_intersect);
        m_last_move.to->pop();
    }

    if (m_last_move.from->type() == CardStack::Type::Stock) {
        auto& waste = this->stack(Waste);
        auto& play = this->stack(Play);
        NonnullRefPtrVector<Card> cards_popped;
        for (size_t i = 0; i < m_last_move.cards.size(); i++) {
            if (!waste.is_empty()) {
                auto card = waste.pop();
                cards_popped.prepend(card);
            }
        }
        for (auto& card : cards_popped) {
            m_focused_cards.append(card);
            play.push(move(card));
        }
    }

    if (m_last_move.from->type() == CardStack::Type::Waste && m_last_move.to->type() == CardStack::Type::Stock)
        pop_waste_to_play_stack();

    score_move(*m_last_move.from, *m_last_move.to, true);

    m_last_move = {};
    if (on_undo_availability_change)
        on_undo_availability_change(false);
    invalidate_layout();
}

void Game::dump_layout() const
{
    if constexpr (SOLITAIRE_DEBUG) {
        dbgln("------------------------------");
        for (const auto& stack : m_stacks)
            dbgln("{}", stack);
    }
}

}
