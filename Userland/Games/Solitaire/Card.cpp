/*
 * Copyright (c) 2020, Till Mayer <till.mayer@web.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Card.h"
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>

namespace Solitaire {

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

Card::Card(Type type, uint8_t value)
    : m_rect(Gfx::IntRect({}, { width, height }))
    , m_front(*Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { width, height }))
    , m_type(type)
    , m_value(value)
{
    VERIFY(value < card_count);
    Gfx::IntRect paint_rect({ 0, 0 }, { width, height });

    if (s_background.is_null()) {
        s_background = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { width, height });
        Gfx::Painter bg_painter(*s_background);

        s_background->fill(Color::White);
        auto image = Gfx::Bitmap::load_from_file("/res/icons/solitaire/buggie-deck.png");
        VERIFY(!image.is_null());

        float aspect_ratio = image->width() / static_cast<float>(image->height());
        auto target_size = Gfx::IntSize(static_cast<int>(aspect_ratio * (height - 5)), height - 5);

        bg_painter.draw_scaled_bitmap(
            { { (width - target_size.width()) / 2, (height - target_size.height()) / 2 }, target_size },
            *image, image->rect());
        bg_painter.draw_rect(paint_rect, Color::Black);
    }

    Gfx::Painter painter(m_front);
    auto& font = Gfx::FontDatabase::default_bold_font();
    static const String labels[] = { "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K" };

    auto label = labels[value];
    m_front->fill(Color::White);
    painter.draw_rect(paint_rect, Color::Black);
    paint_rect.set_height(paint_rect.height() / 2);
    paint_rect.shrink(10, 6);

    painter.draw_text(paint_rect, label, font, Gfx::TextAlignment::TopLeft, color());

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
        { paint_rect.x() + (font.width(label) - symbol->size().width()) / 2, font.glyph_height() + paint_rect.y() + 3 },
        symbol, color());

    for (int y = height / 2; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            m_front->set_pixel(x, y, m_front->get_pixel(width - x - 1, height - y - 1));
        }
    }
}

Card::~Card()
{
}

void Card::draw(GUI::Painter& painter) const
{
    VERIFY(!s_background.is_null());
    painter.blit(position(), m_upside_down ? *s_background : *m_front, m_front->rect());
}

void Card::clear(GUI::Painter& painter, const Color& background_color) const
{
    painter.fill_rect({ old_positon(), { width, height } }, background_color);
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

}
