/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
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

void Card::paint(GUI::Painter& painter) const
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

void Card::clear_and_paint(GUI::Painter& painter, Color const& background_color)
{
    if (is_old_position_valid())
        clear(painter, background_color);

    paint(painter);
    save_old_position();
}

NonnullRefPtrVector<Card> create_standard_deck(Shuffle shuffle)
{
    return create_deck(1, 1, 1, 1, shuffle);
}

NonnullRefPtrVector<Card> create_deck(unsigned full_club_suit_count, unsigned full_diamond_suit_count, unsigned full_heart_suit_count, unsigned full_spade_suit_count, Shuffle shuffle)
{
    NonnullRefPtrVector<Card> deck;
    deck.ensure_capacity(Card::card_count * (full_club_suit_count + full_diamond_suit_count + full_heart_suit_count + full_spade_suit_count));

    auto add_cards_for_suit = [&deck](Cards::Suit suit, unsigned number_of_suits) {
        for (auto i = 0u; i < number_of_suits; ++i) {
            for (auto rank = 0; rank < Card::card_count; ++rank) {
                deck.append(Card::construct(suit, static_cast<Cards::Rank>(rank)));
            }
        }
    };

    add_cards_for_suit(Cards::Suit::Clubs, full_club_suit_count);
    add_cards_for_suit(Cards::Suit::Diamonds, full_diamond_suit_count);
    add_cards_for_suit(Cards::Suit::Hearts, full_heart_suit_count);
    add_cards_for_suit(Cards::Suit::Spades, full_spade_suit_count);

    if (shuffle == Shuffle::Yes)
        shuffle_deck(deck);

    return deck;
}

void shuffle_deck(NonnullRefPtrVector<Card>& deck)
{
    auto iteration_count = deck.size() * 4;
    for (auto i = 0u; i < iteration_count; ++i)
        deck.append(deck.take(get_random_uniform(deck.size())));
}

}
