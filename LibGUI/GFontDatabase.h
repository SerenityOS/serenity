#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/Function.h>

class Font;

class GFontDatabase {
public:
    static GFontDatabase& the();

    RetainPtr<Font> get_by_name(const String&);
    void for_each_font(Function<void(const String&)>);
    void for_each_fixed_width_font(Function<void(const String&)>);

private:
    GFontDatabase();
    ~GFontDatabase();

    struct Metadata {
        String path;
        bool is_fixed_width;
        int glyph_height;
    };

    HashMap<String, Metadata> m_name_to_metadata;
};
