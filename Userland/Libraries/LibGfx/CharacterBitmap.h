/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Size.h"
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>

namespace Gfx {

class CharacterBitmap {
public:
    CharacterBitmap() = delete;
    constexpr CharacterBitmap(StringView ascii_data, unsigned width, unsigned height)
        : m_bits(ascii_data)
        , m_size(width, height)
    {
    }

    constexpr ~CharacterBitmap() = default;

    constexpr bool bit_at(unsigned x, unsigned y) const { return m_bits[y * width() + x] == '#'; }
    constexpr StringView bits() const { return m_bits; }

    constexpr IntSize size() const { return m_size; }
    constexpr unsigned width() const { return m_size.width(); }
    constexpr unsigned height() const { return m_size.height(); }

private:
    StringView m_bits {};
    IntSize m_size {};
};

}
