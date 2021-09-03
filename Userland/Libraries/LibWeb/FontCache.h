/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/FlyString.h>
#include <YAK/HashMap.h>
#include <YAK/String.h>
#include <LibGfx/Font.h>
#include <LibGfx/Forward.h>

struct FontSelector {
    FlyString family;
    int size { 0 };
    int weight { 0 };

    bool operator==(const FontSelector& other) const
    {
        return family == other.family && size == other.size && weight == other.weight;
    }
};

namespace YAK {
template<>
struct Traits<FontSelector> : public GenericTraits<FontSelector> {
    static unsigned hash(const FontSelector& key) { return pair_int_hash(pair_int_hash(key.family.hash(), key.weight), key.size); }
};
}

class FontCache {
public:
    static FontCache& the();
    RefPtr<Gfx::Font> get(const FontSelector&) const;
    void set(const FontSelector&, NonnullRefPtr<Gfx::Font>);

private:
    FontCache() { }
    mutable HashMap<FontSelector, NonnullRefPtr<Gfx::Font>> m_fonts;
};
