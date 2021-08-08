/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Size.h"
#include <AK/BitmapView.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibGfx/OneBitBitmap.h>

namespace Gfx {

class CharacterBitmap : public RefCounted<CharacterBitmap>
    , public OneBitBitmap {
public:
    static NonnullRefPtr<CharacterBitmap> create_from_ascii(const char* asciiData, unsigned width, unsigned height);
    static NonnullRefPtr<CharacterBitmap> create_from_bitmap(IntSize&, BitmapView const&);
    ~CharacterBitmap();

    bool bit_at(int x, int y) const override { return m_bits[y * width() + x] == '#'; }
    void set_bit_at(int, int, bool) override { VERIFY_NOT_REACHED(); }
    const char* bits() const { return m_bits; }

    unsigned width() const { return size().width(); }
    unsigned height() const { return size().height(); }

private:
    CharacterBitmap(const char* b, unsigned w, unsigned h);
    CharacterBitmap(IntSize const&, BitmapView const&);

    const char* m_bits { nullptr };
    bool m_own_bits { false };
};

}
