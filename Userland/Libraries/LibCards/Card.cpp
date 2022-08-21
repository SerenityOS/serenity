/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Card.h"
#include <LibCards/CardPainter.h>

namespace Cards {

Card::Card(Suit suit, Rank rank)
    : m_rect(Gfx::IntRect({}, { width, height }))
    , m_suit(suit)
    , m_rank(rank)
{
    VERIFY(to_underlying(rank) < card_count);
}

void Card::draw(GUI::Painter& painter) const
{
    auto& card_painter = CardPainter::the();
    auto bitmap = [&]() {
        if (m_inverted)
            return m_upside_down ? card_painter.card_back_inverted() : card_painter.card_front_inverted(m_suit, m_rank);

        return m_upside_down ? card_painter.card_back() : card_painter.card_front(m_suit, m_rank);
    }();
    painter.blit(position(), bitmap, bitmap->rect());
}

void Card::clear(GUI::Painter& painter, Color const& background_color) const
{
    painter.fill_rect({ old_position(), { width, height } }, background_color);
}

void Card::save_old_position()
{
    m_old_position = m_rect.location();
    m_old_position_valid = true;
}

void Card::clear_and_draw(GUI::Painter& painter, Color const& background_color)
{
    if (is_old_position_valid())
        clear(painter, background_color);

    draw(painter);
    save_old_position();
}

}
