/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Card.h"
#include <AK/Vector.h>

namespace Solitaire {

class CardStack final {
public:
    enum Type {
        Invalid,
        Stock,
        Normal,
        Waste,
        Foundation
    };

    CardStack();
    CardStack(const Gfx::IntPoint& position, Type type);

    bool is_empty() const { return m_stack.is_empty(); }
    bool is_focused() const { return m_focused; }
    Type type() const { return m_type; }
    size_t count() const { return m_stack.size(); }
    const Card& peek() const { return m_stack.last(); }
    Card& peek() { return m_stack.last(); }
    const Gfx::IntRect& bounding_box() const { return m_bounding_box; }

    void set_focused(bool focused) { m_focused = focused; }

    void push(NonnullRefPtr<Card> card);
    NonnullRefPtr<Card> pop();
    void rebound_cards();

    bool is_allowed_to_push(const Card&) const;
    void add_all_grabbed_cards(const Gfx::IntPoint& click_location, NonnullRefPtrVector<Card>& grabbed);
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
        case Waste:
            return { 2, 1, 8, 1 };
        default:
            return {};
        }
    }

    void calculate_bounding_box();

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
