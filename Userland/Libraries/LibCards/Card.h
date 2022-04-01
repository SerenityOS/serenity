/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Format.h>
#include <LibCore/Object.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Rect.h>
#include <ctype.h>

namespace Cards {

class Card final : public Core::Object {
    C_OBJECT(Card)
public:
    static constexpr int width = 80;
    static constexpr int height = 100;
    static constexpr int card_count = 13;
    static constexpr int card_radius = 5;
    static constexpr Array<StringView, card_count> labels = {
        "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"
    };

    enum class Suit {
        Clubs,
        Diamonds,
        Spades,
        Hearts,
        __Count
    };

    virtual ~Card() override = default;

    Gfx::IntRect& rect() { return m_rect; }
    Gfx::IntPoint position() const { return m_rect.location(); }
    Gfx::IntPoint const& old_position() const { return m_old_position; }
    uint8_t value() const { return m_value; };
    Suit suit() const { return m_suit; }

    bool is_old_position_valid() const { return m_old_position_valid; }
    bool is_moving() const { return m_moving; }
    bool is_upside_down() const { return m_upside_down; }
    bool is_inverted() const { return m_inverted; }
    Gfx::Color color() const { return (m_suit == Suit::Diamonds || m_suit == Suit::Hearts) ? Color::Red : Color::Black; }

    void set_position(const Gfx::IntPoint p) { m_rect.set_location(p); }
    void set_moving(bool moving) { m_moving = moving; }
    void set_upside_down(bool upside_down) { m_upside_down = upside_down; }
    void set_inverted(bool inverted) { m_inverted = inverted; }

    void save_old_position();

    void draw(GUI::Painter&) const;
    void clear(GUI::Painter&, Color const& background_color) const;
    void clear_and_draw(GUI::Painter&, Color const& background_color);

private:
    Card(Suit suit, uint8_t value);

    static NonnullRefPtr<Gfx::Bitmap> invert_bitmap(Gfx::Bitmap&);

    Gfx::IntRect m_rect;
    NonnullRefPtr<Gfx::Bitmap> m_front;
    RefPtr<Gfx::Bitmap> m_front_inverted;
    Gfx::IntPoint m_old_position;
    Suit m_suit;
    uint8_t m_value;
    bool m_old_position_valid { false };
    bool m_moving { false };
    bool m_upside_down { false };
    bool m_inverted { false };
};

}

template<>
struct AK::Formatter<Cards::Card> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Cards::Card const& card)
    {
        StringView suit;

        switch (card.suit()) {
        case Cards::Card::Suit::Clubs:
            suit = "C"sv;
            break;
        case Cards::Card::Suit::Diamonds:
            suit = "D"sv;
            break;
        case Cards::Card::Suit::Hearts:
            suit = "H"sv;
            break;
        case Cards::Card::Suit::Spades:
            suit = "S"sv;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        return Formatter<FormatString>::format(builder, "{:>2}{}", Cards::Card::labels[card.value()], suit);
    }
};
