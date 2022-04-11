/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Forward.h>

struct FontSelector {
    FlyString family;
    float point_size { 0 };
    int weight { 0 };
    int slope { 0 };

    bool operator==(FontSelector const& other) const
    {
        return family == other.family && point_size == other.point_size && weight == other.weight && slope == other.slope;
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
    static FontCache& the();
    RefPtr<Gfx::Font> get(FontSelector const&) const;
    void set(FontSelector const&, NonnullRefPtr<Gfx::Font>);

private:
    FontCache() = default;
    mutable HashMap<FontSelector, NonnullRefPtr<Gfx::Font>> m_fonts;
};
