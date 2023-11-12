/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CardStack.h"

namespace Cards {

CardStack::CardStack()
    : m_position({ 0, 0 })
    , m_type(Type::Invalid)
    , m_base(m_position, { Card::width, Card::height })
{
}

CardStack::CardStack(Gfx::IntPoint position, Type type, RefPtr<CardStack> covered_stack)
    : m_covered_stack(move(covered_stack))
    , m_position(position)
    , m_type(type)
    , m_rules(rules_for_type(type))
    , m_base(m_position, { Card::width, Card::height })
{
    VERIFY(type != Type::Invalid);
    calculate_bounding_box();
}

void CardStack::clear()
{
    m_stack.clear();
    m_stack_positions.clear();
}

void CardStack::paint(GUI::Painter& painter, Gfx::Color background_color)
{
    auto background_markings_color = (background_color.luminosity() > 64) ? Color(0, 0, 0, 128) : Color(255, 255, 255, 128);

    auto draw_background_if_empty = [&]() {
        size_t number_of_moving_cards = 0;
        for (auto const& card : m_stack)
            number_of_moving_cards += card->is_moving() ? 1 : 0;

        if (m_covered_stack && !m_covered_stack->is_empty())
            return false;
        if (!is_empty() && (m_stack.size() != number_of_moving_cards))
            return false;

        auto paint_rect = m_base;
        painter.fill_rect_with_rounded_corners(paint_rect, background_markings_color, Card::card_radius);
        paint_rect.shrink(2, 2);

        if (m_highlighted) {
            auto background_complement = background_color.xored(Color::White);
            painter.fill_rect_with_rounded_corners(paint_rect, background_complement, Card::card_radius - 1);
            paint_rect.shrink(4, 4);
        }

        painter.fill_rect_with_rounded_corners(paint_rect, background_color, Card::card_radius - 1);
        return true;
    };

    switch (m_type) {
    case Type::Stock:
        if (draw_background_if_empty()) {
            auto stock_highlight_color = (background_color.luminosity() < 196) ? Color(255, 255, 255, 128) : Color(0, 0, 0, 64);
            painter.fill_rect(m_base.shrunken(Card::width / 4, Card::height / 4), stock_highlight_color);
            painter.fill_rect(m_base.shrunken(Card::width / 2, Card::height / 2), background_color);
        }
        break;
    case Type::Foundation:
        if (draw_background_if_empty()) {
            for (int y = 0; y < (m_base.height() - 4) / 8; ++y) {
                for (int x = 0; x < (m_base.width() - 4) / 5; ++x) {
                    painter.draw_rect({ 4 + m_base.x() + x * 5, 4 + m_base.y() + y * 8, 1, 1 }, background_markings_color);
                }
            }
        }
        break;
    case Type::Play:
    case Type::Normal:
        draw_background_if_empty();
        break;
    case Type::Waste:
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    if (is_empty())
        return;

    if (m_rules.shift_x == 0 && m_rules.shift_y == 0) {
        auto& card = peek();
        card.paint(painter);
        return;
    }

    RefPtr<Card> previewed_card;

    for (size_t i = 0; i < m_stack.size(); ++i) {
        if (auto& card = m_stack[i]; !card->is_moving()) {
            if (card->is_previewed()) {
                VERIFY(!previewed_card);
                previewed_card = card;
                continue;
            }

            auto highlighted = m_highlighted && (i == m_stack.size() - 1);
            card->clear_and_paint(painter, Gfx::Color::Transparent, highlighted);
        }
    }

    if (previewed_card)
        previewed_card->clear_and_paint(painter, Gfx::Color::Transparent, false);
}

void CardStack::rebound_cards()
{
    VERIFY(m_stack_positions.size() == m_stack.size());

    size_t card_index = 0;
    for (auto& card : m_stack)
        card->set_position(m_stack_positions.at(card_index++));
}

ErrorOr<void> CardStack::add_all_grabbed_cards(Gfx::IntPoint click_location, Vector<NonnullRefPtr<Card>>& grabbed, MovementRule movement_rule)
{
    VERIFY(grabbed.is_empty());

    if (m_type != Type::Normal) {
        auto& top_card = peek();
        if (top_card.rect().contains(click_location)) {
            top_card.set_moving(true);
            TRY(grabbed.try_append(top_card));
        }
        return {};
    }

    RefPtr<Card> last_intersect;

    for (auto& card : m_stack) {
        if (card->rect().contains(click_location)) {
            if (card->is_upside_down())
                continue;

            last_intersect = card;
        } else if (!last_intersect.is_null()) {
            if (grabbed.is_empty()) {
                TRY(grabbed.try_append(*last_intersect));
                last_intersect->set_moving(true);
            }

            if (card->is_upside_down()) {
                grabbed.clear();
                return {};
            }

            card->set_moving(true);
            TRY(grabbed.try_append(card));
        }
    }

    if (grabbed.is_empty() && !last_intersect.is_null()) {
        TRY(grabbed.try_append(*last_intersect));
        last_intersect->set_moving(true);
    }

    // verify valid stack
    bool valid_stack = true;
    uint8_t last_value;
    Color last_color;
    for (size_t i = 0; i < grabbed.size(); i++) {
        auto& card = grabbed.at(i);
        if (i != 0) {
            bool color_match;
            switch (movement_rule) {
            case MovementRule::Alternating:
                color_match = card->color() != last_color;
                break;
            case MovementRule::Same:
                color_match = card->color() == last_color;
                break;
            case MovementRule::Any:
                color_match = true;
                break;
            }

            if (!color_match || to_underlying(card->rank()) != last_value - 1) {
                valid_stack = false;
                break;
            }
        }
        last_value = to_underlying(card->rank());
        last_color = card->color();
    }

    if (!valid_stack) {
        for (auto& card : grabbed) {
            card->set_moving(false);
        }
        grabbed.clear();
    }

    return {};
}

void CardStack::update_disabled_cards(MovementRule movement_rule)
{
    if (m_stack.is_empty())
        return;

    for (auto& card : m_stack)
        card->set_disabled(false);

    Optional<size_t> last_valid_card = {};
    uint8_t last_rank;
    Color last_color;
    for (size_t idx = m_stack.size(); idx > 0; idx--) {
        auto i = idx - 1;
        auto card = m_stack[i];
        if (card->is_upside_down()) {
            if (!last_valid_card.has_value())
                last_valid_card = i + 1;
            break;
        }

        if (i != m_stack.size() - 1) {
            bool color_valid;
            switch (movement_rule) {
            case MovementRule::Alternating:
                color_valid = card->color() != last_color;
                break;
            case MovementRule::Same:
                color_valid = card->color() == last_color;
                break;
            case MovementRule::Any:
                color_valid = true;
                break;
            }

            if (!color_valid || to_underlying(card->rank()) != last_rank + 1) {
                last_valid_card = i + 1;
                break;
            }
        }

        last_rank = to_underlying(card->rank());
        last_color = card->color();
    }

    if (!last_valid_card.has_value())
        return;

    for (size_t i = 0; i < last_valid_card.value(); i++)
        m_stack[i]->set_disabled(true);
}

bool CardStack::is_allowed_to_push(Card const& card, size_t stack_size, MovementRule movement_rule) const
{
    if (m_type == Type::Stock || m_type == Type::Waste || m_type == Type::Play)
        return false;

    if (m_type == Type::Normal && is_empty()) {
        // FIXME: proper solution for this
        if (movement_rule == MovementRule::Alternating) {
            return card.rank() == Rank::King;
        }
        return true;
    }

    if (m_type == Type::Foundation && is_empty())
        return card.rank() == Rank::Ace;

    if (!is_empty()) {
        auto const& top_card = peek();
        if (top_card.is_upside_down())
            return false;

        if (m_type == Type::Foundation) {
            // Prevent player from dragging an entire stack of cards to the foundation stack
            if (stack_size > 1)
                return false;
            return top_card.suit() == card.suit() && m_stack.size() == to_underlying(card.rank());
        }
        if (m_type == Type::Normal) {
            bool color_match;
            switch (movement_rule) {
            case MovementRule::Alternating:
                color_match = card.color() != top_card.color();
                break;
            case MovementRule::Same:
                color_match = card.color() == top_card.color();
                break;
            case MovementRule::Any:
                color_match = true;
                break;
            }

            return color_match && to_underlying(top_card.rank()) == to_underlying(card.rank()) + 1;
        }

        VERIFY_NOT_REACHED();
    }

    return true;
}

bool CardStack::preview_card(Gfx::IntPoint click_location)
{
    RefPtr<Card> last_intersect;

    for (auto& card : m_stack) {
        if (!card->rect().contains(click_location))
            continue;
        if (card->is_upside_down())
            continue;

        last_intersect = card;
    }

    if (!last_intersect)
        return false;

    last_intersect->set_previewed(true);
    return true;
}

void CardStack::clear_card_preview()
{
    for (auto& card : m_stack)
        card->set_previewed(false);
}

bool CardStack::make_top_card_visible()
{
    if (is_empty())
        return false;

    auto& top_card = peek();
    if (top_card.is_upside_down()) {
        top_card.set_upside_down(false);
        return true;
    }

    return false;
}

ErrorOr<void> CardStack::push(NonnullRefPtr<Card> card)
{
    auto top_most_position = m_stack_positions.is_empty() ? m_position : m_stack_positions.last();

    if (!m_stack.is_empty() && m_stack.size() % m_rules.step == 0) {
        if (peek().is_upside_down())
            top_most_position.translate_by(m_rules.shift_x, m_rules.shift_y_upside_down);
        else
            top_most_position.translate_by(m_rules.shift_x, m_rules.shift_y);
    }

    if (m_type == Type::Stock)
        card->set_upside_down(true);

    card->set_position(top_most_position);

    TRY(m_stack.try_append(card));
    TRY(m_stack_positions.try_append(top_most_position));
    calculate_bounding_box();
    return {};
}

NonnullRefPtr<Card> CardStack::pop()
{
    auto card = m_stack.take_last();

    calculate_bounding_box();
    if (m_type == Type::Stock)
        card->set_upside_down(false);

    m_stack_positions.take_last();
    return card;
}

ErrorOr<void> CardStack::take_all(CardStack& stack)
{
    while (!m_stack.is_empty()) {
        auto card = m_stack.take_first();
        m_stack_positions.take_first();
        TRY(stack.push(move(card)));
    }

    calculate_bounding_box();
    return {};
}

void CardStack::calculate_bounding_box()
{
    m_bounding_box = Gfx::IntRect(m_position, { Card::width, Card::height });

    if (m_stack.is_empty())
        return;

    uint16_t width = 0;
    uint16_t height = 0;
    size_t card_position = 0;
    for (auto& card : m_stack) {
        if (card_position % m_rules.step == 0 && card_position != 0) {
            if (card->is_upside_down()) {
                width += m_rules.shift_x;
                height += m_rules.shift_y_upside_down;
            } else {
                width += m_rules.shift_x;
                height += m_rules.shift_y;
            }
        }
        ++card_position;
    }

    m_bounding_box.set_size(Card::width + width, Card::height + height);
}

}
