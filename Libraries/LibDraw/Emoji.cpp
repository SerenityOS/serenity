#include "Emoji.h"
#include "GraphicsBitmap.h"
#include <AK/AKString.h>
#include <AK/HashMap.h>

static HashMap<u32, Emoji> s_emojis;

Emoji::Emoji(NonnullRefPtr<GraphicsBitmap> bitmap)
    : m_bitmap(move(bitmap))
{
}

const Emoji* Emoji::emoji_for_codepoint(u32 codepoint)
{
    auto it = s_emojis.find(codepoint);
    if (it != s_emojis.end())
        return &(*it).value;

    String path = String::format("/res/emoji/U+%X.png", codepoint);

    auto bitmap = GraphicsBitmap::load_from_file(path);
    if (!bitmap)
        return nullptr;

    s_emojis.set(codepoint, Emoji { bitmap.release_nonnull() });
    return &(*s_emojis.find(codepoint)).value;
}
