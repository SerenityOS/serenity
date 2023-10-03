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
#include <LibGUI/Action.h>
#include <LibGUI/Process.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace Cards {

ErrorOr<NonnullRefPtr<GUI::Action>> make_cards_settings_action(GUI::Window* parent)
{
    auto action = GUI::Action::create(
        "&Cards Settings", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/games.png"sv)), [parent](auto&) {
            GUI::Process::spawn_or_show_error(parent, "/bin/GamesSettings"sv, Array { "--open-tab", "cards" });
        },
        parent);
    action->set_status_tip("Open the Game Settings for Cards"_string);
    return action;
}

CardGame::CardGame()
{
    auto background_color = Gfx::Color::from_string(Config::read_string("Games"sv, "Cards"sv, "BackgroundColor"sv));
    set_background_color(background_color.value_or(Color::from_rgb(0x008000)));
}

void CardGame::mark_intersecting_stacks_dirty(Cards::Card const& intersecting_card)
{
    for (auto& stack : stacks()) {
        if (intersecting_card.rect().intersects(stack->bounding_box()))
            update(stack->bounding_box());
    }

    update(intersecting_card.rect());
}

Gfx::IntRect CardGame::moving_cards_bounds() const
{
    if (!is_moving_cards())
        return {};

    // Note: This assumes that the cards are arranged in a line.
    return m_moving_cards.first()->rect().united(m_moving_cards.last()->rect());
}

ErrorOr<void> CardGame::pick_up_cards_from_stack(Cards::CardStack& stack, Gfx::IntPoint click_location, CardStack::MovementRule movement_rule)
{
    TRY(stack.add_all_grabbed_cards(click_location, m_moving_cards, movement_rule));
    m_moving_cards_source_stack = stack;
    return {};
}

RefPtr<CardStack> CardGame::find_stack_to_drop_on(CardStack::MovementRule movement_rule)
{
    auto bounds_to_check = moving_cards_bounds();

    RefPtr<CardStack> closest_stack;
    float closest_distance = FLT_MAX;

    for (auto& stack : stacks()) {
        if (stack == moving_cards_source_stack())
            continue;

        if (stack->bounding_box().intersects(bounds_to_check)
            && stack->is_allowed_to_push(moving_cards().at(0), moving_cards().size(), movement_rule)) {

            auto distance = bounds_to_check.center().distance_from(stack->bounding_box().center());
            if (distance < closest_distance) {
                closest_stack = stack;
                closest_distance = distance;
            }
        }
    }

    return closest_stack;
}

ErrorOr<void> CardGame::drop_cards_on_stack(Cards::CardStack& stack, CardStack::MovementRule movement_rule)
{
    VERIFY(stack.is_allowed_to_push(m_moving_cards.at(0), m_moving_cards.size(), movement_rule));
    for (auto& to_intersect : moving_cards()) {
        mark_intersecting_stacks_dirty(to_intersect);
        TRY(stack.push(to_intersect));
        (void)moving_cards_source_stack()->pop();
    }

    update(moving_cards_source_stack()->bounding_box());
    update(stack.bounding_box());

    return {};
}

void CardGame::clear_moving_cards()
{
    m_moving_cards_source_stack.clear();
    m_moving_cards.clear();
}

void CardGame::dump_layout() const
{
    dbgln("------------------------------");
    for (auto const& stack : stacks())
        dbgln("{}", stack);
}

void CardGame::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
{
    if (domain == "Games" && group == "Cards") {
        if (key == "BackgroundColor") {
            if (auto maybe_color = Gfx::Color::from_string(value); maybe_color.has_value())
                set_background_color(maybe_color.value());
            return;
        }
        if (key == "CardBackImage") {
            CardPainter::the().set_back_image_path(String::from_utf8(value).release_value_but_fixme_should_propagate_errors());
            update();
            return;
        }
        if (key == "CardFrontImages") {
            CardPainter::the().set_front_images_set_name(String::from_utf8(value).release_value_but_fixme_should_propagate_errors());
            update();
            return;
        }
    }
}

Gfx::Color CardGame::background_color() const
{
    return palette().color(background_role());
}

void CardGame::set_background_color(Gfx::Color color)
{
    auto new_palette = palette();
    new_palette.set_color(Gfx::ColorRole::Background, color);
    set_palette(new_palette);

    CardPainter::the().set_background_color(color);
}

void CardGame::preview_card(CardStack& stack, Gfx::IntPoint click_location)
{
    if (!stack.preview_card(click_location))
        return;

    m_previewed_card_stack = stack;
    update(stack.bounding_box());
}

void CardGame::clear_card_preview()
{
    VERIFY(m_previewed_card_stack);

    update(m_previewed_card_stack->bounding_box());
    m_previewed_card_stack->clear_card_preview();
    m_previewed_card_stack = nullptr;
}

}
