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

private:
    GFontDatabase();
    ~GFontDatabase();

    HashMap<String, String> m_name_to_path;
};
