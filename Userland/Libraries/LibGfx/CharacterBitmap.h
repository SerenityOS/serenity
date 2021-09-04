/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Size.h"
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>

namespace Gfx {

class CharacterBitmap : public RefCounted<CharacterBitmap> {
public:
    static NonnullRefPtr<CharacterBitmap> create_from_ascii(char const* asciiData, unsigned width, unsigned height);
    ~CharacterBitmap();

    bool bit_at(unsigned x, unsigned y) const { return m_bits[y * width() + x] == '#'; }
    char const* bits() const { return m_bits; }

    IntSize size() const { return m_size; }
    unsigned width() const { return m_size.width(); }
    unsigned height() const { return m_size.height(); }

private:
    CharacterBitmap(char const* b, unsigned w, unsigned h);

    char const* m_bits { nullptr };
    IntSize m_size;
};

}
