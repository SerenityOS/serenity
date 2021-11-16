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
    enum Type {
        Invalid,
        Stock,
        Normal,
        Waste,
        Play,
        Foundation
    };

    enum MovementRule {
        Alternating,
        Same,
        Any,
    };

    CardStack();
    CardStack(const Gfx::IntPoint& position, Type type);
    CardStack(const Gfx::IntPoint& position, Type type, NonnullRefPtr<CardStack> associated_stack);

    bool is_empty() const { return m_stack.is_empty(); }
    bool is_focused() const { return m_focused; }
    Type type() const { return m_type; }
    const NonnullRefPtrVector<Card>& stack() const { return m_stack; }
    size_t count() const { return m_stack.size(); }
    const Card& peek() const { return m_stack.last(); }
    Card& peek() { return m_stack.last(); }
    const Gfx::IntRect& bounding_box() const { return m_bounding_box; }

    void set_focused(bool focused) { m_focused = focused; }

    void push(NonnullRefPtr<Card> card);
    NonnullRefPtr<Card> pop();
    void move_to_stack(CardStack&);
    void rebound_cards();

    bool is_allowed_to_push(const Card&, size_t stack_size = 1, MovementRule movement_rule = Alternating) const;
    void add_all_grabbed_cards(const Gfx::IntPoint& click_location, NonnullRefPtrVector<Card>& grabbed, MovementRule movement_rule = Alternating);
    void draw(GUI::Painter&, const Gfx::Color& background_color);
    void clear();

private:
    struct StackRules {
        uint8_t shift_x { 0 };
        uint8_t shift_y { 0 };
        uint8_t step { 1 };
        uint8_t shift_y_upside_down { 0 };
    };

    constexpr StackRules rules_for_type(Type stack_type)
    {
        switch (stack_type) {
        case Foundation:
            return { 2, 1, 4, 1 };
        case Normal:
            return { 0, 20, 1, 3 };
        case Stock:
            return { 2, 1, 8, 1 };
        case Waste:
            return { 0, 0, 1, 0 };
        case Play:
            return { 20, 0, 1, 0 };
        default:
            return {};
        }
    }

    void calculate_bounding_box();

    RefPtr<CardStack> m_associated_stack;
    NonnullRefPtrVector<Card> m_stack;
    Vector<Gfx::IntPoint> m_stack_positions;
    Gfx::IntPoint m_position;
    Gfx::IntRect m_bounding_box;
    Type m_type { Invalid };
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

        for (const auto& card : stack.stack()) {
            cards.appendff("{}{}", (first_card ? "" : " "), card);
            first_card = false;
        }

        return Formatter<FormatString>::format(builder, "{:<10} {:>16}: {}", type, stack.bounding_box(), cards.build());
    }
};
