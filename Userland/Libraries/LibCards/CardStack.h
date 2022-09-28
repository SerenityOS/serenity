/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Card.h"
#include <AK/Format.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>

namespace Cards {

class CardStack final : public RefCounted<CardStack> {
public:
    enum class Type {
        Invalid,
        Stock,
        Normal,
        Waste,
        Play,
        Foundation
    };

    enum class MovementRule {
        Alternating,
        Same,
        Any,
    };

    CardStack();
    CardStack(Gfx::IntPoint const& position, Type type, RefPtr<CardStack> covered_stack = nullptr);

    bool is_empty() const { return m_stack.is_empty(); }
    bool is_focused() const { return m_focused; }
    Type type() const { return m_type; }
    NonnullRefPtrVector<Card> const& stack() const { return m_stack; }
    size_t count() const { return m_stack.size(); }
    Card const& peek() const { return m_stack.last(); }
    Card& peek() { return m_stack.last(); }
    Gfx::IntRect const& bounding_box() const { return m_bounding_box; }

    void set_focused(bool focused) { m_focused = focused; }

    void push(NonnullRefPtr<Card> card);
    NonnullRefPtr<Card> pop();
    void move_to_stack(CardStack&);
    void rebound_cards();

    bool is_allowed_to_push(Card const&, size_t stack_size = 1, MovementRule movement_rule = MovementRule::Alternating) const;
    void add_all_grabbed_cards(Gfx::IntPoint const& click_location, NonnullRefPtrVector<Card>& grabbed, MovementRule movement_rule = MovementRule::Alternating);
    void paint(GUI::Painter&, Gfx::Color const& background_color);
    void clear();

private:
    struct StackRules {
        uint8_t shift_x { 0 };
        uint8_t shift_y { 0 };
        uint8_t step { 1 };
        uint8_t shift_y_upside_down { 0 };
    };

    static constexpr StackRules rules_for_type(Type stack_type)
    {
        switch (stack_type) {
        case Type::Foundation:
            return { 2, 1, 4, 1 };
        case Type::Normal:
            return { 0, 20, 1, 3 };
        case Type::Stock:
            return { 2, 1, 8, 1 };
        case Type::Waste:
            return { 0, 0, 1, 0 };
        case Type::Play:
            return { 20, 0, 1, 0 };
        default:
            return {};
        }
    }

    void calculate_bounding_box();

    // An optional stack that this stack is painted on top of.
    // eg, in Solitaire the Play stack is positioned over the Waste stack.
    RefPtr<CardStack> m_covered_stack;

    NonnullRefPtrVector<Card> m_stack;
    Vector<Gfx::IntPoint> m_stack_positions;
    Gfx::IntPoint m_position;
    Gfx::IntRect m_bounding_box;
    Type m_type { Type::Invalid };
    StackRules m_rules;
    bool m_focused { false };
    Gfx::IntRect m_base;
};

}

template<>
struct AK::Formatter<Cards::CardStack> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Cards::CardStack const& stack)
    {
        StringView type;

        switch (stack.type()) {
        case Cards::CardStack::Type::Stock:
            type = "Stock"sv;
            break;
        case Cards::CardStack::Type::Normal:
            type = "Normal"sv;
            break;
        case Cards::CardStack::Type::Foundation:
            type = "Foundation"sv;
            break;
        case Cards::CardStack::Type::Waste:
            type = "Waste"sv;
            break;
        case Cards::CardStack::Type::Play:
            type = "Play"sv;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        StringBuilder cards;
        bool first_card = true;

        for (auto const& card : stack.stack()) {
            cards.appendff("{}{}", (first_card ? "" : " "), card);
            first_card = false;
        }

        return Formatter<FormatString>::format(builder, "{:<10} {:>16}: {}"sv, type, stack.bounding_box(), cards.build());
    }
};
