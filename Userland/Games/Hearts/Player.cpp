/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Player.h"
#include "Helpers.h"

namespace Hearts {

size_t Player::pick_low_points_low_value_card()
{
    int min_points = -1;
    int min_value = -1;
    int card_index = -1;
    for (size_t i = 0; i < hand.size(); i++) {
        auto& card = hand[i];
        if (card.is_null())
            continue;
        auto points = hearts_card_points(*card);
        auto value = hearts_card_value(*card);
        if (min_points != -1 && (points > min_points || static_cast<int>(value) > min_value))
            continue;
        min_points = points;
        min_value = static_cast<int>(value);
        card_index = i;
    }
    VERIFY(card_index != -1);
    return card_index;
}

Optional<size_t> Player::pick_low_points_high_value_card(Optional<Card::Type> type)
{
    int min_points = -1;
    Optional<size_t> card_index;
    for (ssize_t i = hand.size() - 1; i >= 0; i--) {
        auto& card = hand[i];
        if (card.is_null())
            continue;
        if (type.has_value() && card->type() != type.value())
            continue;
        auto points = hearts_card_points(*card);
        if (min_points == -1 || points < min_points) {
            min_points = points;
            card_index = i;
        }
    }
    VERIFY(card_index.has_value() || type.has_value());
    return card_index;
}

Optional<size_t> Player::pick_lower_value_card(Card& other_card)
{
    for (ssize_t i = hand.size() - 1; i >= 0; i--) {
        auto& card = hand[i];
        if (card.is_null())
            continue;
        if (card->type() == other_card.type() && hearts_card_value(*card) < hearts_card_value(other_card))
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
        if (card->type() == other_card.type() && hearts_card_value(*card) > hearts_card_value(other_card))
            return i;
    }
    return {};
}

size_t Player::pick_max_points_card()
{
    auto queen_of_spades_maybe = pick_specific_card(Card::Type::Spades, CardValue::Queen);
    if (queen_of_spades_maybe.has_value())
        return queen_of_spades_maybe.value();
    if (has_card_of_type(Card::Type::Hearts))
        return pick_last_card();
    return pick_low_points_high_value_card().value();
}

Optional<size_t> Player::pick_specific_card(Card::Type type, CardValue value)
{
    for (size_t i = 0; i < hand.size(); i++) {
        auto& card = hand[i];
        if (card.is_null())
            continue;
        if (card->type() == type && hearts_card_value(*card) == value)
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

bool Player::has_card_of_type(Card::Type type)
{
    auto matching_card = hand.first_matching([&](auto const& other_card) {
        return !other_card.is_null() && other_card->type() == type;
    });
    return matching_card.has_value();
}

}
