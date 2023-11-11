/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Debug.h>
#include <AK/Random.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

namespace Solitaire {

static constexpr uint8_t new_game_animation_delay = 2;
static constexpr int s_timer_interval_ms = 1000 / 60;
static constexpr int s_timer_solving_interval_ms = 100;

ErrorOr<NonnullRefPtr<Game>> Game::try_create()
{
    auto game = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Game()));

    TRY(game->add_stack(Gfx::IntPoint { 10, 10 }, CardStack::Type::Stock));
    TRY(game->add_stack(Gfx::IntPoint { 10 + Card::width + 10, 10 }, CardStack::Type::Waste));
    TRY(game->add_stack(Gfx::IntPoint { 10 + Card::width + 10, 10 }, CardStack::Type::Play, game->stack_at_location(Waste)));
    TRY(game->add_stack(Gfx::IntPoint { Game::width - 4 * Card::width - 40, 10 }, CardStack::Type::Foundation));
    TRY(game->add_stack(Gfx::IntPoint { Game::width - 3 * Card::width - 30, 10 }, CardStack::Type::Foundation));
    TRY(game->add_stack(Gfx::IntPoint { Game::width - 2 * Card::width - 20, 10 }, CardStack::Type::Foundation));
    TRY(game->add_stack(Gfx::IntPoint { Game::width - Card::width - 10, 10 }, CardStack::Type::Foundation));
    TRY(game->add_stack(Gfx::IntPoint { 10, 10 + Card::height + 10 }, CardStack::Type::Normal));
    TRY(game->add_stack(Gfx::IntPoint { 10 + Card::width + 10, 10 + Card::height + 10 }, CardStack::Type::Normal));
    TRY(game->add_stack(Gfx::IntPoint { 10 + 2 * Card::width + 20, 10 + Card::height + 10 }, CardStack::Type::Normal));
    TRY(game->add_stack(Gfx::IntPoint { 10 + 3 * Card::width + 30, 10 + Card::height + 10 }, CardStack::Type::Normal));
    TRY(game->add_stack(Gfx::IntPoint { 10 + 4 * Card::width + 40, 10 + Card::height + 10 }, CardStack::Type::Normal));
    TRY(game->add_stack(Gfx::IntPoint { 10 + 5 * Card::width + 50, 10 + Card::height + 10 }, CardStack::Type::Normal));
    TRY(game->add_stack(Gfx::IntPoint { 10 + 6 * Card::width + 60, 10 + Card::height + 10 }, CardStack::Type::Normal));

    return game;
}

Game::Game() = default;

static float rand_float()
{
    return get_random_uniform(RAND_MAX) / static_cast<float>(RAND_MAX);
}

void Game::deal_next_card()
{
    VERIFY(m_state == State::NewGameAnimation);

    auto& current_pile = stack_at_location(piles.at(m_new_game_animation_pile));

    if (current_pile.count() < m_new_game_animation_pile) {
        auto card = m_new_deck.take_last();
        card->set_upside_down(true);
        current_pile.push(card).release_value_but_fixme_should_propagate_errors();
    } else {
        current_pile.push(m_new_deck.take_last()).release_value_but_fixme_should_propagate_errors();
        ++m_new_game_animation_pile;
    }

    update(current_pile.bounding_box());

    if (m_new_game_animation_pile == piles.size()) {
        auto& stock_pile = stack_at_location(Stock);
        while (!m_new_deck.is_empty())
            stock_pile.push(m_new_deck.take_last()).release_value_but_fixme_should_propagate_errors();

        update(stock_pile.bounding_box());

        m_state = State::WaitingForNewGame;
        stop_timer();
    }
}

void Game::timer_event(Core::TimerEvent&)
{
    switch (m_state) {
    case State::StartGameOverAnimationNextFrame: {
        m_state = State::GameOverAnimation;
        set_background_fill_enabled(false);
        break;
    }
    case State::GameOverAnimation: {
        if (m_animation.position().x() >= Game::width || m_animation.card_rect().right() <= 0)
            create_new_animation_card();

        if (m_animation.tick())
            update(m_animation.card_rect());
        break;
    }
    case State::NewGameAnimation: {
        if (m_new_game_animation_delay < new_game_animation_delay) {
            ++m_new_game_animation_delay;
        } else {
            m_new_game_animation_delay = 0;
            deal_next_card();
        }
        break;
    }
    case State::Solving: {
        step_solve();
        break;
    }
    default:
        break;
    }
}

