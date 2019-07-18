#include <LibCore/CDirIterator.h>
#include <LibGUI/GFontDatabase.h>
#include <LibDraw/Font.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>

static GFontDatabase* s_the;

GFontDatabase& GFontDatabase::the()
{
    if (!s_the)
        s_the = new GFontDatabase;
    return *s_the;
}

GFontDatabase::GFontDatabase()
{
    CDirIterator di("/res/fonts", CDirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "CDirIterator: %s\n", di.error_string());
        exit(1);
    }
    while (di.has_next()) {
        String name = di.next_path();
        auto path = String::format("/res/fonts/%s", name.characters());
        if (auto font = Font::load_from_file(path)) {
            Metadata metadata;
            metadata.path = path;
            metadata.glyph_height = font->glyph_height();
            metadata.is_fixed_width = font->is_fixed_width();
            m_name_to_metadata.set(font->name(), move(metadata));
        }
    }
}

GFontDatabase::~GFontDatabase()
{
}

void GFontDatabase::for_each_font(Function<void(const StringView&)> callback)
{
    for (auto& it : m_name_to_metadata) {
        callback(it.key);
    }
}

void GFontDatabase::for_each_fixed_width_font(Function<void(const StringView&)> callback)
{
    for (auto& it : m_name_to_metadata) {
        if (it.value.is_fixed_width)
            callback(it.key);
    }
}

RefPtr<Font> GFontDatabase::get_by_name(const StringView& name)
{
    auto it = m_name_to_metadata.find(name);
    if (it == m_name_to_metadata.end())
        return nullptr;
    return Font::load_from_file((*it).value.path);
}
