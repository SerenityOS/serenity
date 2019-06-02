#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>

class Font;

struct Metadata {
    String path;
    bool is_fixed_width;
    int glyph_height;
};

class GFontDatabase {
public:
    static GFontDatabase& the();

    RetainPtr<Font> get_by_name(const StringView&);
    void for_each_font(Function<void(const StringView&)>);
    void for_each_fixed_width_font(Function<void(const StringView&)>);

    Metadata get_metadata_by_name(const StringView& name) const
    {
        return m_name_to_metadata.get(name);
    };

private:
    GFontDatabase();
    ~GFontDatabase();

    HashMap<String, Metadata> m_name_to_metadata;
};
