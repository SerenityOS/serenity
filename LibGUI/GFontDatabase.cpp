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
        if (auto font = Font::load_from_file(path))
            m_name_to_path.set(font->name(), path);
    }
    closedir(dirp);
}

GFontDatabase::~GFontDatabase()
{
}

void GFontDatabase::for_each_font(Function<void(const String&)> callback)
{
    for (auto& it : m_name_to_path) {
        callback(it.key);
    }
}

RetainPtr<Font> GFontDatabase::get_by_name(const String& name)
{
    auto it = m_name_to_path.find(name);
    if (it == m_name_to_path.end())
        return nullptr;
    return Font::load_from_file((*it).value);
}
