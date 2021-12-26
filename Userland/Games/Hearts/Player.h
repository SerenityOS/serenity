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

    NonnullRefPtrVector<Card> pick_cards_to_pass(PassingDirection);
    size_t pick_lead_card(Function<bool(Card&)>, Function<bool(Card&)>);
    Optional<size_t> pick_low_points_high_value_card(Optional<Card::Type> type = {});
    Optional<size_t> pick_lower_value_card(Card& other_card);
    Optional<size_t> pick_slightly_higher_value_card(Card& other_card);
    size_t pick_max_points_card();
    Optional<size_t> pick_specific_card(Card::Type type, CardValue value);
    size_t pick_last_card();
    bool has_card_of_type(Card::Type type);
    Vector<CardWithIndex> hand_sorted_by_points_and_value() const;

    void sort_hand() { quick_sort(hand, hearts_card_less); }
    void remove_cards(NonnullRefPtrVector<Card> const& cards);

    Vector<RefPtr<Card>> hand;
    Vector<RefPtr<Card>> cards_taken;
    Vector<int> scores;
    Gfx::IntPoint first_card_position;
    Gfx::IntPoint card_offset;
    Gfx::IntRect name_position;
    Gfx::TextAlignment name_alignment;
    Gfx::IntPoint taken_cards_target;
    String name;
    bool is_human { false };
};

}

template<>
struct AK::Formatter<Hearts::Player> : Formatter<FormatString> {
    void format(FormatBuilder& builder, Hearts::Player const& player)
    {
        builder.put_string(player.name);
    }
};
