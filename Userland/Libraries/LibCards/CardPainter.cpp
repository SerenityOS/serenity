/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CardPainter.h"
#include <AK/Array.h>
#include <AK/GenericShorthands.h>
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
    m_back_image_path = MUST(String::from_byte_string(Config::read_string("Games"sv, "Cards"sv, "CardBackImage"sv, "/res/graphics/cards/backs/Red.png"sv)));
    set_front_images_set_name(MUST(String::from_byte_string(Config::read_string("Games"sv, "Cards"sv, "CardFrontImages"sv, "Classic"sv))));
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
    "   ###   "
    "  #####  "
    "  #####  "
    "## ### ##"
    "#########"
    "#########"
    " ## # ## "
    "   ###   "
    "   ###   "sv,
    9, 9
};

static constexpr u8 s_disabled_alpha = 90;

NonnullRefPtr<Gfx::Bitmap> CardPainter::get_bitmap_or_create(Suit suit, Rank rank, CardPainter::PaintCache& cache, Function<void(Gfx::Bitmap&)> creator)
{
    auto suit_id = to_underlying(suit);
    auto rank_id = to_underlying(rank);

    auto& existing_bitmap = cache[suit_id][rank_id];
    if (!existing_bitmap.is_null())
        return *existing_bitmap;

    auto bitmap = create_card_bitmap();
    creator(bitmap);
    cache[suit_id][rank_id] = move(bitmap);
    return *cache[suit_id][rank_id];
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_front(Suit suit, Rank rank)
{
    return get_bitmap_or_create(suit, rank, m_cards, [this, suit, rank](auto& bitmap) {
        paint_card_front(bitmap, suit, rank);
    });
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_back()
{
    if (!m_card_back.is_null())
        return *m_card_back;

    m_card_back = create_card_bitmap();
    paint_card_back(*m_card_back);

    return *m_card_back;
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_front_highlighted(Suit suit, Rank rank)
{
    return get_bitmap_or_create(suit, rank, m_cards_highlighted, [this, suit, rank](auto& bitmap) {
        paint_highlighted_card(bitmap, card_front(suit, rank));
    });
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_front_disabled(Suit suit, Rank rank)
{
    return get_bitmap_or_create(suit, rank, m_cards_disabled, [this, suit, rank](auto& bitmap) {
        paint_disabled_card(bitmap, card_front(suit, rank));
    });
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_front_inverted(Suit suit, Rank rank)
{
    return get_bitmap_or_create(suit, rank, m_cards_inverted, [this, suit, rank](auto& bitmap) {
        paint_inverted_card(bitmap, card_front(suit, rank));
    });
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_back_inverted()
{
    if (!m_card_back_inverted.is_null())
        return *m_card_back_inverted;

    m_card_back_inverted = create_card_bitmap();
    paint_inverted_card(card_back(), *m_card_back_inverted);

    return *m_card_back_inverted;
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::card_back_disabled()
{
    if (!m_card_back_disabled.is_null())
        return *m_card_back_disabled;

    m_card_back_disabled = create_card_bitmap();
    paint_disabled_card(*m_card_back_disabled, card_back());

    return *m_card_back_disabled;
}

void CardPainter::set_back_image_path(StringView path)
{
    if (m_back_image_path == path)
        return;

    m_back_image_path = MUST(String::from_utf8(path));
    if (!m_card_back.is_null())
        paint_card_back(*m_card_back);
    if (!m_card_back_inverted.is_null())
        paint_inverted_card(*m_card_back_inverted, *m_card_back);
}

void CardPainter::set_front_images_set_name(AK::StringView path)
{
    if (m_front_images_set_name == path)
        return;

    m_front_images_set_name = MUST(String::from_utf8(path));

    if (m_front_images_set_name.is_empty()) {
        for (auto& pip_bitmap : m_suit_pips)
            pip_bitmap = nullptr;
        for (auto& pip_bitmap : m_suit_pips_flipped_vertically)
            pip_bitmap = nullptr;
    } else {
        auto diamond = Gfx::Bitmap::load_from_file(MUST(String::formatted("/res/graphics/cards/fronts/{}/diamond.png", m_front_images_set_name))).release_value_but_fixme_should_propagate_errors();
        m_suit_pips[to_underlying(Suit::Diamonds)] = diamond;
        m_suit_pips_flipped_vertically[to_underlying(Suit::Diamonds)] = diamond->flipped(Gfx::Orientation::Vertical).release_value_but_fixme_should_propagate_errors();

        auto club = Gfx::Bitmap::load_from_file(MUST(String::formatted("/res/graphics/cards/fronts/{}/club.png", m_front_images_set_name))).release_value_but_fixme_should_propagate_errors();
        m_suit_pips[to_underlying(Suit::Clubs)] = club;
        m_suit_pips_flipped_vertically[to_underlying(Suit::Clubs)] = club->flipped(Gfx::Orientation::Vertical).release_value_but_fixme_should_propagate_errors();

        auto heart = Gfx::Bitmap::load_from_file(MUST(String::formatted("/res/graphics/cards/fronts/{}/heart.png", m_front_images_set_name))).release_value_but_fixme_should_propagate_errors();
        m_suit_pips[to_underlying(Suit::Hearts)] = heart;
        m_suit_pips_flipped_vertically[to_underlying(Suit::Hearts)] = heart->flipped(Gfx::Orientation::Vertical).release_value_but_fixme_should_propagate_errors();

        auto spade = Gfx::Bitmap::load_from_file(MUST(String::formatted("/res/graphics/cards/fronts/{}/spade.png", m_front_images_set_name))).release_value_but_fixme_should_propagate_errors();
        m_suit_pips[to_underlying(Suit::Spades)] = spade;
        m_suit_pips_flipped_vertically[to_underlying(Suit::Spades)] = spade->flipped(Gfx::Orientation::Vertical).release_value_but_fixme_should_propagate_errors();
    }

    // Clear all bitmaps using front images
    for (auto& suit_array : m_cards) {
        for (auto& card_bitmap : suit_array)
            card_bitmap = nullptr;
    }

    for (auto& suit_array : m_cards_highlighted) {
        for (auto& card_bitmap : suit_array)
            card_bitmap = nullptr;
    }
}

void CardPainter::set_background_color(Color background_color)
{
    m_background_color = background_color;

    // Clear any cached card bitmaps that depend on the background color.
    for (auto& suit_array : m_cards_highlighted) {
        for (auto& rank_array : suit_array)
            rank_array = nullptr;
    }
}

NonnullRefPtr<Gfx::Bitmap> CardPainter::create_card_bitmap()
{
    return Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, { Card::width, Card::height }).release_value_but_fixme_should_propagate_errors();
}

void CardPainter::paint_card_front_pips(Gfx::Bitmap& bitmap, Suit suit, Rank rank)
{
    Gfx::Painter painter { bitmap };
    auto& pip_bitmap = m_suit_pips[to_underlying(suit)];
    auto& pip_bitmap_flipped_vertically = m_suit_pips_flipped_vertically[to_underlying(suit)];

    struct Pip {
        int x;
        int y;
        bool flip_vertically;
    };

    auto paint_pips = [&](Span<Pip> pips) {
        for (auto& pip : pips) {
            auto& bitmap = pip.flip_vertically ? pip_bitmap_flipped_vertically : pip_bitmap;
            painter.blit({ pip.x - bitmap->width() / 2, pip.y - bitmap->height() / 2 }, *bitmap, bitmap->rect());
        }
    };

    constexpr int column_left = Card::width * 1 / 3;
    constexpr int column_middle = Card::width * 1 / 2;
    constexpr int column_right = Card::width - column_left;
    constexpr int row_top = Card::height / 6;
    constexpr int row_middle = Card::height / 2;
    constexpr int row_bottom = Card::height - row_top - 1;
    constexpr int row_2_of_4 = row_top + (row_bottom - row_top) * 1 / 3;
    constexpr int row_3_of_4 = Card::height - row_2_of_4 - 1;
    constexpr int row_2_of_5 = row_top + (row_bottom - row_top) * 1 / 4;
    constexpr int row_4_of_5 = Card::height - row_2_of_5 - 1;
    constexpr int row_2_of_7 = row_top + (row_bottom - row_top) * 1 / 6;
    constexpr int row_6_of_7 = Card::height - row_2_of_7 - 1;

    switch (rank) {
    case Rank::Ace:
        paint_pips(Array<Pip, 1>({ Pip { column_middle, row_middle, false } }));
        break;
    case Rank::Two:
        paint_pips(Array<Pip, 2>({ { column_middle, row_top, false },
            { column_middle, row_bottom, true } }));
        break;
    case Rank::Three:
        paint_pips(Array<Pip, 3>({ { column_middle, row_top, false },
            { column_middle, row_middle, false },
            { column_middle, row_bottom, true } }));
        break;
    case Rank::Four:
        paint_pips(Array<Pip, 4>({ { column_left, row_top, false },
            { column_right, row_top, false },
            { column_left, row_bottom, true },
            { column_right, row_bottom, true } }));
        break;
    case Rank::Five:
        paint_pips(Array<Pip, 5>({ { column_left, row_top, false },
            { column_right, row_top, false },
            { column_middle, row_middle, false },
            { column_left, row_bottom, true },
            { column_right, row_bottom, true } }));
        break;
    case Rank::Six:
        paint_pips(Array<Pip, 6>({ { column_left, row_top, false },
            { column_right, row_top, false },
            { column_left, row_middle, false },
            { column_right, row_middle, false },
            { column_left, row_bottom, true },
            { column_right, row_bottom, true } }));
        break;
    case Rank::Seven:
        paint_pips(Array<Pip, 7>({ { column_left, row_top, false },
            { column_right, row_top, false },
            { column_middle, row_2_of_5, false },
            { column_left, row_middle, false },
            { column_right, row_middle, false },
            { column_left, row_bottom, true },
            { column_right, row_bottom, true } }));
        break;
    case Rank::Eight:
        paint_pips(Array<Pip, 8>({ { column_left, row_top, false },
            { column_right, row_top, false },
            { column_middle, row_2_of_5, false },
            { column_left, row_middle, false },
            { column_right, row_middle, false },
            { column_middle, row_4_of_5, true },
            { column_left, row_bottom, true },
            { column_right, row_bottom, true } }));
        break;
    case Rank::Nine:
        paint_pips(Array<Pip, 9>({ { column_left, row_top, false },
            { column_right, row_top, false },
            { column_left, row_2_of_4, false },
            { column_right, row_2_of_4, false },
            { column_middle, row_middle, false },
            { column_left, row_3_of_4, true },
            { column_right, row_3_of_4, true },
            { column_left, row_bottom, true },
            { column_right, row_bottom, true } }));
        break;
    case Rank::Ten:
        paint_pips(Array<Pip, 10>({ { column_left, row_top, false },
            { column_right, row_top, false },
            { column_middle, row_2_of_7, false },
            { column_left, row_2_of_4, false },
            { column_right, row_2_of_4, false },
            { column_left, row_3_of_4, true },
            { column_right, row_3_of_4, true },
            { column_middle, row_6_of_7, true },
            { column_left, row_bottom, true },
            { column_right, row_bottom, true } }));
        break;

    case Rank::Jack:
    case Rank::Queen:
    case Rank::King:
    case Rank::__Count:
        break;
    }
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

    auto text_rect = Gfx::IntRect { 1, 6, font.width_rounded_up("10"sv), font.pixel_size_rounded_up() };
    painter.draw_text(text_rect, card_rank_label(rank), font, Gfx::TextAlignment::Center, suit_color);

    painter.draw_bitmap(
        { text_rect.x() + (text_rect.width() - suit_symbol.size().width()) / 2, text_rect.bottom() + 4 },
        suit_symbol, suit_color);

    for (int y = Card::height / 2; y < Card::height; ++y) {
        for (int x = 0; x < Card::width; ++x)
            bitmap.set_pixel(x, y, bitmap.get_pixel(Card::width - x - 1, Card::height - y - 1));
    }

    if (!m_front_images_set_name.is_empty()) {
        // Paint pips for number cards except the ace of spades
        if (!first_is_one_of(rank, Rank::Ace, Rank::Jack, Rank::Queen, Rank::King)
            || (rank == Rank::Ace && suit != Suit::Spades)) {
            paint_card_front_pips(bitmap, suit, rank);
        } else {
            // Paint pictures for royal cards and ace of spades
            StringView rank_name;
            switch (rank) {
            case Rank::Ace:
                rank_name = "ace"sv;
                break;
            case Rank::Jack:
                rank_name = "jack"sv;
                break;
            case Rank::Queen:
                rank_name = "queen"sv;
                break;
            case Rank::King:
                rank_name = "king"sv;
                break;
            default:
                break;
            }

            StringView suit_name;
            switch (suit) {
            case Suit::Diamonds:
                suit_name = "diamonds"sv;
                break;
            case Suit::Clubs:
                suit_name = "clubs"sv;
                break;
            case Suit::Hearts:
                suit_name = "hearts"sv;
                break;
            case Suit::Spades:
                suit_name = "spades"sv;
                break;
            case Suit::__Count:
                return;
            }

            auto front_image_path = MUST(String::formatted("/res/graphics/cards/fronts/{}/{}-{}.png", m_front_images_set_name, suit_name, rank_name));
            auto maybe_front_image = Gfx::Bitmap::load_from_file(front_image_path);
            if (maybe_front_image.is_error()) {
                dbgln("Failed to load `{}`: {}", front_image_path, maybe_front_image.error());
                return;
            }
            auto front_image = maybe_front_image.release_value();
            painter.blit({ (bitmap.width() - front_image->width()) / 2, (bitmap.height() - front_image->height()) / 2 }, front_image, front_image->rect());
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

    auto image = Gfx::Bitmap::load_from_file(m_back_image_path).release_value_but_fixme_should_propagate_errors();
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

void CardPainter::paint_highlighted_card(Gfx::Bitmap& bitmap, Gfx::Bitmap const& source_to_highlight)
{
    Gfx::Painter painter { bitmap };
    auto paint_rect = source_to_highlight.rect();
    auto background_complement = m_background_color.xored(Color::White);

    painter.fill_rect_with_rounded_corners(paint_rect, Color::Black, Card::card_radius);
    paint_rect.shrink(2, 2);
    painter.fill_rect_with_rounded_corners(paint_rect, background_complement, Card::card_radius - 1);
    paint_rect.shrink(4, 4);
    painter.fill_rect_with_rounded_corners(paint_rect, Color::White, Card::card_radius - 1);
    painter.blit({ 4, 4 }, source_to_highlight, source_to_highlight.rect().shrunken(8, 8));
}

void CardPainter::paint_disabled_card(Gfx::Bitmap& bitmap, Gfx::Bitmap const& source_to_disable)
{
    Gfx::Painter painter { bitmap };
    auto disabled_color = Color(Color::Black);
    disabled_color.set_alpha(s_disabled_alpha);

    painter.blit_filtered(Gfx::IntPoint {}, source_to_disable, source_to_disable.rect(), [&](Color color) {
        return color.blend(disabled_color);
    });
}

}
