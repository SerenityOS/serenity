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

#include <LibCore/Object.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Rect.h>
#include <ctype.h>

class Card final : public Core::Object {
    C_OBJECT(Card)
public:
    static constexpr int width = 80;
    static constexpr int height = 100;
    static constexpr int card_count = 13;

    enum Type {
        Clubs,
        Diamonds,
        Hearts,
        Spades,
        __Count
    };

    virtual ~Card() override;

    Gfx::IntRect& rect() { return m_rect; }
    Gfx::IntPoint position() const { return m_rect.location(); }
    const Gfx::IntPoint& old_positon() const { return m_old_position; }
    uint8_t value() const { return m_value; };
    Type type() const { return m_type; }

    bool is_old_position_valid() const { return m_old_position_valid; }
    bool is_moving() const { return m_moving; }
    bool is_upside_down() const { return m_upside_down; }
    Gfx::Color color() const { return (m_type == Diamonds || m_type == Hearts) ? Color::Red : Color::Black; }

    void set_position(const Gfx::IntPoint p) { m_rect.set_location(p); }
    void set_moving(bool moving) { m_moving = moving; }
    void set_upside_down(bool upside_down) { m_upside_down = upside_down; }

    void save_old_position();

    void draw(GUI::Painter&) const;
    void clear(GUI::Painter&, const Color& background_color) const;
    void draw_complete(GUI::Painter&, const Color& background_color);

private:
    Card(Type type, uint8_t value);

    Gfx::IntRect m_rect;
    NonnullRefPtr<Gfx::Bitmap> m_front;
    Gfx::IntPoint m_old_position;
    Type m_type;
    uint8_t m_value;
    bool m_old_position_valid { false };
    bool m_moving { false };
    bool m_upside_down { false };
};
