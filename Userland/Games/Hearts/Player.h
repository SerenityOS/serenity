/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Helpers.h"
#include <AK/QuickSort.h>
#include <LibCards/Card.h>

using Cards::Card;

namespace Hearts {

enum class PassingDirection {
    Left,
    Right,
    Across
};

struct CardWithIndex {
    NonnullRefPtr<Card> card;
    size_t index;
};

struct Player {
    AK_MAKE_NONMOVABLE(Player);

public:
    Player()
    {
    }

    Vector<NonnullRefPtr<Card>> pick_cards_to_pass(PassingDirection);
    size_t pick_lead_card(Function<bool(Card&)>, Function<bool(Card&)>);
    Optional<size_t> pick_low_points_high_value_card(Optional<Cards::Suit> suit = {});
    Optional<size_t> pick_lower_value_card(Card& other_card);
    Optional<size_t> pick_slightly_higher_value_card(Card& other_card);
    size_t pick_max_points_card(Function<bool(Card&)>);
    Optional<size_t> pick_specific_card(Cards::Suit suit, CardValue value);
    size_t pick_last_card();
    bool has_card_of_suit(Cards::Suit suit);
    Vector<CardWithIndex> hand_sorted_by_fn(bool (*)(CardWithIndex&, CardWithIndex&)) const;

    void sort_hand() { quick_sort(hand, hearts_card_less); }
    void remove_cards(Vector<NonnullRefPtr<Card>> const& cards);

    Vector<RefPtr<Card>> hand;
    Vector<RefPtr<Card>> cards_taken;
    Vector<int> scores;
    Gfx::IntPoint first_card_position;
    Gfx::IntPoint card_offset;
    Gfx::IntRect name_position;
    Gfx::TextAlignment name_alignment;
    Gfx::IntPoint taken_cards_target;
    ByteString name;
    bool is_human { false };
};

}

template<>
struct AK::Formatter<Hearts::Player> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Hearts::Player const& player)
    {
        return builder.put_string(player.name);
    }
};
