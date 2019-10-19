#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibDraw/Emoji.h>
#include <LibDraw/GraphicsBitmap.h>

static HashMap<u32, RefPtr<GraphicsBitmap>> s_emojis;

const GraphicsBitmap* Emoji::emoji_for_codepoint(u32 codepoint)
{
    auto it = s_emojis.find(codepoint);
    if (it != s_emojis.end())
        return (*it).value.ptr();

    String path = String::format("/res/emoji/U+%X.png", codepoint);

    auto bitmap = GraphicsBitmap::load_from_file(path);
    if (!bitmap) {
        s_emojis.set(codepoint, nullptr);
        return nullptr;
    }

    s_emojis.set(codepoint, bitmap);
    return bitmap.ptr();
}
