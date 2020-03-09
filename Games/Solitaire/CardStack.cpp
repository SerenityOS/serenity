/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "CardStack.h"

CardStack::CardStack()
    : m_position({ 0, 0 })
    , m_type(Invalid)
    , m_shift_x(0)
    , m_shift_y(0)
    , m_step(1)
    , m_base(m_position, { Card::width, Card::height })
{
}

CardStack::CardStack(const Gfx::Point& position, Type type, uint8_t shift_x, uint8_t shift_y, uint8_t step)
    : m_position(position)
    , m_type(type)
    , m_shift_x(shift_x)
    , m_shift_y(shift_y)
    , m_step(step)
    , m_base(m_position, { Card::width, Card::height })
{
    ASSERT(step && type != Invalid);
    calculate_bounding_box();
}

void CardStack::clear()
{
    m_stack.clear();
    m_stack_positions.clear();
}

void CardStack::draw(GUI::Painter& painter, const Gfx::Color& background_color)
{
    switch (m_type) {
    case Stock:
        if (is_empty()) {
            painter.fill_rect(m_base.shrunken(Card::width / 4, Card::height / 4), background_color.lightened(1.5));
            painter.fill_rect(m_base.shrunken(Card::width / 2, Card::height / 2), background_color);
            painter.draw_rect(m_base, Color::Black);
        }
        break;
    case Foundation:
        if (is_empty() || (m_stack.size() == 1 && peek().is_moving())) {
            painter.draw_rect(m_base, Color::DarkGray);
            for (int y = 0; y < (m_base.height() - 4) / 8; ++y) {
                for (int x = 0; x < (m_base.width() - 4) / 5; ++x) {
                    painter.draw_rect({ 4 + m_base.x() + x * 5, 4 + m_base.y() + y * 8, 1, 1 }, Color::DarkGray);
                }
            }
        }
        break;
    case Waste:
        if (is_empty() || (m_stack.size() == 1 && peek().is_moving()))
            painter.draw_rect(m_base, Color::DarkGray);
        break;
    case Normal:
        painter.draw_rect(m_base, Color::DarkGray);
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    if (is_empty())
        return;

    if (m_shift_x == 0 && m_shift_y == 0) {
        auto& card = peek();
        card.draw(painter);
        return;
    }

    for (auto& card : m_stack) {
        if (!card.is_moving())
            card.draw_complete(painter, background_color);
    }

    m_dirty = false;
}
void CardStack::rebound_cards()
{
    ASSERT(m_stack_positions.size() == m_stack.size());

    size_t card_index = 0;
    for (auto& card : m_stack)
        card.set_position(m_stack_positions.at(card_index++));
}

void CardStack::add_all_grabbed_cards(const Gfx::Point& click_location, NonnullRefPtrVector<Card>& grabbed)
{
    ASSERT(grabbed.is_empty());

    if (m_type != Normal) {
        auto& top_card = peek();
        if (top_card.rect().contains(click_location)) {
            top_card.set_moving(true);
            grabbed.append(top_card);
        }
        return;
    }

    RefPtr<Card> last_intersect;

    for (auto& card : m_stack) {
        if (card.rect().contains(click_location)) {
            if (card.is_upside_down())
                continue;

            last_intersect = card;
        } else if (!last_intersect.is_null()) {
            if (grabbed.is_empty()) {
                grabbed.append(*last_intersect);
                last_intersect->set_moving(true);
            }

            if (card.is_upside_down()) {
                grabbed.clear();
                return;
            }

            card.set_moving(true);
            grabbed.append(card);
        }
    }

    if (grabbed.is_empty() && !last_intersect.is_null()) {
        grabbed.append(*last_intersect);
        last_intersect->set_moving(true);
    }
}

bool CardStack::is_allowed_to_push(const Card& card) const
{
    if (m_type == Stock || m_type == Waste)
        return false;

    if (m_type == Normal && is_empty())
        return card.value() == 12;

    if (m_type == Foundation && is_empty())
        return card.value() == 0;

    if (!is_empty()) {
        auto& top_card = peek();
        if (top_card.is_upside_down())
            return false;

        if (m_type == Foundation) {
            return top_card.type() == card.type() && m_stack.size() == card.value();
        } else if (m_type == Normal) {
            return top_card.color() != card.color() && top_card.value() == card.value() + 1;
        }

        ASSERT_NOT_REACHED();
    }

    return true;
}

void CardStack::push(NonnullRefPtr<Card> card)
{
    int size = m_stack.size();
    int ud_shift = (m_type == Normal) ? 3 : 1;
    auto top_most_position = m_stack_positions.is_empty() ? m_position : m_stack_positions.last();

    if (size && size % m_step == 0) {
        if (peek().is_upside_down())
            top_most_position.move_by(m_shift_x, ((m_shift_y == 0) ? 0 : ud_shift));
        else
            top_most_position.move_by(m_shift_x, m_shift_y);
    }

    if (m_type == Stock)
        card->set_upside_down(true);

    card->set_position(top_most_position);

    m_stack.append(card);
    m_stack_positions.append(top_most_position);
    calculate_bounding_box();
}

NonnullRefPtr<Card> CardStack::pop()
{
    auto card = m_stack.take_last();

    calculate_bounding_box();
    if (m_type == Stock)
        card->set_upside_down(false);

    m_stack_positions.take_last();
    return card;
}

void CardStack::calculate_bounding_box()
{
    m_bounding_box = Gfx::Rect(m_position, { Card::width, Card::height });

    if (m_stack.is_empty())
        return;

    uint16_t width = 0;
    uint16_t height = 0;
    int card_position = 0;
    for (auto& card : m_stack) {
        if (card_position % m_step == 0 && card_position) {
            if (card.is_upside_down() && m_type != Stock) {
                int ud_shift = (m_type == Normal) ? 3 : 1;
                width += m_shift_x;
                height += (m_shift_y == 0) ? 0 : ud_shift;
            } else {
                width += m_shift_x;
                height += m_shift_y;
            }
        }
        ++card_position;
    }

    m_bounding_box.set_size(Card::width + width, Card::height + height);
}
