/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCards/Card.h>

using Cards::Card;

namespace Hearts {

enum class CardValue : uint8_t {
    Number_2,
    Number_3,
    Number_4,
    Number_5,
    Number_6,
    Number_7,
    Number_8,
    Number_9,
    Number_10,
    Jack,
    Queen,
    King,
    Ace
};

inline CardValue hearts_card_value(Card const& card)
{
    // Ace has a higher value than all other cards in Hearts
    if (card.rank() == Cards::Rank::Ace)
        return CardValue::Ace;
    else
        return static_cast<CardValue>(to_underlying(card.rank()) - 1);
}

inline uint8_t hearts_card_points(Card const& card)
{
    if (card.suit() == Cards::Suit::Hearts)
        return 1;
    else if (card.suit() == Cards::Suit::Spades && hearts_card_value(card) == CardValue::Queen)
        return 13;
    else
        return 0;
}

inline bool hearts_card_less(RefPtr<Card>& card1, RefPtr<Card>& card2)
{
    if (card1->suit() != card2->suit())
        return card1->suit() < card2->suit();
    else
        return hearts_card_value(*card1) < hearts_card_value(*card2);
}

}
