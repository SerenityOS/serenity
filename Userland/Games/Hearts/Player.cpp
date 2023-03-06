/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Player.h"
#include "Helpers.h"
#include <AK/Debug.h>
#include <AK/QuickSort.h>

namespace Hearts {

static bool compare_card_value(CardWithIndex& cwi1, CardWithIndex& cwi2)
{
    return hearts_card_value(*cwi2.card) < hearts_card_value(*cwi1.card);
}

static bool compare_card_points_and_value(CardWithIndex& cwi1, CardWithIndex& cwi2)
{
    if (hearts_card_points(*cwi2.card) < hearts_card_points(*cwi1.card))
        return true;
    if (hearts_card_points(*cwi1.card) == hearts_card_points(*cwi2.card) && hearts_card_value(*cwi2.card) < hearts_card_value(*cwi1.card))
        return true;
    return false;
}

Vector<NonnullRefPtr<Card>> Player::pick_cards_to_pass(PassingDirection)
{
    auto sorted_hand = hand_sorted_by_fn(compare_card_value);
    Vector<NonnullRefPtr<Card>> cards;
    cards.append(*sorted_hand[0].card);
    cards.append(*sorted_hand[1].card);
    cards.append(*sorted_hand[2].card);
    return cards;
}

Vector<CardWithIndex> Player::hand_sorted_by_fn(bool (*fn)(CardWithIndex&, CardWithIndex&)) const
{
    Vector<CardWithIndex> sorted_hand;
    for (size_t i = 0; i < hand.size(); i++) {
        auto& card = hand[i];
        if (card)
            sorted_hand.empend(*card, i);
    }
    quick_sort(sorted_hand, fn);
    return sorted_hand;
}

size_t Player::pick_lead_card(Function<bool(Card&)> valid_play, Function<bool(Card&)> prefer_card)
{
    auto sorted_hand = hand_sorted_by_fn(compare_card_points_and_value);

    if constexpr (HEARTS_DEBUG) {
        dbgln("Sorted hand:");
        for (auto& cwi : sorted_hand)
            dbgln("{}", *cwi.card);
        dbgln("----");
    }

    ssize_t last_index = -1;
    for (auto& cwi : sorted_hand) {
        if (!valid_play(*cwi.card))
            continue;
        if (prefer_card(*cwi.card)) {
            dbgln_if(HEARTS_DEBUG, "Preferring card {}", *cwi.card);
            return cwi.index;
        }
        last_index = cwi.index;
    }
    return last_index;
}

Optional<size_t> Player::pick_low_points_high_value_card(Optional<Cards::Suit> suit)
{
    auto sorted_hand = hand_sorted_by_fn(compare_card_value);
    int min_points = -1;
    Optional<size_t> card_index;
    for (auto& cwi : sorted_hand) {
        if (suit.has_value() && cwi.card->suit() != suit.value())
            continue;
        auto points = hearts_card_points(*cwi.card);
        if (min_points == -1 || points < min_points) {
            min_points = points;
            card_index = cwi.index;
        }
    }
    VERIFY(card_index.has_value() || suit.has_value());
    return card_index;
}

Optional<size_t> Player::pick_lower_value_card(Card& other_card)
{
    for (ssize_t i = hand.size() - 1; i >= 0; i--) {
        auto& card = hand[i];
        if (card.is_null())
            continue;
        if (card->suit() == other_card.suit() && hearts_card_value(*card) < hearts_card_value(other_card))
            return i;
    }
    return {};
}

Optional<size_t> Player::pick_slightly_higher_value_card(Card& other_card)
{
    for (size_t i = 0; i < hand.size(); i++) {
        auto& card = hand[i];
        if (card.is_null())
            continue;
        if (card->suit() == other_card.suit() && hearts_card_value(*card) > hearts_card_value(other_card))
            return i;
    }
    return {};
}

size_t Player::pick_max_points_card(Function<bool(Card&)> ignore_card)
{
    auto queen_of_spades_maybe = pick_specific_card(Cards::Suit::Spades, CardValue::Queen);
    if (queen_of_spades_maybe.has_value())
        return queen_of_spades_maybe.value();
    if (has_card_of_suit(Cards::Suit::Hearts)) {
        auto highest_hearts_card_index = pick_last_card();
        auto& card = hand[highest_hearts_card_index];
        if (!ignore_card(*card))
            return highest_hearts_card_index;
    }
    return pick_low_points_high_value_card().value();
}

Optional<size_t> Player::pick_specific_card(Cards::Suit suit, CardValue value)
{
    for (size_t i = 0; i < hand.size(); i++) {
        auto& card = hand[i];
        if (card.is_null())
            continue;
        if (card->suit() == suit && hearts_card_value(*card) == value)
            return i;
    }
    return {};
}

size_t Player::pick_last_card()
{
    for (ssize_t i = hand.size() - 1; i >= 0; i--) {
        auto& card = hand[i];
        if (card.is_null())
            continue;
        return i;
    }
    VERIFY_NOT_REACHED();
}

bool Player::has_card_of_suit(Cards::Suit suit)
{
    auto matching_card = hand.first_matching([&](auto const& other_card) {
        return !other_card.is_null() && other_card->suit() == suit;
    });
    return matching_card.has_value();
}

void Player::remove_cards(Vector<NonnullRefPtr<Card>> const& cards)
{
    for (auto& card : cards) {
        hand.remove_first_matching([&card](auto& other_card) {
            return other_card == card;
        });
    }
}

}
