#include <LibGUI/GFontDatabase.h>
#include <SharedGraphics/Font.h>
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
    DIR* dirp = opendir("/res/fonts");
    if (!dirp) {
        perror("opendir");
        exit(1);
    }
    while (auto* de = readdir(dirp)) {
        if (de->d_name[0] == '.')
            continue;
        auto path = String::format("/res/fonts/%s", de->d_name);
        if (auto font = Font::load_from_file(path)) {
            Metadata metadata;
            metadata.path = path;
            metadata.glyph_height = font->glyph_height();
            metadata.is_fixed_width = font->is_fixed_width();
            m_name_to_metadata.set(font->name(), move(metadata));
        }
    }
    closedir(dirp);
}

GFontDatabase::~GFontDatabase()
{
}

void GFontDatabase::for_each_font(Function<void(const String&)> callback)
{
    for (auto& it : m_name_to_metadata) {
        callback(it.key);
    }
}

void GFontDatabase::for_each_fixed_width_font(Function<void(const String&)> callback)
{
    for (auto& it : m_name_to_metadata) {
        if (it.value.is_fixed_width)
            callback(it.key);
    }
}

RetainPtr<Font> GFontDatabase::get_by_name(const String& name)
{
    auto it = m_name_to_metadata.find(name);
    if (it == m_name_to_metadata.end())
        return nullptr;
    return Font::load_from_file((*it).value.path);
}
