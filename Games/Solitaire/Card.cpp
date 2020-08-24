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

#include "Card.h"
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>

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
    , m_front(*Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { width, height }))
    , m_type(type)
    , m_value(value)
{
    ASSERT(value < card_count);
    Gfx::IntRect paint_rect({ 0, 0 }, { width, height });

    if (s_background.is_null()) {
        s_background = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { width, height });
        Gfx::Painter bg_painter(*s_background);

        s_background->fill(Color::White);
        auto image = Gfx::Bitmap::load_from_file("/res/icons/solitaire/buggie-deck.png");
        ASSERT(!image.is_null());

        float aspect_ratio = image->width() / static_cast<float>(image->height());
        auto target_size = Gfx::IntSize(static_cast<int>(aspect_ratio * (height - 5)), height - 5);

        bg_painter.draw_scaled_bitmap(
            { { (width - target_size.width()) / 2, (height - target_size.height()) / 2 }, target_size },
            *image, image->rect());
        bg_painter.draw_rect(paint_rect, Color::Black);
    }

    Gfx::Painter painter(m_front);
    auto& font = Gfx::Font::default_bold_font();
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
        ASSERT_NOT_REACHED();
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
    ASSERT(!s_background.is_null());
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

void Card::draw_complete(GUI::Painter& painter, const Color& background_color)
{
    if (is_old_position_valid())
        clear(painter, background_color);

    draw(painter);
    save_old_position();
}
