/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterBitmap.h"

namespace Gfx {

CharacterBitmap::CharacterBitmap(const char* ascii_data, unsigned width, unsigned height)
    : OneBitBitmap(OneBitBitmap::Type::CharacterBitmap, { (int)width, (int)height })
    , m_bits(ascii_data)
{
}

CharacterBitmap::CharacterBitmap(IntSize const& size, BitmapView const& bitmap)
    : OneBitBitmap(OneBitBitmap::Type::CharacterBitmap, size)
    , m_own_bits(true)
{
    char* bits = new char[size.width() * size.height()];
    for (int y = 0; y < size.height(); y++) {
        for (int x = 0; x < size.width(); x++) {
            size_t index = y * size.width() + x;
            bits[index] = bitmap.get(index) ? '#' : ' ';
        }
    }

    m_bits = bits;
}

CharacterBitmap::~CharacterBitmap()
{
    if (m_own_bits)
        delete[] const_cast<char*>(m_bits);
}

NonnullRefPtr<CharacterBitmap> CharacterBitmap::create_from_ascii(const char* asciiData, unsigned width, unsigned height)
{
    return adopt_ref(*new CharacterBitmap(asciiData, width, height));
}

NonnullRefPtr<CharacterBitmap> CharacterBitmap::create_from_bitmap(IntSize& size, BitmapView const& bitmap)
{
    return adopt_ref(*new CharacterBitmap(size, bitmap));
}

}
