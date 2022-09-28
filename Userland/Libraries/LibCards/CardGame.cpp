/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
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
