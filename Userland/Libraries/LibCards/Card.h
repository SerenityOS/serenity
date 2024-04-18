/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <LibCore/EventReceiver.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Rect.h>

namespace Cards {

enum class Rank : u8 {
    Ace,
    Two,
    Three,
    Four,
    Five,
    Six,
    Seven,
    Eight,
    Nine,
    Ten,
    Jack,
    Queen,
    King,
    __Count
};

constexpr StringView card_rank_label(Rank rank)
{
    switch (rank) {
    case Rank::Ace:
        return "A"sv;
    case Rank::Two:
        return "2"sv;
    case Rank::Three:
        return "3"sv;
    case Rank::Four:
        return "4"sv;
    case Rank::Five:
        return "5"sv;
    case Rank::Six:
        return "6"sv;
    case Rank::Seven:
        return "7"sv;
    case Rank::Eight:
        return "8"sv;
    case Rank::Nine:
        return "9"sv;
    case Rank::Ten:
        return "10"sv;
    case Rank::Jack:
        return "J"sv;
    case Rank::Queen:
        return "Q"sv;
    case Rank::King:
        return "K"sv;
    case Rank::__Count:
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

enum class Suit : u8 {
    Clubs,
    Diamonds,
    Spades,
    Hearts,
    __Count
};

class Card final : public Core::EventReceiver {
    C_OBJECT(Card)
public:
    static constexpr int width = 80;
    static constexpr int height = 110;
    static constexpr int card_count = to_underlying(Rank::__Count);
    static constexpr int card_radius = 7;

    virtual ~Card() override = default;

    Gfx::IntRect& rect() { return m_rect; }
    Gfx::IntRect const& rect() const { return m_rect; }
    Gfx::IntPoint position() const { return m_rect.location(); }
    Gfx::IntPoint old_position() const { return m_old_position; }
    Rank rank() const { return m_rank; }
    Suit suit() const { return m_suit; }

    bool is_old_position_valid() const { return m_old_position_valid; }
    bool is_moving() const { return m_moving; }
    bool is_upside_down() const { return m_upside_down; }
    bool is_inverted() const { return m_inverted; }
    bool is_previewed() const { return m_previewed; }
    bool is_disabled() const { return m_disabled; }
    Gfx::Color color() const { return (m_suit == Suit::Diamonds || m_suit == Suit::Hearts) ? Color::Red : Color::Black; }

    void set_position(Gfx::IntPoint const p) { m_rect.set_location(p); }
    void set_moving(bool moving) { m_moving = moving; }
    void set_upside_down(bool upside_down) { m_upside_down = upside_down; }
    void set_inverted(bool inverted) { m_inverted = inverted; }
    void set_previewed(bool previewed) { m_previewed = previewed; }
    void set_disabled(bool disabled) { m_disabled = disabled; }

    void save_old_position();

    void paint(GUI::Painter&, bool highlighted = false) const;
    void clear(GUI::Painter&, Color background_color) const;
    void clear_and_paint(GUI::Painter& painter, Color background_color, bool highlighted);

private:
    Card(Suit, Rank);

    Gfx::IntRect m_rect;
    Gfx::IntPoint m_old_position;
    Suit m_suit;
    Rank m_rank;
    bool m_old_position_valid { false };
    bool m_moving { false };
    bool m_upside_down { false };
    bool m_inverted { false };
    bool m_previewed { false };
    bool m_disabled { false };
};

enum class Shuffle {
    No,
    Yes,
};
ErrorOr<Vector<NonnullRefPtr<Card>>> create_standard_deck(Shuffle);
ErrorOr<Vector<NonnullRefPtr<Card>>> create_deck(unsigned full_club_suit_count, unsigned full_diamond_suit_count, unsigned full_heart_suit_count, unsigned full_spade_suit_count, Shuffle);

}

template<>
struct AK::Formatter<Cards::Card> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Cards::Card const& card)
    {
        StringView suit;

        switch (card.suit()) {
        case Cards::Suit::Clubs:
            suit = "C"sv;
            break;
        case Cards::Suit::Diamonds:
            suit = "D"sv;
            break;
        case Cards::Suit::Hearts:
            suit = "H"sv;
            break;
        case Cards::Suit::Spades:
            suit = "S"sv;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        return Formatter<FormatString>::format(builder, "{:>2}{}"sv, Cards::card_rank_label(card.rank()), suit);
    }
};
