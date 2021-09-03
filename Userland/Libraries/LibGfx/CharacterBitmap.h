/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Size.h"
#include <YAK/RefCounted.h>
#include <YAK/RefPtr.h>

namespace Gfx {

class CharacterBitmap : public RefCounted<CharacterBitmap> {
public:
    static NonnullRefPtr<CharacterBitmap> create_from_ascii(const char* asciiData, unsigned width, unsigned height);
    ~CharacterBitmap();

    bool bit_at(unsigned x, unsigned y) const { return m_bits[y * width() + x] == '#'; }
    const char* bits() const { return m_bits; }

    IntSize size() const { return m_size; }
    unsigned width() const { return m_size.width(); }
    unsigned height() const { return m_size.height(); }

private:
    CharacterBitmap(const char* b, unsigned w, unsigned h);

    const char* m_bits { nullptr };
    IntSize m_size;
};

}
