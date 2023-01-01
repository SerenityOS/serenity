/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CardPainter.h"
#include <LibConfig/Client.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>

namespace Cards {

CardPainter& CardPainter::the()
{
    static CardPainter s_card_painter;
    return s_card_painter;
}

CardPainter::CardPainter()
{
    m_background_image_path = Config::read_string("Games"sv, "Cards"sv, "CardBackImage"sv, "/res/icons/cards/buggie-deck.png"sv);
}

static constexpr Gfx::CharacterBitmap s_diamond {
    "    #    "
    "   ###   "
    "  #####  "
    " ####### "
    "#########"
    " ####### "
    "  #####  "
    "   ###   "
    "    #    "sv,
    9, 9
};

static constexpr Gfx::CharacterBitmap s_heart {
    "  #   #  "
    " ### ### "
    "#########"
    "#########"
    "#########"
    " ####### "
    "  #####  "
    "   ###   "
    "    #    "sv,
    9, 9
};

static constexpr Gfx::CharacterBitmap s_spade {
    "    #    "
    "   ###   "
    "  #####  "
    " ####### "
    "#########"
    "#########"
    " ## # ## "
    "   ###   "
    "   ###   "sv,
    9, 9
};

static constexpr Gfx::CharacterBitmap s_club {
    "    ###    "
    "   #####   "
    "   #####   "
    " ## ### ## "
    "###########"
    "###########"
    "#### # ####"
    " ## ### ## "
    "    ###    "sv,
    11, 9
};

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_front(Suit suit, Rank rank)
{
    auto suit_id = to_underlying(suit);
    auto rank_id = to_underlying(rank);

    auto& existing_bitmap = m_cards[suit_id][rank_id];
    if (!existing_bitmap.is_null())
        return *existing_bitmap;

    m_cards[suit_id][rank_id] = create_card_bitmap();
    paint_card_front(*m_cards[suit_id][rank_id], suit, rank);

    return *m_cards[suit_id][rank_id];
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_back()
{
    if (!m_card_back.is_null())
        return *m_card_back;

    m_card_back = create_card_bitmap();
    paint_card_back(*m_card_back);

    return *m_card_back;
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_front_inverted(Suit suit, Rank rank)
{
    auto suit_id = to_underlying(suit);
    auto rank_id = to_underlying(rank);

    auto& existing_bitmap = m_cards_inverted[suit_id][rank_id];
    if (!existing_bitmap.is_null())
        return *existing_bitmap;

    m_cards_inverted[suit_id][rank_id] = create_card_bitmap();
    paint_inverted_card(*m_cards_inverted[suit_id][rank_id], card_front(suit, rank));

    return *m_cards_inverted[suit_id][rank_id];
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_back_inverted()
{
    if (!m_card_back_inverted.is_null())
        return *m_card_back_inverted;

    m_card_back_inverted = create_card_bitmap();
    paint_inverted_card(card_back(), *m_card_back_inverted);

    return *m_card_back_inverted;
}

void CardPainter::set_background_image_path(DeprecatedString path)
{
    if (m_background_image_path == path)
        return;

    m_background_image_path = path;
    if (!m_card_back.is_null())
        paint_card_back(*m_card_back);
    if (!m_card_back_inverted.is_null())
        paint_inverted_card(*m_card_back_inverted, *m_card_back);
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::create_card_bitmap()
{
    return Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { Card::width, Card::height }).release_value_but_fixme_should_propagate_errors();
}

void CardPainter::paint_card_front(Gfx::Bitmap& bitmap, Cards::Suit suit, Cards::Rank rank)
{
    auto const suit_color = (suit == Suit::Diamonds || suit == Suit::Hearts) ? Color::Red : Color::Black;

    auto const& suit_symbol = [&]() -> Gfx::CharacterBitmap const& {
        switch (suit) {
        case Suit::Diamonds:
            return s_diamond;
        case Suit::Clubs:
            return s_club;
        case Suit::Spades:
            return s_spade;
        case Suit::Hearts:
            return s_heart;
        default:
            VERIFY_NOT_REACHED();
        }
    }();

    Gfx::Painter painter { bitmap };
    auto paint_rect = bitmap.rect();
    auto& font = Gfx::FontDatabase::default_font().bold_variant();

    painter.fill_rect_with_rounded_corners(paint_rect, Color::Black, Card::card_radius);
    paint_rect.shrink(2, 2);
    painter.fill_rect_with_rounded_corners(paint_rect, Color::White, Card::card_radius - 1);

    paint_rect.set_height(paint_rect.height() / 2);
    paint_rect.shrink(10, 6);

    auto text_rect = Gfx::IntRect { 4, 6, static_cast<int>(ceilf(font.width("10"sv))), font.glyph_height() };
    painter.draw_text(text_rect, card_rank_label(rank), font, Gfx::TextAlignment::Center, suit_color);

    painter.draw_bitmap(
        { text_rect.x() + (text_rect.width() - suit_symbol.size().width()) / 2, text_rect.bottom() + 5 },
        suit_symbol, suit_color);

    for (int y = Card::height / 2; y < Card::height; ++y) {
        for (int x = 0; x < Card::width; ++x) {
            bitmap.set_pixel(x, y, bitmap.get_pixel(Card::width - x - 1, Card::height - y - 1));
        }
    }
}

void CardPainter::paint_card_back(Gfx::Bitmap& bitmap)
{
    Gfx::Painter painter { bitmap };
    auto paint_rect = bitmap.rect();
    painter.clear_rect(paint_rect, Gfx::Color::Transparent);

    painter.fill_rect_with_rounded_corners(paint_rect, Color::Black, Card::card_radius);
    auto inner_paint_rect = paint_rect.shrunken(2, 2);
    painter.fill_rect_with_rounded_corners(inner_paint_rect, Color::White, Card::card_radius - 1);

    auto image = Gfx::Bitmap::try_load_from_file(m_background_image_path).release_value_but_fixme_should_propagate_errors();
    painter.blit({ (bitmap.width() - image->width()) / 2, (bitmap.height() - image->height()) / 2 }, image, image->rect());
}

void CardPainter::paint_inverted_card(Gfx::Bitmap& bitmap, Gfx::Bitmap const& source_to_invert)
{
    Gfx::Painter painter { bitmap };
    painter.clear_rect(bitmap.rect(), Gfx::Color::Transparent);
    painter.blit_filtered(Gfx::IntPoint {}, source_to_invert, source_to_invert.rect(), [&](Color color) {
        return color.inverted();
    });
}

}
