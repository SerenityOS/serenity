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

#pragma once

#include "Card.h"
#include <AK/Vector.h>

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
    CardStack(const Gfx::Point& position, Type type, uint8_t shift_x, uint8_t shift_y, uint8_t step = 1);

    bool is_dirty() const { return m_dirty; }
    bool is_empty() const { return m_stack.is_empty(); }
    bool is_focused() const { return m_focused; }
    Type type() const { return m_type; }
    size_t count() const { return m_stack.size(); }
    const Card& peek() const { return m_stack.last(); }
    Card& peek() { return m_stack.last(); }
    const Gfx::Rect& bounding_box() const { return m_bounding_box; }

    void set_focused(bool focused) { m_focused = focused; }
    void set_dirty() { m_dirty = true; };

    void push(NonnullRefPtr<Card> card);
    NonnullRefPtr<Card> pop();
    void rebound_cards();

    bool is_allowed_to_push(const Card&) const;
    void add_all_grabbed_cards(const Gfx::Point& click_location, NonnullRefPtrVector<Card>& grabbed);
    void draw(GUI::Painter&, const Gfx::Color& background_color);
    void clear();

private:
    void calculate_bounding_box();

    NonnullRefPtrVector<Card> m_stack;
    Vector<Gfx::Point> m_stack_positions;
    Gfx::Point m_position;
    Gfx::Rect m_bounding_box;
    Type m_type { Invalid };
    uint8_t m_shift_x { 0 };
    uint8_t m_shift_y { 0 };
    uint8_t m_step {};
    bool m_focused { false };
    bool m_dirty { false };
    Gfx::Rect m_base;
};
