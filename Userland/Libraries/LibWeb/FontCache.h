/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Forward.h>

struct FontSelector {
    FlyString family;
    float point_size { 0 };
    int weight { 0 };
    int width { 0 };
    int slope { 0 };

    bool operator==(FontSelector const& other) const
    {
        return family == other.family && point_size == other.point_size && weight == other.weight && width == other.width && slope == other.slope;
    }
};

namespace AK {
template<>
struct Traits<FontSelector> : public GenericTraits<FontSelector> {
    static unsigned hash(FontSelector const& key) { return pair_int_hash(pair_int_hash(key.family.hash(), key.weight), key.point_size); }
};
}

class FontCache {
public:
    FontCache() = default;
    RefPtr<Gfx::Font const> get(FontSelector const&) const;
    void set(FontSelector const&, NonnullRefPtr<Gfx::Font const>);

    NonnullRefPtr<Gfx::Font const> scaled_font(Gfx::Font const&, float scale_factor);

private:
    mutable HashMap<FontSelector, NonnullRefPtr<Gfx::Font const>> m_fonts;
};