void Game::create_new_animation_card()
{
    auto suit = static_cast<Cards::Suit>(get_random_uniform(to_underlying(Cards::Suit::__Count)));
    auto rank = static_cast<Cards::Rank>(get_random_uniform(to_underlying(Cards::Rank::__Count)));
    Gfx::IntPoint position { get_random_uniform(Game::width - Card::width), get_random_uniform(Game::height / 8) };

    int x_direction = position.x() > (Game::width / 2) ? -1 : 1;
    m_animation = Animation(suit, rank, position, rand_float() + .4f, x_direction * (get_random_uniform(3) + 2), .6f + rand_float() * .4f);
}

void Game::set_background_fill_enabled(bool enabled)
{
    Widget* widget = this;
    while (widget) {
        widget->set_fill_with_background_color(enabled);
        widget = widget->parent_widget();
    }
}

void Game::start_game_over_animation()
{
    if (m_state == State::GameOverAnimation || m_state == State::StartGameOverAnimationNextFrame)
        return;

    m_last_move = {};
    if (on_undo_availability_change)
        on_undo_availability_change(false);

    create_new_animation_card();

    // We wait one frame, to make sure that the foundation stacks are repainted before we start.
    // Otherwise, if the game ended from an attempt_to_move_card_to_foundations() move, the
    // foundations could appear empty or otherwise incorrect.
    m_state = State::StartGameOverAnimationNextFrame;

    start_timer(s_timer_interval_ms);

    if (on_game_end)
        on_game_end(GameOverReason::Victory, m_score);
}

void Game::stop_game_over_animation()
{
    if (m_state != State::GameOverAnimation)
        return;

    set_background_fill_enabled(true);
    m_state = State::NewGameAnimation;
    update();

    stop_timer();
}

void Game::setup(Mode mode)
{
    if (m_state == State::NewGameAnimation)
        stop_timer();

    stop_game_over_animation();
    m_mode = mode;

    if (on_game_end)
        on_game_end(GameOverReason::NewGame, m_score);

    for (auto& stack : stacks())
        stack->clear();

    m_new_deck.clear();
    m_new_game_animation_pile = 0;
    m_passes_left_before_punishment = recycle_rules().passes_allowed_before_punishment;
    m_score = 0;
    update_score(0);
    if (on_undo_availability_change)
        on_undo_availability_change(false);

    m_new_deck = Cards::create_standard_deck(Cards::Shuffle::Yes).release_value_but_fixme_should_propagate_errors();

    clear_moving_cards();

    m_state = State::NewGameAnimation;
    start_timer(s_timer_interval_ms);
    update();
}

void Game::start_timer_if_necessary()
{
    if (on_game_start && m_state == State::WaitingForNewGame) {
        on_game_start();
        m_state = State::GameInProgress;
    }
}

void Game::score_move(CardStack& from, CardStack& to, bool inverse)
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

void Game::score_flip(bool inverse)
{
    update_score(5 * (inverse ? -1 : 1));
}

void Game::update_score(int to_add)
{
    m_score = max(static_cast<int>(m_score) + to_add, 0);

    if (on_score_update)
        on_score_update(m_score);
}

void Game::keydown_event(GUI::KeyEvent& event)
{
    if (is_moving_cards() || m_state == State::NewGameAnimation || m_state == State::GameOverAnimation) {
        event.ignore();
        return;
    }

    if (event.shift() && event.key() == KeyCode::Key_F12) {
        start_game_over_animation();
    } else if (event.key() == KeyCode::Key_Tab) {
        auto_move_eligible_cards_to_foundations();
    } else if (event.key() == KeyCode::Key_Space) {
        draw_cards();
    } else if (event.shift() && event.key() == KeyCode::Key_F11) {
        if constexpr (SOLITAIRE_DEBUG) {
            dump_layout();
        }
    } else {
        event.ignore();
    }
}

