/*
 * Copyright (c) 2026, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Game.h"
#include <AK/Random.h>
#include <LibCards/CardPainter.h>
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

namespace TriPeaks {

static constexpr int s_peak_center_x[] = { 84, 370, 656 };
static constexpr int s_peak_top_y = 5;
static constexpr int s_row_spacing_y = 85;
static constexpr int s_card_spacing_x = 56;
static constexpr int s_stock_x = 320;
static constexpr int s_stock_y = 380;
static constexpr int s_waste_x = 420;
static constexpr int s_waste_y = 380;

ErrorOr<NonnullRefPtr<Game>> Game::try_create()
{
    auto game = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Game()));

    TRY(game->add_stack(Gfx::IntPoint { s_stock_x, s_stock_y }, CardStack::Type::Stock));
    TRY(game->add_stack(Gfx::IntPoint { s_waste_x, s_waste_y }, CardStack::Type::Waste));

    for (u32 peak = 0; peak < 3; ++peak) {
        u32 pos_in_peak = 0;
        for (u32 row = 0; row < 4; ++row) {
            auto cards_in_row = row + 1u;
            for (u32 col = 0; col < cards_in_row; ++col) {
                auto position = card_position_for_location(peak, row, col);
                TRY(game->add_stack(position, CardStack::Type::Normal));
                ++pos_in_peak;
            }
        }
        VERIFY(pos_in_peak == cards_per_peak);
    }

    return game;
}

Game::Game() = default;

Gfx::IntPoint Game::card_position_for_location(u32 peak, u32 row, u32 col)
{
    auto center_x = s_peak_center_x[peak];
    auto cards_in_row = row + 1u;

    int row_total_width = (int(cards_in_row) - 1) * s_card_spacing_x;
    int row_left = center_x - row_total_width / 2;

    auto x = row_left + (int)col * s_card_spacing_x;
    auto y = s_peak_top_y + (int)row * s_row_spacing_y;

    return { x, y };
}

void Game::setup()
{
    if (on_game_end)
        on_game_end(GameOverReason::NewGame, m_score);

    for (auto& stack : stacks())
        stack->clear();

    m_score = 0;
    m_streak = 0;
    m_last_move = {};
    if (on_undo_availability_change)
        on_undo_availability_change(false);
    if (on_score_update)
        on_score_update(0);
    if (on_streak_update)
        on_streak_update(0);

    auto deck = Cards::create_standard_deck(Cards::Shuffle::Yes).release_value_but_fixme_should_propagate_errors();

    auto& stock = stack_at_location(Stock);

    for (u32 peak = 0; peak < 3; ++peak) {
        for (u32 pos = 0; pos < cards_per_peak; ++pos) {
            auto& stack = stack_at_location(TableauFirst + peak * cards_per_peak + pos);
            auto card = deck.take_last();
            card->set_upside_down(true);
            card->set_disabled(true);
            stack.push(card).release_value_but_fixme_should_propagate_errors();
        }
    }

    for (u32 peak = 0; peak < 3; ++peak) {
        for (u32 pos = 6; pos < 10; ++pos) {
            update_card_disabled_status(peak, pos);
        }
    }

    while (!deck.is_empty())
        stock.push(deck.take_last()).release_value_but_fixme_should_propagate_errors();

    auto first_waste = stock.pop();
    first_waste->set_upside_down(false);
    auto& waste = stack_at_location(Waste);
    waste.push(first_waste).release_value_but_fixme_should_propagate_errors();

    m_state = State::WaitingForNewGame;
    update();
}

void Game::update_card_disabled_status(u32 peak, u32 pos)
{
    auto& stack = stack_at_location(TableauFirst + peak * cards_per_peak + pos);
    if (stack.is_empty())
        return;

    bool is_covered = false;
    for (auto const& pair : tableau_cover_pairs) {
        if (pair.covered == pos) {
            auto& left_stack = stack_at_location(TableauFirst + peak * cards_per_peak + pair.coverer_left);
            auto& right_stack = stack_at_location(TableauFirst + peak * cards_per_peak + pair.coverer_right);
            if (!left_stack.is_empty() || !right_stack.is_empty()) {
                is_covered = true;
            }
            break;
        }
    }

    if (!is_covered) {
        stack.peek().set_upside_down(false);
        stack.peek().set_disabled(false);
    }
}

void Game::flip_uncovered_cards(u32 peak, u32 removed_pos)
{
    for (auto const& pair : tableau_cover_pairs) {
        if (pair.coverer_left == removed_pos || pair.coverer_right == removed_pos) {
            update_card_disabled_status(peak, pair.covered);
            auto& stack = stack_at_location(TableauFirst + peak * cards_per_peak + pair.covered);
            update(stack.bounding_box());
        }
    }
}

static bool is_one_rank_away(Cards::Rank a, Cards::Rank b)
{
    if (a == b)
        return false;
    auto a_val = to_underlying(a);
    auto b_val = to_underlying(b);
    if (a_val == (b_val + 1) % 13 || b_val == (a_val + 1) % 13)
        return true;
    return false;
}

bool Game::has_valid_moves() const
{
    auto const& waste = stacks()[Waste];
    if (waste->is_empty())
        return true;

    auto const& waste_top = waste->peek();

    for (u32 peak = 0; peak < 3; ++peak) {
        for (u32 pos = 0; pos < cards_per_peak; ++pos) {
            auto const& stack = stacks()[TableauFirst + peak * cards_per_peak + pos];
            if (stack->is_empty())
                continue;
            auto const& card = stack->peek();
            if (!card.is_disabled() && is_one_rank_away(card.rank(), waste_top.rank()))
                return true;
        }
    }

    return false;
}

void Game::remove_tableau_card(u32 peak, u32 pos)
{
    auto& tableau_stack = stack_at_location(TableauFirst + peak * cards_per_peak + pos);
    auto& waste = stack_at_location(Waste);

    Vector<u32> flipped_positions;
    for (auto const& pair : tableau_cover_pairs) {
        if (pair.coverer_left == pos || pair.coverer_right == pos) {
            auto& covered_stack = stack_at_location(TableauFirst + peak * cards_per_peak + pair.covered);
            if (!covered_stack.is_empty() && covered_stack.peek().is_upside_down())
                flipped_positions.append(pair.covered);
        }
    }

    auto card = tableau_stack.pop();
    card->set_upside_down(false);

    update(tableau_stack.bounding_box());

    update_streak();
    update_score(25 + m_streak * 5);

    waste.push(card).release_value_but_fixme_should_propagate_errors();
    update(waste.bounding_box());

    flip_uncovered_cards(peak, pos);

    start_timer_if_necessary();
    check_for_victory();

    m_last_move.type = LastMove::Type::TableauRemove;
    m_last_move.tableau_peak = peak;
    m_last_move.tableau_pos = pos;
    m_last_move.flipped_positions = move(flipped_positions);
    m_last_move.score_before = m_score - (25 + m_streak * 5);
    m_last_move.streak_before = m_streak - 1;
    if (on_undo_availability_change)
        on_undo_availability_change(true);

    if (stack_at_location(Stock).is_empty() && !has_valid_moves())
        check_for_game_over();
}

void Game::draw_from_stock()
{
    auto& stock = stack_at_location(Stock);
    auto& waste = stack_at_location(Waste);

    if (stock.is_empty())
        return;

    auto old_stock_rect = stock.bounding_box();

    auto card = stock.pop();
    card->set_upside_down(false);
    waste.push(card).release_value_but_fixme_should_propagate_errors();

    update(old_stock_rect);
    update(stock.bounding_box());
    update(waste.bounding_box());

    m_streak = 0;
    if (on_streak_update)
        on_streak_update(m_streak);

    start_timer_if_necessary();

    m_last_move.type = LastMove::Type::StockDraw;
    m_last_move.score_before = m_score;
    m_last_move.streak_before = m_streak;
    if (on_undo_availability_change)
        on_undo_availability_change(true);

    if (stock.is_empty() && !has_valid_moves())
        check_for_game_over();
}

void Game::perform_undo()
{
    if (m_last_move.type == LastMove::Type::Invalid)
        return;

    if (m_last_move.type == LastMove::Type::StockDraw) {
        auto& stock = stack_at_location(Stock);
        auto& waste = stack_at_location(Waste);

        auto card = waste.pop();
        card->set_upside_down(true);
        stock.push(card).release_value_but_fixme_should_propagate_errors();

        update(stock.bounding_box());
        update(waste.bounding_box());
        update();
    }

    if (m_last_move.type == LastMove::Type::TableauRemove) {
        auto peak = m_last_move.tableau_peak;
        auto pos = m_last_move.tableau_pos;
        auto& tableau_stack = stack_at_location(TableauFirst + peak * cards_per_peak + pos);
        auto& waste = stack_at_location(Waste);

        auto card = waste.pop();
        tableau_stack.push(card).release_value_but_fixme_should_propagate_errors();

        for (auto flipped_pos : m_last_move.flipped_positions) {
            auto& stack = stack_at_location(TableauFirst + peak * cards_per_peak + flipped_pos);
            if (!stack.is_empty()) {
                stack.peek().set_upside_down(true);
                stack.peek().set_disabled(true);
            }
        }

        update(tableau_stack.bounding_box());
        update(waste.bounding_box());
        update();
    }

    m_score = m_last_move.score_before;
    m_streak = m_last_move.streak_before;
    if (on_score_update)
        on_score_update(m_score);
    if (on_streak_update)
        on_streak_update(m_streak);

    m_last_move = {};
    if (on_undo_availability_change)
        on_undo_availability_change(false);
}

void Game::update_score(u32 delta)
{
    m_score += delta;
    if (on_score_update)
        on_score_update(m_score);
}

void Game::update_streak()
{
    ++m_streak;
    if (on_streak_update)
        on_streak_update(m_streak);
}

void Game::check_for_victory()
{
    for (u32 peak = 0; peak < 3; ++peak) {
        for (u32 pos = 0; pos < cards_per_peak; ++pos) {
            if (!stack_at_location(TableauFirst + peak * cards_per_peak + pos).is_empty())
                return;
        }
    }

    update_score(100);
    m_state = State::GameOver;
    if (on_game_end)
        on_game_end(GameOverReason::Victory, m_score);
}

void Game::check_for_game_over()
{
    m_state = State::GameOver;
    if (on_game_end)
        on_game_end(GameOverReason::NewGame, m_score);
}

void Game::start_timer_if_necessary()
{
    if (on_game_start && m_state == State::WaitingForNewGame) {
        on_game_start();
        m_state = State::GameInProgress;
    }
}

void Game::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    auto bg_color = background_color();
    for (auto& stack : stacks()) {
        if (stack->is_empty() && (stack->type() == CardStack::Type::Normal || stack->type() == CardStack::Type::Stock))
            continue;
        stack->paint(painter, bg_color);
    }
}

void Game::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousedown_event(event);

    if (m_state == State::GameOver)
        return;

    auto click_location = event.position();

    auto& stock = stack_at_location(Stock);
    if (stock.bounding_box().contains(click_location)) {
        draw_from_stock();
        return;
    }

    auto& waste = stack_at_location(Waste);
    if (waste.is_empty())
        return;

    auto& waste_top = waste.peek();

    for (int peak = 2; peak >= 0; --peak) {
        for (int pos = cards_per_peak - 1; pos >= 0; --pos) {
            auto& stack = stack_at_location(TableauFirst + peak * cards_per_peak + pos);
            if (stack.is_empty())
                continue;

            auto& card = stack.peek();
            if (card.is_disabled())
                continue;

            if (card.rect().contains(click_location)) {
                if (is_one_rank_away(card.rank(), waste_top.rank())) {
                    remove_tableau_card(peak, pos);
                }
                return;
            }
        }
    }
}

void Game::mouseup_event(GUI::MouseEvent& event)
{
    GUI::Frame::mouseup_event(event);
}

void Game::mousemove_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousemove_event(event);
}

void Game::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == KeyCode::Key_F2) {
        setup();
        return;
    }

    if (event.shift() && event.key() == KeyCode::Key_F12) {
        for (auto& stack : stacks()) {
            if (stack->type() == CardStack::Type::Normal)
                stack->clear();
        }
        check_for_victory();
        return;
    }

    if (event.key() == KeyCode::Key_Escape || event.key() == KeyCode::Key_Q) {
        GUI::Application::the()->quit();
        return;
    }

    event.ignore();
}

}
