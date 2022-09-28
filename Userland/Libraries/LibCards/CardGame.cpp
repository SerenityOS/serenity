/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CardGame.h"
#include <LibCards/CardPainter.h>
#include <LibConfig/Client.h>
#include <LibGfx/Palette.h>

namespace Cards {

CardGame::CardGame()
{
    auto background_color = Gfx::Color::from_string(Config::read_string("Games"sv, "Cards"sv, "BackgroundColor"sv));
    set_background_color(background_color.value_or(Color::from_rgb(0x008000)));
}

void CardGame::add_stack(NonnullRefPtr<CardStack> stack)
{
    m_stacks.append(move(stack));
}

void CardGame::mark_intersecting_stacks_dirty(Cards::Card const& intersecting_card)
{
    for (auto& stack : stacks()) {
        if (intersecting_card.rect().intersects(stack.bounding_box()))
            update(stack.bounding_box());
    }

    update(intersecting_card.rect());
}

Gfx::IntRect CardGame::moving_cards_bounds() const
{
    if (!is_moving_cards())
        return {};

    // Note: This assumes that the cards are arranged in a line.
    return m_moving_cards.first().rect().united(m_moving_cards.last().rect());
}

void CardGame::pick_up_cards_from_stack(Cards::CardStack& stack, Gfx::IntPoint click_location, CardStack::MovementRule movement_rule)
{
    if (m_moving_cards_source_stack)
        m_moving_cards_source_stack->set_focused(false);
    stack.add_all_grabbed_cards(click_location, m_moving_cards, movement_rule);
    stack.set_focused(true);
    m_moving_cards_source_stack = stack;
}

RefPtr<CardStack> CardGame::find_stack_to_drop_on(CardStack::MovementRule movement_rule) const
{
    auto bounds_to_check = moving_cards_bounds();

    RefPtr<CardStack> closest_stack;
    float closest_distance = FLT_MAX;

    for (auto const& stack : stacks()) {
        if (stack.is_focused())
            continue;

        if (stack.bounding_box().intersects(bounds_to_check)
            && stack.is_allowed_to_push(moving_cards().at(0), moving_cards().size(), movement_rule)) {

            auto distance = bounds_to_check.center().distance_from(stack.bounding_box().center());
            if (distance < closest_distance) {
                closest_stack = stack;
                closest_distance = distance;
            }
        }
    }

    return closest_stack;
}

void CardGame::drop_cards_on_stack(Cards::CardStack& stack, CardStack::MovementRule movement_rule)
{
    VERIFY(stack.is_allowed_to_push(m_moving_cards.at(0), m_moving_cards.size(), movement_rule));
    for (auto& to_intersect : moving_cards()) {
        mark_intersecting_stacks_dirty(to_intersect);
        stack.push(to_intersect);
        (void)moving_cards_source_stack()->pop();
    }

    update(moving_cards_source_stack()->bounding_box());
    update(stack.bounding_box());
}

void CardGame::clear_moving_cards()
{
    if (!m_moving_cards_source_stack.is_null())
        m_moving_cards_source_stack->set_focused(false);
    m_moving_cards_source_stack.clear();
    m_moving_cards.clear();
}

void CardGame::dump_layout() const
{
    dbgln("------------------------------");
    for (auto const& stack : stacks())
        dbgln("{}", stack);
}

void CardGame::config_string_did_change(String const& domain, String const& group, String const& key, String const& value)
{
    if (domain == "Games" && group == "Cards") {
        if (key == "BackgroundColor") {
            if (auto maybe_color = Gfx::Color::from_string(value); maybe_color.has_value())
                set_background_color(maybe_color.value());
            return;
        }
        if (key == "CardBackImage") {
            CardPainter::the().set_background_image_path(value);
            update();
            return;
        }
    }
}

Gfx::Color CardGame::background_color() const
{
    return palette().color(background_role());
}

void CardGame::set_background_color(Gfx::Color const& color)
{
    auto new_palette = palette();
    new_palette.set_color(Gfx::ColorRole::Background, color);
    set_palette(new_palette);
}
}
