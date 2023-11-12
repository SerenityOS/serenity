/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/String.h>
#include <LibCards/Card.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>

namespace Cards {

class CardPainter {
public:
    static CardPainter& the();

    NonnullRefPtr<Gfx::Bitmap> card_front(Suit, Rank);
    NonnullRefPtr<Gfx::Bitmap> card_back();
    NonnullRefPtr<Gfx::Bitmap> card_front_inverted(Suit, Rank);
    NonnullRefPtr<Gfx::Bitmap> card_back_inverted();
    NonnullRefPtr<Gfx::Bitmap> card_front_highlighted(Suit, Rank);
    NonnullRefPtr<Gfx::Bitmap> card_front_disabled(Suit, Rank);
    NonnullRefPtr<Gfx::Bitmap> card_back_disabled();

    void set_back_image_path(StringView path);
    void set_front_images_set_name(StringView path);
    void set_background_color(Color);

private:
    using PaintCache = Array<Array<RefPtr<Gfx::Bitmap>, to_underlying(Rank::__Count)>, to_underlying(Suit::__Count)>;

    CardPainter();

    NonnullRefPtr<Gfx::Bitmap> get_bitmap_or_create(Suit, Rank, PaintCache&, Function<void(Gfx::Bitmap&)>);

    NonnullRefPtr<Gfx::Bitmap> create_card_bitmap();
    void paint_card_front(Gfx::Bitmap&, Suit, Rank);
    void paint_card_front_pips(Gfx::Bitmap&, Suit, Rank);
    void paint_card_back(Gfx::Bitmap&);
    void paint_inverted_card(Gfx::Bitmap& bitmap, Gfx::Bitmap const& source_to_invert);
    void paint_highlighted_card(Gfx::Bitmap& bitmap, Gfx::Bitmap const& source_to_highlight);
    void paint_disabled_card(Gfx::Bitmap& bitmap, Gfx::Bitmap const& source_to_disable);

    Array<RefPtr<Gfx::Bitmap>, to_underlying(Suit::__Count)> m_suit_pips;
    Array<RefPtr<Gfx::Bitmap>, to_underlying(Suit::__Count)> m_suit_pips_flipped_vertically;

    PaintCache m_cards;
    PaintCache m_cards_inverted;
    PaintCache m_cards_highlighted;
    PaintCache m_cards_disabled;

    RefPtr<Gfx::Bitmap> m_card_back;
    RefPtr<Gfx::Bitmap> m_card_back_inverted;
    RefPtr<Gfx::Bitmap> m_card_back_disabled;

    String m_back_image_path;
    String m_front_images_set_name;
    Color m_background_color;
};

}
