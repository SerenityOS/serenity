/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Card.h"
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>

namespace Cards {

static const NonnullRefPtr<Gfx::CharacterBitmap> s_diamond = Gfx::CharacterBitmap::create_from_ascii(
    "    #    "
    "   ###   "
    "  #####  "
    " ####### "
    "#########"
    " ####### "
    "  #####  "
    "   ###   "
    "    #    ",
    9, 9);

static const NonnullRefPtr<Gfx::CharacterBitmap> s_heart = Gfx::CharacterBitmap::create_from_ascii(
    "  #   #  "
    " ### ### "
    "#########"
    "#########"
    "#########"
    " ####### "
    "  #####  "
    "   ###   "
    "    #    ",
    9, 9);

static const NonnullRefPtr<Gfx::CharacterBitmap> s_spade = Gfx::CharacterBitmap::create_from_ascii(
    "    #    "
    "   ###   "
    "  #####  "
    " ####### "
    "#########"
    "#########"
    " ## # ## "
    "   ###   "
    "   ###   ",
    9, 9);

static const NonnullRefPtr<Gfx::CharacterBitmap> s_club = Gfx::CharacterBitmap::create_from_ascii(
    "    ###    "
    "   #####   "
    "   #####   "
    " ## ### ## "
    "###########"
    "###########"
    "#### # ####"
    " ## ### ## "
    "    ###    ",
    11, 9);

static RefPtr<Gfx::Bitmap> s_background;
static RefPtr<Gfx::Bitmap> s_background_inverted;

Card::Card(Type type, uint8_t value)
    : m_rect(Gfx::IntRect({}, { width, height }))
    , m_front(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { width, height }).release_value_but_fixme_should_propagate_errors())
    , m_type(type)
    , m_value(value)
{
    VERIFY(value < card_count);
    Gfx::IntRect paint_rect({ 0, 0 }, { width, height });

    if (s_background.is_null()) {
        s_background = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { width, height }).release_value_but_fixme_should_propagate_errors();
        Gfx::Painter bg_painter(*s_background);

        auto image = Gfx::Bitmap::try_load_from_file("/res/icons/cards/buggie-deck.png").release_value_but_fixme_should_propagate_errors();

        float aspect_ratio = image->width() / static_cast<float>(image->height());
        auto target_size = Gfx::IntSize(static_cast<int>(aspect_ratio * (height - 5)), height - 5);

        bg_painter.fill_rect_with_rounded_corners(paint_rect, Color::Black, card_radius);
        auto inner_paint_rect = paint_rect.shrunken(2, 2);
        bg_painter.fill_rect_with_rounded_corners(inner_paint_rect, Color::White, card_radius - 1);

        bg_painter.draw_scaled_bitmap(
            { { (width - target_size.width()) / 2, (height - target_size.height()) / 2 }, target_size },
            *image, image->rect());

        s_background_inverted = invert_bitmap(*s_background);
    }

    Gfx::Painter painter(m_front);
    auto& font = Gfx::FontDatabase::default_font().bold_variant();

    auto label = labels[value];
    painter.fill_rect_with_rounded_corners(paint_rect, Color::Black, card_radius);
    paint_rect.shrink(2, 2);
    painter.fill_rect_with_rounded_corners(paint_rect, Color::White, card_radius - 1);

    paint_rect.set_height(paint_rect.height() / 2);
    paint_rect.shrink(10, 6);

    auto text_rect = Gfx::IntRect { 4, 6, font.width("10"), font.glyph_height() };
    painter.draw_text(text_rect, label, font, Gfx::TextAlignment::Center, color());

    NonnullRefPtr<Gfx::CharacterBitmap> symbol = s_diamond;
    switch (m_type) {
    case Diamonds:
        symbol = s_diamond;
        break;
    case Clubs:
        symbol = s_club;
        break;
    case Spades:
        symbol = s_spade;
        break;
    case Hearts:
        symbol = s_heart;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    painter.draw_bitmap(
        { text_rect.x() + (text_rect.width() - symbol->size().width()) / 2, text_rect.bottom() + 5 },
        symbol, color());

    for (int y = height / 2; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            m_front->set_pixel(x, y, m_front->get_pixel(width - x - 1, height - y - 1));
        }
    }

    m_front_inverted = invert_bitmap(*m_front);
}

Card::~Card()
{
}

void Card::draw(GUI::Painter& painter) const
{
    VERIFY(!s_background.is_null());
    if (m_inverted)
        painter.blit(position(), m_upside_down ? *s_background_inverted : *m_front_inverted, m_front_inverted->rect());
    else
        painter.blit(position(), m_upside_down ? *s_background : *m_front, m_front->rect());
}

void Card::clear(GUI::Painter& painter, const Color& background_color) const
{
    painter.fill_rect({ old_position(), { width, height } }, background_color);
}

void Card::save_old_position()
{
    m_old_position = m_rect.location();
    m_old_position_valid = true;
}

void Card::clear_and_draw(GUI::Painter& painter, const Color& background_color)
{
    if (is_old_position_valid())
        clear(painter, background_color);

    draw(painter);
    save_old_position();
}

NonnullRefPtr<Gfx::Bitmap> Card::invert_bitmap(Gfx::Bitmap& bitmap)
{
    auto inverted_bitmap = bitmap.clone().release_value_but_fixme_should_propagate_errors();
    for (int y = 0; y < inverted_bitmap->height(); y++) {
        for (int x = 0; x < inverted_bitmap->width(); x++) {
            inverted_bitmap->set_pixel(x, y, inverted_bitmap->get_pixel(x, y).inverted());
        }
    }
    return *inverted_bitmap;
}

}
