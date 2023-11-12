/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Card.h"
#include <AK/Random.h>
#include <LibCards/CardPainter.h>

namespace Cards {

Card::Card(Suit suit, Rank rank)
    : m_rect(Gfx::IntRect({}, { width, height }))
    , m_suit(suit)
    , m_rank(rank)
{
    VERIFY(to_underlying(rank) < card_count);
}

void Card::paint(GUI::Painter& painter, bool highlighted) const
{
    VERIFY(!(highlighted && m_disabled));

    auto& card_painter = CardPainter::the();
    auto bitmap = [&]() {
        if (m_inverted)
            return m_upside_down ? card_painter.card_back_inverted() : card_painter.card_front_inverted(m_suit, m_rank);
        if (highlighted) {
            VERIFY(!m_upside_down);
            return card_painter.card_front_highlighted(m_suit, m_rank);
        }
        if (m_disabled) {
            return m_upside_down ? card_painter.card_back_disabled() : card_painter.card_front_disabled(m_suit, m_rank);
        }
        return m_upside_down ? card_painter.card_back() : card_painter.card_front(m_suit, m_rank);
    }();
    painter.blit(position(), bitmap, bitmap->rect());
}

void Card::clear(GUI::Painter& painter, Color background_color) const
{
    painter.fill_rect({ old_position(), { width, height } }, background_color);
}

void Card::save_old_position()
{
    m_old_position = m_rect.location();
    m_old_position_valid = true;
}

void Card::clear_and_paint(GUI::Painter& painter, Color background_color, bool highlighted)
{
    if (is_old_position_valid())
        clear(painter, background_color);

    paint(painter, highlighted);
    save_old_position();
}

ErrorOr<Vector<NonnullRefPtr<Card>>> create_standard_deck(Shuffle shuffle)
{
    return create_deck(1, 1, 1, 1, shuffle);
}

ErrorOr<Vector<NonnullRefPtr<Card>>> create_deck(unsigned full_club_suit_count, unsigned full_diamond_suit_count, unsigned full_heart_suit_count, unsigned full_spade_suit_count, Shuffle shuffle)
{
    Vector<NonnullRefPtr<Card>> deck;
    TRY(deck.try_ensure_capacity(Card::card_count * (full_club_suit_count + full_diamond_suit_count + full_heart_suit_count + full_spade_suit_count)));

    auto add_cards_for_suit = [&deck](Cards::Suit suit, unsigned number_of_suits) -> ErrorOr<void> {
        for (auto i = 0u; i < number_of_suits; ++i) {
            for (auto rank = 0; rank < Card::card_count; ++rank) {
                deck.unchecked_append(TRY(Card::try_create(suit, static_cast<Cards::Rank>(rank))));
            }
        }
        return {};
    };

    TRY(add_cards_for_suit(Cards::Suit::Clubs, full_club_suit_count));
    TRY(add_cards_for_suit(Cards::Suit::Diamonds, full_diamond_suit_count));
    TRY(add_cards_for_suit(Cards::Suit::Hearts, full_heart_suit_count));
    TRY(add_cards_for_suit(Cards::Suit::Spades, full_spade_suit_count));

    if (shuffle == Shuffle::Yes)
        AK::shuffle(deck);

    return deck;
}

}
