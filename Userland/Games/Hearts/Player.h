/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Helpers.h"
#include <LibCards/Card.h>

using Cards::Card;

namespace Hearts {

struct Player {
    AK_MAKE_NONMOVABLE(Player);

public:
    Player()
    {
    }

    size_t pick_low_points_low_value_card();
    Optional<size_t> pick_low_points_high_value_card(Optional<Card::Type> type = {});
    Optional<size_t> pick_lower_value_card(Card& other_card);
    Optional<size_t> pick_slightly_higher_value_card(Card& other_card);
    size_t pick_max_points_card();
    Optional<size_t> pick_specific_card(Card::Type type, CardValue value);
    size_t pick_last_card();
    bool has_card_of_type(Card::Type type);

    Vector<RefPtr<Card>> hand;
    Vector<RefPtr<Card>> cards_taken;
    Gfx::IntPoint first_card_position;
    Gfx::IntPoint card_offset;
    Gfx::IntRect name_position;
    Gfx::TextAlignment name_alignment;
    Gfx::IntPoint taken_cards_target;
    String name;
};

}

template<>
struct AK::Formatter<Hearts::Player> : Formatter<FormatString> {
    void format(FormatBuilder& builder, Hearts::Player const& player)
    {
        builder.put_string(player.name);
    }
};
