/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterBitmap.h"

namespace Gfx {

CharacterBitmap::CharacterBitmap(char const* ascii_data, unsigned width, unsigned height)
    : m_bits(ascii_data)
    , m_size(width, height)
{
}

CharacterBitmap::~CharacterBitmap()
{
}

NonnullRefPtr<CharacterBitmap> CharacterBitmap::create_from_ascii(char const* asciiData, unsigned width, unsigned height)
{
    return adopt_ref(*new CharacterBitmap(asciiData, width, height));
}

}