void Game::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousedown_event(event);

    if (m_state == State::NewGameAnimation || m_state == State::GameOverAnimation)
        return;

    auto click_location = event.position();
    for (auto& to_check : stacks()) {
        if (to_check->type() == CardStack::Type::Waste)
            continue;

        if (to_check->bounding_box().contains(click_location)) {
            if (to_check->type() == CardStack::Type::Stock) {
                draw_cards();
            } else if (!to_check->is_empty()) {
                auto& top_card = to_check->peek();

                if (top_card.is_upside_down()) {
                    if (top_card.rect().contains(click_location)) {
                        top_card.set_upside_down(false);
                        score_flip();
                        start_timer_if_necessary();
                        update(top_card.rect());
                        remember_flip_for_undo(top_card);

                        if (on_move)
                            on_move();
                    }
                } else if (!is_moving_cards()) {
                    if (is_auto_collecting() && attempt_to_move_card_to_foundations(to_check)) {
                        if (on_move)
                            on_move();
                        break;
                    }

                    if (event.button() == GUI::MouseButton::Secondary) {
                        preview_card(to_check, click_location);
                    } else {
                        pick_up_cards_from_stack(to_check, click_location, Cards::CardStack::MovementRule::Alternating).release_value_but_fixme_should_propagate_errors();
                        m_mouse_down_location = click_location;
                        m_mouse_down = true;
                    }

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
    clear_hovered_stack();

    if (is_previewing_card()) {
        clear_card_preview();
        return;
    }

    if (!is_moving_cards() || m_state == State::NewGameAnimation || m_state == State::GameOverAnimation)
        return;

    bool rebound = true;
    if (auto target_stack = find_stack_to_drop_on(Cards::CardStack::MovementRule::Alternating); !target_stack.is_null()) {
        auto& stack = *target_stack;
        remember_move_for_undo(*moving_cards_source_stack(), stack, moving_cards());

        drop_cards_on_stack(stack, Cards::CardStack::MovementRule::Alternating).release_value_but_fixme_should_propagate_errors();

        if (moving_cards_source_stack()->type() == CardStack::Type::Play)
            pop_waste_to_play_stack();

        score_move(*moving_cards_source_stack(), stack);
        rebound = false;

        if (on_move)
            on_move();
    }

    if (rebound) {
        for (auto& to_intersect : moving_cards())
            mark_intersecting_stacks_dirty(to_intersect);

        moving_cards_source_stack()->rebound_cards();
        update(moving_cards_source_stack()->bounding_box());
    }

    m_mouse_down = false;
}

void Game::mousemove_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousemove_event(event);

    if (!m_mouse_down || m_state == State::NewGameAnimation || m_state == State::GameOverAnimation)
        return;

    auto click_location = event.position();
    int dx = click_location.dx_relative_to(m_mouse_down_location);
    int dy = click_location.dy_relative_to(m_mouse_down_location);

    if (auto target_stack = find_stack_to_drop_on(Cards::CardStack::MovementRule::Alternating)) {
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

    if (m_state == State::GameOverAnimation) {
        setup(mode());
        return;
    }

    if (m_state == State::NewGameAnimation) {
        while (m_state == State::NewGameAnimation)
            deal_next_card();
        return;
    }

    auto click_location = event.position();
    for (auto& to_check : stacks()) {
        if (to_check->type() != CardStack::Type::Normal && to_check->type() != CardStack::Type::Play)
            continue;

        if (to_check->bounding_box().contains(click_location) && !to_check->is_empty()) {
            auto& top_card = to_check->peek();
            if (!top_card.is_upside_down() && top_card.rect().contains(click_location))
                attempt_to_move_card_to_foundations(to_check);

            break;
        }
    }
}

void Game::check_for_game_over()
{
    for (auto foundationID : foundations) {
        auto& foundation = stack_at_location(foundationID);

        if (foundation.count() != Card::card_count)
            return;
    }

    if (has_timer())
        stop_timer();
    start_game_over_animation();
}

void Game::draw_cards()
{
    auto& waste = stack_at_location(Waste);
    auto& stock = stack_at_location(Stock);
    auto& play = stack_at_location(Play);

    if (stock.is_empty()) {
        if (waste.is_empty() && play.is_empty())
            return;

        update(waste.bounding_box());
        update(play.bounding_box());

        Vector<NonnullRefPtr<Card>> moved_cards;
        while (!play.is_empty()) {
            auto card = play.pop();
            stock.push(card).release_value_but_fixme_should_propagate_errors();
            moved_cards.prepend(card);
        }

        while (!waste.is_empty()) {
            auto card = waste.pop();
            stock.push(card).release_value_but_fixme_should_propagate_errors();
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
        play.take_all(waste).release_value_but_fixme_should_propagate_errors();

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

        Vector<NonnullRefPtr<Card>> cards_drawn;
        for (size_t i = 0; (i < cards_to_draw) && !stock.is_empty(); ++i) {
            auto card = stock.pop();
            cards_drawn.prepend(card);
            play.push(move(card)).release_value_but_fixme_should_propagate_errors();
        }

        remember_move_for_undo(stock, play, cards_drawn);

        if (play.bounding_box().size().width() > play_bounding_box.size().width())
            update(play.bounding_box());
        else
            update(play_bounding_box);
    }

    start_timer_if_necessary();
}

void Game::pop_waste_to_play_stack()
{
    auto& waste = stack_at_location(Waste);
    auto& play = stack_at_location(Play);
    if (play.is_empty() && !waste.is_empty()) {
        auto card = waste.pop();
        moving_cards().append(card);
        play.push(move(card)).release_value_but_fixme_should_propagate_errors();
    }
}

bool Game::attempt_to_move_card_to_foundations(CardStack& from)
{
    if (from.is_empty())
        return false;

    auto& top_card = from.peek();
    if (top_card.is_upside_down())
        return false;

    bool card_was_moved = false;

    for (auto foundationID : foundations) {
        auto& foundation = stack_at_location(foundationID);

        if (foundation.is_allowed_to_push(top_card)) {
            update(from.bounding_box());

            auto card = from.pop();

            mark_intersecting_stacks_dirty(card);
            foundation.push(card).release_value_but_fixme_should_propagate_errors();

            Vector<NonnullRefPtr<Card>> moved_card;
            moved_card.append(card);
            remember_move_for_undo(from, foundation, moved_card);

            score_move(from, foundation);

            update(foundation.bounding_box());

            card_was_moved = true;
            break;
        }
    }

    if (card_was_moved) {
        if (from.type() == CardStack::Type::Play)
            pop_waste_to_play_stack();

        start_timer_if_necessary();
        check_for_game_over();
    }

    return card_was_moved;
}

void Game::auto_move_eligible_cards_to_foundations()
{
    while (true) {
        bool card_was_moved = false;
        for (auto& to_check : stacks()) {
            if (to_check->type() != CardStack::Type::Normal && to_check->type() != CardStack::Type::Play)
                continue;

            if (attempt_to_move_card_to_foundations(to_check))
                card_was_moved = true;
        }

        if (!card_was_moved)
            break;
    }
}

void Game::paint_event(GUI::PaintEvent& event)
{
    Gfx::Color background_color = this->background_color();

    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    if (m_state == State::GameOverAnimation) {
        m_animation.draw(painter);
        return;
    }

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
            check_for_game_over();
            for (auto& card : moving_cards())
                card->set_moving(false);
        }

        clear_moving_cards();
    }
}

void Game::remember_move_for_undo(CardStack& from, CardStack& to, Vector<NonnullRefPtr<Card>> moved_cards)
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
    Vector<NonnullRefPtr<Card>> cards;
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
        m_last_move.cards[0]->set_upside_down(true);
        if (on_undo_availability_change)
            on_undo_availability_change(false);
        invalidate_layout();
        score_flip(true);
        return;
    }

    if (m_last_move.from->type() == CardStack::Type::Play && m_mode == Mode::SingleCardDraw) {
        auto& waste = stack_at_location(Waste);
        if (!m_last_move.from->is_empty())
            waste.push(m_last_move.from->pop()).release_value_but_fixme_should_propagate_errors();
    }

    for (auto& to_intersect : m_last_move.cards) {
        mark_intersecting_stacks_dirty(to_intersect);
        m_last_move.from->push(to_intersect).release_value_but_fixme_should_propagate_errors();
        (void)m_last_move.to->pop();
    }

    if (m_last_move.from->type() == CardStack::Type::Stock) {
        auto& waste = stack_at_location(Waste);
        auto& play = stack_at_location(Play);
        Vector<NonnullRefPtr<Card>> cards_popped;
        for (size_t i = 0; i < m_last_move.cards.size(); i++) {
            if (!waste.is_empty()) {
                auto card = waste.pop();
                cards_popped.prepend(card);
            }
        }
        for (auto& card : cards_popped) {
            play.push(card).release_value_but_fixme_should_propagate_errors();
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

bool Game::can_solve()
{
    if (m_state != State::GameInProgress)
        return false;

    for (auto const& stack : stacks()) {
        switch (stack->type()) {
        case Cards::CardStack::Type::Waste:
        case Cards::CardStack::Type::Stock:
            if (!stack->is_empty())
                return false;
            break;
        case Cards::CardStack::Type::Normal:
            if (!stack->is_empty() && stack->stack().first()->is_upside_down())
                return false;
            break;
        default:
            break;
        }
    }

    return true;
}

void Game::start_solving()
{
    if (!can_solve())
        return;

    m_state = State::Solving;
    start_timer(s_timer_solving_interval_ms);
}

void Game::step_solve()
{
    for (auto& stack : stacks()) {
        if (stack->type() != Cards::CardStack::Type::Normal)
            continue;

        if (attempt_to_move_card_to_foundations(stack))
            break;
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
