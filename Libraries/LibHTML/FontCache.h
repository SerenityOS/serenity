#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>

class Font;

struct FontSelector {
    String family;
    String weight;

    bool operator==(const FontSelector& other) const
    {
        return family == other.family && weight == other.weight;
    }
};

namespace AK {
template<>
struct Traits<FontSelector> : public GenericTraits<FontSelector> {
    static unsigned hash(const FontSelector& key) { return pair_int_hash(key.family.hash(), key.weight.hash()); }
};
}

class FontCache {
public:
    static FontCache& the();
    RefPtr<Font> get(const FontSelector&) const;
    void set(const FontSelector&, NonnullRefPtr<Font>);

private:
    FontCache() {}
    mutable HashMap<FontSelector, NonnullRefPtr<Font>> m_fonts;
};
